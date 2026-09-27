-- Applied after membership_requests_and_checkins so database roles
-- (SQL editor, service role) can still issue a plan. Members cannot.

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

revoke all on function private.keep_issued_membership() from public, anon, authenticated;
revoke all on function private.guard_membership_request() from public, anon, authenticated;
