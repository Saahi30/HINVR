-- Remember when a dead mandir was last looked up on YouTube Search.
alter table public.mandirs
  add column if not exists live_searched_at timestamptz;
