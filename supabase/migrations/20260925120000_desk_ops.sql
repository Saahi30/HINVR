-- Desk ops: request inbox + phone on profiles so staff can call members.
-- Safe to re-run.

alter table public.profiles
  add column if not exists phone_e164 text not null default '';

create table if not exists public.desk_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  kind text not null check (kind in ('VISIT', 'CONCIERGE', 'POOJA', 'YATRA')),
  summary text not null,
  city text not null default '',
  mandir_id text references public.mandirs (id) on delete set null,
  status text not null default 'new' check (status in ('new', 'open', 'done')),
  staff_note text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists desk_requests_status_created
  on public.desk_requests (status, created_at desc);

create index if not exists desk_requests_user_created
  on public.desk_requests (user_id, created_at desc);

drop trigger if exists desk_requests_touch on public.desk_requests;
create trigger desk_requests_touch
  before update on public.desk_requests
  for each row execute function private.touch_updated_at();

alter table public.desk_requests enable row level security;

drop policy if exists desk_requests_select on public.desk_requests;
create policy desk_requests_select
  on public.desk_requests for select to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

drop policy if exists desk_requests_insert_own on public.desk_requests;
create policy desk_requests_insert_own
  on public.desk_requests for insert to authenticated
  with check (user_id = (select auth.uid()));

drop policy if exists desk_requests_staff_update on public.desk_requests;
create policy desk_requests_staff_update
  on public.desk_requests for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

grant select, insert, update on public.desk_requests to authenticated;
