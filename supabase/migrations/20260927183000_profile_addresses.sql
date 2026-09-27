-- Named places for a member: Home, Parents' home, and any other label.

alter table public.profiles
  add column if not exists addresses jsonb not null default '[]'::jsonb;

update public.profiles
set addresses = jsonb_build_array(jsonb_build_object('label', 'Home', 'address', address))
where coalesce(address, '') <> ''
  and addresses = '[]'::jsonb;

alter table public.profiles drop column if exists address;
