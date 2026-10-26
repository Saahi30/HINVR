-- Prices the server trusts, and one atomic approve / decline for buy requests.
-- Safe to re-run.

create table if not exists public.membership_prices (
  tier text primary key check (tier in ('Darshan', 'Gold', 'Platinum', 'Nri')),
  amount_inr integer not null check (amount_inr >= 0),
  currency text not null default 'INR' check (currency ~ '^[A-Z]{3}$'),
  amount_minor integer not null check (amount_minor >= 0),
  active boolean not null default true,
  updated_at timestamptz not null default now()
);

drop trigger if exists membership_prices_touch on public.membership_prices;
create trigger membership_prices_touch
  before update on public.membership_prices
  for each row execute function private.touch_updated_at();

-- Same prices as membership/lib/plans.ts and the phone's PlansScreen. NRI is invoiced in INR.
insert into public.membership_prices (tier, amount_inr, currency, amount_minor, active)
values
  ('Darshan', 999, 'INR', 99900, true),
  ('Gold', 4999, 'INR', 499900, true),
  ('Platinum', 14999, 'INR', 1499900, true),
  ('Nri', 12499, 'INR', 1249900, true)
on conflict (tier) do nothing;

alter table public.membership_prices enable row level security;
revoke all on public.membership_prices from anon, authenticated;
grant select on public.membership_prices to anon, authenticated;
grant insert, update on public.membership_prices to authenticated;

drop policy if exists membership_prices_read on public.membership_prices;
create policy membership_prices_read
  on public.membership_prices for select to anon, authenticated
  using (true);

drop policy if exists membership_prices_finance_insert on public.membership_prices;
create policy membership_prices_finance_insert
  on public.membership_prices for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'finance'])));

drop policy if exists membership_prices_finance_update on public.membership_prices;
create policy membership_prices_finance_update
  on public.membership_prices for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'finance'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'finance'])));

-- The member never sets the amount. It always comes from the price table.
create or replace function private.price_membership_request()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  price integer;
begin
  if tg_op = 'UPDATE' and new.status <> 'pending' then
    new.amount_inr := old.amount_inr;
    return new;
  end if;
  select mp.amount_inr into price
  from public.membership_prices mp
  where mp.tier = new.tier and mp.active;
  if price is null then
    raise exception 'That plan is not on sale right now.';
  end if;
  new.amount_inr := price;
  return new;
end;
$$;

revoke all on function private.price_membership_request() from public, anon, authenticated;

drop trigger if exists membership_requests_price on public.membership_requests;
create trigger membership_requests_price
  before insert or update of tier, amount_inr, status on public.membership_requests
  for each row execute function private.price_membership_request();

-- Requests already waiting were priced by the phone. Re-price them from the table.
update public.membership_requests r
set amount_inr = p.amount_inr
from public.membership_prices p
where p.tier = r.tier
  and r.status = 'pending'
  and r.amount_inr <> p.amount_inr;

-- Members keep a request pending. Staff close requests only through the RPCs below.
create or replace function private.guard_membership_request()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    return new;
  end if;
  if private.has_staff_role(array['owner', 'admin', 'finance']) then
    if tg_op = 'UPDATE'
      and new.status is distinct from old.status
      and coalesce(current_setting('hinvr.membership_rpc', true), '') <> 'on'
    then
      raise exception 'Approve or decline with approve_membership_request / decline_membership_request.';
    end if;
    return new;
  end if;
  if tg_op = 'UPDATE' and old.status <> 'pending' then
    raise exception 'This request is already closed.';
  end if;
  new.status := 'pending';
  new.staff_note := '';
  if tg_op = 'UPDATE' then
    new.user_id := old.user_id;
  else
    new.user_id := auth.uid();
  end if;
  return new;
end;
$$;

revoke all on function private.guard_membership_request() from public, anon, authenticated;

alter table public.membership_requests drop constraint if exists membership_requests_staff_note_check;
update public.membership_requests set staff_note = left(staff_note, 1000) where char_length(staff_note) > 1000;
alter table public.membership_requests
  add constraint membership_requests_staff_note_check check (char_length(staff_note) <= 1000);

-- Gapless yearly serials: HNV-2026-0001, INV-2026-0001. A row lock per (prefix, year)
-- serialises callers; a rollback gives the number back, which a sequence would not.
create table if not exists private.serial_counters (
  prefix text not null,
  year int not null,
  last_value int not null default 0,
  primary key (prefix, year)
);

revoke all on table private.serial_counters from public, anon, authenticated;

create or replace function private.next_serial(p_prefix text, p_year int)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  counter int;
  seen int;
  pattern text := '^' || p_prefix || '-' || p_year::text || '-(\d+)$';
  candidate text;
begin
  if p_prefix not in ('HNV', 'INV') then
    raise exception 'Unknown serial %', p_prefix;
  end if;

  insert into private.serial_counters (prefix, year, last_value)
  values (p_prefix, p_year, 0)
  on conflict (prefix, year) do nothing;

  select last_value into counter
  from private.serial_counters
  where prefix = p_prefix and year = p_year
  for update;

  -- Continue after anything typed by hand (members table, older desk builds).
  if p_prefix = 'HNV' then
    select coalesce(max((regexp_match(member_id, pattern))[1]::int), 0) into seen
    from (
      select member_id from public.profiles
      union all
      select member_id from public.membership_purchases
    ) ids;
  else
    select coalesce(max((regexp_match(invoice_number, pattern))[1]::int), 0) into seen
    from public.membership_purchases;
  end if;

  counter := greatest(counter, seen);
  loop
    counter := counter + 1;
    candidate := p_prefix || '-' || p_year::text || '-' || lpad(counter::text, 4, '0');
    exit when p_prefix = 'HNV' and not exists (select 1 from public.profiles where member_id = candidate);
    exit when p_prefix = 'INV' and not exists (select 1 from public.membership_purchases where invoice_number = candidate);
  end loop;

  update private.serial_counters
  set last_value = counter
  where prefix = p_prefix and year = p_year;
  return candidate;
end;
$$;

revoke all on function private.next_serial(text, int) from public, anon, authenticated;

create or replace function private.approve_membership_request(
  p_request_id uuid,
  p_valid_from date default null,
  p_note text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  req public.membership_requests;
  prof public.profiles;
  start_on date := coalesce(p_valid_from, private.today_ist());
  end_on date;
  new_member_id text;
  invoice text;
  purchase_id uuid;
  note text := left(btrim(coalesce(p_note, '')), 1000);
begin
  if not private.has_staff_role(array['owner', 'admin', 'finance']) then
    raise exception 'Only owner, admin, or finance can approve memberships.' using errcode = '42501';
  end if;

  select * into req
  from public.membership_requests
  where id = p_request_id
  for update;
  if not found then
    raise exception 'That request no longer exists.';
  end if;
  if req.status <> 'pending' then
    raise exception 'This request is already %.', req.status;
  end if;

  select * into prof
  from public.profiles
  where id = req.user_id
  for update;
  if not found then
    raise exception 'That member no longer has an account.';
  end if;

  if start_on < private.today_ist() - 31 or start_on > private.today_ist() + 366 then
    raise exception 'Valid from must be within a month back or a year ahead.';
  end if;
  end_on := (start_on + interval '1 year')::date;

  new_member_id := nullif(btrim(prof.member_id), '');
  if new_member_id is null then
    new_member_id := private.next_serial('HNV', extract(year from start_on)::int);
  end if;
  invoice := private.next_serial('INV', extract(year from private.today_ist())::int);

  perform set_config('hinvr.membership_rpc', 'on', true);

  update public.profiles
  set tier = req.tier,
      member_id = new_member_id,
      valid_until = end_on
  where id = req.user_id;

  insert into public.membership_purchases (
    user_id, tier, member_id, amount_inr, currency, invoice_number,
    purchased_on, valid_from, valid_until
  ) values (
    req.user_id, req.tier, new_member_id, req.amount_inr, 'INR', invoice,
    private.today_ist(), start_on, end_on
  )
  returning id into purchase_id;

  update public.membership_requests
  set status = 'approved',
      staff_note = note
  where id = req.id;

  perform set_config('hinvr.membership_rpc', '', true);

  return jsonb_build_object(
    'request_id', req.id,
    'user_id', req.user_id,
    'tier', req.tier,
    'member_id', new_member_id,
    'invoice_number', invoice,
    'purchase_id', purchase_id,
    'amount_inr', req.amount_inr,
    'valid_from', start_on,
    'valid_until', end_on,
    'status', 'approved'
  );
end;
$$;

create or replace function private.decline_membership_request(p_request_id uuid, p_reason text default '')
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  req public.membership_requests;
  note text := left(btrim(coalesce(p_reason, '')), 1000);
begin
  if not private.has_staff_role(array['owner', 'admin', 'finance']) then
    raise exception 'Only owner, admin, or finance can decline memberships.' using errcode = '42501';
  end if;

  select * into req
  from public.membership_requests
  where id = p_request_id
  for update;
  if not found then
    raise exception 'That request no longer exists.';
  end if;
  if req.status <> 'pending' then
    raise exception 'This request is already %.', req.status;
  end if;

  perform set_config('hinvr.membership_rpc', 'on', true);
  update public.membership_requests
  set status = 'declined',
      staff_note = note
  where id = req.id;
  perform set_config('hinvr.membership_rpc', '', true);

  return jsonb_build_object('request_id', req.id, 'status', 'declined', 'staff_note', note);
end;
$$;

create or replace function public.approve_membership_request(
  p_request_id uuid,
  p_valid_from date default null,
  p_note text default ''
)
returns jsonb
language sql
security invoker
set search_path = ''
as $$
  select private.approve_membership_request(p_request_id, p_valid_from, p_note);
$$;

create or replace function public.decline_membership_request(p_request_id uuid, p_reason text default '')
returns jsonb
language sql
security invoker
set search_path = ''
as $$
  select private.decline_membership_request(p_request_id, p_reason);
$$;

revoke all on function private.approve_membership_request(uuid, date, text) from public;
revoke all on function private.decline_membership_request(uuid, text) from public;
revoke all on function public.approve_membership_request(uuid, date, text) from public;
revoke all on function public.decline_membership_request(uuid, text) from public;
grant execute on function private.approve_membership_request(uuid, date, text) to authenticated;
grant execute on function private.decline_membership_request(uuid, text) to authenticated;
grant execute on function public.approve_membership_request(uuid, date, text) to authenticated;
grant execute on function public.decline_membership_request(uuid, text) to authenticated;
