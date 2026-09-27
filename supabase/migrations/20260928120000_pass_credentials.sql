-- Signed, rotating pass credentials and the physical-card waitlist.
-- The private signing key never leaves the database. The desk verifies with the public key.

alter table public.profiles
  add column if not exists pass_generation integer not null default 0;

create or replace function private.keep_issued_membership()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if private.is_staff() or auth.uid() is null then
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
    new.valid_until := '';
    new.pass_generation := 0;
  end if;
  return new;
end;
$$;

revoke all on function private.keep_issued_membership() from public, anon, authenticated;

create table if not exists private.pass_signing_keys (
  kid text primary key,
  private_jwk jsonb not null,
  public_jwk jsonb not null,
  created_at timestamptz not null default now()
);

revoke all on table private.pass_signing_keys from public, anon, authenticated;

create table if not exists public.pass_signing_keys (
  kid text primary key,
  public_jwk jsonb not null,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

alter table public.pass_signing_keys enable row level security;

drop policy if exists pass_signing_keys_select on public.pass_signing_keys;
create policy pass_signing_keys_select
  on public.pass_signing_keys for select to authenticated
  using (active = true);

grant select on public.pass_signing_keys to authenticated;

create table if not exists public.pass_credentials (
  id uuid primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  generation integer not null,
  member_id text not null,
  tier text not null,
  purpose text not null check (purpose in ('desk', 'wallet')),
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists pass_credentials_user_purpose
  on public.pass_credentials (user_id, purpose, created_at desc);

alter table public.pass_credentials enable row level security;

drop policy if exists pass_credentials_select on public.pass_credentials;
create policy pass_credentials_select
  on public.pass_credentials for select to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

grant select on public.pass_credentials to authenticated;

alter table public.pass_checkins
  add column if not exists credential_id uuid references public.pass_credentials (id);

create or replace function private.bump_pass_on_membership_change()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not (private.is_staff() or auth.uid() is null) then
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

drop trigger if exists profiles_bump_pass_generation on public.profiles;
create trigger profiles_bump_pass_generation
  before update on public.profiles
  for each row execute function private.bump_pass_on_membership_change();

create table if not exists public.physical_card_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  member_id text not null default '',
  ship_name text not null default '',
  ship_address text not null default '',
  status text not null default 'waitlist' check (status in ('waitlist', 'printing', 'shipped', 'cancelled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists physical_card_one_open
  on public.physical_card_requests (user_id)
  where status in ('waitlist', 'printing');

create index if not exists physical_card_status_created
  on public.physical_card_requests (status, created_at desc);

drop trigger if exists physical_card_touch on public.physical_card_requests;
create trigger physical_card_touch
  before update on public.physical_card_requests
  for each row execute function private.touch_updated_at();

create or replace function private.guard_physical_card()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if private.is_staff() or auth.uid() is null then
    return new;
  end if;
  if tg_op = 'INSERT' then
    new.status := 'waitlist';
    new.user_id := auth.uid();
    return new;
  end if;
  if old.status <> 'waitlist' then
    raise exception 'This card request is already with the desk.';
  end if;
  new.user_id := old.user_id;
  new.member_id := old.member_id;
  new.ship_name := old.ship_name;
  new.ship_address := old.ship_address;
  if new.status <> 'cancelled' then
    new.status := old.status;
  end if;
  return new;
end;
$$;

revoke all on function private.guard_physical_card() from public, anon, authenticated;

drop trigger if exists physical_card_guard on public.physical_card_requests;
create trigger physical_card_guard
  before insert or update on public.physical_card_requests
  for each row execute function private.guard_physical_card();

alter table public.physical_card_requests enable row level security;

drop policy if exists physical_card_select on public.physical_card_requests;
create policy physical_card_select
  on public.physical_card_requests for select to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

drop policy if exists physical_card_insert_own on public.physical_card_requests;
create policy physical_card_insert_own
  on public.physical_card_requests for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'waitlist');

drop policy if exists physical_card_update on public.physical_card_requests;
create policy physical_card_update
  on public.physical_card_requests for update to authenticated
  using (
    private.is_staff()
    or (user_id = (select auth.uid()) and status = 'waitlist')
  )
  with check (
    private.is_staff()
    or (user_id = (select auth.uid()) and status = 'cancelled')
  );

grant select, insert, update on public.physical_card_requests to authenticated;

create or replace function public.ensure_pass_signing_key(p_private jsonb, p_public jsonb)
returns jsonb
language plpgsql
security definer
set search_path = private, public
as $$
declare
  existing_private jsonb;
  existing_public jsonb;
begin
  if coalesce(auth.role(), '') <> 'service_role' then
    raise exception 'forbidden';
  end if;
  if jsonb_exists(p_public, 'd') then
    raise exception 'refusing a public key that can sign';
  end if;
  select private_jwk, public_jwk
    into existing_private, existing_public
    from private.pass_signing_keys
    where kid = 'primary';
  if existing_private is not null then
    return jsonb_build_object('kid', 'primary', 'private_jwk', existing_private, 'public_jwk', existing_public);
  end if;
  insert into private.pass_signing_keys (kid, private_jwk, public_jwk)
  values ('primary', p_private, p_public)
  on conflict (kid) do nothing;
  select private_jwk, public_jwk
    into existing_private, existing_public
    from private.pass_signing_keys
    where kid = 'primary';
  insert into public.pass_signing_keys (kid, public_jwk, active)
  values ('primary', existing_public, true)
  on conflict (kid) do update
    set public_jwk = excluded.public_jwk, active = true;
  return jsonb_build_object('kid', 'primary', 'private_jwk', existing_private, 'public_jwk', existing_public);
end;
$$;

revoke all on function public.ensure_pass_signing_key(jsonb, jsonb) from public, anon, authenticated;
grant execute on function public.ensure_pass_signing_key(jsonb, jsonb) to service_role;

create or replace function public.rotate_member_pass(p_user_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  next_generation integer;
begin
  if coalesce(auth.role(), '') <> 'service_role' then
    raise exception 'forbidden';
  end if;
  update public.profiles
    set pass_generation = pass_generation + 1
    where id = p_user_id
    returning pass_generation into next_generation;
  if next_generation is null then
    raise exception 'no member';
  end if;
  update public.pass_credentials
    set revoked_at = now()
    where user_id = p_user_id and revoked_at is null;
  return next_generation;
end;
$$;

revoke all on function public.rotate_member_pass(uuid) from public, anon, authenticated;
grant execute on function public.rotate_member_pass(uuid) to service_role;
