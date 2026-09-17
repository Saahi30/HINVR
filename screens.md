# HINVR phone screen inventory

`plan.md` owns product scope. `design.md` owns visual treatment. This file owns routes, gates, and empty states for the phone wedge.

## Entry flow

1. **Splash** (`splash`) — Sanctum; sync session/catalog; continue after 2.5 seconds.
2. **Onboarding** (`onboarding`) — three Sabha pages; shown once.
3. **Phone** (`auth/phone`) — phone number; OTP can remain mocked in this wedge.
4. **OTP** (`auth/otp/{phone}`) — six digits.
5. **Profile setup** (`auth/setup`) — name, city, language, Me/Parents/Family.
6. **Main** (`main`) — four tabs only.

## Main tabs

### Home (`home`)

- Member greeting, tier, validity, Pass/Profile/Notifications actions.
- Live status card, six-tile bento, custom request, nearby mandirs.
- Featured mandir with Live/VR/Pass/Assist dock.
- “See it like family” local-host rail.
- Home is Sabha; media transitions to Sanctum.

### Mandirs (`mandirs`)

- Search by name/place.
- Filters: Live, VR, Pass accepted, Nearby (profile city), Favorites.
- Cards show honest LIVE/VR/PASS badges.
- Empty: “No mandir by that name” + Ask Concierge.
- Detail (`mandirs/{id}`): photo hero, actions, favorite, timings, honesty copy.

### Pass (`pass`)

- Sanctum boarding pass.
- Gold/Platinum/NRI: name, tier, member ID, validity, scannable QR.
- None/Darshan: frosted credential + membership CTA.
- How it works (`pass/how`) and Plan a visit (`pass/visit`).
- Visit form stores a local desk request: mandir, date, party, assist, notes.

### Concierge (`concierge`)

- Sabha visual thread: FAQ facts and photo cards.
- Composer creates a local desk request; no scripture chatbot.
- FAQ (`concierge/faq/{id}`) always includes family-custom disclaimer.

## Supporting routes

- **Live** (`live`, `live/{id}`): directory is Sabha; official player is Sanctum. Live remains open to all. Missing/down feed shows a respectful still and honesty copy.
- **VR** (`vr`, `vr/{id}`): recorded embeds only, labeled not live. Gold+ gate opens a membership sheet. True gyro/360 is later.
- **Plans** (`plans`, `plans/success`): Darshan/Gold/Platinum; mock purchase for this wedge; Gold recommended.
- **Profile** (`profile`): membership details, edit name/city, legal, sign out.
- **Notifications** (`notifications`): reminder empty state; FCM later.
- **Pooja** (`pooja`): one-city waitlist only.
- **Yatra** (`yatra`): club-desk waitlist only.
- **Legal** (`profile/legal`): service, stream attribution, and non-temple-board disclaimer.

## Gate matrix

| Surface | None | Darshan | Gold / Platinum / NRI |
|---|---|---|---|
| Directory and Live | Open | Open | Open |
| Digital QR and visit request | Plans | Plans | Open |
| Recorded VR | Plans sheet | Plans sheet | Open |
| Concierge ticket | Open | Open | Open |

## Global rules

- No fifth tab. Pass is the raised seal in the four-tab dock.
- Never say “skip the line”; use official entry, Priority Assist, desk, buggy, wheelchair, host.
- Never mark a recording live. Attribute official temple feeds.
- Browse surfaces use Sabha linen; Splash, players, and Pass use Sanctum stone.
- Empty/gated states explain why and offer one next action; no dead toasts.
