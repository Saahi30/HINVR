-- Street address captured from the phone during profile setup.

alter table public.profiles
  add column if not exists address text not null default '';
