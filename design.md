# HINVR design bible

Use this file when a screen, color, type, or layout decision is unclear. Product *what* lives in `plan.md`. Screen *inventory* lives in `screens.md`. This file is the *feel*.

Inspiration screenshots (Swiggy Crew, Sept 2026) sit in [`design/inspiration/`](design/inspiration/). Steal **liveliness**, not the travel brand.

---

## How to use this file

1. Before drawing or coding a screen, read **Atmosphere** and the matching row in **Screen translations**.
2. If two options both “work,” pick the one that looks more like a lifestyle magazine and less like a settings app.
3. Colors, copy, and photography can change. The **patterns** below should not: photography-first cards, serif headlines, cream space, big radii, nested actions on the card.

When this file and `screens.md` disagree on IA (tabs, gates, routes), **`screens.md` wins**. When they disagree on how something *looks*, **this file wins**.

---

## What “lively” actually means

Crew does not feel lively because it is orange, or because it has a lot of buttons. It feels lively because:

| Crew move | Why it works | HINVR version |
|---|---|---|
| Full-bleed photography as the UI | Color and life come from the *place*, not from chrome | Temple, aarti, gopuram, sabha, river, diya — real photos, never clipart gods |
| Serif headlines, sans for chrome | Reads as a magazine, not a utility | Same pairing. Headlines carry devotion. Labels stay quiet |
| Cream canvas, not #FFFFFF | Warm paper. Photos sit on it | Warm linen / sandstone, not hospital white, not mystic purple |
| Huge corner radii (24–32dp) | Soft, club, contemporary | Same. Nothing sharp except the metal pass |
| Horizontal rails, not endless lists | Discovery. Peek of the next card | Live now, mandirs, yatra, exclusive rates |
| Nested actions *on* the card | The event *is* the nav | Pass / Live / Assist / Stay docked under a mandir card |
| Painterly tiles, not Material icons | Each category is a scene | Live, VR, Pass, Pandit, Concierge, Yatra as illustrated rooms |
| Chat as rich cards | Concierge feels like a desk, not WhatsApp | Photo cards for flights→mandirs, structured white cards for facts |
| One dark, tactile splash | Brand as an object you could hold | Stone + gold seal. One diya. One bell |
| Copy that sells a feeling | “See it like a local…” | “Sit in the sabha from anywhere.” |

**Liveliness is photography + type + space.** If a screen is only labeled boxes on a dark fill, it is not HINVR yet. That is the current Home, and it is the first thing to unlearn.

---

## Steal this / leave this

**Steal**

- Editorial storefront (home is a magazine, catalog is a list)
- Cream storefront + dark sanctum (two atmospheres, next section)
- Glass dock attached to a portrait photo card
- Overlapping circular portraits on people cards
- Bento grid of illustrated services
- Floating white status card over a hero photo (“BLR to BOM” → “Tirupati · LIVE”)
- Chat thread of *visual* cards with timestamps
- Brand as a circular seal, not a hamburger
- Benefit-led section titles, left-aligned, with room to breathe

**Leave**

- Swiggy orange, Crew burgundy-as-identity, navy-as-default-text if it fights gold/saffron
- Concert / villa / forex subject matter
- “Skip the line” as a slogan (see `plan.md` — legally and politically wrong)
- Cartoon Om, neon mystic gradients, AI deities, purple cosmos
- Replacing the 4-tab IA with a single FAB that *is* the whole nav  
  Keep **Home · Mandirs · Pass · Concierge**. The Crew orb becomes the **Pass seal** (raised, gold/vermillion), not a fifth mystery button.

---

## Two atmospheres

Crew is cream everywhere except splash. HINVR needs *dusk* as well — that is the product (temple, pass, live aarti). Do not pick one and flatten the other.

### Sabha (light) — the club storefront

Used for: Home, Mandirs directory, Concierge home, Plans, Pooja catalog, Yatra browse, Profile lists, Onboarding pages 2–3.

- Canvas: warm linen `#F4EDE3` (token `linen`)
- Ink: temple stone `#1A120C` (existing `ink`)
- Quiet text: warm grey-brown `#6B5E4E` (`inkMuted`)
- Cards: white / ivory `#FFF8F0` (`ivory`) floating on linen
- Accents from photography; chrome stays gold + saffron, used sparingly

This is where Crew’s liveliness lives. **Default new browse screens to Sabha unless they are immersive.**

### Sanctum (dark) — the rite

Used for: Splash, Live player, VR player, Pass (the card itself), Pay success, maybe Plans comparison if it should feel like a vault.

- Canvas: temple stone `#1A120C` / `#100B08` (existing `dusk` / `duskDeep`)
- Type: cream `#F3E6D0`
- Light: antique gold, diya amber, saffron for the one primary action
- Photography still full-bleed; gold hairline, not cream cards stacked in a cave

Splash matches Crew’s dark pill: one object, tactile, almost nothing else.

### Switching

Going Home → Live player is linen → night. That cut should feel like entering the mandap, not like a theme bug. Status bar: dark icons on Sabha, light icons on Sanctum.

---

## Color

Existing Compose tokens in `app/.../ui/theme/Color.kt` stay the sacred set. Add light-surface tokens when Home is rebuilt; do not recast the whole app as Crew navy.

| Token | Hex | Role |
|---|---|---|
| `linen` | `#F4EDE3` | Sabha background |
| `ivory` | `#FFF8F0` | Raised cards on linen |
| `ink` | `#1A120C` | Primary text on Sabha (already exists) |
| `inkMuted` | `#6B5E4E` | Secondary on Sabha |
| `dusk` / `duskDeep` | `#1A120C` / `#100B08` | Sanctum canvas |
| `stone` / `stoneRaised` | `#2C2118` / `#3D2E22` | Dark cards, docks |
| `cream` / `creamMuted` | `#F3E6D0` / `#D9C7A8` | Text on Sanctum |
| `gold` / `goldDim` | `#C9A227` / `#8A7018` | Membership, pass, selected tab |
| `saffron` / `saffronDeep` | `#C45C26` / `#9A3F16` | Primary CTA |
| `vermillion` | `#B3392B` | Seal, live dot, urgency — not error-only |
| `amber` / `flame` | `#E8A317` / `#FFF1C1` | Diya, success, live glow |

**Rules**

- One primary CTA per screen, saffron on Sabha, gold on Sanctum.
- Never color-code deities or sampradaya.
- Live pill: vermillion dot + cream “LIVE”, not green-for-go.
- Membership chip: gold fill or gold hairline, never a Material “tonal” purple.
- Photography supplies teal, sky, marigold. UI chrome does not compete with it.

Crew’s palette is cream + navy + burgundy. Ours is **linen + stone + gold + fire**. Same temperature, different religion.

---

## Type

Already the right idea in `Type.kt`: **serif for display/headlines, sans for everything else.** Commit to real files, not `FontFamily.Serif` / `SansSerif` system defaults.

**Proposed (Google Fonts, Play-safe)**

| Role | Face | Use |
|---|---|---|
| Display / section titles | **Fraunces** (soft optical) or **Newsreader** | “Sit in the sabha from anywhere.” Home rails. Plans headline |
| Hindi display | **Tiro Devanagari Hindi** | Same role in `values-hi` |
| UI / body / prices | **Figtree** or **Outfit** | Labels, buttons, member ID, ₹ |
| Wordmark | Custom or Fraunces with wide tracking | **HINVR** — letterspaced, not a cheap stencil |

**Scale (phone)**

| Style | Size | Weight | Notes |
|---|---|---|---|
| Display | 28–34 | Medium serif | 1–2 lines. Never all-caps serif |
| Section title | 22–26 | Semibold serif | Left aligned, 20–24dp side inset |
| Card title on photo | 18–22 | Bold sans, white | With gradient scrim |
| Eyebrow | 11 | Medium sans, +1.2 tracking, caps | `GOA` → `TIRUPATI` · `LIVE NOW` |
| Body | 14–16 | Regular sans | Quiet brown on linen |
| Price / QR meta | 22–28 | Semibold sans | Pass and exclusive-rate cards |
| Button | 14 | Semibold sans | Sentence case. “Watch live darshan” not “SUBMIT” |

**Copy voice** (this is type, not marketing fluff)

Write like Crew’s headlines: short, slightly arrogant, specific.

| Don’t | Do |
|---|---|
| Featured temples | Happening in the sabha tonight |
| Book a pandit | A pandit at the door. Not a directory. |
| Live streams | Sit in the sabha from anywhere |
| Member discount | Ignore the queue. Keep the rite. *(assist, not skip-the-line)* |
| See it like a local | See it like family, not a tourist |
| You see rates… | You see the aarti. The internet sees a thumbnail. |

One line of serif, then the product. If the headline could sit on a temple trust website, it is too polite.

---

## Shape, space, depth

| Token | Value | Where |
|---|---|---|
| `radius.card` | 28dp | Photo cards, service tiles |
| `radius.sheet` | 32dp | Filters, Plans sheet |
| `radius.pill` | 999dp | Chips, search, composer |
| `radius.seal` | 50% | Pass tab mark, splash logo well |
| Side inset | 20–24dp | Never 16dp Material default on Home |
| Section gap | 28–36dp | Rails need air. Crew’s “cream-space” |
| Card peek | 28–40dp of next card | Horizontal rails *must* leak the next item |
| Shadow | 8–16dp blur, 4–8% black, y=6 | Soft lift on linen. No harsh Material elevation |
| Hairline | 1dp gold @ 20–30% | Dark cards, pass edge |

**Glass:** `backdrop blur` ~20dp + ivory/cream @ 70–85% on the *dock* under a photo card (Crew’s Passes / Flights / Stay / Dining bar). Do not frost the whole screen.

**Notch:** the Crew dock has a small center bite into the photo. Worth copying once on the **mandir hero card**. If it is expensive in Compose, a clean attached pill is fine; the attached-ness matters more than the bite.

**Overlap:** circular portraits sit 16–24dp over the top edge of a guide/pandit card. Live “now playing” avatars can stack the same way.

**Nothing flush to the home indicator.** Bottom nav / seal floats with padding. Crew’s orb never kisses the edge.

---

## Imagery

This is the product surface. Budget for photos the way Crew budgets for villas.

**Yes**

- Real mandir exteriors at blue hour (gopuram, shikhara, ghat)
- Sabha interior, lamps, stone floor, smoke as atmosphere not filter
- Crowd as scale, never as chaos-porn
- Hands with diya, feet on stone, prasad — cropped, respectful
- Painterly *illustrated* tiles for the six home services (same energy as Crew’s villa / jet / passport — scene, not icon)
- Local hosts / pandits: real faces, no stock “smiling guru”

**No**

- AI Vishnu with lens flare
- Random Om watermark
- Flat Material temple icon as the hero
- Stock Bali / generic “spiritual retreat”
- Watermarked tourist snaps

**Overlays:** bottom third of every photo card gets a dark gradient (transparent → 60–75% stone) so white type holds. Never thin white type on a bright sky.

**Honesty labels** on media: `Official temple stream` · `Recorded 360 · not live`. Crew is glossy; we can be glossy and still not lie.

Placeholder rule until assets exist: linen card + gold hairline + serif name is better than a stretch-blurred thumbnail. Never grey “image failed.”

---

## Motion

| Moment | Motion |
|---|---|
| Splash | Seal eases in, diya catches, one bell, 2.5s. Skippable after 0.8s. No bhajan loop |
| Home rails | Inertial horizontal scroll. Cards do not snap-fight the user |
| Live pill | Soft pulse on the vermillion dot, 1.2s, low amplitude |
| Pass seal | Slight press scale 0.96. Gold sheen is a gradient, not a spinning gif |
| Crew-style text ring | Optional on Home: `NEW DARSHAN · ASK CONCIERGE ·` rotating around the seal. Kill it if it feels like a casino |
| Screen change | Shared-axis or fade. No bounce. Entering Live/VR is a fade to black |
| Pay success | Diya, not confetti |
| Chat cards | Appear as incoming blocks with timestamp, not typing-dot theatre for too long |

If it wiggles constantly, it is not lively. It is anxious.

---

## Component patterns

Each pattern names the Crew screenshot, then the HINVR job.

### 1. Dark tactile splash

Ref: `03-splash-dark-logo-pill.jpg`

Centered vertical pill on charcoal. Logo stacked. Almost no UI. Ours: stone field, gold/cream **HINVR** (or a compact seal), diya or a thin amber bar as the only motion. 2.5 seconds. This is already close in code; keep Sanctum, do not put splash on linen.

### 2. Hero photo + floating status card

Ref: `06-hero-photo-status-card.jpg`, `09-home-bento-grid.jpg`

Full-bleed photo (balloons → **ghat / gopuram / aarti**). Serif line over it. White rounded card docks on the photo: **“Tirupati · LIVE”** / “Updated 12 min ago” / clipboard-or-list glyph. That card is the member’s *current rite or open request*, not a flight PNR.

Header: wordmark left, two circular stone icon buttons right (Pass wallet, Profile) — same as Crew’s briefcase + person.

### 3. Bento service grid

Ref: `07-service-grid-illustrations.jpg`, `09-home-bento-grid.jpg`

Asymmetric rounded tiles with **illustrations**, serif name, one-line benefit in sans.

| Tile | Benefit line |
|---|---|
| Live Darshan | Aarti, from the sabha |
| VR Darshan | 360. Move your phone |
| Priority Pass | Show this at the desk |
| Book Pandit | One city. Verified |
| Concierge | Ask. Then a human |
| Yatra | Club desk, not a portal |

Uneven heights (Crew’s Stays tall / Visa square). Do **not** use the current 88dp two-column text boxes.

### 4. Portrait photo card + glass dock

Ref: `01-event-carousel-glass-nav.jpg`, `04-local-guides-overlap-cards.jpg`

Horizontal rail of tall rounded photos. Title + `PLACE · MONTH` on a bottom scrim. Attached glass bar with **four actions for that mandir**:

`Live` · `VR` · `Pass` · `Assist`

Active item is darker/bolder. This replaces a generic “open detail” tap as the only move. Tapping the photo still opens Temple detail.

Use on Home (“Happening in the sabha”) and optionally on a featured mandir.

### 5. Exclusive rate / collection rails

Ref: `05-exclusive-photo-carousels.jpg`, `08-rate-cards-price-overlay.jpg`

Serif section title, then tall photo cards:

```
TIRUPATI
Sri Venkateswara
Pass accepted
Assist from the desk
```

Or yatra: place eyebrow, serif name, “Member fare from ₹…”, struck “typical online ₹…”. Same hierarchy as W Goa. Do not invent fake VIP lane prices.

### 6. People cards (hosts, pandits, “see it like family”)

Ref: `04-local-guides-overlap-cards.jpg`

Ivory card, circular photo overlapping the top, flag **or** small deity-mark (subtle), city caps, name bold, bio with a **vertical saffron quote rule**. Section title: “See it like family, not a tourist.”

### 7. Concierge as visual chat

Ref: `02-concierge-chat-photo-cards.jpg`

Not iMessage bubbles as the default.

- Facts (dates, dress, timings) → ivory structured card
- A mandir / yatra / pooja suggestion → **photo card** with serif title + circular arrow
- Hours / disclaimer → small floating pill (“Specialists 8am–10pm IST”)
- Composer: circle `+` · pill “Ask about puja, dates, dress…” · mic
- Header: close, title of thread, **call** (Platinum)

`screens.md` already says iMessage-style. Visual *cards inside* the thread are how it stays lively. Saffron send is fine; the cards do the work.

### 8. Pass as the metal object

Crew’s circular **CR EW** seal is the brand. Ours is the **Pass**.

- Tab icon when selected: raised circular seal (gold on vermillion-stone gradient), stacked **HI / NV** or a compact diya-mark — not a Material credit-card outline
- Pass screen (Sanctum): boarding-pass card, big QR, name, tier, id. Photography optional as a faint gopuram in the stone, never busy
- Non-member: same chrome, frosted lock, “Your name on a temple pass.”

### 9. Wide club CTA

Ref: `07-service-grid-illustrations.jpg` bottom pill

Ivory pill, ink text: **Custom request for HINVR** (serif emphasis on **HINVR**). Opens Concierge composer. Use on Home below the grid *in addition to* tabs — Crew’s “Custom travel request for CREW.”

---

## Screen translations

IA and gates stay as `screens.md`. This is look-and-feel only.

| Screen | Atmosphere | Build it like |
|---|---|---|
| **Splash** | Sanctum | Pattern 1. Diya + seal. No buttons |
| **Onboarding** | Sabha (pages) with one Sanctum card mock | Full-bleed photo per page, serif line, skip text, saffron Continue |
| **Phone / OTP** | Sabha | Big digits, gold underline, quiet legal. Not a dark cave unless we want login to feel like the vault — prefer linen so OTP is easy at noon |
| **Profile setup** | Sabha | One screen, large chips for Me / Parents / Family |
| **Home** | Sabha, hero may be photo | Patterns 2 + 3 + 4 + 9. Live rail is photo chips with LIVE pill, never a text empty-state as the hero |
| **Notifications** | Sabha | Rows, not cards. Empty: serif “We will remind you before aarti.” |
| **Mandirs** | Sabha | Search pill, chips, **photo** directory cards (name, place, LIVE/VR/PASS badges). Filters sheet 32dp |
| **Temple detail** | Hero Sanctum photo, body Sabha | Pattern 4 dock becomes the action row. Honest timings. Assist CTA is copy-safe |
| **Live list** | Sabha | Same cards, LIVE first |
| **Live player** | Sanctum | Full bleed video. Minimal chrome. Official-stream pill. Donate later |
| **VR list / player** | Sabha list, Sanctum player | 360 honesty label. Preview then Plans |
| **Pass** | Sanctum | Pattern 8. Expensive even when waitlisted |
| **How the pass works** | Sabha | Three quiet steps. Footer sentence from `screens.md` stays |
| **Concierge** | Sabha | Pattern 7. Quick asks as ivory chips. Calendar as a second section, not a dump |
| **Plans** | Sabha, Gold card slightly Sanctum | Three club cards, Gold recommended, serif prices. Fine print on assist |
| **Pay success** | Sanctum | Diya. “Your pass is ready.” |
| **Profile** | Sabha | Grouped ivory rows. Photo encouraged because it prints on the pass |
| **Pooja / Yatra** | Sabha | Pattern 5 rails. Club desk, not MakeMyTrip |

**Bottom nav (all Main tabs)**

Linen or translucent ivory blur on Sabha screens; stone on Pass. Selected Pass = seal. Other tabs: line icons, gold when selected, inkMuted when not. Do not use the current all-dusk bar on a cream Home — it will look like two apps stacked.

---

## Empty, error, gated

From `screens.md`, restated visually:

- Empty live rail: next aarti as a **photo card with a clock**, not a sentence in muted cream
- No search hits: serif line + “Ask Concierge — we will add it.”
- Stream down: still. Temple name. “The feed is the temple’s, not ours.”
- Gated tap: Plans sheet with the **reason** in serif (“VR darshan is included in Gold”), never a toast
- Loading: linen skeletons with 28dp radii, not circular spinners in the hero

---

## Gap vs the app today

The skeleton is the right *graph* and the wrong *surface*.

| Now | Bible |
|---|---|
| Entire main app is dusk stone | Sabha linen for storefront, Sanctum for rite |
| Home = 88dp labeled boxes | Bento illustrations + photo rails |
| Nearby = brown rectangles + a gold dot | Portrait photo cards with peek |
| Bottom bar = Material dark nav | Light blur + Pass as seal |
| Type = system serif/sans | Fraunces + Figtree (or the pair above) |
| No photography | Photography is the UI |

P0 in `screens.md` can stay “openable in an emulator.” It does not feel like a company until Home matches patterns 2–4.

---

## Compose notes (when we implement)

- Tokens: keep `HinvrColors`; add `linen`, `ivory`, `inkMuted`. Do not drive Sabha off `MaterialTheme.colorScheme.background` if that stays dusk.
- Shapes: `RoundedCornerShape(28.dp)` as default card; 16dp is too Material for this brand.
- Fonts: download Fraunces + Figtree into `res/font`, wire in `Type.kt`. Hindi: Tiro Devanagari.
- Blur: `Modifier.blur` / `RenderEffect` for docks; degrade to 85% ivory if API is old.
- Images: Coil, `ContentScale.Crop`, always a scrim modifier on text-on-photo.
- Do not add a fifth tab. Raise the Pass item.

---

## Decision log

Change colors here if the brand shifts. Do not silently restyle screens without updating this file.

| Date | Decision |
|---|---|
| 2026-09-06 | Crew is the liveliness reference. Palette stays temple (linen / stone / gold / fire), not Crew navy-burgundy. Dual atmosphere: Sabha + Sanctum. Four tabs remain; Pass is the seal. |

When someone asks “should this look like Crew?” the answer is: **same pulse, our gods, our card.**
