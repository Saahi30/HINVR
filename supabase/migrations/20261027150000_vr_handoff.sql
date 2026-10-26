-- "View in VR": the phone asks for something to open on the member's headset, and the
-- headset, which has no push, polls for it with its device secret. Safe to re-run.

create table if not exists public.vr_handoffs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  target text not null check (target in ('live', 'sanctum', 'tour')),
  mandir_id text references public.mandirs (id) on delete set null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default now() + interval '10 minutes',
  claimed_at timestamptz,
  claimed_device uuid references public.vr_devices (device_id) on delete set null
);

create index if not exists vr_handoffs_user_created on public.vr_handoffs (user_id, created_at desc);

alter table public.vr_handoffs enable row level security;
revoke all on public.vr_handoffs from anon, authenticated;
grant select on public.vr_handoffs to authenticated;

drop policy if exists vr_handoffs_select_own on public.vr_handoffs;
create policy vr_handoffs_select_own
  on public.vr_handoffs for select to authenticated
  using (user_id = (select auth.uid()));

-- The member's phone asks; only one request is open at a time.
create or replace function private.request_vr_handoff(p_target text, p_mandir_id text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  v_target text := lower(nullif(btrim(coalesce(p_target, '')), ''));
  v_mandir text := nullif(btrim(coalesce(p_mandir_id, '')), '');
  made public.vr_handoffs;
begin
  if me is null then
    raise exception 'Sign in again.';
  end if;
  if v_target is null or v_target not in ('live', 'sanctum', 'tour') then
    raise exception 'Nothing to open in VR.';
  end if;
  if not exists (select 1 from public.vr_devices d where d.user_id = me) then
    raise exception 'Pair a headset first.';
  end if;
  if (
    select count(*) from public.vr_handoffs h
    where h.user_id = me and h.created_at > now() - interval '10 minutes'
  ) >= 10 then
    raise exception 'Too many requests. Wait a minute.';
  end if;
  if v_mandir is not null and not exists (select 1 from public.mandirs m where m.id = v_mandir) then
    v_mandir := null;
  end if;

  update public.vr_handoffs
  set expires_at = now()
  where user_id = me and claimed_at is null and expires_at > now();

  insert into public.vr_handoffs (user_id, target, mandir_id)
  values (me, v_target, v_mandir)
  returning * into made;
  return jsonb_build_object('id', made.id, 'expires_at', made.expires_at);
end;
$$;

create or replace function public.request_vr_handoff(p_target text, p_mandir_id text default null)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
begin
  return private.request_vr_handoff(p_target, p_mandir_id);
end;
$$;

-- The headset's battery, as last reported. It rides on the claim call below, which the headset
-- makes anyway, and only when it changed: no extra requests, no extra table.
alter table public.vr_devices
  add column if not exists battery_pct smallint check (battery_pct between 0 and 100),
  add column if not exists charging boolean,
  add column if not exists status_at timestamptz;
grant select (battery_pct, charging, status_at) on public.vr_devices to authenticated;

-- Re-runnable: an earlier run may have made the two-argument claim.
drop function if exists public.vr_claim_handoff(uuid, text);
drop function if exists private.vr_claim_handoff(uuid, text);

-- The headset takes the newest open request of its member, once, and may report its battery.
create or replace function private.vr_claim_handoff(
  p_device_id uuid,
  p_secret text,
  p_battery integer default null,
  p_charging boolean default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  linked public.vr_devices;
  taken public.vr_handoffs;
begin
  select * into linked
  from public.vr_devices
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''));
  if not found then
    raise exception 'Pair this headset again.';
  end if;

  if p_battery is not null then
    update public.vr_devices d
    set battery_pct = least(greatest(p_battery, 0), 100), charging = p_charging, status_at = now()
    where d.device_id = linked.device_id;
  end if;

  update public.vr_handoffs h
  set claimed_at = now(), claimed_device = linked.device_id
  where h.id = (
    select o.id from public.vr_handoffs o
    where o.user_id = linked.user_id and o.claimed_at is null and o.expires_at > now()
    order by o.created_at desc
    limit 1
    for update skip locked
  )
    and h.claimed_at is null
  returning * into taken;

  if taken.id is null then
    return null;
  end if;
  return jsonb_build_object('id', taken.id, 'target', taken.target, 'mandir_id', taken.mandir_id);
end;
$$;

create or replace function public.vr_claim_handoff(
  p_device_id uuid,
  p_secret text,
  p_battery integer default null,
  p_charging boolean default null
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
begin
  return private.vr_claim_handoff(p_device_id, p_secret, p_battery, p_charging);
end;
$$;

-- The phone watches its request: taken by which headset, or run out.
create or replace function private.vr_handoff_status(p_id uuid)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select jsonb_build_object(
    'claimed_at', h.claimed_at,
    'device_name', d.name,
    'expired', h.claimed_at is null and h.expires_at <= now()
  )
  from public.vr_handoffs h
  left join public.vr_devices d on d.device_id = h.claimed_device
  where h.id = p_id and h.user_id = auth.uid();
$$;

create or replace function public.vr_handoff_status(p_id uuid)
returns jsonb
language sql
stable
security invoker
set search_path = ''
as $$
  select private.vr_handoff_status(p_id);
$$;

revoke all on function private.request_vr_handoff(text, text) from public;
revoke all on function public.request_vr_handoff(text, text) from public;
revoke all on function private.vr_claim_handoff(uuid, text, integer, boolean) from public;
revoke all on function public.vr_claim_handoff(uuid, text, integer, boolean) from public;
revoke all on function private.vr_handoff_status(uuid) from public;
revoke all on function public.vr_handoff_status(uuid) from public;

grant execute on function private.request_vr_handoff(text, text) to authenticated;
grant execute on function public.request_vr_handoff(text, text) to authenticated;
grant execute on function private.vr_claim_handoff(uuid, text, integer, boolean) to anon, authenticated;
grant execute on function public.vr_claim_handoff(uuid, text, integer, boolean) to anon, authenticated;
grant execute on function private.vr_handoff_status(uuid) to authenticated;
grant execute on function public.vr_handoff_status(uuid) to authenticated;
