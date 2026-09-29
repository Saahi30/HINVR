-- Quest headsets paired to a member account.
-- The phone shows a short-lived QR code; the headset scans it and proves itself
-- afterwards with its own device secret. A headset belongs to one account at a time.

create schema if not exists private;
grant usage on schema private to authenticated;

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.vr_devices (
  device_id uuid primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  secret_hash text not null,
  name text not null default 'Meta Quest',
  paired_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now()
);

create index if not exists vr_devices_user
  on public.vr_devices (user_id);

create table if not exists public.vr_pairing_codes (
  code_hash text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

create index if not exists vr_pairing_codes_user
  on public.vr_pairing_codes (user_id);

alter table public.vr_devices enable row level security;
alter table public.vr_pairing_codes enable row level security;

revoke all on public.vr_devices from anon, authenticated;
revoke all on public.vr_pairing_codes from anon, authenticated;
grant select (device_id, user_id, name, paired_at, last_seen_at) on public.vr_devices to authenticated;

drop policy if exists vr_devices_select_own on public.vr_devices;
create policy vr_devices_select_own
  on public.vr_devices for select to authenticated
  using (user_id = auth.uid());

create or replace function private.vr_hash(p_value text)
returns text
language sql
immutable
set search_path = public
as $$
  select encode(extensions.digest(convert_to(p_value, 'UTF8'), 'sha256'), 'hex');
$$;

create or replace function private.vr_profile_json(p_device public.vr_devices)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'display_name', p.display_name,
    'city', p.city,
    'tier', p.tier,
    'member_id', p.member_id,
    'valid_until', p.valid_until,
    'email', coalesce(u.email, ''),
    'device_name', p_device.name,
    'paired_at', p_device.paired_at
  )
  from public.profiles p
  left join auth.users u on u.id = p.id
  where p.id = p_device.user_id;
$$;

-- Phone: a fresh single-use code for the QR. Older codes for this member stop working.
create or replace function private.start_vr_pairing()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  code text;
  expires timestamptz := now() + interval '5 minutes';
begin
  if me is null then
    raise exception 'Sign in again.';
  end if;
  delete from public.vr_pairing_codes where user_id = me or expires_at < now();
  code := upper(encode(extensions.gen_random_bytes(16), 'hex'));
  insert into public.vr_pairing_codes (code_hash, user_id, expires_at)
  values (private.vr_hash(code), me, expires);
  return jsonb_build_object('code', code, 'expires_at', expires);
end;
$$;

create or replace function public.start_vr_pairing()
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
begin
  return private.start_vr_pairing();
end;
$$;

-- Phone: remove one of this member's headsets.
create or replace function private.unlink_vr_device(p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Sign in again.';
  end if;
  delete from public.vr_devices
  where device_id = p_device_id
    and user_id = auth.uid();
end;
$$;

create or replace function public.unlink_vr_device(p_device_id uuid)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  perform private.unlink_vr_device(p_device_id);
end;
$$;

-- Headset: trade a scanned code for a link to the member who showed it.
create or replace function private.claim_vr_pairing(
  p_code text,
  p_device_id uuid,
  p_secret text,
  p_name text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  owner uuid;
  existing public.vr_devices;
  linked public.vr_devices;
  cleaned_name text := left(trim(coalesce(p_name, '')), 60);
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
  returning user_id into owner;

  if owner is null then
    raise exception 'That pairing code has expired. Show a new one on your phone.';
  end if;

  select * into existing from public.vr_devices where device_id = p_device_id;
  if found and existing.user_id <> owner then
    raise exception 'This headset is linked to another account. Unpair it from the headset profile first.';
  end if;

  insert into public.vr_devices (device_id, user_id, secret_hash, name, paired_at, last_seen_at)
  values (
    p_device_id,
    owner,
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

create or replace function public.claim_vr_pairing(
  p_code text,
  p_device_id uuid,
  p_secret text,
  p_name text
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
begin
  return private.claim_vr_pairing(p_code, p_device_id, p_secret, p_name);
end;
$$;

-- Headset: who am I linked to? Null once the link is gone.
create or replace function private.vr_device_profile(p_device_id uuid, p_secret text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  linked public.vr_devices;
begin
  update public.vr_devices
  set last_seen_at = now()
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''))
  returning * into linked;
  if not found then
    return null;
  end if;
  return private.vr_profile_json(linked);
end;
$$;

create or replace function public.vr_device_profile(p_device_id uuid, p_secret text)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
begin
  return private.vr_device_profile(p_device_id, p_secret);
end;
$$;

-- Headset: drop its own link.
create or replace function private.unpair_vr_device(p_device_id uuid, p_secret text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.vr_devices
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''));
end;
$$;

create or replace function public.unpair_vr_device(p_device_id uuid, p_secret text)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  perform private.unpair_vr_device(p_device_id, p_secret);
end;
$$;

revoke all on function private.vr_hash(text) from public;
revoke all on function private.vr_profile_json(public.vr_devices) from public;
revoke all on function private.start_vr_pairing() from public;
revoke all on function private.unlink_vr_device(uuid) from public;
revoke all on function private.claim_vr_pairing(text, uuid, text, text) from public;
revoke all on function private.vr_device_profile(uuid, text) from public;
revoke all on function private.unpair_vr_device(uuid, text) from public;
revoke all on function public.start_vr_pairing() from public;
revoke all on function public.unlink_vr_device(uuid) from public;
revoke all on function public.claim_vr_pairing(text, uuid, text, text) from public;
revoke all on function public.vr_device_profile(uuid, text) from public;
revoke all on function public.unpair_vr_device(uuid, text) from public;

-- The headset has no member session, so it calls these with the publishable key.
grant usage on schema private to anon;

grant execute on function private.start_vr_pairing() to authenticated;
grant execute on function private.unlink_vr_device(uuid) to authenticated;
grant execute on function public.start_vr_pairing() to authenticated;
grant execute on function public.unlink_vr_device(uuid) to authenticated;

grant execute on function private.claim_vr_pairing(text, uuid, text, text) to anon, authenticated;
grant execute on function private.vr_device_profile(uuid, text) to anon, authenticated;
grant execute on function private.unpair_vr_device(uuid, text) to anon, authenticated;
grant execute on function public.claim_vr_pairing(text, uuid, text, text) to anon, authenticated;
grant execute on function public.vr_device_profile(uuid, text) to anon, authenticated;
grant execute on function public.unpair_vr_device(uuid, text) to anon, authenticated;
