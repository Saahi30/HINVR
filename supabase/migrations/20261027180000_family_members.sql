-- Family on one membership: named people (no logins of their own) who come with the
-- member to the desk, and whom an offering can be made for. How many a plan carries is
-- in app_settings.family_limits. Safe to re-run.

create table if not exists public.family_members (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  name text not null check (char_length(btrim(name)) between 1 and 60),
  relation text not null check (relation in (
    'mother', 'father', 'spouse', 'son', 'daughter', 'grandmother', 'grandfather', 'sibling', 'other'
  )),
  created_at timestamptz not null default now()
);

create index if not exists family_members_user on public.family_members (user_id, created_at);

alter table public.family_members enable row level security;
revoke all on public.family_members from anon, authenticated;
grant select, insert, update, delete on public.family_members to authenticated;

drop policy if exists family_members_select on public.family_members;
create policy family_members_select
  on public.family_members for select to authenticated
  using (
    user_id = (select auth.uid())
    or (select private.has_staff_role(array['owner', 'admin', 'finance', 'desk']))
  );

drop policy if exists family_members_insert_own on public.family_members;
create policy family_members_insert_own
  on public.family_members for insert to authenticated
  with check (user_id = (select auth.uid()));

drop policy if exists family_members_update_own on public.family_members;
create policy family_members_update_own
  on public.family_members for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

drop policy if exists family_members_delete_own on public.family_members;
create policy family_members_delete_own
  on public.family_members for delete to authenticated
  using (user_id = (select auth.uid()));

-- How many family members each plan carries. Staff can change it in app_settings.
insert into public.app_settings (key, value)
values ('family_limits', '{"None": 0, "Darshan": 0, "Gold": 2, "Platinum": 4, "Nri": 4}'::jsonb)
on conflict (key) do nothing;

create or replace function private.family_limit(p_tier text)
returns integer
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select (s.value ->> coalesce(p_tier, 'None'))::int from public.app_settings s where s.key = 'family_limits'),
    0
  );
$$;

-- A member can add family up to their plan's limit. Renaming someone is always allowed.
create or replace function private.enforce_family_limit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  plan text;
  allowed integer;
  have integer;
begin
  new.name := btrim(new.name);
  if tg_op = 'UPDATE' then
    new.user_id := old.user_id;
    return new;
  end if;
  select p.tier into plan from public.profiles p where p.id = new.user_id;
  allowed := private.family_limit(plan);
  select count(*) into have from public.family_members f where f.user_id = new.user_id;
  if have >= allowed then
    if allowed = 0 then
      raise exception 'Your plan does not include family members.';
    end if;
    raise exception 'Your plan includes % family members.', allowed;
  end if;
  return new;
end;
$$;

drop trigger if exists family_members_limit on public.family_members;
create trigger family_members_limit
  before insert or update on public.family_members
  for each row execute function private.enforce_family_limit();

revoke all on function private.family_limit(text) from public;
revoke all on function private.enforce_family_limit() from public, anon, authenticated;
grant execute on function private.family_limit(text) to authenticated, service_role;

-- The first [limit] family members by when they were added: who the plan covers today.
create or replace function private.family_json(p_user uuid)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(jsonb_agg(jsonb_build_object('id', f.id, 'name', f.name, 'relation', f.relation)
                            order by f.created_at), '[]'::jsonb)
  from (
    select fm.*
    from public.family_members fm
    where fm.user_id = p_user
    order by fm.created_at
    limit (select private.family_limit(p.tier) from public.profiles p where p.id = p_user)
  ) f;
$$;

revoke all on function private.family_json(uuid) from public;
grant execute on function private.family_json(uuid) to anon, authenticated, service_role;

-- Headset profile: everything it had, plus the family an offering can be made for.
create or replace function private.vr_profile_json(p_device public.vr_devices)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select jsonb_build_object(
    'display_name', p.display_name,
    'city', p.city,
    'tier', p.tier,
    'member_id', p.member_id,
    'valid_until', coalesce(to_char(p.valid_until, 'FMDD Mon YYYY'), ''),
    'email', coalesce(u.email, ''),
    'device_name', p_device.name,
    'paired_at', p_device.paired_at,
    'family', private.family_json(p.id)
  )
  from public.profiles p
  left join auth.users u on u.id = p.id
  where p.id = p_device.user_id;
$$;

revoke all on function private.vr_profile_json(public.vr_devices) from public;

-- Offerings made for one named family member --------------------------------------

alter table public.offerings
  add column if not exists family_member_id uuid references public.family_members (id) on delete set null;

-- A seventh argument can't be added in place: drop the six-argument pair first.
drop function if exists public.record_offering(uuid, text, text, text, text, text);
drop function if exists private.record_offering(uuid, text, text, text, text, text);

create or replace function private.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null,
  p_dedicated_to text default null,
  p_family_member uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  linked public.vr_devices;
  allowed constant text[] := array[
    'ghanta','diya','aarti','pushp','agarbatti','prasad','tilak','daan','japa','abhishek','deepmala'
  ];
  dedications constant text[] := array['self','family','parents','children','ancestors','friend','teacher'];
  v_mandir text := nullif(btrim(coalesce(p_mandir_id, '')), '');
  v_sankalp text := nullif(left(btrim(coalesce(p_sankalp, '')), 500), '');
  v_for text := lower(nullif(btrim(coalesce(p_dedicated_to, '')), ''));
  v_family uuid := p_family_member;
begin
  if p_kind is null or p_kind <> all (allowed) then
    raise exception 'Unknown offering.';
  end if;
  select * into linked
  from public.vr_devices
  where device_id = p_device_id
    and secret_hash = private.vr_hash(coalesce(p_secret, ''));
  if not found then
    raise exception 'Pair this headset again.';
  end if;

  if (
    select count(*) from public.offerings o
    where o.device_id = linked.device_id and o.created_at > now() - interval '1 minute'
  ) >= 20
  or (
    select count(*) from public.offerings o
    where o.user_id = linked.user_id and o.created_at > now() - interval '10 minutes'
  ) >= 60
  then
    raise exception 'Too many offerings at once. Wait a minute.';
  end if;

  if v_mandir is not null and not exists (select 1 from public.mandirs m where m.id = v_mandir) then
    v_mandir := null;
  end if;
  -- An unknown dedication is dropped, not refused: the offering still counts.
  if v_for is not null and v_for <> all (dedications) then
    v_for := null;
  end if;
  -- Only this member's own family; anyone else's id is ignored.
  if v_family is not null then
    if exists (select 1 from public.family_members f where f.id = v_family and f.user_id = linked.user_id) then
      v_for := 'family';
    else
      v_family := null;
    end if;
  end if;

  insert into public.offerings (device_id, user_id, kind, mandir_id, sankalp, dedicated_to, family_member_id)
  values (linked.device_id, linked.user_id, p_kind, v_mandir, v_sankalp, v_for, v_family);
  return jsonb_build_object('ok', true, 'kind', p_kind);
end;
$$;

create or replace function public.record_offering(
  p_device_id uuid,
  p_secret text,
  p_kind text,
  p_mandir_id text default null,
  p_sankalp text default null,
  p_dedicated_to text default null,
  p_family_member uuid default null
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
begin
  return private.record_offering(p_device_id, p_secret, p_kind, p_mandir_id, p_sankalp, p_dedicated_to, p_family_member);
end;
$$;

revoke all on function private.record_offering(uuid, text, text, text, text, text, uuid) from public;
revoke all on function public.record_offering(uuid, text, text, text, text, text, uuid) from public;
grant execute on function private.record_offering(uuid, text, text, text, text, text, uuid) to anon, authenticated;
grant execute on function public.record_offering(uuid, text, text, text, text, text, uuid) to anon, authenticated;
