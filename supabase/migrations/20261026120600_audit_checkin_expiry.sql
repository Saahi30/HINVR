-- Audit trail, the server-side check-in, and the nightly membership expiry.
-- Safe to re-run.

create table if not exists public.audit_log (
  id bigint generated always as identity primary key,
  at timestamptz not null default now(),
  actor uuid,
  actor_email text not null default '',
  action text not null,
  table_name text not null,
  row_id text not null default '',
  before jsonb,
  after jsonb
);

create index if not exists audit_log_at on public.audit_log (at desc);
create index if not exists audit_log_table_at on public.audit_log (table_name, at desc);
create index if not exists audit_log_row on public.audit_log (table_name, row_id);

alter table public.audit_log enable row level security;
revoke all on public.audit_log from anon, authenticated;
grant select on public.audit_log to authenticated;

drop policy if exists audit_log_select on public.audit_log;
create policy audit_log_select
  on public.audit_log for select to authenticated
  using ((select private.has_staff_role(array['owner', 'admin', 'finance'])));

create or replace function private.write_audit(
  p_action text,
  p_table text,
  p_row_id text,
  p_before jsonb,
  p_after jsonb,
  p_actor uuid default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  who uuid := coalesce(p_actor, auth.uid());
begin
  insert into public.audit_log (actor, actor_email, action, table_name, row_id, before, after)
  values (
    who,
    coalesce((select u.email from auth.users u where u.id = who), ''),
    p_action,
    p_table,
    coalesce(p_row_id, ''),
    p_before,
    p_after
  );
end;
$$;

revoke all on function private.write_audit(text, text, text, jsonb, jsonb, uuid) from public, anon, authenticated;

-- Trigger arguments: primary-key column, then the columns worth keeping (none = whole row).
create or replace function private.audit_row()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  pk text := coalesce(tg_argv[0], 'id');
  keep text[] := tg_argv[1:];
  before_row jsonb := case when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old) end;
  after_row jsonb := case when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new) end;
  label text := coalesce(nullif(current_setting('hinvr.audit_action', true), ''), lower(tg_op));
begin
  if array_length(keep, 1) is not null then
    before_row := (select jsonb_object_agg(k, before_row -> k) from unnest(keep) k where before_row ? k);
    after_row := (select jsonb_object_agg(k, after_row -> k) from unnest(keep) k where after_row ? k);
  end if;
  perform private.write_audit(
    label,
    tg_table_name,
    coalesce(after_row ->> pk, before_row ->> pk, ''),
    before_row,
    after_row
  );
  return null;
end;
$$;

revoke all on function private.audit_row() from public, anon, authenticated;

drop trigger if exists profiles_audit_membership on public.profiles;
create trigger profiles_audit_membership
  after update on public.profiles
  for each row
  when (
    old.tier is distinct from new.tier
    or old.member_id is distinct from new.member_id
    or old.valid_until is distinct from new.valid_until
    or old.pass_generation is distinct from new.pass_generation
  )
  execute function private.audit_row('id', 'id', 'tier', 'member_id', 'valid_until', 'pass_generation');

drop trigger if exists membership_purchases_audit on public.membership_purchases;
create trigger membership_purchases_audit
  after insert or update or delete on public.membership_purchases
  for each row execute function private.audit_row('id');

drop trigger if exists membership_requests_audit on public.membership_requests;
create trigger membership_requests_audit
  after update on public.membership_requests
  for each row
  when (old.status is distinct from new.status)
  execute function private.audit_row('id', 'id', 'user_id', 'tier', 'amount_inr', 'status', 'staff_note');

drop trigger if exists staff_audit on public.staff;
create trigger staff_audit
  after insert or update or delete on public.staff
  for each row execute function private.audit_row('user_id');

drop trigger if exists mandirs_audit_delete on public.mandirs;
create trigger mandirs_audit_delete
  after delete on public.mandirs
  for each row execute function private.audit_row('id', 'id', 'name', 'city', 'published');

drop trigger if exists pass_checkins_audit on public.pass_checkins;
create trigger pass_checkins_audit
  after insert on public.pass_checkins
  for each row execute function private.audit_row('id');

drop trigger if exists membership_prices_audit on public.membership_prices;
create trigger membership_prices_audit
  after insert or update or delete on public.membership_prices
  for each row execute function private.audit_row('tier');

-- Server-side check-in -----------------------------------------------------------
-- Called by the desk-check-in edge function after it has verified the token signature.
-- Validates the credential and membership, applies the replay rules, and inserts, all
-- under one per-member lock:
--   same credential within 30 min (same place)  -> "duplicate"   (records only with confirm)
--   same credential at another place within 2 h -> "suspicious"  (records only with confirm, audited)
--   manual member-ID lookups always need confirm (the desk checks photo ID first)
create or replace function public.desk_check_in(
  p_staff_id uuid,
  p_method text,
  p_user_id uuid,
  p_member_id text,
  p_credential_id uuid,
  p_generation integer,
  p_place text,
  p_note text default '',
  p_confirm boolean default false,
  p_scanned_at timestamptz default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  staff_role text;
  cred public.pass_credentials;
  prof public.profiles;
  seen_at timestamptz := coalesce(p_scanned_at, now());
  where_at text := left(btrim(coalesce(p_place, '')), 200);
  note_text text := left(btrim(coalesce(p_note, '')), 500);
  wanted_id text := upper(btrim(coalesce(p_member_id, '')));
  member_json jsonb;
  prev_at timestamptz;
  prev_place text;
  flag text := '';
  saved_id uuid;
  saved_at timestamptz;
begin
  if not private.is_service_role() then
    raise exception 'forbidden' using errcode = '42501';
  end if;

  select s.role into staff_role from public.staff s where s.user_id = p_staff_id;
  if staff_role is null or staff_role not in ('owner', 'admin', 'desk') then
    raise exception 'Only owner, admin, or desk staff can check members in.' using errcode = '42501';
  end if;
  if p_method not in ('signed', 'manual') then
    raise exception 'Unknown check-in method';
  end if;
  if where_at = '' then
    where_at := 'HINVR desk';
  end if;
  if p_scanned_at is not null and (p_scanned_at > now() + interval '2 minutes' or p_scanned_at < now() - interval '12 hours') then
    return jsonb_build_object('status', 'invalid', 'reason', 'This offline scan is too old to record. Scan the pass again.');
  end if;

  if p_method = 'signed' then
    select * into cred from public.pass_credentials c where c.id = p_credential_id;
    if not found then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code was replaced. Ask them to open the pass again.');
    end if;
    if cred.user_id is distinct from p_user_id or cred.member_id is distinct from btrim(coalesce(p_member_id, '')) then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code does not match the account.');
    end if;
    if cred.revoked_at is not null and cred.revoked_at <= seen_at then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code was replaced. Ask them to open the pass again.');
    end if;
    if cred.generation is distinct from p_generation then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code was rotated. Ask them to open the pass again.');
    end if;
    if cred.expires_at + interval '90 seconds' < seen_at then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code has expired. Ask them to reopen the pass.');
    end if;
    select * into prof from public.profiles p where p.id = cred.user_id;
    if not found then
      return jsonb_build_object('status', 'invalid', 'reason', 'This account no longer exists.');
    end if;
    if prof.pass_generation is distinct from p_generation then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code was rotated. Ask them to open the pass again.');
    end if;
    if prof.member_id is distinct from cred.member_id then
      return jsonb_build_object('status', 'invalid', 'reason', 'This code does not match the account.');
    end if;
  else
    if p_user_id is not null then
      select * into prof from public.profiles p where p.id = p_user_id;
    elsif wanted_id <> '' then
      select * into prof from public.profiles p where upper(btrim(p.member_id)) = wanted_id;
    end if;
    if prof.id is null then
      return jsonb_build_object('status', 'invalid', 'reason', 'No member on that ID.');
    end if;
  end if;

  member_json := jsonb_build_object(
    'user_id', prof.id,
    'display_name', coalesce(nullif(btrim(prof.display_name), ''), 'Member'),
    'tier', prof.tier,
    'member_id', prof.member_id,
    'valid_until', prof.valid_until,
    'city', prof.city
  );

  if prof.tier not in ('Gold', 'Platinum', 'Nri') then
    return jsonb_build_object('status', 'invalid', 'member', member_json,
      'reason', 'This account does not have a Gold, Platinum, or NRI pass.');
  end if;
  if prof.valid_until is null or private.membership_end(prof.valid_until) < seen_at then
    return jsonb_build_object('status', 'invalid', 'member', member_json, 'reason', 'This membership has ended.');
  end if;

  perform pg_advisory_xact_lock(hashtext('hinvr.checkin.' || prof.id::text));

  if p_method = 'signed' then
    select c.created_at, c.place into prev_at, prev_place
    from public.pass_checkins c
    where c.credential_id = cred.id
      and c.created_at > now() - interval '2 hours'
      and lower(btrim(c.place)) <> lower(where_at)
    order by c.created_at desc
    limit 1;
    if prev_at is not null then
      flag := 'suspicious';
    else
      select c.created_at, c.place into prev_at, prev_place
      from public.pass_checkins c
      where c.credential_id = cred.id
        and c.created_at > now() - interval '30 minutes'
      order by c.created_at desc
      limit 1;
      if prev_at is not null then
        flag := 'duplicate';
      end if;
    end if;
  else
    select c.created_at, c.place into prev_at, prev_place
    from public.pass_checkins c
    where c.user_id = prof.id
      and c.created_at > now() - interval '30 minutes'
    order by c.created_at desc
    limit 1;
    if prev_at is not null then
      flag := 'duplicate';
    end if;
  end if;

  if not coalesce(p_confirm, false) then
    if flag = 'suspicious' then
      perform private.write_audit(
        'pass.suspicious', 'pass_checkins', cred.id::text, null,
        jsonb_build_object('user_id', prof.id, 'place', where_at, 'previous_place', prev_place,
          'previous_at', prev_at, 'scanned_at', seen_at),
        p_staff_id
      );
    end if;
    if p_method = 'manual' or flag <> '' then
      return jsonb_build_object(
        'status', case when p_method = 'manual' then 'manual' else flag end,
        'recorded', false,
        'flag', flag,
        'member', member_json,
        'previous', case when prev_at is null then null else jsonb_build_object('at', prev_at, 'place', prev_place) end
      );
    end if;
  end if;

  insert into public.pass_checkins (
    user_id, member_id, place, note, credential_id, method, flag, checked_in_by, scanned_at
  ) values (
    prof.id, prof.member_id, where_at, note_text,
    case when p_method = 'signed' then cred.id end,
    p_method, flag, p_staff_id, p_scanned_at
  )
  returning id, created_at into saved_id, saved_at;

  return jsonb_build_object(
    'status', case when p_method = 'manual' then 'manual' else 'ok' end,
    'recorded', true,
    'flag', flag,
    'member', member_json,
    'checkin', jsonb_build_object('id', saved_id, 'created_at', saved_at, 'place', where_at),
    'previous', case when prev_at is null then null else jsonb_build_object('at', prev_at, 'place', prev_place) end
  );
end;
$$;

revoke all on function public.desk_check_in(uuid, text, uuid, text, uuid, integer, text, text, boolean, timestamptz)
  from public, anon, authenticated;
grant execute on function public.desk_check_in(uuid, text, uuid, text, uuid, integer, text, text, boolean, timestamptz)
  to service_role;

-- Nightly expiry ---------------------------------------------------------------------
-- After valid_until (IST) the plan drops to None. member_id and valid_until stay as history.
create or replace function private.expire_memberships()
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  n integer;
begin
  perform set_config('hinvr.audit_action', 'membership.expired', true);
  update public.profiles
  set tier = 'None'
  where tier <> 'None'
    and valid_until is not null
    and valid_until < private.today_ist();
  get diagnostics n = row_count;
  perform set_config('hinvr.audit_action', '', true);
  return n;
end;
$$;

revoke all on function private.expire_memberships() from public, anon, authenticated;

select cron.unschedule(jobid)
from cron.job
where jobname = 'expire-memberships';

-- 00:10 IST.
select cron.schedule(
  'expire-memberships',
  '40 18 * * *',
  $$select private.expire_memberships()$$
);

-- Copy: the pass tile is "Priority Assist", never a queue skip. Only rows still on the seed text.
update public.home_services
set title = 'Priority Assist'
where id = 'pass' and title = 'Priority Pass';
