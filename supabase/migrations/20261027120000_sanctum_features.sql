-- Sanctum features: japa, abhishek and deepmala offerings, offerings dedicated to
-- someone, aarti times per mandir, and a festival calendar the headset reads.
-- Nothing here is a temple booking. Safe to re-run.

-- Offerings ---------------------------------------------------------------------

alter table public.offerings add column if not exists dedicated_to text;

alter table public.offerings drop constraint if exists offerings_kind_check;
alter table public.offerings
  add constraint offerings_kind_check
  check (kind in (
    'ghanta', 'diya', 'aarti', 'pushp', 'agarbatti', 'prasad', 'tilak', 'daan',
    'japa', 'abhishek', 'deepmala'
  )) not valid;

alter table public.offerings drop constraint if exists offerings_dedicated_to_check;
alter table public.offerings
  add constraint offerings_dedicated_to_check
  check (dedicated_to is null or dedicated_to in (
    'self', 'family', 'parents', 'children', 'ancestors', 'friend', 'teacher'
  ));

-- A sixth argument can't be added in place: drop the five-argument pair first. The
-- default keeps headsets that still send five arguments working.
drop function if exists public.record_offering(uuid, text, text, text, text);
drop function if exists private.record_offering(uuid, text, text, text, text);

create or replace function private.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null,
  p_dedicated_to text default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  linked public.vr_devices;
  allowed constant text[] := array[
    'ghanta','diya','aarti','pushp','agarbatti','prasad','tilak','daan','japa','abhishek','deepmala'
  ];
  dedications constant text[] := array['self','family','parents','children','ancestors','friend','teacher'];
  v_mandir text := nullif(btrim(coalesce(p_mandir_id, '')), '');
  v_sankalp text := nullif(left(btrim(coalesce(p_sankalp, '')), 500), '');
  v_for text := lower(nullif(btrim(coalesce(p_dedicated_to, '')), ''));
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
  -- An unknown dedication is dropped, not refused: the offering still counts.
  if v_for is not null and v_for <> all (dedications) then
    v_for := null;
  end if;

  insert into public.offerings (device_id, user_id, kind, mandir_id, sankalp, dedicated_to)
  values (linked.device_id, linked.user_id, p_kind, v_mandir, v_sankalp, v_for);
  return jsonb_build_object('ok', true, 'kind', p_kind);
end;
$$;

create or replace function public.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null,
  p_dedicated_to text default null
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
begin
  return private.record_offering(p_device_id, p_secret, p_kind, p_mandir_id, p_sankalp, p_dedicated_to);
end;
$$;

revoke all on function private.record_offering(uuid, text, text, text, text, text) from public;
revoke all on function public.record_offering(uuid, text, text, text, text, text) from public;
grant execute on function private.record_offering(uuid, text, text, text, text, text) to anon, authenticated;
grant execute on function public.record_offering(uuid, text, text, text, text, text) to anon, authenticated;

-- Aarti times ---------------------------------------------------------------------

-- Start times in India time, 'HH:MM', entered by staff from the temple's own schedule.
alter table public.mandirs add column if not exists aarti_times text[];

alter table public.mandirs drop constraint if exists mandirs_aarti_times_check;
alter table public.mandirs
  add constraint mandirs_aarti_times_check
  check (
    aarti_times is null
    or (
      cardinality(aarti_times) <= 12
      and array_position(aarti_times, null) is null
      and (
        cardinality(aarti_times) = 0
        or array_to_string(aarti_times, ',')
          ~ '^([01][0-9]|2[0-3]):[0-5][0-9](,([01][0-9]|2[0-3]):[0-5][0-9])*$'
      )
    )
  );

-- Festivals ---------------------------------------------------------------------

create table if not exists public.festivals (
  id text primary key,
  kind text not null check (kind in ('deepmala', 'abhishek', 'modak', 'hibiscus')),
  name text not null check (char_length(name) between 1 and 80),
  name_hi text not null default '' check (char_length(name_hi) <= 80),
  starts_on date not null,
  ends_on date not null,
  enabled boolean not null default false,
  notes text not null default '' check (char_length(notes) <= 500),
  updated_at timestamptz not null default now(),
  constraint festivals_dates_check check (ends_on >= starts_on and ends_on - starts_on <= 15)
);

create index if not exists festivals_dates on public.festivals (starts_on, ends_on) where enabled;

alter table public.festivals enable row level security;
revoke all on public.festivals from anon, authenticated;
grant select on public.festivals to anon, authenticated;
grant insert, update, delete on public.festivals to authenticated;

drop policy if exists festivals_read on public.festivals;
create policy festivals_read
  on public.festivals for select to anon, authenticated
  using (enabled = true or (select private.is_staff()));

drop policy if exists festivals_staff_insert on public.festivals;
create policy festivals_staff_insert
  on public.festivals for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists festivals_staff_update on public.festivals;
create policy festivals_staff_update
  on public.festivals for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists festivals_staff_delete on public.festivals;
create policy festivals_staff_delete
  on public.festivals for delete to authenticated
  using ((select private.has_staff_role(array['owner', 'admin'])));

create or replace function private.touch_festival()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists festivals_touch on public.festivals;
create trigger festivals_touch
  before update on public.festivals
  for each row execute function private.touch_festival();

-- The festival on today's Indian date, if one is enabled. The headset calls this with
-- the publishable key, so it returns the festival and nothing else.
create or replace function private.active_festival()
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select jsonb_build_object(
    'id', f.id,
    'kind', f.kind,
    'name', f.name,
    'name_hi', f.name_hi,
    'starts_on', f.starts_on,
    'ends_on', f.ends_on
  )
  from public.festivals f
  where f.enabled
    and private.today_ist() between f.starts_on and f.ends_on
  order by f.starts_on desc, f.id
  limit 1;
$$;

create or replace function public.active_festival()
returns jsonb
language sql
stable
security invoker
set search_path = ''
as $$
  select private.active_festival();
$$;

revoke all on function private.touch_festival() from public;
revoke all on function private.active_festival() from public;
revoke all on function public.active_festival() from public;
grant execute on function private.active_festival() to anon, authenticated;
grant execute on function public.active_festival() to anon, authenticated;

-- Best-known next dates, all off until someone checks them against a panchang.
insert into public.festivals (id, kind, name, name_hi, starts_on, ends_on, enabled, notes)
values
  ('diwali-2026', 'deepmala', 'Diwali', 'दीपावली', '2026-11-08', '2026-11-09', false,
   'Verify the date against a panchang before enabling.'),
  ('dev-deepawali-2026', 'deepmala', 'Dev Deepawali', 'देव दीपावली', '2026-11-24', '2026-11-24', false,
   'Verify the date against a panchang before enabling.'),
  ('maha-shivratri-2027', 'abhishek', 'Maha Shivratri', 'महाशिवरात्रि', '2027-03-06', '2027-03-06', false,
   'Verify the date against a panchang before enabling.'),
  ('ganesh-chaturthi-2027', 'modak', 'Ganesh Chaturthi', 'गणेश चतुर्थी', '2027-09-04', '2027-09-04', false,
   'Verify the date against a panchang before enabling.'),
  ('sharad-navratri-2027', 'hibiscus', 'Sharad Navratri', 'शारदीय नवरात्रि', '2027-09-30', '2027-10-08', false,
   'Verify the date against a panchang before enabling.')
on conflict (id) do nothing;
