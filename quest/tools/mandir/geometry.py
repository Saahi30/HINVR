"""Small mesh toolkit for the sanctum: primitives, per-vertex lighting and a GLB writer.

Pure numpy + Pillow, so the room can be rebuilt on any laptop in a few seconds.
Space is headset space in metres: +X right, +Y up, -Z toward the garbha griha.
A mesh is a tuple (positions N×3, normals N×3, uvs N×2, triangles M×3).
"""

from __future__ import annotations

import io
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

TAU = 2.0 * math.pi


def _unit(v):
    v = np.asarray(v, dtype=np.float64)
    length = np.linalg.norm(v, axis=-1, keepdims=True)
    return v / np.maximum(length, 1e-12)


# ---------------------------------------------------------------- primitives


def quad(origin, u, v, cell: float, tile: float):
    """A gridded rectangle origin + s·u + t·v, facing u×v. UVs are world-planar, so neighbours line up."""
    origin, u, v = (np.asarray(x, dtype=np.float64) for x in (origin, u, v))
    lu, lv = np.linalg.norm(u), np.linalg.norm(v)
    nu, nv = max(1, math.ceil(lu / cell)), max(1, math.ceil(lv / cell))
    s, t = np.meshgrid(np.linspace(0, 1, nu + 1), np.linspace(0, 1, nv + 1), indexing="ij")
    p = (origin + s[..., None] * u + t[..., None] * v).reshape(-1, 3)
    n = np.repeat(_unit(np.cross(u, v))[None], len(p), axis=0)
    uv = np.stack([p @ (u / lu), -(p @ (v / lv))], axis=1) / tile
    i, j = np.meshgrid(np.arange(nu), np.arange(nv), indexing="ij")
    a = (i * (nv + 1) + j).ravel()
    b = a + nv + 1
    tris = np.concatenate([np.stack([a, b, b + 1], 1), np.stack([a, b + 1, a + 1], 1)])
    return p, n, uv, tris


def box(mn, mx, cell=0.25, tile=1.0, skip=()):
    """An axis-aligned box. [skip] names faces never seen ("+x", "-y", ...), to save vertices."""
    (x0, y0, z0), (x1, y1, z1) = mn, mx
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    faces = {
        "+x": ((x1, y0, z1), (0, 0, -dz), (0, dy, 0)),
        "-x": ((x0, y0, z0), (0, 0, dz), (0, dy, 0)),
        "+z": ((x0, y0, z1), (dx, 0, 0), (0, dy, 0)),
        "-z": ((x1, y0, z0), (-dx, 0, 0), (0, dy, 0)),
        "+y": ((x0, y1, z1), (dx, 0, 0), (0, 0, -dz)),
        "-y": ((x0, y0, z0), (dx, 0, 0), (0, 0, dz)),
    }
    return merge(*(quad(o, u, v, cell, tile) for name, (o, u, v) in faces.items() if name not in skip))


def lathe(profile, segments=24, center=(0, 0, 0), tile=1.0, normals2d=None, smooth=True, arc=(0.0, TAU), inward=False):
    """Revolve a (radius, height) profile, listed bottom to top along the outside, about +Y."""
    pts = np.asarray(profile, dtype=np.float64)
    if not smooth:
        # Each profile segment becomes its own band with a flat normal: crisp mouldings.
        parts = []
        for k in range(len(pts) - 1):
            a, b = pts[k], pts[k + 1]
            if np.allclose(a, b):
                continue
            t = b - a
            nrm = _unit([t[1], -t[0]])
            parts.append(lathe([a, b], segments, center, tile, normals2d=[nrm, nrm], arc=arc, inward=inward))
        return merge(*parts)
    if normals2d is None:
        prev = np.vstack([pts[:1], pts[:-1]])
        nxt = np.vstack([pts[1:], pts[-1:]])
        t = nxt - prev
        normals2d = _unit(np.stack([t[:, 1], -t[:, 0]], 1))
    normals2d = np.asarray(normals2d, dtype=np.float64)
    theta = np.linspace(arc[0], arc[1], segments + 1)
    r, y = pts[:, 0][:, None], pts[:, 1][:, None]
    sin, cos = np.sin(theta)[None], np.cos(theta)[None]
    p = np.stack([r * sin, np.broadcast_to(y, (len(pts), len(theta))), r * cos], -1).reshape(-1, 3) + center
    nr, ny = normals2d[:, 0][:, None], normals2d[:, 1][:, None]
    n = np.stack([nr * sin, np.broadcast_to(ny, (len(pts), len(theta))), nr * cos], -1).reshape(-1, 3)
    arclen = np.concatenate([[0], np.cumsum(np.linalg.norm(np.diff(pts, axis=0), axis=1))])
    ring = max(pts[:, 0].max(), 1e-3) * (arc[1] - arc[0])
    u = np.broadcast_to(((theta - arc[0]) / (arc[1] - arc[0]) * ring / tile)[None], (len(pts), len(theta)))
    v = np.broadcast_to((arclen / tile)[:, None], (len(pts), len(theta)))
    uv = np.stack([u, v], -1).reshape(-1, 2)
    cols = segments + 1
    j, k = np.meshgrid(np.arange(len(pts) - 1), np.arange(segments), indexing="ij")
    a = (j * cols + k).ravel()
    tris = np.concatenate([np.stack([a, a + 1, a + 1 + cols], 1), np.stack([a, a + 1 + cols, a + cols], 1)])
    if inward:
        n = -n
        tris = tris[:, ::-1]
    return p, n, uv, tris


def sphere(center, radius, segments=14, rings=8, scale=(1, 1, 1), tile=0.2):
    phi = np.linspace(0, math.pi, rings + 1)
    profile = np.stack([radius * np.sin(phi), -radius * np.cos(phi)], 1)
    normals = np.stack([np.sin(phi), -np.cos(phi)], 1)
    return scaled(lathe(profile, segments, (0, 0, 0), tile, normals2d=normals), scale, center)


def torus(center, major, minor, segments=24, rings=8, tile=0.2):
    a = np.linspace(-math.pi, math.pi, rings + 1)
    profile = np.stack([major + minor * np.cos(a), minor * np.sin(a)], 1)
    normals = np.stack([np.cos(a), np.sin(a)], 1)
    return lathe(profile, segments, center, tile, normals2d=normals)


def tube(p0, p1, radius, segments=8, tile=0.2, caps=False):
    """A cylinder between two points."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    length = float(np.linalg.norm(p1 - p0))
    profile = [(radius, 0.0), (radius, length)]
    if caps:
        profile = [(0.0, 0.0)] + profile + [(0.0, length)]
    mesh = lathe(profile, segments, (0, 0, 0), tile, smooth=not caps)
    return moved(mesh, align_y(p1 - p0), p0)


def disc(center, radius, segments=24, normal_up=True, tile=1.0):
    return lathe([(0.0, 0.0), (radius, 0.0)] if not normal_up else [(radius, 0.0), (0.0, 0.0)], segments, center, tile,
                 normals2d=[(0, 1), (0, 1)] if normal_up else [(0, -1), (0, -1)])


def chain(top, bottom, link=0.045, wire=0.006):
    """Alternating oval links hanging from [top] to [bottom]."""
    top, bottom = np.asarray(top, float), np.asarray(bottom, float)
    count = max(1, int(np.linalg.norm(top - bottom) / (link * 0.78)))
    parts = []
    for k in range(count):
        c = top + (bottom - top) * ((k + 0.5) / count)
        # A flat ring, stretched, stood upright; every other link turned a quarter.
        ring = scaled(torus((0, 0, 0), link * 0.32, wire, 10, 5), (1, 1, 1.7))
        parts.append(moved(ring, align_y(bottom - top) @ rot_y(90 * (k % 2)) @ rot_x(90), c))
    return merge(*parts)


# ---------------------------------------------------------------- transforms


def rot_x(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_y(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_z(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def align_y(direction):
    """Rotation taking +Y onto [direction]."""
    d = _unit(direction)
    y = np.array([0.0, 1.0, 0.0])
    axis = np.cross(y, d)
    s, c = np.linalg.norm(axis), float(y @ d)
    if s < 1e-9:
        return np.eye(3) if c > 0 else rot_x(180)
    k = axis / s
    kx = np.array([[0, -k[2], k[1]], [k[2], 0, -k[0]], [-k[1], k[0], 0]])
    return np.eye(3) + s * kx + (1 - c) * (kx @ kx)


def moved(mesh, rotation=None, offset=(0, 0, 0)):
    p, n, uv, t = mesh
    if rotation is not None:
        p = p @ np.asarray(rotation).T
        n = n @ np.asarray(rotation).T
    return p + np.asarray(offset, float), n, uv, t


def scaled(mesh, scale, offset=(0, 0, 0)):
    p, n, uv, t = mesh
    s = np.asarray(scale, float)
    return p * s + np.asarray(offset, float), _unit(n / s), uv, t


def merge(*meshes):
    meshes = [m for m in meshes if m is not None and len(m[0])]
    if not meshes:
        return np.zeros((0, 3)), np.zeros((0, 3)), np.zeros((0, 2)), np.zeros((0, 3), int)
    tris, base = [], 0
    for m in meshes:
        tris.append(m[3] + base)
        base += len(m[0])
    return (np.concatenate([m[0] for m in meshes]), np.concatenate([m[1] for m in meshes]),
            np.concatenate([m[2] for m in meshes]), np.concatenate(tris))


def jitter(mesh, amount, seed):
    """Lumpy surfaces (laddoo, marigold): push each vertex along its normal by a repeatable random amount."""
    p, n, uv, t = mesh
    rng = np.random.default_rng(seed)
    # Weld by position first so seams don't tear open.
    key = np.round(p, 5)
    _, inverse = np.unique(key, axis=0, return_inverse=True)
    push = rng.uniform(-amount, amount, inverse.max() + 1)[inverse.ravel()]
    return p + n * push[:, None], n, uv, t


def bounds(mesh):
    return mesh[0].min(axis=0), mesh[0].max(axis=0)


# ---------------------------------------------------------------- lighting


class Occluders:
    """What casts shadow and ambient occlusion: solid boxes and vertical columns."""

    def __init__(self):
        self.boxes: list[tuple[np.ndarray, np.ndarray]] = []
        self.columns: list[tuple[float, float, float, float, float]] = []

    def add_box(self, mn, mx):
        self.boxes.append((np.asarray(mn, float), np.asarray(mx, float)))

    def add_column(self, x, z, radius, y0, y1):
        self.columns.append((x, z, radius, y0, y1))

    def within(self, mn, mx):
        """Only the occluders touching the box mn..mx: each chunk of vertices tests what is near it."""
        near = Occluders()
        near.boxes = [(a, b) for a, b in self.boxes if np.all(b >= mn) and np.all(a <= mx)]
        near.columns = [c for c in self.columns
                        if c[0] + c[2] >= mn[0] and c[0] - c[2] <= mx[0] and c[1] + c[2] >= mn[2] and c[1] - c[2] <= mx[2]
                        and c[4] >= mn[1] and c[3] <= mx[1]]
        return near

    def blocked(self, origin, direction, length):
        """For N rays, whether anything solid lies within [length]."""
        hit = np.zeros(len(origin), bool)
        with np.errstate(divide="ignore", invalid="ignore"):
            inv = 1.0 / np.where(np.abs(direction) < 1e-9, 1e-9, direction)
            for mn, mx in self.boxes:
                # A hair smaller than drawn, so a vertex on a box's face isn't inside it.
                mn, mx = mn + 0.003, mx - 0.003
                t1 = (mn - origin) * inv
                t2 = (mx - origin) * inv
                near = np.minimum(t1, t2).max(axis=1)
                far = np.maximum(t1, t2).min(axis=1)
                hit |= (far >= np.maximum(near, 0.0)) & (near < length) & (far > 0.0)
            ox, oz = origin[:, 0], origin[:, 2]
            dx, dz = direction[:, 0], direction[:, 2]
            a = dx * dx + dz * dz
            for x, z, r, y0, y1 in self.columns:
                fx, fz = ox - x, oz - z
                b = 2 * (fx * dx + fz * dz)
                c = fx * fx + fz * fz - r * r
                disc_ = b * b - 4 * a * c
                ok = (disc_ >= 0) & (a > 1e-12)
                root = np.sqrt(np.maximum(disc_, 0))
                t0 = (-b - root) / (2 * np.maximum(a, 1e-12))
                y = origin[:, 1] + direction[:, 1] * t0
                hit |= ok & (t0 > 0) & (t0 < length) & (y >= y0) & (y <= y1)
        return hit


def _hemisphere(count, seed=7):
    rng = np.random.default_rng(seed)
    # Stratified cosine-weighted directions in a z-up frame.
    side = int(math.ceil(math.sqrt(count)))
    u, v = np.meshgrid((np.arange(side) + 0.5) / side, (np.arange(side) + 0.5) / side)
    u = (u.ravel() + rng.uniform(-0.2, 0.2, side * side) / side)[:count]
    v = (v.ravel() + rng.uniform(-0.2, 0.2, side * side) / side)[:count]
    r, phi = np.sqrt(u), TAU * v
    return np.stack([r * np.cos(phi), r * np.sin(phi), np.sqrt(np.maximum(0, 1 - u))], 1)


def light_vertices(p, n, occluders: Occluders, lights, ambient, ao_reach=0.75, ao_rays=16, chunk=3000):
    """Irradiance at each vertex: lamps with shadows, plus ambient scaled by occlusion. Linear RGB, unbounded.

    Also returns the occlusion (0 shut in .. 1 open) and, per light, the shadowed n·l/d² term,
    which the brass highlight pass reuses.
    """
    out = np.zeros((len(p), 3))
    ao_out = np.zeros(len(p))
    seen = np.zeros((len(lights), len(p)))
    dirs = _hemisphere(ao_rays)
    # Neighbouring vertices in the same chunk, so each chunk only tests the occluders around it.
    cell = np.floor(p / 0.8).astype(int)
    order = np.lexsort((cell[:, 1], cell[:, 2], cell[:, 0]))
    for s in range(0, len(p), chunk):
        idx_chunk = order[s:s + chunk]
        P, N = p[idx_chunk], n[idx_chunk]
        lo, hi = P.min(axis=0), P.max(axis=0)
        start = P + N * 0.012
        up = np.where(np.abs(N[:, 1:2]) > 0.9, [[1.0, 0, 0]], [[0, 1.0, 0]])
        T = _unit(np.cross(N, up))
        B = np.cross(N, T)
        near = occluders.within(lo - ao_reach, hi + ao_reach)
        open_ = np.zeros(len(P))
        for d in dirs:
            ray = T * d[0] + B * d[1] + N * d[2]
            open_ += ~near.blocked(start, ray, np.full(len(P), ao_reach))
        ao = open_ / len(dirs)
        ao_out[idx_chunk] = ao
        e = np.outer(ao, ambient)
        for k, light in enumerate(lights):
            at = np.asarray(light["at"], float)
            to = at - P
            dist = np.linalg.norm(to, axis=1)
            l = to / dist[:, None]
            ndotl = np.einsum("ij,ij->i", N, l)
            lit = ndotl > 0
            if light.get("shadows", True) and lit.any():
                idx = np.nonzero(lit)[0]
                path = occluders.within(np.minimum(lo, at), np.maximum(hi, at))
                lit[idx] = ~path.blocked(start[idx], l[idx], dist[idx] - light.get("radius", 0.05))
            term = np.where(lit, ndotl, 0.0) / (dist * dist + 0.06)
            seen[k, idx_chunk] = term
            e += np.outer(light["power"] * term, light["color"])
        out[idx_chunk] = e
    return out, ao_out, seen


def tonemap(e, exposure, contrast=1.0):
    """Soft shoulder so lamp hot spots never clip hard; [contrast] above 1 deepens the shadows."""
    return (1.0 - np.exp(-np.asarray(e) * exposure)) ** contrast


# ---------------------------------------------------------------- glTF


def jpeg(image: Image.Image, size: int, quality=86) -> bytes:
    image = image.convert("RGB")
    if max(image.size) > size:
        scale = size / max(image.size)
        image = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.LANCZOS)
    out = io.BytesIO()
    image.save(out, "JPEG", quality=quality, optimize=True)
    return out.getvalue()


def png(image: Image.Image) -> bytes:
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    return out.getvalue()


class Gltf:
    """Collects meshes, materials and images, then writes one .glb."""

    def __init__(self, generator="HINVR build_sanctum.py"):
        self.doc = {"asset": {"version": "2.0", "generator": generator}, "buffers": [], "bufferViews": [],
                    "accessors": [], "materials": [], "meshes": [], "nodes": [], "scenes": [{"nodes": []}], "scene": 0}
        self.blob = bytearray()
        self.material_index: dict[str, int] = {}
        self.image_index: dict[str, int] = {}

    def _view(self, data: bytes, target=None):
        while len(self.blob) % 4:
            self.blob.append(0)
        view = {"buffer": 0, "byteOffset": len(self.blob), "byteLength": len(data)}
        if target:
            view["target"] = target
        self.blob.extend(data)
        self.doc["bufferViews"].append(view)
        return len(self.doc["bufferViews"]) - 1

    def _accessor(self, array, kind, component, normalized=False, target=34962, minmax=False):
        array = np.ascontiguousarray(array)
        acc = {"bufferView": self._view(array.tobytes(), target), "componentType": component,
               "count": int(array.shape[0]), "type": kind}
        if normalized:
            acc["normalized"] = True
        if minmax:
            acc["min"] = array.min(axis=0).astype(float).tolist()
            acc["max"] = array.max(axis=0).astype(float).tolist()
        self.doc["accessors"].append(acc)
        return len(self.doc["accessors"]) - 1

    def image(self, key: str, data: bytes, mime="image/jpeg"):
        if key in self.image_index:
            return self.image_index[key]
        self.doc.setdefault("images", []).append({"name": key, "bufferView": self._view(data), "mimeType": mime})
        self.doc.setdefault("samplers", [{"magFilter": 9729, "minFilter": 9987, "wrapS": 10497, "wrapT": 10497}])
        self.doc.setdefault("textures", []).append({"sampler": 0, "source": len(self.doc["images"]) - 1})
        self.image_index[key] = len(self.doc["textures"]) - 1
        return self.image_index[key]

    def material(self, name, color=(1, 1, 1, 1), texture=None, metallic=0.0, roughness=0.8, unlit=False,
                 blend=False, double=False, emissive=None):
        if name in self.material_index:
            return self.material_index[name]
        pbr = {"baseColorFactor": [float(c) for c in (list(color) + [1.0])[:4]], "metallicFactor": metallic,
               "roughnessFactor": roughness}
        if texture is not None:
            pbr["baseColorTexture"] = {"index": texture}
        mat = {"name": name, "pbrMetallicRoughness": pbr}
        if unlit:
            mat["extensions"] = {"KHR_materials_unlit": {}}
            self.doc.setdefault("extensionsUsed", [])
            if "KHR_materials_unlit" not in self.doc["extensionsUsed"]:
                self.doc["extensionsUsed"].append("KHR_materials_unlit")
        if emissive is not None:
            mat["emissiveFactor"] = [float(c) for c in emissive]
        if blend:
            mat["alphaMode"] = "BLEND"
        if double:
            mat["doubleSided"] = True
        self.doc["materials"].append(mat)
        self.material_index[name] = len(self.doc["materials"]) - 1
        return self.material_index[name]

    def mesh(self, name, primitives):
        """primitives: list of (material index, mesh tuple, colors N×3/N×4 in 0..1 or None)."""
        prims = []
        for material, (p, n, uv, t), colors in primitives:
            if not len(p):
                continue
            attrs = {
                "POSITION": self._accessor(p.astype(np.float32), "VEC3", 5126, minmax=True),
                "NORMAL": self._accessor(_unit(n).astype(np.float32), "VEC3", 5126),
                "TEXCOORD_0": self._accessor(uv.astype(np.float32), "VEC2", 5126),
            }
            if colors is not None:
                c = np.asarray(colors, float)
                if c.shape[1] == 3:
                    c = np.hstack([c, np.ones((len(c), 1))])
                attrs["COLOR_0"] = self._accessor(np.round(np.clip(c, 0, 1) * 255).astype(np.uint8), "VEC4", 5121,
                                                  normalized=True)
            index_type = (np.uint16, 5123) if len(p) < 65536 else (np.uint32, 5125)
            indices = self._accessor(t.astype(index_type[0]).ravel(), "SCALAR", index_type[1], target=34963)
            prims.append({"attributes": attrs, "indices": indices, "material": material})
        self.doc["meshes"].append({"name": name, "primitives": prims})
        self.doc["nodes"].append({"name": name, "mesh": len(self.doc["meshes"]) - 1})
        self.doc["scenes"][0]["nodes"].append(len(self.doc["nodes"]) - 1)

    def write(self, path: Path):
        while len(self.blob) % 4:
            self.blob.append(0)
        self.doc["buffers"] = [{"byteLength": len(self.blob)}]
        body = json.dumps(self.doc, separators=(",", ":")).encode()
        body += b" " * ((-len(body)) % 4)
        total = 12 + 8 + len(body) + 8 + len(self.blob)
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "wb") as f:
            f.write(struct.pack("<III", 0x46546C67, 2, total))
            f.write(struct.pack("<II", len(body), 0x4E4F534A))
            f.write(body)
            f.write(struct.pack("<II", len(self.blob), 0x004E4942))
            f.write(self.blob)
        return total


def read_gltf(path: Path):
    """Meshes of a Poly Haven .gltf, with node translations applied. Returns [(material name, mesh, diffuse path)]."""
    doc = json.loads(path.read_text())
    blob = (path.parent / doc["buffers"][0]["uri"]).read_bytes()
    kinds = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}
    types = {5126: np.float32, 5125: np.uint32, 5123: np.uint16, 5121: np.uint8}

    def read(index):
        acc = doc["accessors"][index]
        view = doc["bufferViews"][acc["bufferView"]]
        dtype = types[acc["componentType"]]
        width = kinds[acc["type"]]
        start = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
        stride = view.get("byteStride", 0) or width * np.dtype(dtype).itemsize
        raw = np.frombuffer(blob, np.uint8, count=stride * (acc["count"] - 1) + width * np.dtype(dtype).itemsize,
                            offset=start)
        rows = np.lib.stride_tricks.as_strided(raw, (acc["count"], width * np.dtype(dtype).itemsize), (stride, 1))
        return np.frombuffer(rows.copy().tobytes(), dtype).reshape(acc["count"], width).astype(np.float64)

    out = []
    for node in doc["nodes"]:
        if "mesh" not in node:
            continue
        offset = np.asarray(node.get("translation", [0, 0, 0]), float)
        for prim in doc["meshes"][node["mesh"]]["primitives"]:
            a = prim["attributes"]
            p = read(a["POSITION"]) + offset
            n = read(a["NORMAL"])
            uv = read(a["TEXCOORD_0"])
            t = read(prim["indices"]).astype(int).reshape(-1, 3)
            mat = doc["materials"][prim["material"]]
            tex = mat["pbrMetallicRoughness"]["baseColorTexture"]["index"]
            image = path.parent / doc["images"][doc["textures"][tex]["source"]]["uri"]
            # glTF v points down the image; our writer keeps it as is.
            out.append((mat["name"], (p, n, uv, t), image))
    return out
