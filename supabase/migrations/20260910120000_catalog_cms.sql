-- Catalog CMS for the HINVR Android app and the web desk.
-- Safe to re-run.

create schema if not exists private;

create or replace function private.touch_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table if not exists public.staff (
  user_id uuid primary key references auth.users (id) on delete cascade,
  email text not null default '',
  role text not null default 'editor' check (role in ('owner', 'editor')),
  created_at timestamptz not null default now()
);

create table if not exists public.mandirs (
  id text primary key,
  name text not null,
  place text not null default '',
  city text not null default '',
  scene text not null default '',
  photo_url text not null default '',
  live boolean not null default false,
  vr boolean not null default false,
  pass_accepted boolean not null default false,
  next_aarti text,
  timings text not null default '',
  updated_label text not null default '',
  live_url text not null default '',
  vr_url text not null default '',
  sort_order int not null default 0,
  published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists mandirs_published_sort
  on public.mandirs (published, sort_order);

create table if not exists public.home_services (
  id text primary key,
  title text not null,
  benefit text not null,
  photo_url text not null default '',
  route text not null,
  scene text not null default '',
  tall boolean not null default false,
  sort_order int not null default 0,
  published boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.app_settings (
  key text primary key,
  value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

drop trigger if exists mandirs_touch on public.mandirs;
create trigger mandirs_touch
  before update on public.mandirs
  for each row execute function private.touch_updated_at();

drop trigger if exists home_services_touch on public.home_services;
create trigger home_services_touch
  before update on public.home_services
  for each row execute function private.touch_updated_at();

drop trigger if exists app_settings_touch on public.app_settings;
create trigger app_settings_touch
  before update on public.app_settings
  for each row execute function private.touch_updated_at();

create or replace function private.is_staff()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.staff where user_id = auth.uid()
  );
$$;

create or replace function private.is_owner()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.staff
    where user_id = auth.uid()
      and role = 'owner'
  );
$$;

create or replace function private.bootstrap_staff()
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  n int;
begin
  if auth.uid() is null then
    raise exception 'Not signed in';
  end if;

  select count(*) into n from public.staff;
  if n = 0 then
    insert into public.staff (user_id, email, role)
    values (
      auth.uid(),
      coalesce(auth.jwt() ->> 'email', ''),
      'owner'
    );
    return true;
  end if;

  return exists (select 1 from public.staff where user_id = auth.uid());
end;
$$;

create or replace function public.bootstrap_staff()
returns boolean
language sql
security invoker
set search_path = public
as $$
  select private.bootstrap_staff();
$$;

revoke all on function private.is_staff() from public;
revoke all on function private.is_owner() from public;
revoke all on function private.bootstrap_staff() from public;
revoke all on function public.bootstrap_staff() from public;

grant usage on schema private to authenticated;
grant execute on function private.is_staff() to authenticated;
grant execute on function private.is_owner() to authenticated;
grant execute on function private.bootstrap_staff() to authenticated;
grant execute on function public.bootstrap_staff() to authenticated;

alter table public.staff enable row level security;
alter table public.mandirs enable row level security;
alter table public.home_services enable row level security;
alter table public.app_settings enable row level security;

drop policy if exists staff_select on public.staff;
create policy staff_select
  on public.staff for select to authenticated
  using (private.is_staff());

drop policy if exists staff_owner_insert on public.staff;
create policy staff_owner_insert
  on public.staff for insert to authenticated
  with check (private.is_owner());

drop policy if exists staff_owner_update on public.staff;
create policy staff_owner_update
  on public.staff for update to authenticated
  using (private.is_owner())
  with check (private.is_owner());

drop policy if exists staff_owner_delete on public.staff;
create policy staff_owner_delete
  on public.staff for delete to authenticated
  using (private.is_owner());

drop policy if exists mandirs_public_read on public.mandirs;
create policy mandirs_public_read
  on public.mandirs for select to anon, authenticated
  using (published = true);

drop policy if exists mandirs_staff_read on public.mandirs;
create policy mandirs_staff_read
  on public.mandirs for select to authenticated
  using (private.is_staff());

drop policy if exists mandirs_staff_write on public.mandirs;
create policy mandirs_staff_write
  on public.mandirs for insert to authenticated
  with check (private.is_staff());

drop policy if exists mandirs_staff_update on public.mandirs;
create policy mandirs_staff_update
  on public.mandirs for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

drop policy if exists mandirs_staff_delete on public.mandirs;
create policy mandirs_staff_delete
  on public.mandirs for delete to authenticated
  using (private.is_staff());

drop policy if exists home_services_public_read on public.home_services;
create policy home_services_public_read
  on public.home_services for select to anon, authenticated
  using (published = true);

drop policy if exists home_services_staff_read on public.home_services;
create policy home_services_staff_read
  on public.home_services for select to authenticated
  using (private.is_staff());

drop policy if exists home_services_staff_insert on public.home_services;
create policy home_services_staff_insert
  on public.home_services for insert to authenticated
  with check (private.is_staff());

drop policy if exists home_services_staff_update on public.home_services;
create policy home_services_staff_update
  on public.home_services for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

drop policy if exists home_services_staff_delete on public.home_services;
create policy home_services_staff_delete
  on public.home_services for delete to authenticated
  using (private.is_staff());

drop policy if exists app_settings_public_read on public.app_settings;
create policy app_settings_public_read
  on public.app_settings for select to anon, authenticated
  using (true);

drop policy if exists app_settings_staff_insert on public.app_settings;
create policy app_settings_staff_insert
  on public.app_settings for insert to authenticated
  with check (private.is_staff());

drop policy if exists app_settings_staff_update on public.app_settings;
create policy app_settings_staff_update
  on public.app_settings for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

drop policy if exists profiles_staff_select on public.profiles;
create policy profiles_staff_select
  on public.profiles for select to authenticated
  using (private.is_staff());

drop policy if exists profiles_staff_update on public.profiles;
create policy profiles_staff_update
  on public.profiles for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

grant select on public.mandirs, public.home_services, public.app_settings to anon, authenticated;
grant insert, update, delete on public.mandirs, public.home_services, public.app_settings to authenticated;
grant select, insert, update, delete on public.staff to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'media',
  'media',
  true,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
on conflict (id) do update
set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists media_public_read on storage.objects;
create policy media_public_read
  on storage.objects for select
  to public
  using (bucket_id = 'media');

drop policy if exists media_staff_insert on storage.objects;
create policy media_staff_insert
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'media' and private.is_staff());

drop policy if exists media_staff_update on storage.objects;
create policy media_staff_update
  on storage.objects for update
  to authenticated
  using (bucket_id = 'media' and private.is_staff())
  with check (bucket_id = 'media' and private.is_staff());

drop policy if exists media_staff_delete on storage.objects;
create policy media_staff_delete
  on storage.objects for delete
  to authenticated
  using (bucket_id = 'media' and private.is_staff());

insert into public.mandirs (
  id, name, place, city, scene, live, vr, pass_accepted, next_aarti,
  timings, updated_label, sort_order, published
) values
  (
    'tirupati', 'Sri Venkateswara', 'Tirupati, Andhra Pradesh', 'Tirupati', 'Tirupati',
    true, true, true, null,
    'Suprabhatam 2:30 AM · Tomala 3:30 AM · Ekantha 1:00 AM',
    'Updated 12 min ago', 10, true
  ),
  (
    'kashi', 'Kashi Vishwanath', 'Varanasi, Uttar Pradesh', 'Kashi', 'Kashi',
    false, true, true, '4:30 PM',
    'Mangala 3:00 AM · Saptarishi aarti 7:00 PM',
    'Updated 12 min ago', 20, true
  ),
  (
    'shirdi', 'Sai Baba', 'Shirdi, Maharashtra', 'Shirdi', 'Shirdi',
    true, false, true, null,
    'Kakad aarti 5:00 AM · Shej aarti 10:00 PM',
    'Updated 8 min ago', 30, true
  ),
  (
    'kedarnath', 'Kedarnath', 'Rudraprayag, Uttarakhand', 'Kedarnath', 'Kedarnath',
    false, true, false, null,
    'Opening aarti 4:00 AM · Closed in winter',
    'Updated 12 min ago', 40, true
  ),
  (
    'somnath', 'Somnath', 'Gir Somnath, Gujarat', 'Somnath', 'Somnath',
    false, false, true, null,
    'Mangala 6:00 AM · Aarti 7:00 PM',
    'Updated 12 min ago', 50, true
  )
on conflict (id) do nothing;

insert into public.home_services (
  id, title, benefit, route, scene, tall, sort_order, published
) values
  ('live', 'Live Darshan', 'Aarti, from the sabha', 'live', 'LiveAarti', true, 10, true),
  ('vr', 'VR Darshan', '360. Move your phone', 'vr', 'VrHall', false, 20, true),
  ('pass', 'Priority Pass', 'Show this at the desk', 'pass', 'PassDesk', false, 30, true),
  ('pooja', 'Book Pandit', 'One city. Verified', 'pooja', 'PanditDoor', false, 40, true),
  ('concierge', 'Concierge', 'Ask. Then a human', 'concierge', 'ConciergeDesk', false, 50, true),
  ('yatra', 'Yatra', 'Club desk, not a portal', 'yatra', 'YatraRoad', true, 60, true)
on conflict (id) do nothing;

insert into public.app_settings (key, value)
values (
  'home',
  jsonb_build_object(
    'headline', E'You see the aarti.\nThe internet sees a thumbnail.',
    'eyebrow', 'HINVR'
  )
)
on conflict (key) do nothing;
