# HINVR Admin

Catalog CMS for the Android app. Same Supabase project: mandirs, photos, live/VR links, home tiles, members, and staff.

```bash
cd admin
cp .env.example .env.local
# paste NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

`npm test` runs the pass-token and redirect tests (Node 22.18+, no extra runner). The desk imports
the edge functions' signer/verifier directly from `../supabase/functions/_shared/pass-token.ts`,
which is why `next.config.ts` sets the Turbopack root to the repo root.

## First owner

Creating an account gives no access. The first owner is claimed once, from **/setup**, and only
when the team is empty and the account's confirmed email is on the bootstrap list. Put it there
from the SQL editor before claiming:

```sql
insert into private.bootstrap_owner_emails (email) values ('you@example.com');
```

Or skip the page and insert the owner directly:
`insert into public.staff (user_id, email, role) select id, email, 'owner' from auth.users where email = 'you@example.com';`

With **Confirm email** off, anyone who knows the listed address could sign up with it first, so
add the email right before you claim, and remove it afterwards.

## Roles

| Role | Can |
| --- | --- |
| owner | everything, and the team (invite, change roles, remove). The last owner cannot be removed or demoted. |
| admin | everything except the team: memberships, catalog, mandir delete, notices, scanner, audit |
| finance | approve/decline memberships, edit plans and invoices, prices, audit log, member details |
| desk | scanner check-in, desk requests, physical-card list, member contact details |
| editor | mandirs, home screen, live sources, "Check feeds now" |

The database enforces these (RLS and RPC checks); the UI only hides what a role cannot use.
Existing staff keep their old role: former editors become catalog-only, so the owner should assign
roles after the migration. Every membership, invoice, price, team, and check-in change lands in
**Audit log**.

## Edge functions used by the desk

The desk calls `desk-check-in`, `send-notification`, and `live-check` from the browser. Set the
allowed browser origins (no trailing slash):

```bash
supabase secrets set ADMIN_ORIGINS=https://desk.example.com,http://localhost:3000
```

The scanner verifies passes on the server (`desk-check-in`). Offline, it verifies the signature with
cached public keys, shows **Verified offline**, and records the check-in when the desk is back online.

The membership site handoff link is single use; keep **Auth → Email OTP expiration** short
(e.g. 600 seconds) because the same setting bounds that link.

In Supabase Auth, add `http://localhost:3000/auth/callback` and `hinvr://reset` to redirect URLs. The app link is how a member opens the password reset email on the phone. For local use, turn off **Confirm email**.

## Live checker

Each mandir can have YouTube **live sources** (mandir edit page, bottom). Every 10 minutes the `live-check` function asks the YouTube Data API which sources are live and embeddable. It then writes the best one into the mandir's `live` and `live_url`: official first, then priority, then viewers. A source marked **Follow the channel** picks up the channel's next live video when the current one ends. Mandirs with no enabled source stay manual.

If every source for a mandir is dead, it runs a YouTube **live** search (`{name} {city} live darshan`), preferring the same channel. A hit from an **official** source's channel replaces the saved video ID and goes live in the app; a hit from any other channel is held on the source as **needs review** until someone clicks **Use this stream**. Search costs 100 quota units, so the checker searches at most 8 mandirs per run and waits 3 hours before searching the same dead mandir again. **Check feeds now** skips the wait, but runs at most once every 10 minutes for the whole desk. Units used today are stored in `app_settings` (`youtube_quota`, staff-only, incremented atomically) and shown on Overview and the Live board (YouTube’s 10,000/day reset is midnight Pacific). Search is skipped when remaining quota is low.

One-time setup:

1. In Google Cloud, enable **YouTube Data API v3** and create an API key. Checking ~50 sources is cheap (about 20 units). A search for dead mandirs is 100 units each; stay under 8 replacements per run.
2. Pick a long random string for `LIVE_CHECK_SECRET`, then:

```bash
supabase secrets set YOUTUBE_API_KEY=... LIVE_CHECK_SECRET=...
supabase db push
supabase functions deploy live-check   # verify_jwt = false comes from supabase/config.toml
```

3. In the SQL editor, store the two values the schedule reads:

```sql
select vault.create_secret('https://<ref>.supabase.co', 'project_url');
select vault.create_secret('<same LIVE_CHECK_SECRET>', 'live_check_secret');
```

**Check feeds now** on the Live page runs it immediately.
