-- Buy requests the desk approves, and pass check-ins from the scanner.
-- Members cannot change their own plan. Staff issue tier, member id, and validity.

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
  else
    new.tier := 'None';
    new.member_id := '';
    new.valid_until := '';
  end if;
  return new;
end;
$$;

revoke all on function private.keep_issued_membership() from public, anon, authenticated;

drop trigger if exists profiles_keep_membership on public.profiles;
create trigger profiles_keep_membership
  before insert or update on public.profiles
  for each row execute function private.keep_issued_membership();

create unique index if not exists profiles_member_id_unique
  on public.profiles (member_id)
  where member_id <> '';

create table if not exists public.membership_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  tier text not null check (tier in ('Darshan', 'Gold', 'Platinum', 'Nri')),
  amount_inr integer not null check (amount_inr >= 0),
  status text not null default 'pending' check (status in ('pending', 'approved', 'declined')),
  staff_note text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists membership_requests_one_pending
  on public.membership_requests (user_id)
  where status = 'pending';

create index if not exists membership_requests_status_created
  on public.membership_requests (status, created_at desc);

drop trigger if exists membership_requests_touch on public.membership_requests;
create trigger membership_requests_touch
  before update on public.membership_requests
  for each row execute function private.touch_updated_at();

create or replace function private.guard_membership_request()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if private.is_staff() or auth.uid() is null then
    return new;
  end if;
  if tg_op = 'UPDATE' and old.status <> 'pending' then
    raise exception 'This request is already closed.';
  end if;
  new.status := 'pending';
  new.staff_note := '';
  if tg_op = 'UPDATE' then
    new.user_id := old.user_id;
  end if;
  return new;
end;
$$;

revoke all on function private.guard_membership_request() from public, anon, authenticated;

drop trigger if exists membership_requests_guard on public.membership_requests;
create trigger membership_requests_guard
  before insert or update on public.membership_requests
  for each row execute function private.guard_membership_request();

alter table public.membership_requests enable row level security;

drop policy if exists membership_requests_select on public.membership_requests;
create policy membership_requests_select
  on public.membership_requests for select to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

drop policy if exists membership_requests_insert_own on public.membership_requests;
create policy membership_requests_insert_own
  on public.membership_requests for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'pending');

drop policy if exists membership_requests_update on public.membership_requests;
create policy membership_requests_update
  on public.membership_requests for update to authenticated
  using (
    private.is_staff()
    or (user_id = (select auth.uid()) and status = 'pending')
  )
  with check (
    private.is_staff()
    or (user_id = (select auth.uid()) and status = 'pending')
  );

grant select, insert, update on public.membership_requests to authenticated;

create table if not exists public.pass_checkins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  member_id text not null default '',
  place text not null default '',
  note text not null default '',
  created_at timestamptz not null default now()
);

create index if not exists pass_checkins_user_created
  on public.pass_checkins (user_id, created_at desc);

alter table public.pass_checkins enable row level security;

drop policy if exists pass_checkins_select on public.pass_checkins;
create policy pass_checkins_select
  on public.pass_checkins for select to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

drop policy if exists pass_checkins_insert_staff on public.pass_checkins;
create policy pass_checkins_insert_staff
  on public.pass_checkins for insert to authenticated
  with check (private.is_staff());

grant select, insert on public.pass_checkins to authenticated;
