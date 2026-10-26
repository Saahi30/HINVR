-- Input bounds, rate limits, and guards for member- and headset-writable data.
-- Safe to re-run.

-- Offerings ---------------------------------------------------------------------
-- record_offering stays callable with the publishable key: the Quest has no member
-- session and proves itself with its device secret instead.

update public.offerings
set mandir_id = null
where mandir_id is not null
  and not exists (select 1 from public.mandirs m where m.id = offerings.mandir_id);

update public.offerings
set sankalp = left(sankalp, 500)
where char_length(sankalp) > 500;

alter table public.offerings drop constraint if exists offerings_kind_check;
alter table public.offerings
  add constraint offerings_kind_check
  check (kind in ('ghanta', 'diya', 'aarti', 'pushp', 'agarbatti', 'prasad', 'tilak', 'daan')) not valid;

alter table public.offerings drop constraint if exists offerings_sankalp_check;
alter table public.offerings
  add constraint offerings_sankalp_check check (sankalp is null or char_length(sankalp) <= 500);

alter table public.offerings drop constraint if exists offerings_mandir_id_fkey;
alter table public.offerings
  add constraint offerings_mandir_id_fkey
  foreign key (mandir_id) references public.mandirs (id) on delete set null;

create index if not exists offerings_user_created on public.offerings (user_id, created_at desc);
create index if not exists offerings_device_created on public.offerings (device_id, created_at desc);

create or replace function private.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  linked public.vr_devices;
  allowed constant text[] := array['ghanta','diya','aarti','pushp','agarbatti','prasad','tilak','daan'];
  v_mandir text := nullif(btrim(coalesce(p_mandir_id, '')), '');
  v_sankalp text := nullif(left(btrim(coalesce(p_sankalp, '')), 500), '');
begin
  if p_kind is null or p_kind <> all (allowed) then
    raise exception 'Unknown offering.';
  end if;
  select * into linked
  from public.vr_devices
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''));
  if not found then
    raise exception 'Pair this headset again.';
  end if;

  if (
    select count(*) from public.offerings o
    where o.device_id = linked.device_id and o.created_at > now() - interval '1 minute'
  ) >= 20
  or (
    select count(*) from public.offerings o
    where o.user_id = linked.user_id and o.created_at > now() - interval '10 minutes'
  ) >= 60
  then
    raise exception 'Too many offerings at once. Wait a minute.';
  end if;

  if v_mandir is not null and not exists (select 1 from public.mandirs m where m.id = v_mandir) then
    v_mandir := null;
  end if;

  insert into public.offerings (device_id, user_id, kind, mandir_id, sankalp)
  values (linked.device_id, linked.user_id, p_kind, v_mandir, v_sankalp);
  return jsonb_build_object('ok', true, 'kind', p_kind);
end;
$$;

revoke all on function private.record_offering(uuid, text, text, text, text) from public;
grant execute on function private.record_offering(uuid, text, text, text, text) to anon, authenticated;

-- "Today" is the Indian day.
create or replace function private.offering_counts_today()
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(jsonb_object_agg(kind, n), '{}'::jsonb)
  from (
    select o.kind, count(*)::int as n
    from public.offerings o
    where o.created_at >= (private.today_ist()::timestamp at time zone 'Asia/Kolkata')
    group by o.kind
  ) counts;
$$;

revoke all on function private.offering_counts_today() from public;
grant execute on function private.offering_counts_today() to anon, authenticated;

-- The view now runs with the caller's rights (their own offerings only). Global counts
-- come from offering_counts_today(), which returns numbers and nothing else.
create or replace view public.offering_counts_today_view
with (security_invoker = true) as
  select o.kind, count(*)::int as n
  from public.offerings o
  where o.created_at >= (private.today_ist()::timestamp at time zone 'Asia/Kolkata')
  group by o.kind;

revoke all on public.offering_counts_today_view from anon;
grant select on public.offering_counts_today_view to authenticated;

-- Headsets: at most 5 per member. --------------------------------------------------

create or replace function private.claim_vr_pairing(
  p_code text,
  p_device_id uuid,
  p_secret text,
  p_name text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  owner_id uuid;
  existing public.vr_devices;
  linked public.vr_devices;
  cleaned_name text := left(btrim(coalesce(p_name, '')), 60);
begin
  if p_device_id is null or length(coalesce(p_secret, '')) < 32 or length(p_secret) > 256 then
    raise exception 'This headset could not be identified.';
  end if;
  if length(coalesce(p_code, '')) <> 32 then
    raise exception 'That QR code isn''t a HINVR pairing code.';
  end if;

  delete from public.vr_pairing_codes
  where code_hash = private.vr_hash(upper(p_code))
    and expires_at > now()
  returning user_id into owner_id;

  if owner_id is null then
    raise exception 'That pairing code has expired. Show a new one on your phone.';
  end if;

  select * into existing from public.vr_devices where device_id = p_device_id;
  if found and existing.user_id <> owner_id then
    raise exception 'This headset is linked to another account. Unpair it from the headset profile first.';
  end if;

  if not found and (select count(*) from public.vr_devices d where d.user_id = owner_id) >= 5 then
    raise exception 'This account already has 5 headsets. Remove one on your phone first.';
  end if;

  insert into public.vr_devices (device_id, user_id, secret_hash, name, paired_at, last_seen_at)
  values (
    p_device_id,
    owner_id,
    private.vr_hash(p_secret),
    coalesce(nullif(cleaned_name, ''), 'Meta Quest'),
    now(),
    now()
  )
  on conflict (device_id) do update
    set secret_hash = excluded.secret_hash,
        name = excluded.name,
        paired_at = now(),
        last_seen_at = now()
  returning * into linked;

  return private.vr_profile_json(linked);
end;
$$;

revoke all on function private.claim_vr_pairing(text, uuid, text, text) from public;
grant execute on function private.claim_vr_pairing(text, uuid, text, text) to anon, authenticated;

create index if not exists vr_pairing_codes_expires on public.vr_pairing_codes (expires_at);

-- Desk requests: members send, staff answer. ---------------------------------------

update public.desk_requests
set summary = left(summary, 4000), city = left(city, 120), staff_note = left(staff_note, 2000)
where char_length(summary) > 4000 or char_length(city) > 120 or char_length(staff_note) > 2000;

alter table public.desk_requests drop constraint if exists desk_requests_text_lengths_check;
alter table public.desk_requests
  add constraint desk_requests_text_lengths_check
  check (char_length(summary) <= 4000 and char_length(city) <= 120 and char_length(staff_note) <= 2000);

create or replace function private.guard_desk_request()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  new.summary := left(coalesce(new.summary, ''), 4000);
  new.city := left(coalesce(new.city, ''), 120);
  if new.mandir_id is not null and not exists (select 1 from public.mandirs m where m.id = new.mandir_id) then
    new.mandir_id := null;
  end if;
  if auth.uid() is null or private.has_staff_role(array['owner', 'admin', 'finance', 'desk']) then
    return new;
  end if;
  if tg_op = 'UPDATE' then
    raise exception 'Only the desk can change a request.';
  end if;
  new.user_id := auth.uid();
  new.status := 'new';
  new.staff_note := '';
  return new;
end;
$$;

revoke all on function private.guard_desk_request() from public, anon, authenticated;

drop trigger if exists desk_requests_guard on public.desk_requests;
create trigger desk_requests_guard
  before insert or update on public.desk_requests
  for each row execute function private.guard_desk_request();

create index if not exists desk_requests_mandir on public.desk_requests (mandir_id) where mandir_id is not null;

-- Device tokens: an FCM token belongs to whichever account last signed in on that phone.
-- Registering moves it: the old binding is deleted first, so a shared or resold phone
-- stops receiving the previous member's notices. A member keeps at most 10 tokens.
create or replace function private.register_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  cleaned text := btrim(coalesce(p_token, ''));
begin
  if me is null then
    raise exception 'Sign in again.';
  end if;
  if length(cleaned) < 20 or length(cleaned) > 512 then
    raise exception 'Bad token';
  end if;

  delete from public.device_tokens
  where token = cleaned and user_id <> me;

  insert into public.device_tokens (token, user_id, platform, updated_at)
  values (cleaned, me, 'android', now())
  on conflict (token) do update
    set updated_at = now();

  delete from public.device_tokens
  where user_id = me
    and token in (
      select t.token from public.device_tokens t
      where t.user_id = me
      order by t.updated_at desc
      offset 10
    );
end;
$$;

revoke all on function private.register_device_token(text) from public;
grant execute on function private.register_device_token(text) to authenticated;

create index if not exists notifications_created_by on public.notifications (created_by);

-- YouTube quota: one atomic increment per checker run. ------------------------------

create or replace function public.add_youtube_quota(p_day text, p_units integer, p_limit integer, p_last jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  saved jsonb;
begin
  if not private.is_service_role() then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  insert into public.app_settings (key, value)
  values (
    'youtube_quota',
    jsonb_build_object('day', p_day, 'used', greatest(p_units, 0), 'limit', p_limit, 'runs', 1, 'last', p_last)
  )
  on conflict (key) do update
    set value = case
      when public.app_settings.value ->> 'day' = p_day then jsonb_build_object(
        'day', p_day,
        'used', coalesce((public.app_settings.value ->> 'used')::int, 0) + greatest(p_units, 0),
        'limit', p_limit,
        'runs', coalesce((public.app_settings.value ->> 'runs')::int, 0) + 1,
        'last', p_last
      )
      else excluded.value
    end
  returning value into saved;
  return saved;
end;
$$;

revoke all on function public.add_youtube_quota(text, integer, integer, jsonb) from public, anon, authenticated;
grant execute on function public.add_youtube_quota(text, integer, integer, jsonb) to service_role;

-- Manual "Check feeds now": at most once every 10 minutes across the whole desk.
create or replace function public.claim_live_check_manual(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  claimed boolean;
begin
  if not private.is_service_role() then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  insert into public.app_settings (key, value)
  values ('live_check_manual', jsonb_build_object('at', now(), 'by', p_user_id))
  on conflict (key) do update
    set value = excluded.value
    where coalesce((public.app_settings.value ->> 'at')::timestamptz, '-infinity'::timestamptz)
      < now() - interval '10 minutes'
  returning true into claimed;
  return coalesce(claimed, false);
end;
$$;

revoke all on function public.claim_live_check_manual(uuid) from public, anon, authenticated;
grant execute on function public.claim_live_check_manual(uuid) to service_role;

-- Live sources: a search result from a channel that is not allowlisted waits for review
-- instead of going straight to the app.
alter table public.live_sources
  add column if not exists review_video_id text not null default '',
  add column if not exists review_channel_id text not null default '',
  add column if not exists review_channel_name text not null default '',
  add column if not exists review_title text not null default '',
  add column if not exists review_found_at timestamptz;

comment on column public.live_sources.review_video_id is
  'A live video the checker found by search from a channel that is not an official source for this mandir. Staff must approve it before the app shows it.';

-- Catalog URLs ----------------------------------------------------------------------
-- https only; live and VR links only on YouTube hosts. Checked when the value changes,
-- so an old row with a legacy value does not block the live checker.

create or replace function private.url_host(p_url text)
returns text
language sql
immutable
set search_path = ''
as $$
  select lower((regexp_match(btrim(p_url), '^https://([^/?#:@]+)(?::\d+)?(?:[/?#]|$)', 'i'))[1]);
$$;

create or replace function private.check_catalog_urls()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  media_hosts constant text[] := array[
    'youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be',
    'youtube-nocookie.com', 'www.youtube-nocookie.com'
  ];
begin
  if tg_table_name = 'mandirs' then
    if new.live_url <> '' and (tg_op = 'INSERT' or new.live_url is distinct from old.live_url)
      and coalesce(private.url_host(new.live_url), '') <> all (media_hosts)
    then
      raise exception 'Live stream URL must be an https YouTube link.';
    end if;
    if new.vr_url <> '' and (tg_op = 'INSERT' or new.vr_url is distinct from old.vr_url)
      and coalesce(private.url_host(new.vr_url), '') <> all (media_hosts)
    then
      raise exception 'VR URL must be an https YouTube link.';
    end if;
    if new.official_website <> '' and (tg_op = 'INSERT' or new.official_website is distinct from old.official_website)
      and private.url_host(new.official_website) is null
    then
      raise exception 'Official website must start with https://';
    end if;
  end if;
  if new.photo_url <> '' and (tg_op = 'INSERT' or new.photo_url is distinct from old.photo_url)
    and private.url_host(new.photo_url) is null
  then
    raise exception 'Photo URL must start with https://';
  end if;
  return new;
end;
$$;

revoke all on function private.url_host(text) from public;
revoke all on function private.check_catalog_urls() from public, anon, authenticated;
grant execute on function private.url_host(text) to anon, authenticated, service_role;

drop trigger if exists mandirs_check_urls on public.mandirs;
create trigger mandirs_check_urls
  before insert or update on public.mandirs
  for each row execute function private.check_catalog_urls();

drop trigger if exists home_services_check_urls on public.home_services;
create trigger home_services_check_urls
  before insert or update on public.home_services
  for each row execute function private.check_catalog_urls();

do $$
declare
  r record;
begin
  for r in
    select id, live_url, vr_url, official_website, photo_url
    from public.mandirs
    where (live_url <> '' and coalesce(private.url_host(live_url), '') not in
            ('youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be', 'youtube-nocookie.com', 'www.youtube-nocookie.com'))
       or (vr_url <> '' and coalesce(private.url_host(vr_url), '') not in
            ('youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be', 'youtube-nocookie.com', 'www.youtube-nocookie.com'))
       or (official_website <> '' and private.url_host(official_website) is null)
       or (photo_url <> '' and private.url_host(photo_url) is null)
  loop
    raise notice 'mandirs.% has a URL the new rules would refuse: live=% vr=% site=% photo=%',
      r.id, r.live_url, r.vr_url, r.official_website, r.photo_url;
  end loop;
end;
$$;
