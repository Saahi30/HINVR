-- One row per issued membership. Members can read their own invoices.
-- Only staff can write them, so a member cannot invent a purchase.

create table if not exists public.membership_purchases (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  tier text not null,
  member_id text not null default '',
  amount_inr integer not null check (amount_inr >= 0),
  invoice_number text not null check (invoice_number <> ''),
  purchased_on date not null,
  valid_from date not null,
  valid_until date not null,
  created_at timestamptz not null default now(),
  unique (invoice_number)
);

create index if not exists membership_purchases_user_id_idx
  on public.membership_purchases (user_id, purchased_on desc);

alter table public.membership_purchases enable row level security;

drop policy if exists membership_purchases_select on public.membership_purchases;
create policy membership_purchases_select
  on public.membership_purchases
  for select
  to authenticated
  using (user_id = (select auth.uid()) or private.is_staff());

drop policy if exists membership_purchases_insert on public.membership_purchases;
create policy membership_purchases_insert
  on public.membership_purchases
  for insert
  to authenticated
  with check (private.is_staff());

drop policy if exists membership_purchases_update on public.membership_purchases;
create policy membership_purchases_update
  on public.membership_purchases
  for update
  to authenticated
  using (private.is_staff())
  with check (private.is_staff());

grant select, insert, update on public.membership_purchases to authenticated;
