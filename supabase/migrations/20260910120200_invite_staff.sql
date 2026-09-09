-- Owner can attach an existing Auth user to staff by email.

create or replace function private.invite_staff(p_email text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid;
  normalized text;
begin
  if not private.is_owner() then
    raise exception 'Only the owner can invite staff';
  end if;

  normalized := lower(trim(p_email));
  if normalized is null or normalized = '' then
    raise exception 'Email is required';
  end if;

  select id into uid
  from auth.users
  where lower(email) = normalized
  limit 1;

  if uid is null then
    raise exception 'No account with that email. Ask them to create an account on this admin first.';
  end if;

  insert into public.staff (user_id, email, role)
  values (uid, normalized, 'editor')
  on conflict (user_id) do update
    set email = excluded.email;
end;
$$;

create or replace function public.invite_staff(p_email text)
returns void
language sql
security invoker
set search_path = public
as $$
  select private.invite_staff(p_email);
$$;

revoke all on function private.invite_staff(text) from public;
revoke all on function public.invite_staff(text) from public;
grant execute on function private.invite_staff(text) to authenticated;
grant execute on function public.invite_staff(text) to authenticated;
