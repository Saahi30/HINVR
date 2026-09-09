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

In Supabase Auth, add `http://localhost:3000/auth/callback` to redirect URLs. For local use, turn off **Confirm email**.
