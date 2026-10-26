"""Shrinks the textures inside sanctum.glb without touching its geometry.

Run from anywhere (Python 3 with Pillow):
    python quest/tools/mandir/optimize_glb.py quest/app/src/main/assets/mandir/sanctum.glb

What it does, and nothing else:
  * drops images that are pixel-identical to an earlier one and points their textures at it
    (Blender exported the lantern's diffuse twice, once as JPEG and once as PNG);
  * resizes every image to at most 1024 px on its longest side and re-encodes it as JPEG
    (normal maps keep full 4:4:4 chroma so their vectors don't smear);
  * rewrites the binary chunk so every buffer view stays 4-byte aligned.

Meshes, accessors, nodes, materials and texture transforms are copied byte for byte, so the
markers in markers.json still line up with the room. Check the result with:
    npx @gltf-transform/cli validate quest/app/src/main/assets/mandir/sanctum.glb

Last run (October 2026): 35,438,320 -> 6,657,332 bytes; 25 -> 24 images; texture memory as
reported by `gltf-transform inspect` 391 MB -> 134 MB. Accessor data, meshes, nodes and materials
were byte-identical before and after, and `validate` reported the same (pre-existing) warnings and
no errors. KTX2 would cut GPU memory further but needs `toktx`, which isn't installed here.
"""

import io
import json
import pathlib
import struct
import sys

from PIL import Image

MAX_SIZE = 1024
QUALITY = 88


def read_glb(path: pathlib.Path):
    data = path.read_bytes()
    magic, version, _ = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67 or version != 2:
        raise SystemExit(f"{path} is not a glTF 2.0 binary")
    json_len, json_type = struct.unpack_from("<II", data, 12)
    if json_type != 0x4E4F534A:
        raise SystemExit("first chunk is not JSON")
    doc = json.loads(data[20:20 + json_len])
    bin_at = 20 + json_len
    bin_len, bin_type = struct.unpack_from("<II", data, bin_at)
    if bin_type != 0x004E4942:
        raise SystemExit("second chunk is not BIN")
    return doc, data[bin_at + 8:bin_at + 8 + bin_len]


def view_bytes(doc, blob, index):
    view = doc["bufferViews"][index]
    start = view.get("byteOffset", 0)
    return blob[start:start + view["byteLength"]]


def is_normal_map(doc, image_index):
    for material in doc.get("materials", []):
        normal = material.get("normalTexture")
        if normal and doc["textures"][normal["index"]]["source"] == image_index:
            return True
    return False


def encode(image: Image.Image, normal: bool) -> bytes:
    image = image.convert("RGB")
    if max(image.size) > MAX_SIZE:
        scale = MAX_SIZE / max(image.size)
        size = (max(1, round(image.width * scale)), max(1, round(image.height * scale)))
        image = image.resize(size, Image.LANCZOS)
    out = io.BytesIO()
    image.save(out, "JPEG", quality=QUALITY, optimize=True, subsampling=0 if normal else 2)
    return out.getvalue()


def main(path: pathlib.Path) -> None:
    doc, blob = read_glb(path)
    images = doc.get("images", [])

    # Pixel-identical images collapse onto the first one.
    keep, remap, pixels = [], {}, []
    for index, entry in enumerate(images):
        decoded = Image.open(io.BytesIO(view_bytes(doc, blob, entry["bufferView"]))).convert("RGB")
        twin = next((k for k, seen in pixels if seen.size == decoded.size and seen.tobytes() == decoded.tobytes()), None)
        if twin is not None:
            remap[index] = remap[twin]
            continue
        remap[index] = len(keep)
        keep.append((index, decoded))
        pixels.append((index, decoded))

    image_views = {entry["bufferView"] for entry in images}
    new_views, chunks, offset = [], [], 0
    old_to_new_view = {}

    def append(payload: bytes, template: dict) -> int:
        nonlocal offset
        view = {k: v for k, v in template.items() if k not in ("byteOffset", "byteLength")}
        view["buffer"] = 0
        view["byteOffset"] = offset
        view["byteLength"] = len(payload)
        chunks.append(payload)
        pad = (-len(payload)) % 4
        chunks.append(b"\0" * pad)
        offset += len(payload) + pad
        new_views.append(view)
        return len(new_views) - 1

    for index, view in enumerate(doc["bufferViews"]):
        if index in image_views:
            continue
        old_to_new_view[index] = append(view_bytes(doc, blob, index), view)

    new_images = []
    for old_index, decoded in keep:
        entry = images[old_index]
        payload = encode(decoded, is_normal_map(doc, old_index))
        view_index = append(payload, {})
        new_images.append({
            **{k: v for k, v in entry.items() if k not in ("bufferView", "mimeType", "uri")},
            "bufferView": view_index,
            "mimeType": "image/jpeg",
        })

    for accessor in doc.get("accessors", []):
        if "bufferView" in accessor:
            accessor["bufferView"] = old_to_new_view[accessor["bufferView"]]
    for texture in doc.get("textures", []):
        texture["source"] = remap[texture["source"]]
    doc["images"] = new_images
    doc["bufferViews"] = new_views
    doc["buffers"] = [{"byteLength": offset}]

    json_bytes = json.dumps(doc, separators=(",", ":")).encode("utf-8")
    json_bytes += b" " * ((-len(json_bytes)) % 4)
    bin_bytes = b"".join(chunks)
    total = 12 + 8 + len(json_bytes) + 8 + len(bin_bytes)
    out = io.BytesIO()
    out.write(struct.pack("<III", 0x46546C67, 2, total))
    out.write(struct.pack("<II", len(json_bytes), 0x4E4F534A))
    out.write(json_bytes)
    out.write(struct.pack("<II", len(bin_bytes), 0x004E4942))
    out.write(bin_bytes)
    before = path.stat().st_size
    path.write_bytes(out.getvalue())
    print(f"{path.name}: {before} -> {total} bytes, images {len(images)} -> {len(new_images)}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    main(pathlib.Path(sys.argv[1]))
