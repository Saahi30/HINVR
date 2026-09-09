The repo is empty, so this is a greenfield product. Treat it as a **membership club for Hindu faith**, not a VR novelty. VR is the hook. Recurring membership plus real-world access is the business.

## The actual product

**HINVR** should feel like Priority Pass + Amex concierge + a temple streaming network.

The member is not buying an app. They are buying status, access, and someone who handles the religious logistics they do not want to figure out.

Two paying customers matter most:

| Segment | Why they pay | What they buy |
|---|---|---|
| **NRI families** | Guilt, distance, parents in India | Live/VR darshan, remote pooja, pass for parents, concierge |
| **Domestic affluent / elderly** | Crowds, queues, mobility | VIP lane, buggy, pandit at home, tour help |

Build for NRIs first. They pay more, complain less about “why is this ₹4,999/year,” and they are the ones who will gift the physical card to parents.

Do **not** lead with “we sell skip-the-line at Tirupati.” Temple boards control that. Selling unofficial VIP access will get you banned, sued, or both. The honest product is:

- official booking through temple channels
- partner temples that actually want members
- on-ground assist (meet, buggy, wheelchair, prasad, hotel)
- digital proof of membership (QR / card)

## Do not build the whole ecosystem in v1

If you ship Live Darshan + VR + VIP lanes + pandits + concierge + yatra together, you will have a beautiful empty app.

**Wedge (first 6 months):** Membership + temple directory + live darshan + digital pass.

That is enough to look like a club, take payments, and start temple conversations.

```
Phase 0    Brand, splash, auth, pay, membership
Phase 1    Mandir directory + Live Darshan
Phase 2    Digital Priority Pass (QR + card art)
Phase 3    Home pooja in 1–2 cities
Phase 4    Dharma concierge (chat, then phone)
Phase 5    Yatra packages (affiliate first)
Phase 6    True VR / 360 live at partner mandirs
```

VR last, not first. Real 6DOF VR per temple is a studio project. What Indians already accept as “VR darshan” is **360 live or 360 recorded video on a phone**, maybe with a cheap headset. Ship that. Call it VR. Upgrade later.

## What the Android app should feel like

### Splash
Hindu, not cartoon-god clipart.

- Palette: deep saffron, vermillion, antique gold, temple-stone brown, diya amber
- Motion: a single diya lighting, or a temple gopuram silhouette resolving out of incense haze
- Sound: one temple bell, then silence. Let the user skip. Never autoplay a bhajan loop
- Avoid random Om watermarks, purple “mystic” gradients, or AI-looking deities
- 2.5 seconds, then home. Membership check happens behind the splash

Think **stone, fire, gold** — like standing in a sabha mandap at dusk.

### Home (information architecture)

Do **not** dump a raw list of 10,000 mandirs on launch. Use category tiles, then lists.

```
┌─────────────────────────────────────┐
│  HINVR          [Pass]  [Profile]   │
│  “Jai Shri Ram, Rahul”              │
│  Gold member · valid till Apr 2027  │
├──────────┬──────────┬───────────────┤
│ Live     │ VR       │ Priority      │
│ Darshan  │ Darshan  │ Pass          │
├──────────┬──────────┬───────────────┤
│ Book     │ Dharma   │ Yatra         │
│ Pandit   │ Concierge│ Packages      │
└─────────────────────────────────────┘
  Nearby mandirs
  [Tirupati] [Kashi] [Shirdi] [Kedarnath]
```

Bottom nav, 4 tabs only:

1. **Home**
2. **Mandirs**
3. **Pass**
4. **Concierge**

Everything else (pooja, yatra, profile) lives inside those.

**Mandirs tab** is the long-term catalog: search, state, deity, live-now, VR-available, pass-accepted. That is your inventory. Home is the storefront.

## Membership — this is the company

Price it like a club, not like a streaming app.

| Tier | Price (starting point) | What they get |
|---|---|---|
| **Darshan** | ₹999 / yr or free with ads | Live streams, directory, basic chat |
| **Gold** | ₹4,999 / yr | QR pass, 2 home poojas, concierge chat, yatra discount |
| **Platinum** | ₹14,999 / yr | Physical metal card, phone concierge, buggy assist at partners, family add-on, priority pooja |
| **NRI** | $99–199 / yr | Same as Platinum, USD pricing, international support hours |

The **physical card** is marketing, not tech. People put it in their wallet. They gift it at Diwali. Photograph it. That is how you get distribution.

The **QR** is the real credential: signed, time-bound, rotatable, works offline for a few hours, scanned at the partner desk.

Pass screen should look like a boarding pass:

- Member name, photo, tier, member ID
- Big QR
- “Show this at the HINVR desk”
- Add to Google Wallet later
- “Request physical card” CTA

## Feature-by-feature, in business order

### 1. Live Darshan (launch)
Do not wait to install cameras. Day one, embed **official temple YouTube / website streams** you have rights to show. Label them clearly: “Official TTD stream,” not “HINVR exclusive.”

App features that make this yours:

- “Live now” rail
- reminder before aarti
- picture-in-picture
- cast to TV (NRI living room)
- “offer digital prasad / donate” after the stream (temple or your trust)

This is how you get daily opens. Daily opens justify the membership.

### 2. VR Darshan (phase 6, prototype in phase 1)
Phase 1: one 360 video of one famous mandir, played with gyroscope + optional cardboard mode. That is enough for the splash-store screenshot.

Phase 6: 360 live cameras at 3 partner temples, then Quest/phone VR if the unit economics work.

Do not start in Unity. Start with ExoPlayer 360 / SceneView. Unity only when a temple is paying for a real experience.

### 3. Priority Pass / VIP lane / buggy
This is ops, not an app screen.

You need a human at the temple, a desk, a WhatsApp group, and a temple MoU. Until you have **3 signed partner mandirs**, the Pass tab is a waitlist + “we will call you before your visit.”

Partner profile that will actually say yes:

- medium famous temples with parking chaos
- private trusts, not ASI / board-controlled giants first
- places that already sell VIP tickets and want a digital channel

Service you can honestly sell before exclusive lanes exist:

- we book the **official** special-entry ticket
- we arrange buggy / wheelchair / elderly assist
- a local host meets the member
- prasad + hotel + taxi

Call it **Priority Assist**, not “skip the queue,” until you own a lane.

### 4. Pandit / home pooja
This is a marketplace. Urban Company already taught India how this works.

Launch in **one city** (Delhi-NCR or Ahmedabad or Hyderabad). 20 verified pandits. 8 SKUs:

- Satyanarayan
- Griha pravesh
- Namkaran
- Wedding consult
- Shraddh / pitru
- Kaal sarp / dosh (careful, easy to get sleazy)
- Daily puja for NRI homes (local pandit visits parents)
- Office / shop opening

You take 20–30% commission. Gold members get 2 included, which is a customer-acquisition cost, not a feature.

Verification is the product: ID, sampradaya, language, reviews, on-time rate. Unverified pandits will kill the brand.

### 5. Dharma Concierge
Two layers. Do not pretend an LLM is a pandit.

**Layer A — chat (cheap, 24/7)**  
RAG over a curated corpus: festival dates, vrata rules, temple timings, packing lists, “what to wear at Kashi,” “can I go to temple after a death in the family.” Always cite, always say “ask your kulguru for your family custom.”

**Layer B — phone on demand (Platinum)**  
A trained agent, 8am–10pm IST, who can also escalate to a real pandit. This is what NRIs will pay for: “My father died, I land tomorrow, what do I do.”

This is also your highest-trust surface. One wrong answer about shraddh and the brand is done. Human review on anything about death, marriage, or doshas.

### 6. Yatra packages
Do not become a tour operator in year one. White-label 2–3 existing operators (Chardham, Varanasi, Tirupati+Srisailam, Dwarka-Somnath). You take margin + member discount. Your job is trust and the pass, not buses.

## Android stack (practical)

| Layer | Choice | Why |
|---|---|---|
| UI | Kotlin + Jetpack Compose | Native, fast, Play-friendly |
| Auth | Phone OTP + Google | India default |
| Backend | Supabase or Firebase + later a real API | Fast now, migrate when ops get real |
| Payments | Razorpay (UPI, cards, international) | Membership + pooja |
| Live video | ExoPlayer | YouTube embeds + HLS later |
| QR | CameraX + ML Kit, signed JWT/QR | Offline-tolerant pass |
| Chat | Your backend + a model with RAG | Concierge |
| Push | FCM | Aarti reminders |
| Languages | English + Hindi first, then Tamil/Telugu/Gujarati | NRI + temple belt |

Skip Flutter if VR quality matters later. Native Android now, Unity module later if needed.

## The hard parts (these decide if this is a company)

**1. Temple supply.**  
Hire a partnerships person before a second engineer. No MoUs, no Pass, no story.

**2. Rights.**  
Streaming a temple’s aarti without a license is how you get a takedown and a political problem. Start with official embeds.

**3. Trust.**  
You are touching shraddh, money, and old people. One fake pandit or fake VIP scam and you are finished. KYC members for Platinum. KYC pandits. Printed desk process at temples.

**4. Unit economics.**  
A Platinum member who uses buggy + 2 poojas + 3 concierge calls + a Chardham discount can easily cost more than ₹14,999. Cap entitlements. Sell extras.

**5. Politics and optics.**  
Stay sampradaya-neutral. No caste language. No “faster darshan because you paid” as the public slogan. Frame it as **access, assist, and devotion for those who cannot stand in a 6-hour queue**.

## 90-day build plan

**Weeks 1–3 — App skeleton**  
Splash, login, membership paywall, home categories, empty states that already look premium.

**Weeks 4–6 — Mandirs + Live**  
50 temples with real data (timings, deity, location, official stream if any). 8–10 live tiles. Search. Favorites.

**Weeks 7–8 — Pass**  
QR credential, Pass screen, Google Wallet later. Waitlist for physical card.

**Weeks 9–10 — One city pooja**  
Catalog, slot booking, payment, pandit assignment, rating.

**Weeks 11–12 — Concierge v0**  
FAQ + festival calendar + “call me” ticket for Gold+. No open-ended scripture chatbot yet.

Parallel, non-engineering: 10 temple meetings, 1 MoU, 20 pandits, brand kit, card mockups.

## What I would put on the first Play Store listing

> HINVR — Live darshan, a digital temple pass, and a dharma concierge for your family.

Not “the VR temple metaverse.” That sentence does not convert Indian parents.

---

If you want to go deeper next, the two most useful follow-ups are:

1. **Screen-by-screen app map** (every Compose screen, nav graph, empty states) so you can start building
2. **Membership legal + temple partnership pitch** (what you can actually promise without getting shut down)
