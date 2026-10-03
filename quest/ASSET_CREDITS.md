# Mandir asset credits

Every file in the HINVR sanctum is either openly licensed or modeled by HINVR. Nothing is taken from temple websites, Google Street View, or photographs of a garbha griha.

`tools/mandir/fetch_assets.py` downloads the sources below and records each license from the source API in `tools/mandir/sources/credits.json`. `tools/mandir/build_sanctum.py` (Blender) builds and bakes the room. `tools/mandir/cut_audio.sh` cuts the sounds.

Anything marked CC BY or CC BY-SA must keep its credit in the headset's Legal panel (`MandirCredits` in `MandirPanels.kt`). Modified CC BY-SA files (the cut sounds and the resized courtyard) stay under CC BY-SA 4.0.

## Textures and models (Poly Haven, CC0)

| Asset | Used for | Author | Source |
|---|---|---|---|
| granite_tile | Mandap floor | Charlotte Baglioni | https://polyhaven.com/a/granite_tile |
| sandstone_cracks | Pillars, plinths, threshold | Rob Tuytel | https://polyhaven.com/a/sandstone_cracks |
| red_sandstone_pavement | Mandap walls | Amal Kumar | https://polyhaven.com/a/red_sandstone_pavement |
| red_sandstone_tiles | Garbha griha | Amal Kumar | https://polyhaven.com/a/red_sandstone_tiles |
| marble_01 | Ceiling | Rob Tuytel | https://polyhaven.com/a/marble_01 |
| brass_diya_lantern | Hanging lamps | Bhargav Kubal | https://polyhaven.com/a/brass_diya_lantern |
| brass_pot_01 | Kalash at the threshold | Rico Cilliers | https://polyhaven.com/a/brass_pot_01 |
| brass_vase_03 | Lota on the offering stand | Rico Cilliers | https://polyhaven.com/a/brass_vase_03 |
| bananas | Naivedya | Alexander Shulha | https://polyhaven.com/a/bananas |
| food_pomegranate_01 | Naivedya | Oliver Harries | https://polyhaven.com/a/food_pomegranate_01 |

## Sound and image (Wikimedia Commons)

| File | Used for | Author | License | Source |
|---|---|---|---|---|
| Ghonto (holophonic).flac | Hanging ghanta, aarti peal | Subhashish Panigrahi | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Ghonto_(holophonic).flac |
| Conch shell.ogg | Shankh | Dbolton | CC BY 2.5 | https://commons.wikimedia.org/wiki/File:Conch_shell.ogg |
| Ram Mandir 360.jpg | Courtyard outside the door, daylight in the bake | Gaurav Dhwaj Khadka | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Ram_Mandir_360.jpg |
| Bell-ring.flac | Aarti hand bell | qubodup | CC0 | https://commons.wikimedia.org/wiki/File:Bell-ring.flac |
| Sanskrit chanting for Aarti Puja Prayer - Female Voice.ogg | Aarti chant | Aarti Shri Radha Govind Dev Ji, Jaipur | CC0 | https://commons.wikimedia.org/wiki/File:Sanskrit_chanting_for_Aarti_Puja_Prayer_-_Female_Voice.ogg |
| Coins dropped in wooden moneybox.ogg | Daan peti | ezwa | Public domain | https://commons.wikimedia.org/wiki/File:Coins_dropped_in_wooden_moneybox.ogg |
| Cracking peanuts.ogg | Coconut (slowed, lowered) | stephan | Public domain | https://commons.wikimedia.org/wiki/File:Cracking_peanuts.ogg |
| Bones breaking wood fire ice crackling.ogg | Flame crackle, lighting | stephan | Public domain | https://commons.wikimedia.org/wiki/File:Bones_breaking_wood_fire_ice_crackling.ogg |

## Modeled by HINVR

The mandap, pillars, garbha griha, Shiva lingam and yoni pitha, ghanta, diya, aarti thali, agarbatti and stand, marigold, coconut, laddoo, kumkum bowl, and daan peti are built procedurally in `build_sanctum.py`, textured with the CC0 materials above.

The sanctum ambience (`res/raw/sanctum_loop.m4a`) and the welcome bell (`res/raw/temple_bell.mp3`) were already in the app before the mandir was added.
