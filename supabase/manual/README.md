# Manual operations

Steps that must not run as migrations, because the order against function deploys matters.

## Move the pass signing key out of the database (one time)

Until now the private key lived in `private.pass_signing_keys` and `ensure_pass_signing_key`
returned it to the edge functions on every pass. The new functions sign with a key from the
`PASS_SIGNING_KEYS` secret and keep only public keys in `public.pass_signing_keys`. Reusing the
same key (same `kid`, normally `primary`) means passes already on phones and in Wallet keep scanning.

1. **Export.** In the SQL editor run `export_db_signing_key.sql`. It prints two values.
2. **Set secrets** (from a terminal, not a shared doc):

   ```bash
   supabase secrets set \
     PASS_SIGNING_KEYS='<pass_signing_keys value>' \
     PASS_SIGNING_ACTIVE_KID=primary
   ```

3. **Deploy** the functions (`pass-credential`, `pass-wallet`, `desk-check-in`, …) and open a pass
   on a phone. That registers the public key and proves signing works from the secret.
4. **Drop** the database copy: run `drop_db_signing_key.sql`. It refuses to run if no public key
   has been registered yet.

Do not run step 4 before step 3: the old functions still read the key from the database.

## Rotate the signing key

Tokens live 6 hours (desk) and 24 hours (Wallet). A rotated-out key keeps verifying for two days.

1. `node supabase/manual/new-signing-key.mjs 2026-11` prints a new key and its kid.
2. Add it to `PASS_SIGNING_KEYS` next to the old one (the secret holds a JSON object of
   `kid → private JWK`), and set `PASS_SIGNING_ACTIVE_KID` to the new kid:

   ```bash
   supabase secrets set PASS_SIGNING_KEYS='{"primary":{…},"2026-11":{…}}' PASS_SIGNING_ACTIVE_KID=2026-11
   ```

3. The first pass issued afterwards calls `register_pass_public_key`: the new kid becomes
   `active`, the old one gets `verify_until = now() + 2 days`. Desks pick up the new public key
   within 30 minutes (they also refresh when they come back online).
4. After two days remove the old kid from `PASS_SIGNING_KEYS`. Its row can stay; it no longer
   verifies once `verify_until` passes.

A compromised key: rotate as above, then in SQL set
`update public.pass_signing_keys set verify_until = now() where kid = '<old>';`
and bump every member's pass so old codes stop matching:
`update public.profiles set pass_generation = pass_generation + 1 where tier <> 'None';`
(members reopen the pass to get a new code).

Never reuse a kid for a different key: `register_pass_public_key` refuses it.
