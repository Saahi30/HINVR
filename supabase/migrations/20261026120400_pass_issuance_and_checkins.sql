-- Pass signing moves out of the database. The private key lives in the edge-function
-- secret PASS_SIGNING_KEYS; the database keeps public keys only, so desks can verify
-- tokens signed by the current key and by a recently rotated one.
-- private.pass_signing_keys is NOT dropped here: see supabase/manual/README.md.
-- Safe to re-run.

alter table public.pass_signing_keys
  add column if not exists verify_until timestamptz;

comment on table public.pass_signing_keys is
  'Public pass keys (no private material). active = the kid currently signing. A key verifies until verify_until (null = no end).';

drop policy if exists pass_signing_keys_select on public.pass_signing_keys;
create policy pass_signing_keys_select
  on public.pass_signing_keys for select to authenticated
  using (verify_until is null or verify_until > now());

create or replace function private.is_service_role()
returns boolean
language sql
stable
set search_path = ''
as $$
  select coalesce(auth.jwt() ->> 'role', '') = 'service_role';
$$;

revoke all on function private.is_service_role() from public, anon, authenticated;

-- Edge functions register the public half of the key they sign with.
-- Refuses private material, and refuses to swap the key behind an existing kid.
-- When a new kid becomes active, older keys keep verifying for two more days
-- (wallet tokens live 24 h).
create or replace function public.register_pass_public_key(p_kid text, p_public_jwk jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  existing jsonb;
begin
  if not private.is_service_role() then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  if p_kid is null or p_kid !~ '^[A-Za-z0-9_-]{1,64}$' then
    raise exception 'Bad key id';
  end if;
  if p_public_jwk is null
    or jsonb_typeof(p_public_jwk) <> 'object'
    or p_public_jwk ? 'd'
    or coalesce(p_public_jwk ->> 'kty', '') <> 'EC'
    or coalesce(p_public_jwk ->> 'crv', '') <> 'P-256'
    or coalesce(p_public_jwk ->> 'x', '') = ''
    or coalesce(p_public_jwk ->> 'y', '') = ''
  then
    raise exception 'Expected a public P-256 JWK';
  end if;

  select public_jwk into existing from public.pass_signing_keys where kid = p_kid;
  if existing is not null
    and (existing ->> 'x' is distinct from p_public_jwk ->> 'x' or existing ->> 'y' is distinct from p_public_jwk ->> 'y')
  then
    raise exception 'Key id % is already registered with a different key. Use a new kid.', p_kid;
  end if;

  insert into public.pass_signing_keys (kid, public_jwk, active, verify_until)
  values (p_kid, jsonb_build_object('kty', 'EC', 'crv', 'P-256', 'x', p_public_jwk ->> 'x', 'y', p_public_jwk ->> 'y'), true, null)
  on conflict (kid) do update
    set active = true,
        verify_until = null;

  update public.pass_signing_keys
  set active = false,
      verify_until = coalesce(verify_until, now() + interval '2 days')
  where kid <> p_kid and active;
end;
$$;

revoke all on function public.register_pass_public_key(text, jsonb) from public, anon, authenticated;
grant execute on function public.register_pass_public_key(text, jsonb) to service_role;

-- One transaction: validate the membership, rotate or cap older credentials, insert the new one.
-- Desk credentials: keep the newest 3 live per member, so a second phone does not knock the
-- first one out. Rotate retires everything and bumps the generation.
create or replace function public.issue_pass_credential(
  p_user_id uuid,
  p_purpose text,
  p_rotate boolean default false,
  p_ttl_seconds integer default 21600
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  prof public.profiles;
  ends_at timestamptz;
  expires timestamptz;
  next_generation integer;
  keep_live integer;
  credential uuid := gen_random_uuid();
begin
  if not private.is_service_role() then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  if p_purpose not in ('desk', 'wallet') then
    raise exception 'Unknown pass purpose';
  end if;
  if p_ttl_seconds is null or p_ttl_seconds < 300 or p_ttl_seconds > 86400 then
    raise exception 'Bad pass lifetime';
  end if;

  select * into prof from public.profiles where id = p_user_id for update;
  if not found then
    raise exception 'Couldn''t open this membership.' using hint = 'no_member';
  end if;
  if prof.tier not in ('Gold', 'Platinum', 'Nri') then
    raise exception 'Gold, Platinum, or NRI opens the pass.' using hint = 'pass_tier';
  end if;
  if btrim(prof.member_id) = '' then
    raise exception 'The desk has not issued a member ID yet.' using hint = 'member_id';
  end if;
  if prof.valid_until is null then
    raise exception 'The desk has not set a validity date.' using hint = 'validity';
  end if;

  ends_at := private.membership_end(prof.valid_until);
  expires := least(now() + make_interval(secs => p_ttl_seconds), ends_at);
  if expires <= now() + interval '60 seconds' then
    raise exception 'This membership has ended.' using hint = 'ended';
  end if;

  next_generation := prof.pass_generation;
  if p_rotate then
    next_generation := next_generation + 1;
    update public.profiles set pass_generation = next_generation where id = p_user_id;
    update public.pass_credentials
      set revoked_at = now()
      where user_id = p_user_id and revoked_at is null;
  else
    keep_live := case when p_purpose = 'desk' then 3 else 2 end;
    update public.pass_credentials
      set revoked_at = now()
      where id in (
        select c.id
        from public.pass_credentials c
        where c.user_id = p_user_id
          and c.purpose = p_purpose
          and c.revoked_at is null
          and c.expires_at > now()
        order by c.created_at desc
        offset keep_live - 1
      );
  end if;

  insert into public.pass_credentials (id, user_id, generation, member_id, tier, purpose, expires_at)
  values (credential, p_user_id, next_generation, btrim(prof.member_id), prof.tier, p_purpose, expires);

  return jsonb_build_object(
    'id', credential,
    'user_id', p_user_id,
    'generation', next_generation,
    'member_id', btrim(prof.member_id),
    'tier', prof.tier,
    'purpose', p_purpose,
    'expires_at', expires,
    'expires_unix', floor(extract(epoch from expires))::bigint,
    'membership_end_unix', floor(extract(epoch from ends_at))::bigint,
    'valid_until', prof.valid_until,
    'display_name', coalesce(nullif(btrim(prof.display_name), ''), 'Member')
  );
end;
$$;

revoke all on function public.issue_pass_credential(uuid, text, boolean, integer) from public, anon, authenticated;
grant execute on function public.issue_pass_credential(uuid, text, boolean, integer) to service_role;

-- Check-ins: how the desk confirmed the member, and anything odd about it.
alter table public.pass_checkins
  add column if not exists method text not null default 'manual',
  add column if not exists flag text not null default '',
  add column if not exists checked_in_by uuid references auth.users (id) on delete set null,
  add column if not exists scanned_at timestamptz;

update public.pass_checkins
set method = 'signed'
where credential_id is not null and method = 'manual';

alter table public.pass_checkins drop constraint if exists pass_checkins_method_check;
alter table public.pass_checkins
  add constraint pass_checkins_method_check check (method in ('signed', 'manual'));

alter table public.pass_checkins drop constraint if exists pass_checkins_flag_check;
alter table public.pass_checkins
  add constraint pass_checkins_flag_check check (flag in ('', 'duplicate', 'suspicious'));

update public.pass_checkins
set place = left(place, 200), note = left(note, 500)
where char_length(place) > 200 or char_length(note) > 500;

alter table public.pass_checkins drop constraint if exists pass_checkins_text_lengths_check;
alter table public.pass_checkins
  add constraint pass_checkins_text_lengths_check check (char_length(place) <= 200 and char_length(note) <= 500);

-- Purging old credentials must not take check-in history with it.
alter table public.pass_checkins drop constraint if exists pass_checkins_credential_id_fkey;
alter table public.pass_checkins
  add constraint pass_checkins_credential_id_fkey
  foreign key (credential_id) references public.pass_credentials (id) on delete set null;

create index if not exists pass_checkins_credential_created
  on public.pass_checkins (credential_id, created_at desc)
  where credential_id is not null;

create index if not exists pass_checkins_created
  on public.pass_checkins (created_at desc);

create index if not exists pass_credentials_expires
  on public.pass_credentials (expires_at);

-- Daily: forget credentials that expired more than 30 days ago.
create extension if not exists pg_cron;

select cron.unschedule(jobid)
from cron.job
where jobname = 'purge-pass-credentials';

select cron.schedule(
  'purge-pass-credentials',
  '17 21 * * *',
  $$delete from public.pass_credentials where expires_at < now() - interval '30 days'$$
);
