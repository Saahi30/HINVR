"""Downloads every openly licensed source asset for the HINVR sanctum.

Run from anywhere:  python3 quest/tools/mandir/fetch_assets.py
Files land in quest/tools/mandir/sources/ (not committed). The license of each file is
written next to it in sources/credits.json, which build_sanctum.py and ASSET_CREDITS.md use.
"""

import json
import pathlib
import time
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent
SOURCES = ROOT / "sources"
AGENT = "HINVR-asset-fetch/1.0 (https://hinvr.app)"

# Poly Haven, CC0. Textures are used for the room; models are offering props.
POLY_TEXTURES = {
    "granite_tile": "floor",
    "sandstone_cracks": "pillars",
    "red_sandstone_pavement": "walls",
    "red_sandstone_tiles": "garbha griha",
    "marble_01": "ceiling",
}
POLY_MODELS = {
    "brass_diya_lantern": "hanging lamps in the mandap",
    "brass_pot_01": "kalash beside the threshold",
    "brass_vase_03": "lota on the offering stand",
    "bananas": "naivedya",
    "food_pomegranate_01": "naivedya",
}

# Wikimedia Commons. Licenses vary per file, so they are read from the API, not assumed.
COMMONS = {
    "Ghonto (holophonic).flac": ("audio/ghanta", "hanging ghanta"),
    "Bell-ring.flac": ("audio/aarti_bell", "small aarti bell"),
    "Conch shell.ogg": ("audio/shankh", "shankh at the aarti"),
    "Sanskrit chanting for Aarti Puja Prayer - Female Voice.ogg": ("audio/aarti_chant", "aarti chant"),
    "Coins dropped in wooden moneybox.ogg": ("audio/coins", "daan peti"),
    "Cracking peanuts.ogg": ("audio/crack", "coconut"),
    "Bones breaking wood fire ice crackling.ogg": ("audio/crackle", "diya and agarbatti flame"),
    "Ram Mandir 360.jpg": ("images/courtyard_360", "courtyard outside the mandap door"),
}


def get(url: str) -> bytes:
    for attempt in range(5):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": AGENT})
            with urllib.request.urlopen(request, timeout=120) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == 4:
                raise
            time.sleep(10 * (attempt + 1))
    raise RuntimeError(url)


def save(path: pathlib.Path, url: str) -> None:
    if path.exists() and path.stat().st_size > 0:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(get(url))


def poly_author(asset: str) -> str:
    info = json.loads(get(f"https://api.polyhaven.com/info/{asset}"))
    return ", ".join(info.get("authors", {}).keys())


def fetch_poly_texture(asset: str, credits: list) -> None:
    files = json.loads(get(f"https://api.polyhaven.com/files/{asset}"))
    for key, suffix in (("Diffuse", "diff"), ("nor_gl", "nor_gl"), ("Rough", "rough")):
        url = files[key]["2k"]["jpg"]["url"]
        save(SOURCES / "textures" / asset / f"{asset}_{suffix}_2k.jpg", url)
    credits.append({
        "file": f"textures/{asset}",
        "title": asset,
        "use": POLY_TEXTURES[asset],
        "author": poly_author(asset),
        "license": "CC0",
        "source": f"https://polyhaven.com/a/{asset}",
    })


def fetch_poly_model(asset: str, credits: list) -> None:
    files = json.loads(get(f"https://api.polyhaven.com/files/{asset}"))
    gltf = files["gltf"]["1k"]["gltf"]
    folder = SOURCES / "models" / asset
    save(folder / pathlib.Path(urllib.parse.urlparse(gltf["url"]).path).name, gltf["url"])
    for relative, entry in gltf["include"].items():
        save(folder / relative, entry["url"])
    credits.append({
        "file": f"models/{asset}",
        "title": asset,
        "use": POLY_MODELS[asset],
        "author": poly_author(asset),
        "license": "CC0",
        "source": f"https://polyhaven.com/a/{asset}",
    })


def strip_html(value: str) -> str:
    out, inside = [], False
    for ch in value:
        if ch == "<":
            inside = True
        elif ch == ">":
            inside = False
        elif not inside:
            out.append(ch)
    return " ".join("".join(out).split())


def fetch_commons(credits: list) -> None:
    titles = ["File:" + name for name in COMMONS]
    query = urllib.parse.urlencode({
        "action": "query",
        "prop": "imageinfo",
        "iiprop": "url|extmetadata",
        "format": "json",
        "titles": "|".join(titles),
    })
    pages = json.loads(get(f"https://commons.wikimedia.org/w/api.php?{query}"))["query"]["pages"]
    for page in pages.values():
        name = page["title"].removeprefix("File:")
        stem, use = COMMONS[name]
        info = page["imageinfo"][0]
        meta = info.get("extmetadata", {})
        url = info["url"]
        extension = pathlib.Path(urllib.parse.urlparse(url).path).suffix.lower()
        save(SOURCES / f"{stem}{extension}", url)
        time.sleep(2)
        credits.append({
            "file": f"{stem}{extension}",
            "title": name,
            "use": use,
            "author": strip_html(meta.get("Artist", {}).get("value", "")),
            "license": meta.get("LicenseShortName", {}).get("value", ""),
            "source": info["descriptionurl"] if "descriptionurl" in info else
            "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(page["title"].replace(" ", "_")),
        })


def main() -> None:
    credits: list = []
    for asset in POLY_TEXTURES:
        fetch_poly_texture(asset, credits)
    for asset in POLY_MODELS:
        fetch_poly_model(asset, credits)
    fetch_commons(credits)
    (SOURCES / "credits.json").write_text(json.dumps(credits, indent=2, ensure_ascii=False))
    print(f"{len(credits)} sources in {SOURCES}")


if __name__ == "__main__":
    main()
