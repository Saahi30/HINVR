# HINVR Admin

Catalog CMS for the Android app. Same Supabase project: mandirs, photos, live/VR links, home tiles, members, and staff.

```bash
cd admin
cp .env.example .env.local
# paste NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Create the first desk account — it becomes owner.

In Supabase Auth, add `http://localhost:3000/auth/callback` and `hinvr://reset` to redirect URLs. The app link is how a member opens the password reset email on the phone. For local use, turn off **Confirm email**.

## Live checker

Each mandir can have YouTube **live sources** (mandir edit page, bottom). Every 10 minutes the `live-check` function asks the YouTube Data API which sources are live and embeddable. It then writes the best one into the mandir's `live` and `live_url`: official first, then priority, then viewers. A source marked **Follow the channel** picks up the channel's next live video when the current one ends. Mandirs with no enabled source stay manual.

If every source for a mandir is dead, it runs a YouTube **live** search (`{name} {city} live darshan`), preferring the same channel, and replaces the saved video ID. Search costs 100 quota units, so the checker searches at most 8 mandirs per run and waits 3 hours before searching the same dead mandir again. **Check feeds now** skips the wait. Units used today are stored in `app_settings` (`youtube_quota`) and shown on Overview and the Live board (YouTube’s 10,000/day reset is midnight Pacific). Search is skipped when remaining quota is low.

One-time setup:

1. In Google Cloud, enable **YouTube Data API v3** and create an API key. Checking ~50 sources is cheap (about 20 units). A search for dead mandirs is 100 units each; stay under 8 replacements per run.
2. Pick a long random string for `LIVE_CHECK_SECRET`, then:

```bash
supabase secrets set YOUTUBE_API_KEY=... LIVE_CHECK_SECRET=...
supabase functions deploy live-check --no-verify-jwt
supabase db push
```

3. In the SQL editor, store the two values the schedule reads:

```sql
select vault.create_secret('https://<ref>.supabase.co', 'project_url');
select vault.create_secret('<same LIVE_CHECK_SECRET>', 'live_check_secret');
```

**Check feeds now** on the Live page runs it immediately.
