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

- Canvas: ochre paper `#DCC99A` (token `linen`) under a text-free paper grain
- Ink: temple brown `#4A2F1F` (`ink`)
- Quiet text: warm grey-brown `#7A6454` (`inkMuted`)
- Cards: ivory `#F3EEE2` (`ivory`) with a gold hairline, floating on the paper
- Accents from photography; chrome stays clay + gold line, used sparingly

This is where Crew’s liveliness lives. **Default new browse screens to Sabha unless they are immersive.**

### Sanctum (dark) — the rite

Used for: Splash, Live player, VR player, Pass (the card itself), Pay success, maybe Plans comparison if it should feel like a vault.

- Canvas: temple stone `#4A2F1F` / `#3A241C` (`dusk` / `duskDeep`)
- Type: cream `#F3EEE2`
- Light: antique gold, diya amber; gold for the one primary action
- Photography still full-bleed; gold hairline, not cream cards stacked in a cave

Splash matches Crew’s dark pill: one object, tactile, almost nothing else.

### Switching

Going Home → Live player is linen → night. That cut should feel like entering the mandap, not like a theme bug. Status bar: dark icons on Sabha, light icons on Sanctum.

---

## Color

`app/.../ui/theme/Color.kt` is the source of truth. The table below is what ships. The palette moved from bright linen + saffron to an **ochre manuscript sheet + temple clay**: quieter, older, closer to a printed invitation than a lifestyle app. Gold is linework, not fill.

| Token (`HinvrColors`) | Kotlin name | Hex | Role |
|---|---|---|---|
| `linen` | `Paper` | `#DCC99A` | Sabha canvas: the ochre sheet, under a text-free paper grain |
| `ivory` / `cream` / `flame` | `Surface` | `#F3EEE2` | Raised cards on Paper; text on Sanctum |
| `creamMuted` | `CreamMuted` | `#D4CBBA` | Secondary text on Sanctum |
| `ink` / `dusk` | `Ink` (`TempleStone`) | `#4A2F1F` | Primary text on Sabha; Sanctum canvas |
| `inkMuted` | `InkMuted` | `#7A6454` | Secondary text on Sabha |
| `duskDeep` | `TempleStoneDeep` | `#3A241C` | Deepest Sanctum field, scrims |
| `stone` / `stoneRaised` | `TempleStoneMid` / `TempleStoneLight` | `#5C4032` / `#6E5140` | Dark cards on Sanctum |
| `saffron` / `burgundy` | `Clay` | `#6E4A3C` | **Primary CTA**, selected chip, Live pill field |
| `saffronDeep` / `olive` | `ClayDeep` | `#4A322A` | Pressed CTA, deep clay |
| — | `ServiceClay` (+ `Lift` / `Deep`) | `#8A6756` | Bento service tiles and the bottom dock glass |
| — | `Terracotta` | `#A65336` | Material `primary`; sparing warm accent |
| `vermillion` | `Vermillion` | `#641A1D` | Seal, live dot, urgency — not error-only |
| `gold` / `goldDim` | `AntiqueGold` / `AntiqueGoldDim` | `#B08958` / `#8C6E45` | Hairlines, membership marks, selected tab |
| `amber` | `DiyaAmber` | `#C4A574` | Diya, success, warm glow |
| `photoWarmth` | `PhotoWarmth` | `#493225` | Warm grade over photography |

**Rules**

- One primary CTA per screen: clay on Sabha, gold on Sanctum.
- Gold is a 1dp line or a mark. Never a large fill.
- Never color-code deities or sampradaya.
- Live pill: clay field, vermillion-to-cream dot, cream “LIVE”. Not green-for-go. **Only for a feed the server confirms is live now.** Recordings get a static label (see Motion).
- Membership chip: gold hairline on a crisp 2dp label, never a Material “tonal” purple.
- Photography supplies teal, sky, marigold. UI chrome does not compete with it.

Crew’s palette is cream + navy + burgundy. Ours is **ochre paper + temple clay + gold line**. Same temperature, different religion.

---

## Type

`Type.kt` is the source of truth: **serif for display and headlines, sans for everything else**, from bundled font files, never system defaults.

**Faces (shipped, in `res/font`)**

| Role | Face | Use |
|---|---|---|
| Display / headlines (English) | **Fraunces** variable, weight 680, `SOFT` 50, `WONK` 0, `opsz` 72. Display is *italic* | “Sit in the sabha from anywhere.” Home rails. Plans headline |
| Display / headlines (Hindi) | **Noto Serif Devanagari**, weight 680, upright | Same role in `values-hi` |
| UI / body / prices (English) | **Figtree** 400 / 500 / 600 | Labels, buttons, member ID, ₹ |
| UI / body (Hindi) | **Noto Sans Devanagari** 400–700 | Same role in `values-hi` |
| Wordmark | Fraunces with wide tracking | **HINVR**, letterspaced, not a cheap stencil |

Only these four families ship. Do not add more display faces.

**Scale (phone, as shipped)**

| Style | Material slot | Size / line | Weight | Notes |
|---|---|---|---|---|
| Display | `displayLarge` | 52 / 56 | Bold *italic* serif, −0.8 tracking | One per screen, 1–2 lines, over Paper. Never all-caps |
| Headline | `headlineLarge` | 36 / 42 | Bold serif | Screen titles |
| Section title | `headlineMedium` | 26 / 32 | Bold serif | Left aligned, 22dp side inset |
| Card title | `titleLarge` | 22 / 28 | Bold serif | Ivory cards |
| Card title on photo | card title style | 18 / 22 | SemiBold sans, cream | With gradient scrim |
| Title | `titleMedium` | 16 / 22 | SemiBold sans | Rows, list titles |
| Body | `bodyLarge` / `bodyMedium` | 16 / 24, 14 / 20 | **SemiBold** sans | Heavier than usual on purpose: it holds over the paper grain |
| Button | `labelLarge` | 14 / 20 | SemiBold sans | Sentence case. “Watch live darshan” not “SUBMIT” |
| Eyebrow | `labelSmall` | 11 / 16 | SemiBold sans, +1.2 tracking, caps | `TIRUPATI` · `LIVE NOW` |

Hindi drops the tracking and the italic. Devanagari does not slant.

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

Corners are **crisp, like a printed card**, not soft app bubbles. Softness is kept for things you hold or fill in: the pass, sheets, forms, the dock.

| Token | Value | Where |
|---|---|---|
| `HinvrCardRadius` | 4dp | Photo cards, mandir hero, ivory cards, status cards, selected plan |
| Service tile | 12dp | Bento service tiles |
| Label / chip | 2dp | Status labels, membership badge, filter chips (gold hairline) |
| Form surface | 18–20dp | Inputs, choice cards, visit-request steps |
| Pass object | 24dp | The pass card and its envelope |
| `HinvrSheetRadius` | 32dp | Bottom sheets, Plans gate sheet |
| `HinvrPillRadius` | 999dp | Search, composer, dock, Live pill, CTA pills |
| Seal | 50% | Pass tab seal, splash seal |
| `HinvrSideInset` | 22dp | Never 16dp Material default on Home |
| Section gap | 28–36dp | Rails need air |
| Card peek | 28–40dp of next card | Horizontal rails *must* leak the next item |
| Shadow | 8–12dp, 6–7% black | Soft lift on paper. No harsh Material elevation |
| Hairline | 1dp gold | Ivory cards, chips, pass edge. Full gold on light, 30–60% on dark |

**Glass:** the bottom dock is `ServiceClay` @ 90% over a 16dp backdrop blur, with a 1dp cream hairline @ 34%. 62dp tall, floating pill. It is clay on every tab, Sabha included: the dock is the one dark object on the paper, the way the pass seal is. Do not frost the whole screen.

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
- Local hosts / pandits: real faces of real people, with their consent, no stock “smiling guru”

**No**

- AI Vishnu with lens flare, or any generated deity, murti or stencil
- Generated pictures of a **real, named temple**. A member who has stood at Somnath will see the lie
- Generated “script” or texture with letters in it. Fake Devanagari reads as gibberish to every Hindi reader
- Invented people (hosts, pandits, testimonials) presented as real
- Random Om watermark
- Flat Material temple icon as the hero
- Stock Bali / generic “spiritual retreat”
- Watermarked tourist snaps
- Photos of the garbha griha or murti

**Sourcing rule.** Named places are real, licensed photographs (Wikimedia Commons CC0 / CC BY / CC BY-SA, or our own shoot), credited in each app’s `res/raw/photo_credits.json` (`resource`, `title`, `author`, `license`, `licenseUrl`, `sourceUrl`, `modified`), which the phone’s Legal screen and the headset credits read. Generated imagery is allowed only for the service illustrations and abstract textures (paper, metal), never with text, deities or real places in it, and is logged in [`design/asset_sources.md`](design/asset_sources.md).

**Overlays:** bottom third of every photo card gets a dark gradient (transparent → 60–75% stone) so white type holds. Never thin white type on a bright sky.

**Honesty labels** on media: `Official temple stream` · `Recorded 360 · not live`. Crew is glossy; we can be glossy and still not lie.

Placeholder rule until assets exist: ivory card on paper + gold hairline + serif name is better than a stretch-blurred thumbnail. Never grey “image failed.”

---

## Motion

| Moment | Motion |
|---|---|
| Splash | Seal eases in, diya catches, one bell, 2.5s. Skippable after 0.8s. No bhajan loop |
| Home rails | Inertial horizontal scroll. Cards do not snap-fight the user |
| Live pill | The dot breathes 0.82 → 1.22 scale over 0.9s, reversing. That is the only constantly moving thing on a screen, and it appears only when a feed is live now |
| Recorded label | `RECORDED 360 · NOT LIVE` is a static label (2dp, gold hairline). It never pulses and never uses the Live pill |
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
| VR Darshan | Recorded 360, honestly labelled |
| Priority Assist | Show this at the desk |
| Book Pandit | One city. Verified |
| Concierge | Ask. Then a human |
| Yatra | Club desk, not a portal |

Uneven heights (Crew’s Stays tall / Visa square): honour each tile’s `tall` flag. 12dp corners, `ServiceClay` ombré.

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
| **Account** | Sabha | Email and password, gold underline. Forgot password returns through the recovery link. Paper, easy to read at noon |
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

A floating clay-glass pill on every tab (see **Shape → Glass**). Selected Pass = the raised seal. Other tabs: line icons, gold when selected, cream at reduced alpha when not; labels show on all tabs from 376dp wide, on the active tab only below that. Icons are drawn for this brand. A Material bank icon (`AccountBalance`) is not a mandir.

---

## Empty, error, gated

From `screens.md`, restated visually:

- Empty live rail: next aarti as a **photo card with a clock**, not a sentence in muted cream
- No search hits: serif line + “Ask Concierge — we will add it.”
- Stream down: still. Temple name. “The feed is the temple’s, not ours.”
- Gated tap: Plans sheet with the **reason** in serif (“VR darshan is included in Gold”), never a toast
- Loading: paper skeletons at the component’s own radius (4dp cards, 12dp tiles), not circular spinners in the hero
- Failed send: say so, keep the request, offer retry. Never show “Sent” for something the desk did not receive
- Offline pass: keep the last good code and say until when it is valid. Never blank the pass because the network blinked

---

## Honesty in content

The same rule as the labels, applied to everything on screen:

- No invented threads, timestamps (“Updated 12 min ago”), hosts, reviews or member counts. If the server did not say it, the screen does not say it
- “Live” is a server fact, checked recently. Bundled data defaults to not live
- Desk-only instructions (“paste a 360 URL in the desk”) never reach a member
- Name things for what we sell: **Priority Assist**, not “Priority Pass” (a third-party trademark) and never “skip the line,” not even to deny it

---

## Compose notes

- Tokens: `HinvrColors` in `Theme.kt`; raw colors in `Color.kt`. Sabha reads `colors.linen` / `colors.ink`, never `MaterialTheme.colorScheme.background`.
- Shapes: `HinvrCardRadius` (4dp) for cards; the other radii are in **Shape, space, depth**. Don’t invent new ones per screen.
- Fonts: Fraunces + Figtree, Noto Serif / Sans Devanagari, wired in `Type.kt`.
- Blur: `clayGlass` (haze) for the dock; degrade to solid `ServiceClay` @ 90% if blur is unavailable.
- Images: Coil, `ContentScale.Crop`, always a scrim modifier on text-on-photo. Remote CMS photo first, bundled photo second, typographic placeholder last.
- Touch targets ≥ 48dp, even when the visible icon well is smaller.
- Do not add a fifth tab. Raise the Pass item.

---

## Decision log

Change colors here if the brand shifts. Do not silently restyle screens without updating this file.

| Date | Decision |
|---|---|
| 2026-09-06 | Crew is the liveliness reference. Palette stays temple (linen / stone / gold / fire), not Crew navy-burgundy. Dual atmosphere: Sabha + Sanctum. Four tabs remain; Pass is the seal. |
| 2026-10-26 | The bible adopts the shipped look. Sabha is an ochre manuscript sheet (`#DCC99A`) under a text-free paper grain, not bright linen. Saffron gives way to temple clay (`#6E4A3C`) as the primary CTA; gold becomes linework only. Cards go crisp (4dp, labels 2dp) like a printed invitation; softness stays on the pass (24dp), forms, sheets and pills. Display is Fraunces bold italic at 52sp; body is Figtree SemiBold so it holds over the grain. Hindi pairs Noto Serif / Sans Devanagari. The dock is clay glass on every tab. The Live pill keeps its 0.9s breathing dot but is reserved for server-confirmed live feeds. Named places must be real, credited photographs; generated deity and fake-script imagery is removed. **Open:** the temple and state card photos (`temple_*`, `state_*`) are still generated and must be replaced before public launch. “Priority Pass” is renamed **Priority Assist**. |

When someone asks “should this look like Crew?” the answer is: **same pulse, our gods, our card.**
