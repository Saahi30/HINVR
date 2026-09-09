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

grant execute on function private.is_staff() to anon;

drop policy if exists mandirs_public_read on public.mandirs;
drop policy if exists mandirs_staff_read on public.mandirs;
create policy mandirs_read
  on public.mandirs for select to anon, authenticated
  using (published = true or private.is_staff());

drop policy if exists home_services_public_read on public.home_services;
drop policy if exists home_services_staff_read on public.home_services;
create policy home_services_read
  on public.home_services for select to anon, authenticated
  using (published = true or private.is_staff());

drop policy if exists profiles_select_own on public.profiles;
drop policy if exists profiles_staff_select on public.profiles;
create policy profiles_select
  on public.profiles for select to authenticated
  using (id = (select auth.uid()) or private.is_staff());

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own
  on public.profiles for insert to authenticated
  with check (id = (select auth.uid()));

drop policy if exists profiles_update_own on public.profiles;
drop policy if exists profiles_staff_update on public.profiles;
create policy profiles_update
  on public.profiles for update to authenticated
  using (id = (select auth.uid()) or private.is_staff())
  with check (id = (select auth.uid()) or private.is_staff());
