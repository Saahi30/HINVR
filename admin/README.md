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
