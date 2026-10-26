-- Every staff policy checks a role, not just "is staff". Role checks are wrapped in
-- (select …) so Postgres evaluates them once per statement, not once per row.
--   membership   owner, admin, finance
--   member PII   owner, admin, finance, desk
--   catalog/CMS  owner, admin, editor
-- Safe to re-run.

-- Catalog ---------------------------------------------------------------------

drop policy if exists mandirs_read on public.mandirs;
create policy mandirs_read
  on public.mandirs for select to anon, authenticated
  using (published = true or (select private.is_staff()));

drop policy if exists mandirs_staff_write on public.mandirs;
create policy mandirs_staff_write
  on public.mandirs for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists mandirs_staff_update on public.mandirs;
create policy mandirs_staff_update
  on public.mandirs for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists mandirs_staff_delete on public.mandirs;
create policy mandirs_staff_delete
  on public.mandirs for delete to authenticated
  using ((select private.has_staff_role(array['owner', 'admin'])));

drop policy if exists home_services_read on public.home_services;
create policy home_services_read
  on public.home_services for select to anon, authenticated
  using (published = true or (select private.is_staff()));

drop policy if exists home_services_staff_insert on public.home_services;
create policy home_services_staff_insert
  on public.home_services for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists home_services_staff_update on public.home_services;
create policy home_services_staff_update
  on public.home_services for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists home_services_staff_delete on public.home_services;
create policy home_services_staff_delete
  on public.home_services for delete to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

-- app_settings: the phone reads "home". Operational keys stay with staff and are
-- written only by the service role (edge functions).
drop policy if exists app_settings_public_read on public.app_settings;
create policy app_settings_public_read
  on public.app_settings for select to anon, authenticated
  using (
    key not in ('youtube_quota', 'live_check_manual')
    or (select private.is_staff())
  );

drop policy if exists app_settings_staff_insert on public.app_settings;
create policy app_settings_staff_insert
  on public.app_settings for insert to authenticated
  with check (
    key not in ('youtube_quota', 'live_check_manual')
    and (select private.has_staff_role(array['owner', 'admin', 'editor']))
  );

drop policy if exists app_settings_staff_update on public.app_settings;
create policy app_settings_staff_update
  on public.app_settings for update to authenticated
  using (
    key not in ('youtube_quota', 'live_check_manual')
    and (select private.has_staff_role(array['owner', 'admin', 'editor']))
  )
  with check (
    key not in ('youtube_quota', 'live_check_manual')
    and (select private.has_staff_role(array['owner', 'admin', 'editor']))
  );

drop policy if exists live_sources_staff_select on public.live_sources;
create policy live_sources_staff_select
  on public.live_sources for select to authenticated
  using ((select private.is_staff()));

drop policy if exists live_sources_staff_insert on public.live_sources;
create policy live_sources_staff_insert
  on public.live_sources for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists live_sources_staff_update on public.live_sources;
create policy live_sources_staff_update
  on public.live_sources for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists live_sources_staff_delete on public.live_sources;
create policy live_sources_staff_delete
  on public.live_sources for delete to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists media_staff_insert on storage.objects;
create policy media_staff_insert
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'media' and (select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists media_staff_update on storage.objects;
create policy media_staff_update
  on storage.objects for update
  to authenticated
  using (bucket_id = 'media' and (select private.has_staff_role(array['owner', 'admin', 'editor'])))
  with check (bucket_id = 'media' and (select private.has_staff_role(array['owner', 'admin', 'editor'])));

drop policy if exists media_staff_delete on storage.objects;
create policy media_staff_delete
  on storage.objects for delete
  to authenticated
  using (bucket_id = 'media' and (select private.has_staff_role(array['owner', 'admin', 'editor'])));

-- Members ---------------------------------------------------------------------

drop policy if exists profiles_select on public.profiles;
create policy profiles_select
  on public.profiles for select to authenticated
  using (
    id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
  );

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own
  on public.profiles for insert to authenticated
  with check (id = (select auth.uid()));

drop policy if exists profiles_update on public.profiles;
create policy profiles_update
  on public.profiles for update to authenticated
  using (
    id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance']))
  )
  with check (
    id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance']))
  );

drop policy if exists membership_purchases_select on public.membership_purchases;
create policy membership_purchases_select
  on public.membership_purchases for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance']))
  );

drop policy if exists membership_purchases_insert on public.membership_purchases;
create policy membership_purchases_insert
  on public.membership_purchases for insert to authenticated
  with check ((select private.has_staff_role(array['owner', 'admin', 'finance'])));

drop policy if exists membership_purchases_update on public.membership_purchases;
create policy membership_purchases_update
  on public.membership_purchases for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'finance'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'finance'])));

drop policy if exists membership_requests_select on public.membership_requests;
create policy membership_requests_select
  on public.membership_requests for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance']))
  );

drop policy if exists membership_requests_insert_own on public.membership_requests;
create policy membership_requests_insert_own
  on public.membership_requests for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'pending');

drop policy if exists membership_requests_update on public.membership_requests;
create policy membership_requests_update
  on public.membership_requests for update to authenticated
  using (
    (select private.has_staff_role(array['owner', 'admin', 'finance']))
    or (user_id = (select auth.uid()) and status = 'pending')
  )
  with check (
    (select private.has_staff_role(array['owner', 'admin', 'finance']))
    or (user_id = (select auth.uid()) and status = 'pending')
  );

-- Desk ------------------------------------------------------------------------

drop policy if exists desk_requests_select on public.desk_requests;
create policy desk_requests_select
  on public.desk_requests for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
  );

drop policy if exists desk_requests_insert_own on public.desk_requests;
create policy desk_requests_insert_own
  on public.desk_requests for insert to authenticated
  with check (user_id = (select auth.uid()));

drop policy if exists desk_requests_staff_update on public.desk_requests;
create policy desk_requests_staff_update
  on public.desk_requests for update to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'finance', 'desk'])))
  with check ((select private.has_staff_role(array['owner', 'admin', 'finance', 'desk'])));

drop policy if exists physical_card_select on public.physical_card_requests;
create policy physical_card_select
  on public.physical_card_requests for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
  );

drop policy if exists physical_card_insert_own on public.physical_card_requests;
create policy physical_card_insert_own
  on public.physical_card_requests for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'waitlist');

drop policy if exists physical_card_update on public.physical_card_requests;
create policy physical_card_update
  on public.physical_card_requests for update to authenticated
  using (
    (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
    or (user_id = (select auth.uid()) and status = 'waitlist')
  )
  with check (
    (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
    or (user_id = (select auth.uid()) and status = 'cancelled')
  );

-- Members cannot set desk-only fields on their card request; the member ID comes from the profile.
create or replace function private.guard_physical_card()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.member_id := coalesce((select p.member_id from public.profiles p where p.id = new.user_id), '');
  end if;
  new.ship_name := left(btrim(coalesce(new.ship_name, '')), 120);
  new.ship_address := left(btrim(coalesce(new.ship_address, '')), 1000);
  if auth.uid() is null or private.has_staff_role(array['owner', 'admin', 'finance', 'desk']) then
    return new;
  end if;
  if tg_op = 'INSERT' then
    new.status := 'waitlist';
    new.user_id := auth.uid();
    new.member_id := coalesce((select p.member_id from public.profiles p where p.id = auth.uid()), '');
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

update public.physical_card_requests
set ship_name = left(ship_name, 120), ship_address = left(ship_address, 1000)
where char_length(ship_name) > 120 or char_length(ship_address) > 1000;

alter table public.physical_card_requests drop constraint if exists physical_card_text_lengths_check;
alter table public.physical_card_requests
  add constraint physical_card_text_lengths_check
  check (char_length(ship_name) <= 120 and char_length(ship_address) <= 1000);

-- Pass ------------------------------------------------------------------------

drop policy if exists pass_credentials_select on public.pass_credentials;
create policy pass_credentials_select
  on public.pass_credentials for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin']))
  );

drop policy if exists pass_checkins_select on public.pass_checkins;
create policy pass_checkins_select
  on public.pass_checkins for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
  );

-- Check-ins are written only by the desk-check-in edge function (service role),
-- after it has verified the signed pass on the server.
drop policy if exists pass_checkins_insert_staff on public.pass_checkins;
revoke insert, update, delete on public.pass_checkins from anon, authenticated;
grant select on public.pass_checkins to authenticated;

-- Misc own-row policies, re-created with the per-statement auth.uid().
drop policy if exists vr_devices_select_own on public.vr_devices;
create policy vr_devices_select_own
  on public.vr_devices for select to authenticated
  using (user_id = (select auth.uid()));

drop policy if exists offerings_select_own on public.offerings;
create policy offerings_select_own
  on public.offerings for select to authenticated
  using (user_id = (select auth.uid()));
