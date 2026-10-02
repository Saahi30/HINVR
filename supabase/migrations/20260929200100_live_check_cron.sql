-- Run the live-check function every 10 minutes.
-- Needs two Vault secrets, created once per project:
--   select vault.create_secret('https://<ref>.supabase.co', 'project_url');
--   select vault.create_secret('<same value as LIVE_CHECK_SECRET>', 'live_check_secret');
-- Safe to re-run.

create extension if not exists pg_cron;
create extension if not exists pg_net with schema extensions;

select cron.unschedule(jobid)
from cron.job
where jobname = 'live-check';

select cron.schedule(
  'live-check',
  '*/10 * * * *',
  $$
  select net.http_post(
    url := (select decrypted_secret from vault.decrypted_secrets where name = 'project_url')
      || '/functions/v1/live-check',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-live-check-secret',
      (select decrypted_secret from vault.decrypted_secrets where name = 'live_check_secret')
    ),
    body := '{}'::jsonb,
    timeout_milliseconds := 60000
  );
  $$
);
