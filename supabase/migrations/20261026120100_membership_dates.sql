-- profiles.valid_until becomes a real date (single source of membership validity),
-- membership ends at 23:59:59 IST on that date, and member data gets shape checks.
-- Safe to re-run.

-- Parses every format the desk has written into the old free-text column:
--   2027-09-28 · 2027-09-28T… · 28/09/2027 · 28-09-2027 · 28.09.2027
--   28 Sep 2027 · 28 Sept 2027 · 28 September, 2027 · Sep 28, 2027 · Sep 2027 (end of month)
-- Returns null when it cannot tell.
create or replace function private.parse_membership_date(p_value text)
returns date
language plpgsql
immutable
set search_path = ''
as $$
declare
  text_value text := lower(btrim(coalesce(p_value, '')));
  parts text[];
  month_no int;
  months constant text[] := array['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
begin
  if text_value = '' then
    return null;
  end if;

  parts := regexp_match(text_value, '^(\d{4})-(\d{1,2})-(\d{1,2})');
  if parts is not null then
    return make_date(parts[1]::int, parts[2]::int, parts[3]::int);
  end if;

  parts := regexp_match(text_value, '^(\d{1,2})[-/. ](\d{1,2})[-/. ](\d{4})$');
  if parts is not null then
    return make_date(parts[3]::int, parts[2]::int, parts[1]::int);
  end if;

  parts := regexp_match(text_value, '^(\d{1,2})(?:st|nd|rd|th)?\s+([a-z]+)\.?,?\s+(\d{4})$');
  if parts is not null then
    month_no := array_position(months, left(parts[2], 3));
    if month_no is not null then
      return make_date(parts[3]::int, month_no, parts[1]::int);
    end if;
  end if;

  parts := regexp_match(text_value, '^([a-z]+)\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})$');
  if parts is not null then
    month_no := array_position(months, left(parts[1], 3));
    if month_no is not null then
      return make_date(parts[3]::int, month_no, parts[2]::int);
    end if;
  end if;

  parts := regexp_match(text_value, '^([a-z]+)\.?,?\s+(\d{4})$');
  if parts is not null then
    month_no := array_position(months, left(parts[1], 3));
    if month_no is not null then
      return (make_date(parts[2]::int, month_no, 1) + interval '1 month' - interval '1 day')::date;
    end if;
  end if;

  return null;
exception
  when others then
    -- make_date on 31 Feb and friends.
    return null;
end;
$$;

revoke all on function private.parse_membership_date(text) from public, anon, authenticated;

-- 23:59:59 IST on the given date, as an instant.
create or replace function private.membership_end(p_valid_until date)
returns timestamptz
language sql
immutable
set search_path = ''
as $$
  select case
    when p_valid_until is null then null
    else ((p_valid_until + 1)::timestamp at time zone 'Asia/Kolkata') - interval '1 second'
  end;
$$;

-- Today in India.
create or replace function private.today_ist()
returns date
language sql
stable
set search_path = ''
as $$
  select (now() at time zone 'Asia/Kolkata')::date;
$$;

revoke all on function private.membership_end(date) from public;
revoke all on function private.today_ist() from public;
grant execute on function private.membership_end(date) to anon, authenticated, service_role;
grant execute on function private.today_ist() to anon, authenticated, service_role;

-- Members cannot set their own plan. Owner, admin, and finance can. So can the database itself.
create or replace function private.keep_issued_membership()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null or private.has_staff_role(array['owner', 'admin', 'finance']) then
    return new;
  end if;
  if tg_op = 'UPDATE' then
    new.tier := old.tier;
    new.member_id := old.member_id;
    new.valid_until := old.valid_until;
    new.pass_generation := old.pass_generation;
  else
    new.tier := 'None';
    new.member_id := '';
    new.valid_until := null;
    new.pass_generation := 0;
  end if;
  return new;
end;
$$;

revoke all on function private.keep_issued_membership() from public, anon, authenticated;

-- A membership change retires every pass the member holds.
create or replace function private.bump_pass_on_membership_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not (auth.uid() is null or private.has_staff_role(array['owner', 'admin', 'finance'])) then
    return new;
  end if;
  if tg_op = 'UPDATE'
    and (
      new.tier is distinct from old.tier
      or new.member_id is distinct from old.member_id
      or new.valid_until is distinct from old.valid_until
    )
  then
    new.pass_generation := old.pass_generation + 1;
    update public.pass_credentials
      set revoked_at = now()
      where user_id = new.id and revoked_at is null;
  end if;
  return new;
end;
$$;

revoke all on function private.bump_pass_on_membership_change() from public, anon, authenticated;

do $$
declare
  r record;
  bad int := 0;
begin
  if (
    select data_type
    from information_schema.columns
    where table_schema = 'public' and table_name = 'profiles' and column_name = 'valid_until'
  ) = 'text' then
    for r in
      select id, valid_until
      from public.profiles
      where btrim(valid_until) <> ''
        and private.parse_membership_date(valid_until) is null
    loop
      bad := bad + 1;
      raise notice 'profiles.valid_until for % was "%" and could not be read as a date. It is now null.', r.id, r.valid_until;
    end loop;
    raise notice 'profiles.valid_until: % unparseable value(s) set to null.', bad;

    alter table public.profiles alter column valid_until drop default;
    alter table public.profiles alter column valid_until drop not null;
    alter table public.profiles
      alter column valid_until type date using private.parse_membership_date(valid_until);

    -- A paid member whose text date was unreadable: fall back to the latest invoice.
    for r in
      select p.id, latest.valid_until as invoice_until
      from public.profiles p
      cross join lateral (
        select mp.valid_until
        from public.membership_purchases mp
        where mp.user_id = p.id
        order by mp.purchased_on desc, mp.created_at desc
        limit 1
      ) latest
      where p.valid_until is null and p.tier <> 'None'
    loop
      raise notice 'profiles.valid_until for % filled from the latest invoice: %', r.id, r.invoice_until;
      update public.profiles set valid_until = r.invoice_until where id = r.id;
    end loop;

    for r in
      select p.id, p.valid_until, latest.valid_until as invoice_until
      from public.profiles p
      cross join lateral (
        select mp.valid_until
        from public.membership_purchases mp
        where mp.user_id = p.id
        order by mp.purchased_on desc, mp.created_at desc
        limit 1
      ) latest
      where p.valid_until is distinct from latest.valid_until
    loop
      raise notice 'profiles.valid_until for % is % but the latest invoice says %. Kept the profile date.', r.id, r.valid_until, r.invoice_until;
    end loop;
  end if;
end;
$$;

create index if not exists profiles_valid_until_idx
  on public.profiles (valid_until)
  where tier <> 'None';

-- Headset profile. valid_until stays a display string ("28 Sep 2027", or "") for the Quest.
create or replace function private.vr_profile_json(p_device public.vr_devices)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select jsonb_build_object(
    'display_name', p.display_name,
    'city', p.city,
    'tier', p.tier,
    'member_id', p.member_id,
    'valid_until', coalesce(to_char(p.valid_until, 'FMDD Mon YYYY'), ''),
    'email', coalesce(u.email, ''),
    'device_name', p_device.name,
    'paired_at', p_device.paired_at
  )
  from public.profiles p
  left join auth.users u on u.id = p.id
  where p.id = p_device.user_id;
$$;

revoke all on function private.vr_profile_json(public.vr_devices) from public;

-- Tier values: normalise casing, then constrain.
do $$
declare
  r record;
begin
  for r in
    select id, tier from public.profiles
    where tier not in ('None', 'Darshan', 'Gold', 'Platinum', 'Nri')
  loop
    raise notice 'profiles.tier for % was "%".', r.id, r.tier;
  end loop;
end;
$$;

update public.profiles
set tier = case lower(btrim(tier))
  when 'darshan' then 'Darshan'
  when 'gold' then 'Gold'
  when 'platinum' then 'Platinum'
  when 'nri' then 'Nri'
  else 'None'
end
where tier not in ('None', 'Darshan', 'Gold', 'Platinum', 'Nri');

alter table public.profiles drop constraint if exists profiles_tier_check;
alter table public.profiles
  add constraint profiles_tier_check check (tier in ('None', 'Darshan', 'Gold', 'Platinum', 'Nri'));

-- Phone: E.164 digits with an optional +, or empty. Supabase Auth stores phones without the +.
update public.profiles
set phone_e164 = regexp_replace(phone_e164, '[-\s().]', '', 'g')
where phone_e164 <> '' and phone_e164 ~ '[-\s().]';

do $$
declare
  r record;
begin
  for r in
    select id, phone_e164 from public.profiles
    where phone_e164 <> '' and phone_e164 !~ '^\+?[1-9][0-9]{6,14}$'
  loop
    raise notice 'profiles.phone_e164 for % was "%" and was cleared.', r.id, r.phone_e164;
  end loop;
end;
$$;

update public.profiles
set phone_e164 = ''
where phone_e164 <> '' and phone_e164 !~ '^\+?[1-9][0-9]{6,14}$';

alter table public.profiles drop constraint if exists profiles_phone_e164_check;
alter table public.profiles
  add constraint profiles_phone_e164_check
  check (phone_e164 = '' or phone_e164 ~ '^\+?[1-9][0-9]{6,14}$');

-- Places: always a small array.
update public.profiles
set addresses = '[]'::jsonb
where jsonb_typeof(addresses) is distinct from 'array';

alter table public.profiles drop constraint if exists profiles_addresses_check;
alter table public.profiles
  add constraint profiles_addresses_check
  check (
    jsonb_typeof(addresses) = 'array'
    and jsonb_array_length(addresses) <= 20
    and octet_length(addresses::text) <= 16000
  );

-- Text the member writes.
update public.profiles
set display_name = left(display_name, 120),
    city = left(city, 120),
    language_tag = left(language_tag, 16),
    audience = left(audience, 32)
where char_length(display_name) > 120
   or char_length(city) > 120
   or char_length(language_tag) > 16
   or char_length(audience) > 32;

alter table public.profiles drop constraint if exists profiles_text_lengths_check;
alter table public.profiles
  add constraint profiles_text_lengths_check
  check (
    char_length(display_name) <= 120
    and char_length(city) <= 120
    and char_length(language_tag) <= 16
    and char_length(audience) <= 32
  );

-- Invoices outlive the account (GST retention). Keep a snapshot of who it was for.
alter table public.membership_purchases
  add column if not exists member_name text not null default '',
  add column if not exists member_email text not null default '',
  add column if not exists currency text not null default 'INR';

update public.membership_purchases mp
set member_name = coalesce(p.display_name, ''),
    member_email = coalesce(u.email, '')
from public.profiles p
left join auth.users u on u.id = p.id
where p.id = mp.user_id
  and mp.member_name = ''
  and mp.member_email = '';

alter table public.membership_purchases alter column user_id drop not null;
alter table public.membership_purchases
  drop constraint if exists membership_purchases_user_id_fkey;
alter table public.membership_purchases
  add constraint membership_purchases_user_id_fkey
  foreign key (user_id) references public.profiles (id) on delete set null;

create or replace function private.snapshot_purchase_member()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.user_id is not null and (new.member_name = '' or new.member_email = '') then
    select
      case when new.member_name = '' then coalesce(p.display_name, '') else new.member_name end,
      case when new.member_email = '' then coalesce(u.email, '') else new.member_email end
    into new.member_name, new.member_email
    from public.profiles p
    left join auth.users u on u.id = p.id
    where p.id = new.user_id;
    new.member_name := coalesce(new.member_name, '');
    new.member_email := coalesce(new.member_email, '');
  end if;
  return new;
end;
$$;

revoke all on function private.snapshot_purchase_member() from public, anon, authenticated;

drop trigger if exists membership_purchases_snapshot on public.membership_purchases;
create trigger membership_purchases_snapshot
  before insert or update of user_id on public.membership_purchases
  for each row execute function private.snapshot_purchase_member();

do $$
begin
  if exists (select 1 from public.membership_purchases where valid_until < valid_from) then
    raise notice 'Some membership_purchases rows end before they start. The check applies to new rows only until they are fixed.';
    alter table public.membership_purchases drop constraint if exists membership_purchases_dates_check;
    alter table public.membership_purchases
      add constraint membership_purchases_dates_check check (valid_until >= valid_from) not valid;
  else
    alter table public.membership_purchases drop constraint if exists membership_purchases_dates_check;
    alter table public.membership_purchases
      add constraint membership_purchases_dates_check check (valid_until >= valid_from);
  end if;
end;
$$;
