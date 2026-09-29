-- Broadcast notices from the desk, and the phone tokens that receive them.

create table if not exists public.device_tokens (
  token text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  platform text not null default 'android',
  updated_at timestamptz not null default now()
);

create index if not exists device_tokens_user
  on public.device_tokens (user_id);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  created_by uuid references public.staff (user_id) on delete set null,
  sent_count integer not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists notifications_created
  on public.notifications (created_at desc);

alter table public.device_tokens enable row level security;
alter table public.notifications enable row level security;

revoke all on public.device_tokens from anon, authenticated;
revoke all on public.notifications from anon, authenticated;
grant select on public.notifications to authenticated;

drop policy if exists notifications_select on public.notifications;
create policy notifications_select
  on public.notifications for select to authenticated
  using (true);

create or replace function private.register_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  cleaned text;
begin
  if auth.uid() is null then
    raise exception 'Sign in again.';
  end if;
  cleaned := trim(coalesce(p_token, ''));
  if length(cleaned) < 20 or length(cleaned) > 512 then
    raise exception 'Bad token';
  end if;
  insert into public.device_tokens (token, user_id, platform, updated_at)
  values (cleaned, auth.uid(), 'android', now())
  on conflict (token) do update
    set user_id = excluded.user_id,
        platform = excluded.platform,
        updated_at = now();
end;
$$;

create or replace function public.register_device_token(p_token text)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  perform private.register_device_token(p_token);
end;
$$;

create or replace function private.forget_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Sign in again.';
  end if;
  delete from public.device_tokens
  where token = trim(coalesce(p_token, ''))
    and user_id = auth.uid();
end;
$$;

create or replace function public.forget_device_token(p_token text)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  perform private.forget_device_token(p_token);
end;
$$;

revoke all on function private.register_device_token(text) from public;
revoke all on function private.forget_device_token(text) from public;
revoke all on function public.register_device_token(text) from public;
revoke all on function public.forget_device_token(text) from public;

grant execute on function private.register_device_token(text) to authenticated;
grant execute on function private.forget_device_token(text) to authenticated;
grant execute on function public.register_device_token(text) to authenticated;
grant execute on function public.forget_device_token(text) to authenticated;
