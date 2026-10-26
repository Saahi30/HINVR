# Mandir asset credits

Every file in the HINVR sanctum is either openly licensed or modeled by HINVR. Nothing is taken from temple websites, Google Street View, or photographs of a garbha griha. One photograph is involved: a crop of a CC BY-SA 360 photo of the Ram Mandir courtyard is the view through the mandap's main door, behind the member (below).

`tools/mandir/fetch_assets.py` downloads the sources below and records each license from the source API in `tools/mandir/sources/credits.json`. `tools/mandir/build_sanctum.py` (plain Python with numpy and Pillow, no Blender) builds the room, lights it per vertex and writes the GLBs; `tools/mandir/preview.html` shows the result in a browser. `tools/mandir/cut_audio.sh` cuts the sounds.

Anything marked CC BY or CC BY-SA must keep its credit in the headset's Credits panel (`MandirCredits` / `CreditsPanel` in `Credits.kt`, text in `res/values/strings.xml` → `credits_sanctum_body`). The panel opens from the pairing screen and from the profile, so it needs no account. Modified CC BY-SA files (the cut ghanta) stay under CC BY-SA 4.0.

The build embeds only the diffuse map of each texture, as JPEG (2048 px for the floor and walls, 1024 px for the rest); the room is unlit, so normal and roughness maps are not shipped.

## Textures and models (Poly Haven, CC0)

| Asset | Used for | Author | Source |
|---|---|---|---|
| granite_tile | Mandap floor | Charlotte Baglioni | https://polyhaven.com/a/granite_tile |
| sandstone_cracks | Pillars, beams, mouldings, door frames, ledge | Rob Tuytel | https://polyhaven.com/a/sandstone_cracks |
| red_sandstone_pavement | Mandap walls | Amal Kumar | https://polyhaven.com/a/red_sandstone_pavement |
| red_sandstone_tiles | Garbha griha | Amal Kumar | https://polyhaven.com/a/red_sandstone_tiles |
| marble_01 | Ceiling, aisle, garbha floor | Rob Tuytel | https://polyhaven.com/a/marble_01 |
| brass_diya_lantern | Hanging lamps | Bhargav Kubal | https://polyhaven.com/a/brass_diya_lantern |
| brass_pot_01 | Purna kalash at the door and on the ledge | Rico Cilliers | https://polyhaven.com/a/brass_pot_01 |

## Sound and image (Wikimedia Commons)

| File | Used for | Author | License | Source |
|---|---|---|---|---|
| Ghonto (holophonic).flac | Hanging ghanta, aarti peal | Subhashish Panigrahi | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Ghonto_(holophonic).flac |
| Conch shell.ogg | Shankh | Dbolton | CC BY 2.5 | https://commons.wikimedia.org/wiki/File:Conch_shell.ogg |
| Ram Mandir 360.jpg | The courtyard seen through the main door: a 90° crop, slightly blurred, inside `sanctum.glb` (material `outside`). The adapted image is shared under CC BY-SA 4.0. | Gaurav Dhwaj Khadka | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Ram_Mandir_360.jpg |
| Bell-ring.flac | Aarti hand bell; also the bell that greets the member at launch | qubodup | CC0 | https://commons.wikimedia.org/wiki/File:Bell-ring.flac |
| Sanskrit chanting for Aarti Puja Prayer - Female Voice.ogg | Aarti chant; also the soft ambience under the menu hall | Aarti Shri Radha Govind Dev Ji, Jaipur | CC0 | https://commons.wikimedia.org/wiki/File:Sanskrit_chanting_for_Aarti_Puja_Prayer_-_Female_Voice.ogg |
| Coins dropped in wooden moneybox.ogg | Daan peti | ezwa | Public domain | https://commons.wikimedia.org/wiki/File:Coins_dropped_in_wooden_moneybox.ogg |
| Cracking peanuts.ogg | Coconut (slowed, lowered) | stephan | Public domain | https://commons.wikimedia.org/wiki/File:Cracking_peanuts.ogg |
| Bones breaking wood fire ice crackling.ogg | Flame crackle, lighting | stephan | Public domain | https://commons.wikimedia.org/wiki/File:Bones_breaking_wood_fire_ice_crackling.ogg |

## Modeled by HINVR

The mandap, turned pillars, lantern ceiling and lotus pendant, garbha doorway and marigold toran, deepastambha and garbha lamps, Shiva lingam and yoni pitha, abhishek pot, prabhavali, puja stand and side table, jyoti, daan peti, rangoli, ghanta, diya, aarti thali, agarbatti, marigold, lotus, coconut, laddoo, kumkum bowl, coin, japa mala, copper lota, deepmala tray, hibiscus, modak and the small deepmala flame are built procedurally in `build_sanctum.py`. The room is written twice from one lighting pass: `sanctum.glb` (evening) and `sanctum_day.glb` (day), textured with the CC0 materials above or plain colours.

## Removed for lack of a licence

- `res/raw/sanctum_loop.m4a` (16 MB ambience) and `res/raw/temple_bell.mp3` (welcome bell): no source or licence in the repo, `tools/mandir`, or git history (both arrived in one bulk commit, 72091d6). Deleted; the chant and Bell-ring.flac above replace them.
- `res/drawable-nodpi/ganesha_stencil.jpg` (splash): deity clipart with no recorded source. Deleted; the splash seal (lamp over a lotus) is drawn in Compose in `SanctumOpening.kt`.
- `assets/mandir/courtyard.jpg`: the courtyard 360 panel was never shown, so the resized photo is no longer shipped as its own file (a crop now lives inside `sanctum.glb`, above).
- `assets/mandir/props/bananas.glb`, `pomegranate.glb`: never placed in the sanctum; removed with their sources.
- `res/drawable-nodpi/sanctum_hall.png`: unused.

## Needs an owner's answer

- `res/drawable-nodpi/sanctum_panorama.webp` (menu hall backdrop, converted from PNG): a lamp-lit pillared hall with no deity and no real temple. Its origin is not recorded anywhere in the repo (it arrived in commit 72091d6 with the files above). It looks generated. Record where it came from, or replace it.

## Card art

The temple and state card images (`res/drawable-nodpi/temple_*`, `state_*`) are unchanged; `design/asset_sources.md` records them as generated for the project. They are JPEG data under `.png` names, which is why release PNG crunching is off. `res/raw/photo_credits.json` is empty; the headset's Credits panel lists any entries added there (resource, title, author, license, licenseUrl, sourceUrl, modified) on extra pages and shows nothing extra while it is empty. Mandirs with no bundled card show a typographic card (stone, gold hairline, serif name), never another temple's picture.
