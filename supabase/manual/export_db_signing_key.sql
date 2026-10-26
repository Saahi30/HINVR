-- Step 1 of moving the pass signing key out of the database. Run in the SQL editor.
-- Prints the value for the PASS_SIGNING_KEYS function secret, plus the active kid.
-- Treat the output as a secret: do not paste it into chat, tickets, or git.

select
  jsonb_object_agg(kid, private_jwk)::text as pass_signing_keys,
  (array_agg(kid order by created_at desc))[1] as pass_signing_active_kid
from private.pass_signing_keys;
