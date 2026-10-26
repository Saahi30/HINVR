-- Step 4 of moving the pass signing key out of the database. Run in the SQL editor ONLY
-- after the new functions are deployed with PASS_SIGNING_KEYS set and a pass has opened
-- on a phone (that proves the functions sign from the secret).
-- Removes the private key copy and the RPC that handed it to edge functions.
-- public.pass_signing_keys (public halves only) stays: desks verify with it.

begin;

do $$
begin
  if not exists (select 1 from public.pass_signing_keys where active) then
    raise exception 'No active public key is registered. Open a pass with the new functions first.';
  end if;
end;
$$;

drop function if exists public.ensure_pass_signing_key(jsonb, jsonb);
drop table if exists private.pass_signing_keys;

commit;
