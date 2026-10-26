-- Staff roles: owner, admin, finance, desk, editor.
--   owner   everything, and the only role that manages staff
--   admin   everything except staff management
--   finance memberships, invoices, prices, audit
--   desk    scanner, desk requests, card waitlist (reads member contact details)
--   editor  catalog / CMS only
-- Safe to re-run.

create schema if not exists private;

alter table public.staff drop constraint if exists staff_role_check;
alter table public.staff
  add constraint staff_role_check check (role in ('owner', 'admin', 'finance', 'desk', 'editor'));

-- True when the signed-in user is staff with one of the given roles.
create or replace function private.has_staff_role(p_roles text[])
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.staff s
    where s.user_id = (select auth.uid())
      and s.role = any (p_roles)
  );
$$;

create or replace function public.has_staff_role(p_roles text[])
returns boolean
language sql
stable
security invoker
set search_path = ''
as $$
  select private.has_staff_role(p_roles);
$$;

-- The signed-in user's staff role, or null.
create or replace function public.my_staff_role()
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select s.role from public.staff s where s.user_id = (select auth.uid());
$$;

create or replace function private.is_owner()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.has_staff_role(array['owner']);
$$;

revoke all on function private.has_staff_role(text[]) from public;
revoke all on function public.has_staff_role(text[]) from public;
revoke all on function public.my_staff_role() from public;
-- Policies on anon-readable tables call these, so anon needs execute. auth.uid() is null for anon.
grant execute on function private.has_staff_role(text[]) to anon, authenticated;
grant execute on function public.has_staff_role(text[]) to anon, authenticated;
grant execute on function public.my_staff_role() to authenticated;

-- Emails allowed to claim the first owner seat. Managed from the SQL editor only:
--   insert into private.bootstrap_owner_emails (email) values ('you@example.com');
create table if not exists private.bootstrap_owner_emails (
  email text primary key check (email = lower(trim(email)) and email <> ''),
  created_at timestamptz not null default now()
);

alter table private.bootstrap_owner_emails enable row level security;
revoke all on table private.bootstrap_owner_emails from public, anon, authenticated;

-- First owner. Only when staff is empty AND the caller's verified email is allowlisted.
-- Serialised with an advisory lock so two callers cannot both become owner.
create or replace function private.bootstrap_staff()
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  my_email text;
begin
  if me is null then
    raise exception 'Not signed in';
  end if;

  perform pg_advisory_xact_lock(hashtext('hinvr.bootstrap_staff'));

  if exists (select 1 from public.staff where user_id = me) then
    return true;
  end if;
  if exists (select 1 from public.staff) then
    return false;
  end if;

  select lower(trim(u.email)) into my_email
  from auth.users u
  where u.id = me
    and u.email_confirmed_at is not null;

  if my_email is null
    or not exists (select 1 from private.bootstrap_owner_emails b where b.email = my_email)
  then
    return false;
  end if;

  insert into public.staff (user_id, email, role)
  values (me, my_email, 'owner')
  on conflict (user_id) do nothing;
  return true;
end;
$$;

revoke all on function private.bootstrap_staff() from public;
grant execute on function private.bootstrap_staff() to authenticated;

-- Owner invites an existing Auth user with a role. Replaces the one-argument version.
drop function if exists public.invite_staff(text);
drop function if exists private.invite_staff(text);

create or replace function private.invite_staff(p_email text, p_role text default 'editor')
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  uid uuid;
  normalized text := lower(trim(coalesce(p_email, '')));
  wanted text := lower(trim(coalesce(p_role, 'editor')));
begin
  if not private.has_staff_role(array['owner']) then
    raise exception 'Only an owner can invite staff' using errcode = '42501';
  end if;
  if normalized = '' then
    raise exception 'Email is required';
  end if;
  if wanted not in ('owner', 'admin', 'finance', 'desk', 'editor') then
    raise exception 'Unknown role: %', wanted;
  end if;

  select id into uid
  from auth.users
  where lower(email) = normalized
  limit 1;

  if uid is null then
    raise exception 'No account with that email. Ask them to create an account on this admin first.';
  end if;

  insert into public.staff (user_id, email, role)
  values (uid, normalized, wanted)
  on conflict (user_id) do update
    set email = excluded.email,
        role = excluded.role;
end;
$$;

create or replace function public.invite_staff(p_email text, p_role text default 'editor')
returns void
language sql
security invoker
set search_path = ''
as $$
  select private.invite_staff(p_email, p_role);
$$;

revoke all on function private.invite_staff(text, text) from public;
revoke all on function public.invite_staff(text, text) from public;
grant execute on function private.invite_staff(text, text) to authenticated;
grant execute on function public.invite_staff(text, text) to authenticated;

-- Never leave the desk without an owner.
create or replace function private.keep_an_owner()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.role = 'owner'
    and (tg_op = 'DELETE' or new.role is distinct from 'owner')
    and not exists (
      select 1 from public.staff s where s.role = 'owner' and s.user_id <> old.user_id
    )
  then
    raise exception 'This is the last owner. Make someone else owner first.';
  end if;
  if tg_op = 'DELETE' then
    return old;
  end if;
  return new;
end;
$$;

revoke all on function private.keep_an_owner() from public, anon, authenticated;

drop trigger if exists staff_keep_an_owner on public.staff;
create trigger staff_keep_an_owner
  before update or delete on public.staff
  for each row execute function private.keep_an_owner();

-- Staff table policies: any staff can see the team, only owners change it.
drop policy if exists staff_select on public.staff;
create policy staff_select
  on public.staff for select to authenticated
  using ((select private.is_staff()));

drop policy if exists staff_owner_insert on public.staff;
create policy staff_owner_insert
  on public.staff for insert to authenticated
  with check ((select private.has_staff_role(array['owner'])));

drop policy if exists staff_owner_update on public.staff;
create policy staff_owner_update
  on public.staff for update to authenticated
  using ((select private.has_staff_role(array['owner'])))
  with check ((select private.has_staff_role(array['owner'])));

drop policy if exists staff_owner_delete on public.staff;
create policy staff_owner_delete
  on public.staff for delete to authenticated
  using ((select private.has_staff_role(array['owner'])));
