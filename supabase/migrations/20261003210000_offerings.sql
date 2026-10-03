-- Symbolic offerings from Quest headsets. Authenticated with the device secret,
-- same as vr_device_profile. Nothing here is a temple booking.

create table if not exists public.offerings (
  id uuid primary key default gen_random_uuid(),
  device_id uuid not null references public.vr_devices (device_id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  kind text not null,
  mandir_id text,
  sankalp text,
  created_at timestamptz not null default now()
);

create index if not exists offerings_created on public.offerings (created_at desc);
create index if not exists offerings_kind_day on public.offerings (kind, created_at);

alter table public.offerings enable row level security;
revoke all on public.offerings from anon, authenticated;

drop policy if exists offerings_select_own on public.offerings;
create policy offerings_select_own
  on public.offerings for select to authenticated
  using (user_id = auth.uid());

grant select on public.offerings to authenticated;

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
set search_path = public
as $$
declare
  linked public.vr_devices;
  allowed text[] := array['ghanta','diya','aarti','pushp','agarbatti','prasad','tilak','daan'];
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
  insert into public.offerings (device_id, user_id, kind, mandir_id, sankalp)
  values (linked.device_id, linked.user_id, p_kind, nullif(p_mandir_id, ''), nullif(p_sankalp, ''));
  return jsonb_build_object('ok', true, 'kind', p_kind);
end;
$$;

create or replace function public.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
begin
  return private.record_offering(p_device_id, p_secret, p_kind, p_mandir_id, p_sankalp);
end;
$$;

create or replace function private.offering_counts_today()
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(
    jsonb_object_agg(kind, n),
    '{}'::jsonb
  )
  from (
    select kind, count(*)::int as n
    from public.offerings
    where created_at >= (timezone('utc', now()))::date
    group by kind
  ) counts;
$$;

create or replace function public.offering_counts_today()
returns jsonb
language sql
stable
security invoker
set search_path = public
as $$
  select private.offering_counts_today();
$$;

create or replace view public.offering_counts_today_view as
  select kind, count(*)::int as n
  from public.offerings
  where created_at >= (timezone('utc', now()))::date
  group by kind;

revoke all on public.offering_counts_today_view from anon;
grant select on public.offering_counts_today_view to authenticated;

-- Later hook for paid temple seva. Kept disabled until a mandir is contracted.
create table if not exists public.seva_catalog (
  id text primary key,
  title text not null,
  mandir_id text,
  price_paise integer,
  enabled boolean not null default false
);

alter table public.seva_catalog enable row level security;
revoke all on public.seva_catalog from anon, authenticated;
grant select on public.seva_catalog to authenticated, anon;

drop policy if exists seva_catalog_public_read on public.seva_catalog;
create policy seva_catalog_public_read
  on public.seva_catalog for select to anon, authenticated
  using (enabled = true);

insert into public.seva_catalog (id, title, mandir_id, enabled)
values ('placeholder-seva', 'Temple seva (not offered yet)', null, false)
on conflict (id) do nothing;

revoke all on function private.record_offering(uuid, text, text, text, text) from public;
revoke all on function private.offering_counts_today() from public;
revoke all on function public.record_offering(uuid, text, text, text, text) from public;
revoke all on function public.offering_counts_today() from public;

grant execute on function private.record_offering(uuid, text, text, text, text) to anon, authenticated;
grant execute on function private.offering_counts_today() to anon, authenticated;
grant execute on function public.record_offering(uuid, text, text, text, text) to anon, authenticated;
grant execute on function public.offering_counts_today() to anon, authenticated;
