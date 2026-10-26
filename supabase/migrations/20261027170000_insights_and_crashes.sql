-- Headset crash reports, and the numbers behind the desk's Insights page.
-- Insights only read what is already stored; nothing runs until staff open the page.
-- Safe to re-run.

-- Crash reports ---------------------------------------------------------------------

create table if not exists public.vr_crashes (
  id uuid primary key default gen_random_uuid(),
  device_id uuid references public.vr_devices (device_id) on delete set null,
  user_id uuid references public.profiles (id) on delete set null,
  app_version text not null default '' check (char_length(app_version) <= 40),
  kind text not null default '' check (char_length(kind) <= 120),
  message text not null default '' check (char_length(message) <= 500),
  trace text not null default '' check (char_length(trace) <= 8000),
  happened_at timestamptz,
  received_at timestamptz not null default now()
);

create index if not exists vr_crashes_received on public.vr_crashes (received_at desc);
create index if not exists vr_crashes_device_received on public.vr_crashes (device_id, received_at desc);

alter table public.vr_crashes enable row level security;
revoke all on public.vr_crashes from anon, authenticated;
grant select on public.vr_crashes to authenticated;

drop policy if exists vr_crashes_staff_read on public.vr_crashes;
create policy vr_crashes_staff_read
  on public.vr_crashes for select to authenticated
  using ((select private.has_staff_role(array['owner', 'admin'])));

-- A paired headset reports a crash from its last session. Unknown headsets and floods are
-- turned away quietly: a crash reporter must never itself fail loudly.
create or replace function private.report_vr_crash(
  p_device_id uuid,
  p_secret text,
  p_app_version text,
  p_kind text,
  p_message text,
  p_trace text,
  p_happened_at timestamptz
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  linked public.vr_devices;
begin
  select * into linked
  from public.vr_devices
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''));
  if not found then
    return jsonb_build_object('ok', false);
  end if;

  if (
    select count(*) from public.vr_crashes c
    where c.device_id = linked.device_id and c.received_at > now() - interval '1 hour'
  ) >= 5 then
    return jsonb_build_object('ok', false, 'reason', 'rate');
  end if;

  insert into public.vr_crashes (device_id, user_id, app_version, kind, message, trace, happened_at)
  values (
    linked.device_id,
    linked.user_id,
    left(btrim(coalesce(p_app_version, '')), 40),
    left(btrim(coalesce(p_kind, '')), 120),
    left(btrim(coalesce(p_message, '')), 500),
    left(coalesce(p_trace, ''), 8000),
    -- A headset clock can be wrong; never in the future.
    least(coalesce(p_happened_at, now()), now())
  );
  return jsonb_build_object('ok', true);
end;
$$;

create or replace function public.report_vr_crash(
  p_device_id uuid,
  p_secret text,
  p_app_version text,
  p_kind text,
  p_message text,
  p_trace text,
  p_happened_at timestamptz
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
begin
  return private.report_vr_crash(p_device_id, p_secret, p_app_version, p_kind, p_message, p_trace, p_happened_at);
end;
$$;

revoke all on function private.report_vr_crash(uuid, text, text, text, text, text, timestamptz) from public;
revoke all on function public.report_vr_crash(uuid, text, text, text, text, text, timestamptz) from public;
grant execute on function private.report_vr_crash(uuid, text, text, text, text, text, timestamptz) to anon, authenticated;
grant execute on function public.report_vr_crash(uuid, text, text, text, text, text, timestamptz) to anon, authenticated;

-- Insights ----------------------------------------------------------------------------

-- The desk's numbers for the last [p_days] India days (1..90), in one round trip. Owners,
-- admins and editors; recent crashes (with their traces) only for owners and admins.
create or replace function private.staff_insights(p_days integer default 30)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_days integer := least(greatest(coalesce(p_days, 30), 1), 90);
  v_since timestamptz;
  v_week timestamptz := now() - interval '7 days';
  v_offerings jsonb;
  v_daily jsonb;
  v_kinds jsonb;
  v_mandirs jsonb;
  v_dedications jsonb;
  v_headsets jsonb;
  v_handoffs jsonb;
  v_targets jsonb;
  v_crashes jsonb;
  v_recent jsonb := null;
begin
  if not private.has_staff_role(array['owner', 'admin', 'editor']) then
    raise exception 'Staff only.';
  end if;
  -- Midnight in India, v_days - 1 days ago: today counts as a whole day.
  v_since := ((private.today_ist() - (v_days - 1))::timestamp) at time zone 'Asia/Kolkata';

  -- Every offering query below starts from created_at >= v_since (offerings_created).
  select jsonb_build_object(
    'total', count(*),
    'members', count(distinct o.user_id),
    'headsets', count(distinct o.device_id),
    'japa_rounds', count(*) filter (where o.kind = 'japa'),
    'festival', count(*) filter (where o.kind in ('deepmala', 'abhishek'))
  )
  into v_offerings
  from public.offerings o
  where o.created_at >= v_since;

  select coalesce(jsonb_agg(jsonb_build_object('day', d.day, 'total', d.total, 'kinds', d.kinds) order by d.day), '[]'::jsonb)
  into v_daily
  from (
    select k.day, sum(k.n)::int as total, jsonb_object_agg(k.kind, k.n) as kinds
    from (
      select (o.created_at at time zone 'Asia/Kolkata')::date as day, o.kind, count(*)::int as n
      from public.offerings o
      where o.created_at >= v_since
      group by 1, 2
    ) k
    group by k.day
  ) d;

  select coalesce(jsonb_object_agg(k.kind, k.n), '{}'::jsonb)
  into v_kinds
  from (
    select o.kind, count(*)::int as n
    from public.offerings o
    where o.created_at >= v_since
    group by o.kind
  ) k;

  select coalesce(jsonb_agg(jsonb_build_object('mandir_id', t.mandir_id, 'name', t.name, 'offerings', t.n,
                                               'members', t.members) order by t.n desc), '[]'::jsonb)
  into v_mandirs
  from (
    select o.mandir_id, coalesce(max(m.name), o.mandir_id) as name, count(*)::int as n,
           count(distinct o.user_id)::int as members
    from public.offerings o
    left join public.mandirs m on m.id = o.mandir_id
    where o.created_at >= v_since and o.mandir_id is not null
    group by o.mandir_id
    order by n desc
    limit 8
  ) t;

  select coalesce(jsonb_object_agg(k.dedicated_to, k.n), '{}'::jsonb)
  into v_dedications
  from (
    select o.dedicated_to, count(*)::int as n
    from public.offerings o
    where o.created_at >= v_since and o.dedicated_to is not null
    group by o.dedicated_to
  ) k;

  select jsonb_build_object(
    'paired', count(*),
    'active_week', count(*) filter (where d.last_seen_at >= v_week),
    'reporting_week', count(*) filter (where d.status_at >= v_week),
    'low_battery', count(*) filter (where d.status_at >= v_week and d.battery_pct <= 20 and d.charging is not true)
  )
  into v_headsets
  from public.vr_devices d;

  select jsonb_build_object(
    'requests', count(*),
    'claimed', count(*) filter (where h.claimed_at is not null),
    'median_seconds', round((percentile_cont(0.5) within group (
      order by extract(epoch from h.claimed_at - h.created_at)::double precision
    ) filter (where h.claimed_at is not null))::numeric, 1)
  )
  into v_handoffs
  from public.vr_handoffs h
  where h.created_at >= v_since;

  select coalesce(jsonb_object_agg(t.target, jsonb_build_object('requests', t.n, 'claimed', t.claimed)), '{}'::jsonb)
  into v_targets
  from (
    select h.target, count(*)::int as n, count(*) filter (where h.claimed_at is not null)::int as claimed
    from public.vr_handoffs h
    where h.created_at >= v_since
    group by h.target
  ) t;

  select jsonb_build_object('total', count(*), 'headsets', count(distinct c.device_id))
  into v_crashes
  from public.vr_crashes c
  where c.received_at >= v_since;

  if private.has_staff_role(array['owner', 'admin']) then
    select coalesce(jsonb_agg(jsonb_build_object(
      'id', r.id,
      'received_at', r.received_at,
      'happened_at', r.happened_at,
      'app_version', r.app_version,
      'kind', r.kind,
      'message', r.message,
      'trace', r.trace,
      'headset', r.headset
    ) order by r.received_at desc), '[]'::jsonb)
    into v_recent
    from (
      select c.id, c.received_at, c.happened_at, c.app_version, c.kind, c.message, c.trace,
             coalesce(nullif(d.name, ''), 'Meta Quest') as headset
      from public.vr_crashes c
      left join public.vr_devices d on d.device_id = c.device_id
      order by c.received_at desc
      limit 20
    ) r;
  end if;

  return jsonb_build_object(
    'days', v_days,
    'since', v_since,
    'offerings', v_offerings,
    'daily', v_daily,
    'kinds', v_kinds,
    'mandirs', v_mandirs,
    'dedications', v_dedications,
    'headsets', v_headsets,
    'handoffs', v_handoffs || jsonb_build_object('targets', v_targets),
    'crashes', v_crashes,
    'recent_crashes', v_recent
  );
end;
$$;

create or replace function public.staff_insights(p_days integer default 30)
returns jsonb
language plpgsql
stable
security invoker
set search_path = ''
as $$
begin
  return private.staff_insights(p_days);
end;
$$;

revoke all on function private.staff_insights(integer) from public;
revoke all on function public.staff_insights(integer) from public;
grant execute on function private.staff_insights(integer) to authenticated;
grant execute on function public.staff_insights(integer) to authenticated;
