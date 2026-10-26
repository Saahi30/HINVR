"""Builds the 1:1 HINVR sanctum and its offering props as GLBs for the Quest app. No Blender.

Run:  python quest/tools/mandir/fetch_assets.py     (once: CC0 textures and models, not committed)
      python quest/tools/mandir/build_sanctum.py     (needs numpy and Pillow; about a minute)

Headset space, metres: +X right, +Y up, -Z toward the garbha griha. The member stands
near the origin facing the murti. The room is lit here, once: every lamp's light, its
shadows and the ambient occlusion are worked out per vertex and stored as COLOR_0, and
the room's materials are KHR_materials_unlit. On the headset that costs nothing to draw
and looks the same every frame. The props are separate, lit PBR files, because they move.

Writes app/src/main/assets/mandir/: sanctum.glb, props/*.glb and markers.json, which the
app reads for every position below.
"""

from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

from geometry import (
    Gltf, Occluders, align_y, box, bounds, chain, disc, jitter, jpeg, lathe, light_vertices, merge, moved, quad,
    read_gltf, rot_x, rot_y, rot_z, scaled, sphere, tonemap, torus, tube,
)

HERE = Path(__file__).resolve().parent
SOURCES = HERE / "sources"
OUT = HERE.parent.parent / "app" / "src" / "main" / "assets" / "mandir"

# Mandap: 6.4 m wide, 7 m deep, 4 m to the flat ceiling. Door behind the member at +Z.
HALF_W = 3.20
DOOR_Z = 3.50
FRONT_Z = -3.50
HEIGHT = 4.00
WALL = 0.32
DOOR_HALF = 0.85
DOOR_H = 2.10
# Garbha griha behind the front wall, seen through an opening over a 0.55 m sill.
G_HALF = 1.30
G_BACK = -5.90
G_HEIGHT = 3.30
G_FLOOR = 0.15
OPEN_HALF = 0.615
OPEN_SILL = 0.55
OPEN_TOP = 2.40
# The central bay between the four pillars, with its lantern ceiling.
PILLAR_X = 1.85
PILLAR_Z = (1.55, -1.45)
BAY_Z = 0.05
# The darshan window the app draws behind the murti: a 16:9 stream, 1.6 m wide.
DARSHAN_W = 1.60
DARSHAN_H = 0.90

# Everything the app needs to know about, in one place.
MARK = {
    "spawn": (0.0, 0.0, 0.05),
    "garbha": (0.0, 0.0, -5.2),
    "darshan": (0.0, 1.55, -5.68),
    "stand": (0.0, 0.82, -0.55),
    "jyoti": (-0.47, 0.985, -0.69),
    "incense": (0.44, 0.865, -0.69),
    "ghanta": (0.70, 1.60, -0.25),
    "peti": (0.98, 0.80, -0.62),
    "side": (-0.98, 0.80, -0.62),
    "feet": (0.0, 0.455, -3.15),
    "teleport_mandap": (0.0, 0.0, 0.05),
    "teleport_threshold": (0.0, 0.0, -2.35),
    "door": (0.0, 1.05, 3.6),
}

# How each room material is drawn: a tiling CC0 texture (metres per repeat) or a flat colour.
# "shine" materials get a highlight baked toward where the member stands.
TEXTURES = {
    "floor": ("granite_tile", 1.1, 2048),
    "wall": ("red_sandstone_pavement", 1.7, 2048),
    "stone": ("sandstone_cracks", 0.9, 1024),
    "garbha": ("red_sandstone_tiles", 1.2, 1024),
    "marble": ("marble_01", 1.6, 1024),
}
COLOURS = {
    "brass": ((0.64, 0.40, 0.10), True),
    "blackstone": ((0.030, 0.028, 0.026), True),
    "cloth": ((0.42, 0.015, 0.02), False),
    "gold": ((0.70, 0.46, 0.09), True),
    "marigold": ((1.0, 0.36, 0.0), False),
    "marigold_yellow": ((1.0, 0.62, 0.02), False),
    "leaf": ((0.05, 0.20, 0.02), False),
    "vibhuti": ((0.82, 0.80, 0.74), False),
    "kumkum": ((0.55, 0.01, 0.01), False),
    "sand": ((0.40, 0.28, 0.14), False),
    "clay": ((0.40, 0.13, 0.05), False),
    "rangoli_white": ((0.86, 0.84, 0.78), False),
    "rangoli_magenta": ((0.62, 0.02, 0.20), False),
    "rangoli_yellow": ((1.0, 0.60, 0.02), False),
    "rangoli_green": ((0.03, 0.32, 0.07), False),
    "rangoli_blue": ((0.02, 0.10, 0.45), False),
}
FLAME = (1.0, 0.70, 0.22)
WOOD_TILE = 0.7

# Warm lamp light everywhere, a little daylight through the door behind.
LAMP = (1.0, 0.62, 0.30)
DAY = (0.78, 0.86, 1.0)
# Two moods from one lighting pass: each lamp's shadowed light is worked out once, then weighted.
MOODS = {
    "sanctum": dict(lamp=1.0, day=1.0, ambient=(0.10, 0.072, 0.048), exposure=0.95, contrast=1.7),
    "sanctum_day": dict(lamp=0.4, day=6.0, ambient=(0.62, 0.56, 0.50), exposure=0.95, contrast=1.15),
}


class Room:
    """Geometry per material, plus what blocks light."""

    def __init__(self):
        self.parts: dict[str, list] = {}
        self.solid = Occluders()
        self.lights: list[dict] = []
        self.halos: list = []

    def put(self, material, mesh):
        if mesh is not None and len(mesh[0]):
            self.parts.setdefault(material, []).append(mesh)

    def block(self, material, mn, mx, cell=0.25, skip=(), solid=True):
        self.put(material, box(mn, mx, cell, 1.0, skip))
        if solid:
            self.solid.add_box(mn, mx)

    def flame(self, at, size=1.0, light=None):
        """A small burning wick. [light] is its brightness for the bake."""
        x, y, z = at
        self.put("flame", sphere((x, y + 0.018 * size, z), 0.011 * size, 8, 6, (1, 2.4, 1)))
        if light:
            self.lamp((x, y + 0.03, z), light, radius=0.03)

    def lamp(self, at, power, colour=LAMP, radius=0.05, shadows=True):
        group = "day" if colour == DAY else "lamp"
        self.lights.append({"at": at, "power": power, "color": colour, "radius": radius, "shadows": shadows, "group": group})

    def halo(self, at):
        """A place where flames burn, for the app's flickering glow."""
        self.halos.append([round(float(c), 3) for c in at])


def ceiling_y(x, z):
    """Height of the ceiling over a point: flat, then three steps up into the lantern dome."""
    dz = z - BAY_Z
    if abs(x) > 1.65 or abs(dz) > 1.30:
        return HEIGHT
    if max(abs(x), abs(dz)) > 1.05:
        return 4.20
    if abs(x) + abs(dz) > 1.05:
        return 4.40
    if math.hypot(x, dz) > 0.70:
        return 4.60
    return 4.90


# ---------------------------------------------------------------- architecture


def build_floor(room: Room):
    # Granite either side of a marble aisle that runs from the door to the garbha.
    room.block("floor", (-HALF_W, -0.15, FRONT_Z), (-0.58, 0, DOOR_Z), skip=("+x", "-x", "+z", "-z", "-y"), solid=False)
    room.block("floor", (0.58, -0.15, FRONT_Z), (HALF_W, 0, DOOR_Z), skip=("+x", "-x", "+z", "-z", "-y"), solid=False)
    room.block("marble", (-0.52, -0.15, FRONT_Z), (0.52, 0, DOOR_Z), skip=("+x", "-x", "+z", "-z", "-y"), solid=False)
    for side in (-1, 1):
        x0, x1 = sorted((side * 0.52, side * 0.58))
        room.block("brass", (x0, -0.15, FRONT_Z), (x1, 0.002, DOOR_Z), cell=0.3, skip=("-y", "+z", "-z"), solid=False)
    room.block("stone", (-DOOR_HALF, -0.15, DOOR_Z), (DOOR_HALF, 0.02, DOOR_Z + WALL), skip=("-y", "+x", "-x"), solid=False)
    room.solid.add_box((-4, -0.4, -7), (4, 0, DOOR_Z + WALL))


def build_walls(room: Room):
    skip_side = ("+y", "-y", "+z", "-z")
    room.block("wall", (HALF_W, 0, FRONT_Z - WALL), (HALF_W + WALL, HEIGHT, DOOR_Z + WALL), skip=skip_side + ("+x",))
    room.block("wall", (-HALF_W - WALL, 0, FRONT_Z - WALL), (-HALF_W, HEIGHT, DOOR_Z + WALL), skip=skip_side + ("-x",))
    # Front wall with the garbha opening.
    fz = (FRONT_Z - WALL, FRONT_Z)
    room.block("wall", (-HALF_W, 0, fz[0]), (-OPEN_HALF, HEIGHT, fz[1]), skip=("-z", "-x", "+y", "-y"))
    room.block("wall", (OPEN_HALF, 0, fz[0]), (HALF_W, HEIGHT, fz[1]), skip=("-z", "+x", "+y", "-y"))
    room.block("wall", (-OPEN_HALF, OPEN_TOP, fz[0]), (OPEN_HALF, HEIGHT, fz[1]), skip=("-z", "+y", "+x", "-x"))
    room.block("stone", (-OPEN_HALF, 0, fz[0]), (OPEN_HALF, OPEN_SILL, fz[1]), skip=("-z", "-y", "+x", "-x"))
    # Door wall behind, with the doorway out to the courtyard.
    dz = (DOOR_Z, DOOR_Z + WALL)
    room.block("wall", (-HALF_W, 0, dz[0]), (-DOOR_HALF, HEIGHT, dz[1]), skip=("+z", "-x", "+y", "-y"))
    room.block("wall", (DOOR_HALF, 0, dz[0]), (HALF_W, HEIGHT, dz[1]), skip=("+z", "+x", "+y", "-y"))
    room.block("wall", (-DOOR_HALF, DOOR_H, dz[0]), (DOOR_HALF, HEIGHT, dz[1]), skip=("+z", "+y", "+x", "-x"))

    # Mouldings: a stepped plinth at the foot of every wall, a band at a metre, a cornice under the ceiling.
    def run_along(z0, z1, x_face, inward, y0, y1, depth, mat="stone", cell=0.3):
        x0, x1 = sorted((x_face, x_face + inward * depth))
        skip = ("-y", "+x" if inward < 0 else "-x")
        room.block(mat, (x0, y0, z0), (x1, y1, z1), cell=cell, skip=skip)

    for side in (-1, 1):
        face = side * HALF_W
        inward = -side
        run_along(FRONT_Z, DOOR_Z, face, inward, 0, 0.16, 0.07)
        run_along(FRONT_Z, DOOR_Z, face, inward, 0.16, 0.24, 0.04)
        run_along(FRONT_Z, DOOR_Z, face, inward, 1.02, 1.09, 0.025)
        run_along(FRONT_Z, DOOR_Z, face, inward, 3.56, 3.66, 0.10)
        run_along(FRONT_Z, DOOR_Z, face, inward, 3.66, 3.76, 0.16)
        # Pilasters behind each pillar and in the corners.
        for z in (PILLAR_Z[0], PILLAR_Z[1], FRONT_Z + 0.22, DOOR_Z - 0.22):
            x0, x1 = sorted((face, face + inward * 0.06))
            room.block("stone", (x0, 0.24, z - 0.20), (x1, 3.40, z + 0.20), cell=0.3, skip=("-y",))
            x0, x1 = sorted((face, face + inward * 0.11))
            room.block("stone", (x0, 3.40, z - 0.25), (x1, 3.56, z + 0.25), cell=0.3)
        build_niche(room, side)
    # Plinth and cornice along the front and door walls.
    for z_face, out in ((FRONT_Z, 1), (DOOR_Z, -1)):
        for y0, y1, depth in ((0, 0.16, 0.07), (0.16, 0.24, 0.04), (3.56, 3.66, 0.10), (3.66, 3.76, 0.16)):
            z0, z1 = sorted((z_face, z_face + out * depth))
            gap = OPEN_HALF if z_face == FRONT_Z else DOOR_HALF
            if z_face == FRONT_Z and y1 <= OPEN_SILL:
                room.block("stone", (-HALF_W, y0, z0), (HALF_W, y1, z1), cell=0.3, skip=("-y",))
                continue
            if z_face == FRONT_Z and y0 >= OPEN_TOP or z_face == DOOR_Z and y0 >= DOOR_H:
                room.block("stone", (-HALF_W, y0, z0), (HALF_W, y1, z1), cell=0.3, skip=("-y",) if y1 < 1 else ())
                continue
            room.block("stone", (-HALF_W, y0, z0), (-gap - 0.36, y1, z1), cell=0.3, skip=("-y",))
            room.block("stone", (gap + 0.36, y0, z0), (HALF_W, y1, z1), cell=0.3, skip=("-y",))
    for x in (-2.35, 2.35):
        room.block("stone", (x - 0.20, 0.24, FRONT_Z), (x + 0.20, 3.40, FRONT_Z + 0.06), cell=0.3, skip=("-y",))
        room.block("stone", (x - 0.25, 3.40, FRONT_Z), (x + 0.25, 3.56, FRONT_Z + 0.11), cell=0.3)


def build_niche(room: Room, side: int):
    """A framed niche in a side wall with a small oil lamp burning in it."""
    face = side * HALF_W
    inward = -side
    z, y0, y1, half = BAY_Z, 1.20, 2.20, 0.38

    def slab(d0, d1, zz0, zz1, ya, yb, mat="stone"):
        x0, x1 = sorted((face + inward * d0, face + inward * d1))
        room.block(mat, (x0, ya, zz0), (x1, yb, zz1), cell=0.15)

    slab(0.0, 0.006, z - half, z + half, y0, y1, "garbha")
    slab(0.0, 0.08, z - half - 0.09, z - half, y0 - 0.06, y1 + 0.06)
    slab(0.0, 0.08, z + half, z + half + 0.09, y0 - 0.06, y1 + 0.06)
    slab(0.0, 0.08, z - half - 0.09, z + half + 0.09, y1, y1 + 0.10)
    slab(0.0, 0.14, z - half - 0.12, z + half + 0.12, y1 + 0.10, y1 + 0.16)
    slab(0.0, 0.16, z - half - 0.06, z + half + 0.06, y0 - 0.07, y0)
    # Pointed top over the niche.
    for k in range(4):
        w = 0.32 - k * 0.08
        slab(0.0, 0.06, z - w, z + w, y1 + 0.16 + k * 0.05, y1 + 0.21 + k * 0.05)
    lamp_x = face + inward * 0.09
    room.put("clay", lathe([(0, 0), (0.035, 0.004), (0.05, 0.022), (0.046, 0.026), (0.0, 0.02)], 16, (lamp_x, y0, z)))
    room.flame((lamp_x, y0 + 0.026, z), 1.5, light=0.9)
    room.halo((lamp_x, y0 + 0.05, z))


def build_pillar(room: Room, x: float, z: float):
    room.block("stone", (x - 0.30, 0, z - 0.30), (x + 0.30, 0.28, z + 0.30), cell=0.15, skip=("-y",))
    room.block("stone", (x - 0.25, 0.28, z - 0.25), (x + 0.25, 0.36, z + 0.25), cell=0.15, skip=("-y",))
    profile = [
        (0.23, 0.36), (0.235, 0.40), (0.205, 0.42), (0.205, 0.46), (0.225, 0.48), (0.19, 0.52),
        (0.185, 1.20), (0.205, 1.22), (0.205, 1.28), (0.185, 1.30), (0.18, 2.05),
        (0.20, 2.12), (0.245, 2.25), (0.255, 2.38), (0.235, 2.50), (0.19, 2.58), (0.165, 2.64),
        (0.165, 2.74), (0.20, 2.78), (0.20, 2.84), (0.17, 2.88), (0.20, 2.95), (0.27, 3.04),
        (0.285, 3.12), (0.26, 3.20), (0.20, 3.26), (0.20, 3.30),
    ]
    room.put("stone", lathe(profile, 20, (x, 0, z), smooth=False))
    room.solid.add_column(x, z, 0.20, 0.0, 3.30)
    room.block("stone", (x - 0.27, 3.30, z - 0.27), (x + 0.27, 3.42, z + 0.27), cell=0.15)
    # Bracket arms that carry the beams.
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for y0, y1, reach, half in ((3.42, 3.54, 0.36, 0.13), (3.54, 3.70, 0.50, 0.15)):
            if dx:
                x0, x1 = sorted((x, x + dx * reach))
                room.block("stone", (x0, y0, z - half), (x1, y1, z + half), cell=0.15)
            else:
                z0, z1 = sorted((z, z + dz * reach))
                room.block("stone", (x - half, y0, z0), (x + half, y1, z1), cell=0.15)


def build_beams_and_ceiling(room: Room):
    for z in PILLAR_Z:
        room.block("stone", (-HALF_W, 3.70, z - 0.17), (HALF_W, HEIGHT, z + 0.17), cell=0.3, skip=("+y",))
    for x in (-PILLAR_X, PILLAR_X):
        room.block("stone", (x - 0.17, 3.72, FRONT_Z), (x + 0.17, HEIGHT, DOOR_Z), cell=0.3, skip=("+y",))

    def ring(outer, inner, y0, y1, mat):
        (ox, oz), (ix, iz) = outer, inner
        c = BAY_Z
        room.block(mat, (-ox, y0, c + iz), (ox, y1, c + oz), cell=0.3, skip=("+y",))
        room.block(mat, (-ox, y0, c - oz), (ox, y1, c - iz), cell=0.3, skip=("+y",))
        room.block(mat, (-ox, y0, c - iz), (-ix, y1, c + iz), cell=0.3, skip=("+y",))
        room.block(mat, (ix, y0, c - iz), (ox, y1, c + iz), cell=0.3, skip=("+y",))

    ring((HALF_W, 3.55), (1.65, 1.30), HEIGHT, HEIGHT + 0.30, "marble")
    ring((1.65, 1.30), (1.05, 1.05), 4.20, 4.50, "stone")

    # Corner slabs under the rotated square, and its four inner faces.
    c = BAY_Z
    for sx in (-1, 1):
        for sz in (-1, 1):
            tri = np.array([[sx * 1.05, 4.40, c + sz * 1.05], [0.0, 4.40, c + sz * 1.05], [sx * 1.05, 4.40, c]])
            p = tri
            n = np.tile([0, -1.0, 0], (3, 1))
            uv = p[:, [0, 2]] / 0.9
            facing = np.cross(tri[1] - tri[0], tri[2] - tri[0])[1]
            room.put("marble", (p, n, uv, np.array([[0, 1, 2] if facing < 0 else [0, 2, 1]])))
    diamond = [(1.05, 0.0), (0.0, 1.05), (-1.05, 0.0), (0.0, -1.05)]
    for k in range(4):
        (ax, az), (bx, bz) = diamond[k], diamond[(k + 1) % 4]
        a = np.array([ax, 4.40, c + az])
        u = np.array([bx - ax, 0, bz - az])
        v = np.array([0, 0.22, 0])
        mesh = quad(a, u, v, 0.15, 0.9)
        if np.dot(mesh[1][0], -(a + u / 2 - np.array([0, 4.4, c]))) < 0:
            mesh = quad(a + u, -u, v, 0.15, 0.9)
        room.put("stone", mesh)
    # Flat ring between the diamond and the dome.
    angles = np.linspace(0, 2 * math.pi, 33)
    inner = np.stack([0.70 * np.sin(angles), np.full_like(angles, 4.60), c + 0.70 * np.cos(angles)], 1)
    rd = 1.05 / (np.abs(np.sin(angles)) + np.abs(np.cos(angles)))
    outer = np.stack([rd * np.sin(angles), np.full_like(angles, 4.60), c + rd * np.cos(angles)], 1)
    p = np.vstack([inner, outer])
    m = len(angles)
    tris = []
    for k in range(m - 1):
        tris += [[k, k + 1, m + k + 1], [k, m + k + 1, m + k]]
    tris = np.array(tris)
    n = np.tile([0, -1.0, 0], (len(p), 1))
    # Wind each triangle to face down.
    e1, e2 = p[tris[:, 1]] - p[tris[:, 0]], p[tris[:, 2]] - p[tris[:, 0]]
    flip = np.cross(e1, e2)[:, 1] > 0
    tris[flip] = tris[flip][:, ::-1]
    room.put("marble", (p, n, p[:, [0, 2]] / 0.9, tris))
    # The dome: concentric steps, then a shallow cap.
    steps = [(0.70, 4.60), (0.70, 4.66), (0.62, 4.66), (0.62, 4.72), (0.54, 4.72), (0.54, 4.78), (0.46, 4.78),
             (0.46, 4.83)]
    cap = [(0.46 * math.cos(a), 4.83 + 0.12 * math.sin(a)) for a in np.linspace(0, math.pi / 2, 7)]
    room.put("stone", lathe(steps, 32, (0, 0, c), smooth=False, inward=True))
    room.put("marble", lathe(cap, 32, (0, 0, c), inward=True))
    # Lotus pendant hanging from the crown.
    bud = [(0.0, 4.48), (0.04, 4.50), (0.09, 4.56), (0.13, 4.64), (0.15, 4.72), (0.13, 4.80), (0.10, 4.86),
           (0.10, 4.95)]
    room.put("stone", lathe(bud, 20, (0, 0, c)))
    for k in range(12):
        a = k * 30
        petal = sphere((0, 0, 0), 0.05, 8, 6, (0.55, 1.0, 0.22))
        petal = moved(petal, rot_y(a) @ rot_x(-35), (0, 0, 0))
        offset = np.array([0.13 * math.sin(math.radians(a)), 4.70, c + 0.13 * math.cos(math.radians(a))])
        room.put("stone", moved(petal, None, offset))


def build_doorways(room: Room):
    # Garbha doorway: two stepped frames, a lintel block and a marigold toran.
    z = FRONT_Z
    for inset, depth, top, width in ((0.0, 0.06, 2.60, 0.20), (0.20, 0.03, 2.73, 0.13)):
        x_in = OPEN_HALF + inset
        for s in (-1, 1):
            x0, x1 = sorted((s * x_in, s * (x_in + width)))
            room.block("stone", (x0, 0.24 if inset == 0 else 0.24, z), (x1, top, z + depth), cell=0.15)
        room.block("stone", (-(x_in + width), top - (0.20 if inset == 0 else 0.13), z), (x_in + width, top, z + depth),
                   cell=0.15)
    room.block("stone", (-0.20, 2.60, z), (0.20, 2.94, z + 0.12), cell=0.1)
    room.block("brass", (-0.08, 2.94, z), (0.08, 2.98, z + 0.10), cell=0.1)
    room.put("brass", sphere((0, 3.03, z + 0.05), 0.05, 12, 8, (1, 1.3, 1)))
    build_toran(room, z + 0.10)

    # Main door behind the member: a frame and two open wooden leaves studded with brass.
    zd = DOOR_Z
    for s in (-1, 1):
        x0, x1 = sorted((s * DOOR_HALF, s * (DOOR_HALF + 0.18)))
        room.block("stone", (x0, 0, zd - 0.06), (x1, DOOR_H + 0.18, zd), cell=0.15)
        x0, x1 = sorted((s * (DOOR_HALF + 0.20), s * (DOOR_HALF + 1.00)))
        room.block("wood", (x0, 0.02, zd - 0.13), (x1, DOOR_H - 0.02, zd - 0.07), cell=0.2)
        for i in range(3):
            for j in range(6):
                x = s * (DOOR_HALF + 0.33 + i * 0.27)
                room.put("brass", sphere((x, 0.30 + j * 0.30, zd - 0.135), 0.018, 8, 5))
    room.block("stone", (-(DOOR_HALF + 0.18), DOOR_H, zd - 0.06), (DOOR_HALF + 0.18, DOOR_H + 0.18, zd), cell=0.15)


def bead_string(room: Room, points, radius=0.022, leaf_every=0):
    for k, at in enumerate(points):
        room.put("marigold" if k % 3 else "marigold_yellow", jitter(sphere(at, radius, 7, 5), radius * 0.18, k))


def build_toran(room: Room, z: float):
    """Marigold garland across the garbha doorway, strands hanging, a mango leaf at each end."""
    left, right, sag, top = -0.80, 0.80, 0.20, 2.64
    xs = np.linspace(left, right, 42)
    ys = top - sag * (1 - (xs / right) ** 2)
    bead_string(room, [(x, y, z) for x, y in zip(xs, ys)])
    for x in np.linspace(-0.66, 0.66, 7):
        y_top = top - sag * (1 - (x / right) ** 2)
        length = 0.20 + 0.10 * (1 - abs(x) / 0.66)
        drops = [(x, y_top - 0.04 - k * 0.04, z) for k in range(int(length / 0.04))]
        bead_string(room, drops, 0.019)
        tip = drops[-1][1] - 0.05
        room.put("leaf", scaled(sphere((0, 0, 0), 1.0, 8, 6), (0.022, 0.06, 0.004), (x, tip, z)))
    # Garlands down both jambs.
    for s in (-1, 1):
        ys = np.arange(top - 0.03, 1.25, -0.042)
        bead_string(room, [(s * (OPEN_HALF + 0.10), y, z) for y in ys], 0.021)


def build_deepastambha(room: Room, x: float, z: float):
    """Tall brass lamp stand beside the garbha doorway: three tiers of five wicks."""
    profile = [
        (0.0, 0.0), (0.20, 0.0), (0.20, 0.03), (0.17, 0.05), (0.12, 0.08), (0.06, 0.13), (0.035, 0.20),
        (0.035, 0.48), (0.06, 0.51), (0.035, 0.55), (0.035, 0.84), (0.15, 0.87), (0.17, 0.91), (0.15, 0.92),
        (0.035, 0.93), (0.03, 1.12), (0.11, 1.15), (0.125, 1.19), (0.11, 1.20), (0.03, 1.21), (0.028, 1.38),
        (0.08, 1.41), (0.09, 1.45), (0.08, 1.46), (0.025, 1.47), (0.04, 1.52), (0.05, 1.57), (0.0, 1.66),
    ]
    room.put("brass", lathe(profile, 20, (x, 0, z)))
    for r, y in ((0.155, 0.92), (0.11, 1.20), (0.075, 1.46)):
        for k in range(5):
            a = math.radians(k * 72 + 18)
            room.flame((x + r * math.sin(a), y, z + r * math.cos(a)), 1.8)
    room.lamp((x, 1.25, z), 3.2, radius=0.18)
    room.halo((x, 1.22, z))


def build_garbha(room: Room):
    gz0, gz1 = FRONT_Z - WALL, G_BACK
    room.block("marble", (-G_HALF, 0, gz1), (G_HALF, G_FLOOR, gz0), cell=0.2, skip=("-y", "+z", "-z", "+x", "-x"))
    room.block("garbha", (-G_HALF - WALL, 0, gz1 - WALL), (-G_HALF, G_HEIGHT, gz0), cell=0.2, skip=("-x", "+y", "-y", "+z", "-z"))
    room.block("garbha", (G_HALF, 0, gz1 - WALL), (G_HALF + WALL, G_HEIGHT, gz0), cell=0.2, skip=("+x", "+y", "-y", "+z", "-z"))
    room.block("garbha", (-G_HALF, 0, gz1 - WALL), (G_HALF, G_HEIGHT, gz1), cell=0.2, skip=("-z", "+y", "-y", "+x", "-x"))
    room.block("stone", (-G_HALF - WALL, G_HEIGHT, gz1 - WALL), (G_HALF + WALL, G_HEIGHT + 0.3, gz0), cell=0.2, skip=("+y",))
    for side in (-1, 1):
        x0, x1 = sorted((side * G_HALF, side * (G_HALF - 0.05)))
        room.block("stone", (x0, G_FLOOR, gz1), (x1, G_FLOOR + 0.14, gz0), cell=0.2, skip=("-y",))
    room.block("stone", (-G_HALF, G_FLOOR, gz1), (G_HALF, G_FLOOR + 0.14, gz1 + 0.05), cell=0.2, skip=("-y",))
    room.block("stone", (-G_HALF, G_HEIGHT - 0.18, gz1), (G_HALF, G_HEIGHT, gz1 + 0.10), cell=0.2)

    # The lingam on its yoni, on a stone pedestal.
    lx, lz = MARK["garbha"][0], MARK["garbha"][2]
    base = 0.42
    room.block("stone", (lx - 0.46, G_FLOOR, lz - 0.46), (lx + 0.46, base, lz + 0.46), cell=0.12, skip=("-y",))
    yoni = [(0.0, 0.0), (0.42, 0.0), (0.42, 0.05), (0.37, 0.075), (0.37, 0.11), (0.43, 0.14), (0.43, 0.18),
            (0.40, 0.195), (0.15, 0.195)]
    room.put("blackstone", lathe(yoni, 28, (lx, base, lz), smooth=False))
    room.block("blackstone", (lx + 0.38, base + 0.14, lz - 0.065), (lx + 0.66, base + 0.18, lz + 0.065), cell=0.1, solid=False)
    shaft = [(0.15, 0.195), (0.135, 0.205), (0.13, 0.25), (0.13, 0.48), (0.124, 0.53), (0.105, 0.575),
             (0.075, 0.605), (0.04, 0.622), (0.0, 0.627)]
    room.put("blackstone", lathe(shaft, 28, (lx, base, lz)))
    room.solid.add_column(lx, lz, 0.13, base, base + 0.6)
    for y in (0.36, 0.395, 0.43):
        room.put("vibhuti", lathe([(0.1335, y), (0.1335, y + 0.013)], 12, (lx, base, lz), normals2d=[(1, 0), (1, 0)],
                                  arc=(-1.05, 1.05)))
    room.put("kumkum", sphere((lx, base + 0.402, lz + 0.134), 0.011, 8, 6, (1, 1, 0.5)))
    ring = [(lx + 0.165 * math.sin(a), base + 0.215, lz + 0.165 * math.cos(a)) for a in np.linspace(0, 2 * math.pi, 26)[:-1]]
    bead_string(room, ring, 0.024)

    # Abhishek pot hanging over the lingam.
    # High enough to hang above the darshan window from where the member stands.
    pot_y = 2.38
    room.put("brass", lathe([(0.0, 0.0), (0.03, 0.01), (0.11, 0.09), (0.135, 0.15), (0.13, 0.17)], 20, (lx, pot_y, lz)))
    hook = np.array([lx, 2.85, lz])
    for k in range(3):
        a = math.radians(k * 120)
        room.put("brass", chain((lx + 0.13 * math.sin(a), pot_y + 0.17, lz + 0.13 * math.cos(a)), hook, 0.04, 0.004))
    room.put("brass", chain(hook, (lx, G_HEIGHT, lz), 0.045, 0.005))

    # Prabhavali: a brass arch framing the darshan window behind the murti.
    dx, dy, dz = MARK["darshan"]
    frame_z = dz - 0.03
    half_w, half_h = DARSHAN_W / 2 + 0.03, DARSHAN_H / 2 + 0.03
    bottom, top = dy - half_h, dy + half_h
    for s_ in (-1, 1):
        x0, x1 = sorted((s_ * half_w, s_ * (half_w + 0.08)))
        room.block("brass", (x0, bottom - 0.08, frame_z - 0.02), (x1, top, frame_z + 0.01), cell=0.12, solid=False)
    room.block("brass", (-half_w - 0.08, bottom - 0.08, frame_z - 0.02), (half_w + 0.08, bottom, frame_z + 0.01), cell=0.12,
               solid=False)
    room.block("brass", (-half_w - 0.08, top, frame_z - 0.02), (half_w + 0.08, top + 0.06, frame_z + 0.01), cell=0.12,
               solid=False)
    # A low arch over the top bar, its flame finials, and the kirtimukha at the crown.
    arch_r = half_w + 0.04
    circle = np.linspace(math.pi / 2, 3 * math.pi / 2, 25)
    for minor in (0.035, 0.016):
        a_ = np.linspace(-math.pi, math.pi, 9)
        profile = np.stack([arch_r + (0 if minor > 0.02 else 0.09) + minor * np.cos(a_), minor * np.sin(a_)], 1)
        normals = np.stack([np.cos(a_), np.sin(a_)], 1)
        arch = lathe(profile, 24, (0, 0, 0), normals2d=normals, arc=(math.pi / 2, 3 * math.pi / 2))
        room.put("brass", scaled(moved(arch, rot_x(90), (0, 0, 0)), (1, 0.42, 1), (dx, top + 0.06, frame_z)))
    for a in circle[1:-1:2]:
        at = (dx - (arch_r + 0.15) * math.sin(a), top + 0.06 - (arch_r + 0.15) * math.cos(a) * 0.42, frame_z)
        tilt = math.degrees(a - math.pi) * 0.6
        room.put("brass", moved(sphere((0, 0, 0), 0.03, 8, 6, (0.6, 1.5, 0.5)), rot_z(tilt), at))
    room.put("brass", sphere((dx, top + 0.06 + (arch_r + 0.24) * 0.42, frame_z), 0.07, 14, 8, (1, 1.1, 0.5)))
    # A red cloth hangs behind the arch, so the edges of the window read as a shrine, not a screen.
    room.block("cloth", (-1.20, 0.6, G_BACK), (1.20, G_HEIGHT - 0.2, G_BACK + 0.012), cell=0.15, skip=("-z",), solid=False)

    # Two standing lamps, and the glow from above.
    for s in (-1, 1):
        x, z = s * 0.80, lz + 0.20
        profile = [(0.0, 0.0), (0.13, 0.0), (0.13, 0.02), (0.08, 0.06), (0.03, 0.12), (0.025, 0.62), (0.10, 0.66),
                   (0.12, 0.70), (0.10, 0.71), (0.025, 0.72), (0.03, 0.78), (0.0, 0.86)]
        room.put("brass", lathe(profile, 18, (x, G_FLOOR, z)))
        for k in range(5):
            a = math.radians(k * 72)
            room.flame((x + 0.105 * math.sin(a), G_FLOOR + 0.71, z + 0.105 * math.cos(a)), 1.5)
        room.lamp((x, G_FLOOR + 0.78, z), 1.9, radius=0.12)
        room.halo((x, G_FLOOR + 0.74, z))
    room.lamp((0, 2.9, lz + 0.4), 2.2)
    room.lamp((0, 1.6, FRONT_Z - 0.6), 0.6)


def build_ledge(room: Room):
    """Low stone ledge before the garbha where flowers and naivedya are laid."""
    fx, fy, fz = MARK["feet"]
    z0, z1 = FRONT_Z + 0.02, FRONT_Z + 0.52
    room.block("stone", (-0.65, 0, z0), (0.65, 0.38, z1), cell=0.12, skip=("-y",))
    room.block("stone", (-0.69, 0.38, z0), (0.69, 0.44, z1 + 0.04), cell=0.12)
    room.put("brass", lathe([(0.0, 0.0), (0.20, 0.0), (0.21, 0.015), (0.22, 0.03), (0.205, 0.03), (0.19, 0.012),
                             (0.0, 0.012)], 32, (fx, 0.44, fz)))
    for s in (-1, 1):
        purna_kalash(room, (s * 0.52, 0.44, fz - 0.02), 0.20)


def purna_kalash(room: Room, at, height):
    x, y, z = at
    pot = POT
    lo, hi = bounds(pot[1])
    scale = height / (hi[1] - lo[1])
    room.put("pot", scaled(pot[1], (scale, scale, scale), (x, y - lo[1] * scale, z)))
    top = y + height
    for k in range(7):
        a = k * (360 / 7)
        leaf = scaled(sphere((0, 0, 0), 1.0, 8, 6), (0.028 * height / 0.2, 0.085 * height / 0.2, 0.004))
        leaf = moved(leaf, rot_y(a) @ rot_x(55), (0, 0, 0))
        off = 0.06 * height / 0.2
        room.put("leaf", moved(leaf, None, (x + off * math.sin(math.radians(a)), top + 0.01, z + off * math.cos(math.radians(a)))))
    room.put("clay", jitter(sphere((x, top + 0.06 * height / 0.2, z), 0.055 * height / 0.2, 12, 8, (1, 1.25, 1)), 0.004, 3))


def build_stand(room: Room):
    """The puja chowki in front of the member, under a red cloth, with the jyoti and the incense holder."""
    sx, top, sz = MARK["stand"]
    hx, hz = 0.55, 0.23
    room.block("wood", (sx - hx, top - 0.05, sz - hz), (sx + hx, top - 0.02, sz + hz), cell=0.12)
    room.block("cloth", (sx - hx - 0.012, top - 0.02, sz - hz - 0.012), (sx + hx + 0.012, top, sz + hz + 0.012), cell=0.1)
    room.block("cloth", (sx - hx - 0.012, top - 0.20, sz + hz + 0.002), (sx + hx + 0.012, top - 0.02, sz + hz + 0.012),
               cell=0.1, solid=False)
    room.block("gold", (sx - hx - 0.014, top - 0.20, sz + hz + 0.010), (sx + hx + 0.014, top - 0.17, sz + hz + 0.016),
               cell=0.1, solid=False)
    room.block("gold", (sx - hx - 0.014, top - 0.002, sz + hz + 0.008), (sx + hx + 0.014, top + 0.002, sz + hz + 0.016),
               cell=0.1, solid=False)
    room.block("wood", (sx - hx + 0.03, top - 0.13, sz - hz + 0.03), (sx + hx - 0.03, top - 0.05, sz + hz - 0.03), cell=0.12)
    leg = [(0.03, 0.0), (0.042, 0.03), (0.03, 0.06), (0.026, 0.30), (0.04, 0.36), (0.026, 0.42), (0.028, 0.60),
           (0.038, 0.64), (0.034, top - 0.13)]
    for ix in (-1, 1):
        for iz in (-1, 1):
            room.put("wood", lathe(leg, 12, (sx + ix * (hx - 0.06), 0, sz + iz * (hz - 0.06))))
    jx, jy, jz = MARK["jyoti"]
    lamp = [(0.0, 0.0), (0.045, 0.0), (0.045, 0.008), (0.02, 0.02), (0.012, 0.03), (0.012, 0.11), (0.02, 0.12),
            (0.045, 0.135), (0.05, 0.15), (0.042, 0.153), (0.0, 0.151)]
    room.put("brass", lathe(lamp, 18, (jx, top, jz)))
    room.put("vibhuti", tube((jx, top + 0.15, jz), (jx, jy, jz), 0.003, 6))
    room.lamp((jx, jy + 0.03, jz), 0.40, radius=0.03)
    ix_, iy, iz_ = MARK["incense"]
    room.put("brass", lathe([(0.0, 0.0), (0.05, 0.0), (0.056, 0.012), (0.052, iy - top), (0.046, iy - top)], 18, (ix_, top, iz_)))
    room.put("sand", disc((ix_, iy - 0.001, iz_), 0.047, 18))
    room.solid.add_box((sx - hx, top - 0.13, sz - hz), (sx + hx, top, sz + hz))


def build_side_table(room: Room):
    """A low table left of the stand, under red cloth: the japa mala, and on festivals their offering."""
    sx, top, sz = MARK["side"]
    hx, hz = 0.28, 0.20
    room.block("wood", (sx - hx, top - 0.05, sz - hz), (sx + hx, top - 0.015, sz + hz), cell=0.1)
    room.block("cloth", (sx - hx - 0.01, top - 0.015, sz - hz - 0.01), (sx + hx + 0.01, top, sz + hz + 0.01), cell=0.1)
    room.block("cloth", (sx - hx - 0.01, top - 0.14, sz + hz), (sx + hx + 0.01, top - 0.015, sz + hz + 0.01),
               cell=0.1, solid=False)
    room.block("gold", (sx - hx - 0.012, top - 0.14, sz + hz + 0.008), (sx + hx + 0.012, top - 0.115, sz + hz + 0.014),
               cell=0.1, solid=False)
    leg = [(0.026, 0.0), (0.036, 0.03), (0.024, 0.06), (0.022, 0.40), (0.032, 0.46), (0.024, 0.52), (0.028, top - 0.05)]
    for ix in (-1, 1):
        for iz in (-1, 1):
            room.put("wood", lathe(leg, 10, (sx + ix * (hx - 0.05), 0, sz + iz * (hz - 0.05))))
    room.solid.add_box((sx - hx, top - 0.05, sz - hz), (sx + hx, top, sz + hz))


def build_peti(room: Room):
    px, py, pz = MARK["peti"]
    room.block("stone", (px - 0.24, 0, pz - 0.19), (px + 0.24, 0.48, pz + 0.19), cell=0.12, skip=("-y",))
    room.block("stone", (px - 0.26, 0.46, pz - 0.21), (px + 0.26, 0.50, pz + 0.21), cell=0.12)
    room.block("wood", (px - 0.21, 0.50, pz - 0.16), (px + 0.21, py - 0.03, pz + 0.16), cell=0.1)
    room.block("wood", (px - 0.225, py - 0.03, pz - 0.175), (px + 0.225, py, pz + 0.175), cell=0.1)
    for y in (0.53, py - 0.06):
        room.block("brass", (px - 0.215, y, pz - 0.165), (px + 0.215, y + 0.022, pz + 0.165), cell=0.1, solid=False)
    room.block("blackstone", (px - 0.09, py, pz - 0.015), (px + 0.09, py + 0.003, pz + 0.015), cell=0.1, solid=False)
    room.block("brass", (px - 0.11, py, pz - 0.035), (px + 0.11, py + 0.002, pz + 0.035), cell=0.1, solid=False)
    room.block("brass", (px - 0.03, py - 0.12, pz + 0.165), (px + 0.03, py - 0.05, pz + 0.18), cell=0.1, solid=False)


def build_rug_and_rangoli(room: Room):
    room.block("cloth", (-0.42, 0, -0.20), (0.42, 0.008, 0.42), cell=0.1, skip=("-y",), solid=False)
    for (x0, z0, x1, z1) in ((-0.42, -0.20, 0.42, -0.16), (-0.42, 0.38, 0.42, 0.42), (-0.42, -0.20, -0.38, 0.42),
                             (0.38, -0.20, 0.42, 0.42)):
        room.block("gold", (x0, 0, z0), (x1, 0.011, z1), cell=0.1, skip=("-y",), solid=False)

    cx, cz = 0.0, -1.70

    def petal(mat, r, a, length, width, y):
        mesh = scaled(disc((0, 0, 0), 1.0, 14), (width, 1, length))
        mesh = moved(mesh, rot_y(a), (cx + r * math.sin(math.radians(a)), y, cz + r * math.cos(math.radians(a))))
        room.put(mat, mesh)

    room.put("rangoli_white", disc((cx, 0.004, cz), 0.50, 40))
    for k in range(16):
        petal("rangoli_magenta", 0.36, k * 22.5, 0.12, 0.055, 0.006)
    for k in range(16):
        petal("rangoli_yellow", 0.47, k * 22.5 + 11.25, 0.035, 0.035, 0.006)
    for k in range(10):
        petal("rangoli_yellow", 0.22, k * 36, 0.09, 0.045, 0.008)
    for k in range(8):
        petal("rangoli_green", 0.12, k * 45 + 22.5, 0.06, 0.028, 0.010)
    room.put("rangoli_blue", disc((cx, 0.012, cz), 0.07, 24))
    room.put("clay", lathe([(0, 0), (0.035, 0.004), (0.05, 0.022), (0.046, 0.026), (0.0, 0.02)], 16, (cx, 0.012, cz)))
    room.flame((cx, 0.038, cz), 1.5, light=0.5)
    room.halo((cx, 0.06, cz))


def build_lanterns(room: Room):
    """The hanging brass lanterns, on chains to the ceiling above them."""
    spots = [(0.0, BAY_Z, 3.25), (-1.35, BAY_Z, 3.05), (1.35, BAY_Z, 3.05), (-2.55, -2.2, 2.85), (2.55, -2.2, 2.85)]
    lo, hi = LANTERN_BOUNDS
    scale = 0.55 / (hi[1] - lo[1])
    for x, z, bottom in spots:
        for name, mesh in LANTERN:
            m = scaled(mesh, (scale,) * 3, (x - (lo[0] + hi[0]) / 2 * scale, bottom - lo[1] * scale, z - (lo[2] + hi[2]) / 2 * scale))
            room.put(name, m)
        top = bottom + (hi[1] - lo[1]) * scale
        ceiling = 4.48 if (x, z) == (0.0, BAY_Z) else ceiling_y(x, z)
        room.put("brass", chain((x, ceiling, z), (x, top, z), 0.05, 0.005))
        room.lamp((x, bottom + 0.12, z), 2.4, radius=0.12)


def build_door_kalash(room: Room):
    for s in (-1, 1):
        purna_kalash(room, (s * 1.25, 0.0, DOOR_Z - 0.32), 0.34)


def build_outside(room: Room):
    """The courtyard beyond the main door, from the CC BY-SA photograph."""
    arc = (-1.25, 1.25)
    rows = [(7.0, y) for y in np.linspace(-6.0, 7.5, 16)]
    p, n, uv, t = lathe(rows, 32, (0, 0, DOOR_Z), arc=arc, inward=True)
    # Each point shows what the photograph saw in that direction from the member's eyes.
    eye = np.array([0.0, 1.6, 0.05])
    d = p - eye
    lon = np.degrees(np.arctan2(d[:, 0], d[:, 2]))
    lat = np.degrees(np.arctan2(d[:, 1], np.hypot(d[:, 0], d[:, 2])))
    u = (lon + COURTYARD_SPAN[0] / 2) / COURTYARD_SPAN[0]
    v = (COURTYARD_SPAN[2] - lat) / (COURTYARD_SPAN[2] - COURTYARD_SPAN[1])
    room.put("outside", (p, n, np.stack([u, v], 1), t))
    for s in (-1, 1):
        room.lamp((s * 0.5, 1.7, DOOR_Z + 0.6), 0.9, DAY, radius=0.3)
        room.lamp((s * 0.5, 0.6, DOOR_Z + 0.6), 0.6, DAY, radius=0.3)
    # Sky light falling in through the doorway; barely there at dusk.
    room.lamp((0.0, 2.0, DOOR_Z + 1.4), 0.5, DAY, radius=0.4)


# ---------------------------------------------------------------- materials and bake


def wood_texture(size=512) -> Image.Image:
    rng = np.random.default_rng(11)
    y = np.linspace(0, 1, size)[:, None]
    x = np.linspace(0, 1, size)[None, :]
    wobble = np.cumsum(rng.normal(0, 0.02, size))[:, None]
    grain = np.sin((x * 38 + wobble * 6 + np.sin(y * 9) * 0.4) * math.pi) * 0.5 + 0.5
    noise = np.asarray(Image.fromarray((rng.random((size // 8, size // 8)) * 255).astype(np.uint8)).resize((size, size), Image.BICUBIC)) / 255
    tone = 0.55 + 0.25 * grain ** 3 + 0.2 * noise
    base = np.array([112, 58, 26]) / 255.0
    rgb = np.clip(base[None, None] * tone[..., None] * 1.25, 0, 1)
    return Image.fromarray((rgb * 255).astype(np.uint8))


def courtyard() -> Image.Image | None:
    path = SOURCES / "images" / "courtyard_360.jpg"
    if not path.exists():
        return None
    image = Image.open(path)
    w, h = image.size
    deg = w / 360
    span, low, high = COURTYARD_SPAN
    left = int((COURTYARD_YAW - span / 2) * deg) % w
    box_ = (left, int(h / 2 - high * deg), left + int(span * deg), int(h / 2 - low * deg))
    crop = image.crop(box_) if box_[2] <= w else Image.fromarray(np.hstack([
        np.asarray(image.crop((left, box_[1], w, box_[3]))), np.asarray(image.crop((0, box_[1], box_[2] - w, box_[3])))]))
    return crop.resize((2048, 1280), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.8))


# The photograph's yaw that frames the white shrine, and the window cut from it: degrees wide, lowest and highest latitude.
COURTYARD_YAW = 180.0
COURTYARD_SPAN = (90.0, -50.0, 40.0)


def bake(room: Room):
    """The light that reaches every room vertex, kept per lamp so each mood can weight it."""
    names = sorted(room.parts)
    meshes = {name: merge(*room.parts[name]) for name in names}
    lit = [nm for nm in names if nm not in ("flame", "outside", "lantern_flame")]
    p = np.concatenate([meshes[nm][0] for nm in lit])
    n = np.concatenate([meshes[nm][1] for nm in lit])
    print(f"Lighting {len(p)} vertices, {len(room.lights)} lamps…", flush=True)
    _, ao, seen = light_vertices(p, n, room.solid, room.lights, np.zeros(3))
    return meshes, lit, ao, seen


def colours_for(room: Room, meshes, lit, ao, seen, mood):
    """Vertex colours for one mood, tone-mapped to 0..1."""
    weight = np.array([light["power"] * mood[light["group"]] for light in room.lights])
    tint = np.array([light["color"] for light in room.lights])
    colours = {}
    at = 0
    for nm in lit:
        count = len(meshes[nm][0])
        part = seen[:, at:at + count]
        e = np.outer(ao[at:at + count], mood["ambient"]) + (part * weight[:, None]).T @ tint
        light = tonemap(e, mood["exposure"], mood["contrast"])
        spec_tint = COLOURS.get(nm, (None, False))
        if spec_tint[1]:
            # Polished brass and stone: tint by the metal, add a highlight toward the member's eyes.
            P, N = meshes[nm][0], meshes[nm][1]
            eye = np.where(P[:, 2:3] < -2.0, [[0.0, 1.6, -2.35]], [[0.0, 1.6, 0.05]])
            view = eye - P
            view /= np.linalg.norm(view, axis=1, keepdims=True)
            spec = np.zeros((count, 3))
            for k, lamp in enumerate(room.lights):
                to = np.asarray(lamp["at"]) - P
                to /= np.linalg.norm(to, axis=1, keepdims=True)
                half = to + view
                half /= np.linalg.norm(half, axis=1, keepdims=True)
                ndoth = np.clip(np.einsum("ij,ij->i", N, half), 0, 1)
                spec += np.outer(weight[k] * (part[k] > 0) * ndoth ** 60 * 0.8, lamp["color"])
            facing = np.clip(np.einsum("ij,ij->i", N, view), 0, 1)
            rim = (1 - facing) ** 3 * 0.25 * ao[at:at + count]
            albedo = np.asarray(spec_tint[0])
            colours[nm] = np.clip(albedo * (light * 0.9 + rim[:, None]) + tonemap(spec, 1.0) * albedo ** 0.4, 0, 1)
        else:
            colours[nm] = light
        at += count
    lum = np.concatenate([colours[nm] for nm in lit]) @ [0.2126, 0.7152, 0.0722]
    print("  light (0..1): 10% {:.2f} · median {:.2f} · 90% {:.2f}".format(*np.percentile(lum, [10, 50, 90])))
    for nm in meshes:
        if nm not in colours:
            colours[nm] = np.ones((len(meshes[nm][0]), 3))
    return colours


def write_sanctum(room: Room):
    meshes, lit, ao, seen = bake(room)
    for name, mood in MOODS.items():
        write_mood(room, name, meshes, colours_for(room, meshes, lit, ao, seen, mood))


def write_mood(room: Room, name, meshes, colours):
    gltf = Gltf()
    prims = []
    for nm, mesh in meshes.items():
        p, nrm, uv, t = mesh
        if nm in TEXTURES:
            folder, tile, size = TEXTURES[nm]
            image = Image.open(next((SOURCES / "textures" / folder).glob("*_diff_2k.jpg")))
            mat = gltf.material(nm, texture=gltf.image(folder, jpeg(image, size)), unlit=True)
            uv = uv / tile
        elif nm == "wood":
            mat = gltf.material(nm, texture=gltf.image("wood", jpeg(wood_texture(), 512)), unlit=True)
            uv = uv / WOOD_TILE
        elif nm == "outside":
            image = courtyard()
            if image is None:
                continue
            mat = gltf.material(nm, texture=gltf.image("courtyard", jpeg(image, 2048, 82)), unlit=True)
        elif nm == "flame":
            mat = gltf.material(nm, color=FLAME, unlit=True)
        elif nm in ("lantern", "lantern_glass", "lantern_flame", "pot"):
            tex = gltf.image(IMPORTED_TEXTURES[nm].stem, jpeg(Image.open(IMPORTED_TEXTURES[nm]), 1024))
            if nm == "lantern_glass":
                mat = gltf.material(nm, color=(1, 0.9, 0.7, 0.35), texture=tex, unlit=True, blend=True)
            elif nm == "lantern_flame":
                mat = gltf.material(nm, color=(1, 0.8, 0.45), texture=tex, unlit=True)
            else:
                mat = gltf.material(nm, texture=tex, unlit=True)
        else:
            albedo, shine = COLOURS[nm]
            mat = gltf.material(nm, color=(1, 1, 1) if shine else albedo, unlit=True)
        prims.append((mat, (p, nrm, uv, t), colours[nm]))
    gltf.mesh("sanctum", prims)
    size = gltf.write(OUT / f"{name}.glb")
    verts = sum(len(m[0]) for m in meshes.values())
    tris = sum(len(m[3]) for m in meshes.values())
    print(f"{name}.glb  {size / 1e6:.1f} MB  {verts} vertices  {tris} triangles")


def load_imported():
    global LANTERN, LANTERN_BOUNDS, POT, IMPORTED_TEXTURES
    LANTERN, IMPORTED_TEXTURES = [], {}
    names = {"brass_diya_lantern": "lantern", "brass_diya_lantern_glass": "lantern_glass",
             "brass_diya_lantern_flame": "lantern_flame"}
    for material, mesh, image in read_gltf(SOURCES / "models" / "brass_diya_lantern" / "brass_diya_lantern_1k.gltf"):
        name = names.get(material, "lantern")
        LANTERN.append((name, mesh))
        IMPORTED_TEXTURES[name] = image
    LANTERN_BOUNDS = bounds(merge(*(m for _, m in LANTERN)))
    material, mesh, image = read_gltf(SOURCES / "models" / "brass_pot_01" / "brass_pot_01_1k.gltf")[0]
    POT = ("pot", mesh)
    IMPORTED_TEXTURES["pot"] = image


def build_room() -> Room:
    room = Room()
    build_floor(room)
    build_walls(room)
    for x in (-PILLAR_X, PILLAR_X):
        for z in PILLAR_Z:
            build_pillar(room, x, z)
    build_beams_and_ceiling(room)
    build_doorways(room)
    for s in (-1, 1):
        build_deepastambha(room, s * 1.25, -3.15)
    build_garbha(room)
    build_ledge(room)
    build_stand(room)
    build_peti(room)
    build_side_table(room)
    build_rug_and_rangoli(room)
    build_lanterns(room)
    build_door_kalash(room)
    build_outside(room)
    return room


# ---------------------------------------------------------------- props


BRASS = dict(color=(0.80, 0.55, 0.20), metallic=0.95, roughness=0.32)


def write_prop(name, parts):
    """parts: list of (material name, material kwargs, mesh). Origin is the prop's bottom centre."""
    gltf = Gltf()
    grouped: dict[str, list] = {}
    specs = {}
    for mat, spec, mesh in parts:
        grouped.setdefault(mat, []).append(mesh)
        specs[mat] = spec
    prims = [(gltf.material(mat, **specs[mat]), merge(*meshes), None) for mat, meshes in grouped.items()]
    gltf.mesh(name, prims)
    gltf.write(OUT / "props" / f"{name}.glb")


def build_props():
    clay = dict(color=(0.42, 0.14, 0.06), roughness=0.85)
    ghee = dict(color=(0.95, 0.72, 0.28), roughness=0.25)
    cotton = dict(color=(0.92, 0.9, 0.85), roughness=0.9)
    marigold = dict(color=(1.0, 0.40, 0.0), roughness=0.7)
    marigold_y = dict(color=(1.0, 0.66, 0.02), roughness=0.7)
    green = dict(color=(0.08, 0.26, 0.04), roughness=0.6)
    red = dict(color=(0.60, 0.015, 0.015), roughness=0.95)

    diya_bowl = [(0.0, 0.0), (0.03, 0.002), (0.05, 0.018), (0.056, 0.032), (0.05, 0.034), (0.042, 0.022), (0.0, 0.016)]
    write_prop("diya", [
        ("clay", clay, lathe(diya_bowl, 22)),
        ("ghee", ghee, disc((0, 0.021, 0), 0.043, 20)),
        ("wick", cotton, tube((0, 0.018, 0), (0, 0.045, 0), 0.004, 6, caps=True)),
    ])

    thali = [(0.0, 0.0), (0.12, 0.0), (0.13, 0.012), (0.135, 0.024), (0.128, 0.026), (0.12, 0.012), (0.0, 0.009)]
    small_diya = [(0.0, 0.0), (0.02, 0.004), (0.032, 0.016), (0.03, 0.02), (0.0, 0.014)]
    thali_parts = [
        ("brass", BRASS, lathe(thali, 32)),
        ("brass", BRASS, lathe(small_diya, 16, (0, 0.009, 0))),
        ("ghee", ghee, disc((0, 0.023, 0), 0.026, 16)),
        ("wick", cotton, tube((0, 0.02, 0), (0, 0.04, 0), 0.0035, 6, caps=True)),
        ("kumkum", red, sphere((0.07, 0.012, 0.03), 0.018, 10, 6, (1, 0.45, 1))),
        ("rice", cotton, jitter(sphere((-0.065, 0.012, 0.04), 0.02, 10, 6, (1, 0.4, 1)), 0.002, 5)),
    ]
    rng = np.random.default_rng(4)
    for k in range(14):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0.055, 0.105)
        petal = scaled(sphere((0, 0, 0), 1.0, 6, 4), (0.009, 0.003, 0.014))
        thali_parts.append(("marigold" if k % 2 else "marigold_y", marigold if k % 2 else marigold_y,
                            moved(petal, rot_y(math.degrees(a)), (r * math.sin(a), 0.012, r * math.cos(a)))))
    write_prop("aarti_thali", thali_parts)

    write_prop("agarbatti", [
        ("clay", clay, sphere((0, 0.012, 0), 0.016, 12, 6, (1, 0.75, 1))),
        ("bamboo", dict(color=(0.62, 0.46, 0.24), roughness=0.8), tube((0, 0.0, 0), (0, 0.07, 0), 0.0028, 6)),
        ("masala", dict(color=(0.10, 0.06, 0.035), roughness=0.95), tube((0, 0.07, 0), (0, 0.22, 0), 0.0042, 6)),
        ("ash", dict(color=(0.55, 0.52, 0.48), roughness=0.95), sphere((0, 0.222, 0), 0.0045, 6, 4, (1, 1.4, 1))),
    ])

    flower = [("stem", green, tube((0, 0, 0), (0, 0.02, 0), 0.006, 6)),
              ("marigold", marigold, jitter(sphere((0, 0.034, 0), 0.021, 12, 8), 0.0016, 1))]
    pts = []
    golden = math.pi * (3 - math.sqrt(5))
    for k in range(70):
        y = 1 - (k / 69) * 1.4
        r = math.sqrt(max(0, 1 - y * y))
        pts.append((r * math.cos(golden * k), y, r * math.sin(golden * k)))
    for k, (x, y, z) in enumerate(pts):
        normal = np.array([x, y, z])
        petal = scaled(sphere((0, 0, 0), 1.0, 6, 4), (0.0095, 0.0032, 0.0125))
        rot = align_y(normal)
        flower.append(("marigold" if k % 4 else "marigold_y", marigold if k % 4 else marigold_y,
                       moved(petal, rot, (x * 0.027, 0.034 + y * 0.024, z * 0.027))))
    write_prop("marigold", flower)

    lotus = [("pad", green, disc((0, 0.004, 0), 0.062, 24)),
             ("seed", dict(color=(0.95, 0.75, 0.15), roughness=0.6), sphere((0, 0.03, 0), 0.016, 12, 6, (1, 0.7, 1)))]
    for ring, (count, length, tilt, colour) in enumerate(((10, 0.055, 62, (0.88, 0.36, 0.48)), (8, 0.05, 40, (0.93, 0.48, 0.58)),
                                                         (6, 0.04, 18, (0.97, 0.66, 0.72)))):
        for k in range(count):
            a = k * 360 / count + ring * 15
            petal = scaled(sphere((0, 0, 0), 1.0, 10, 6), (0.017, 0.006, length / 2))
            petal = moved(petal, rot_y(a) @ rot_x(-tilt), (0, 0, 0))
            off = np.array([math.sin(math.radians(a)), 0, math.cos(math.radians(a))]) * length * 0.45
            lotus.append((f"petal{ring}", dict(color=colour, roughness=0.55), moved(petal, None, off + [0, 0.012 + ring * 0.006, 0])))
    write_prop("lotus", lotus)

    husk = dict(color=(0.30, 0.16, 0.06), roughness=0.95)
    nut = jitter(sphere((0, 0.052, 0), 0.05, 18, 12, (1, 1.04, 1)), 0.0025, 9)
    coconut = [("husk", husk, nut), ("tilak", red, sphere((0, 0.064, 0.0505), 0.007, 8, 5, (1, 1.6, 0.4)))]
    for k in range(16):
        a = k * 22.5
        fibre = tube((0, 0, 0), (0, 0.025, 0), 0.0022, 4)
        coconut.append(("fibre", dict(color=(0.45, 0.30, 0.14), roughness=0.95),
                        moved(fibre, rot_y(a) @ rot_x(14 + (k % 3) * 8), (0, 0.1, 0))))
    write_prop("coconut", coconut)

    sweet = dict(color=(0.92, 0.50, 0.06), roughness=0.75)
    laddoo = [("leaf", green, scaled(disc((0, 0.003, 0), 1.0, 20), (0.072, 1, 0.06)))]
    for k, (x, z, y) in enumerate(((-0.026, 0.012, 0.0), (0.026, 0.012, 0.0), (0.0, -0.02, 0.0), (0.0, 0.0, 0.036))):
        laddoo.append(("laddoo", sweet, jitter(sphere((x, 0.027 + y, z), 0.024, 14, 9), 0.0018, 20 + k)))
    write_prop("laddoo", laddoo)

    bowl = [(0.0, 0.0), (0.022, 0.0), (0.035, 0.014), (0.038, 0.026), (0.033, 0.027), (0.03, 0.017), (0.0, 0.012)]
    write_prop("kumkum", [
        ("brass", BRASS, lathe(bowl, 20)),
        ("kumkum", red, lathe([(0.031, 0.019), (0.025, 0.026), (0.012, 0.032), (0.0, 0.034)], 18)),
    ])

    coin = [(0.0, 0.0), (0.0125, 0.0), (0.0125, 0.0022), (0.0108, 0.0022), (0.0102, 0.0017), (0.0, 0.0017)]
    write_prop("coin", [("gold", dict(color=(0.95, 0.70, 0.30), metallic=1.0, roughness=0.25), lathe(coin, 24, smooth=False))])

    # The ghanta hangs from the ceiling: its origin is the hook, the bell [drop] below it.
    gx, gy, gz = MARK["ghanta"]
    drop = ceiling_y(gx, gz) - gy
    outer = [(0.118, -0.12), (0.112, -0.105), (0.098, -0.06), (0.088, -0.01), (0.083, 0.04), (0.072, 0.085),
             (0.05, 0.108), (0.0, 0.118)]
    inner = [(0.108, -0.118), (0.092, -0.06), (0.078, 0.03), (0.055, 0.08), (0.0, 0.098)]
    crown = [(0.0, 0.117), (0.032, 0.118), (0.034, 0.135), (0.014, 0.146), (0.012, 0.16), (0.0, 0.16)]
    c = -drop
    bell = [
        ("brass", BRASS, lathe(outer, 28, (0, c, 0))),
        ("brass", BRASS, lathe(inner, 28, (0, c, 0), inward=True)),
        ("brass", BRASS, torus((0, c - 0.12, 0), 0.113, 0.008, 28, 6)),
        ("brass", BRASS, lathe(crown, 16, (0, c, 0))),
        ("brass", BRASS, moved(torus((0, 0, 0), 0.022, 0.006, 16, 6), rot_x(90), (0, c + 0.18, 0))),
        ("iron", dict(color=(0.25, 0.22, 0.2), metallic=0.8, roughness=0.5), tube((0, c + 0.09, 0), (0, c - 0.09, 0), 0.006, 6)),
        ("iron", dict(color=(0.25, 0.22, 0.2), metallic=0.8, roughness=0.5), sphere((0, c - 0.10, 0), 0.022, 12, 8)),
        ("brass", BRASS, chain((0, 0, 0), (0, c + 0.2, 0), 0.05, 0.0055)),
        ("brass", BRASS, moved(torus((0, 0, 0), 0.03, 0.007, 16, 6), rot_x(90), (0, -0.01, 0))),
    ]
    write_prop("ghanta", bell)
    build_festival_props()
    return drop


# Wick tops of the deepmala's nine small diyas, from the tray's bottom centre.
DEEPMALA_WICKS = [(x * 0.085, 0.046, z * 0.085) for z in (-1, 0, 1) for x in (-1, 0, 1)]


def build_festival_props():
    """The japa mala, and the props the festival pujas bring to the side table."""
    rudraksha = dict(color=(0.30, 0.13, 0.06), roughness=0.75)
    mala = []
    # 108 beads in a loose double coil, the guru bead and its tassel where the coil ends.
    for k in range(108):
        a = k / 54 * 2 * math.pi
        r = 0.052 + 0.016 * (k / 108)
        at = (r * math.sin(a), 0.0045 + (0.007 if 40 < k < 68 else 0.0), r * math.cos(a))
        mala.append(("bead", rudraksha, jitter(sphere(at, 0.0045, 7, 5), 0.0006, k)))
    mala.append(("bead", rudraksha, jitter(sphere((0.0, 0.007, 0.075), 0.007, 9, 6), 0.0008, 200)))
    mala.append(("thread", dict(color=(0.75, 0.08, 0.05), roughness=0.9),
                 moved(lathe([(0.0, 0.0), (0.009, 0.004), (0.004, 0.04), (0.0, 0.042)], 10), rot_x(-80), (0.0, 0.006, 0.08))))
    write_prop("mala", mala)

    copper = dict(color=(0.80, 0.42, 0.26), metallic=1.0, roughness=0.3)
    lota = [(0.0, 0.0), (0.035, 0.0), (0.05, 0.012), (0.062, 0.04), (0.058, 0.07), (0.04, 0.092), (0.03, 0.105),
            (0.036, 0.115), (0.04, 0.122), (0.036, 0.124), (0.028, 0.11)]
    write_prop("lota", [
        ("copper", copper, lathe(lota, 26)),
        ("water", dict(color=(0.45, 0.62, 0.72), metallic=0.0, roughness=0.05), disc((0, 0.108, 0), 0.029, 20)),
    ])

    clay = dict(color=(0.42, 0.14, 0.06), roughness=0.85)
    tray = [(0.0, 0.0), (0.15, 0.0), (0.158, 0.008), (0.162, 0.016), (0.152, 0.017), (0.146, 0.007), (0.0, 0.006)]
    small = [(0.0, 0.0), (0.016, 0.002), (0.028, 0.014), (0.031, 0.022), (0.026, 0.024), (0.022, 0.016), (0.0, 0.012)]
    parts = [("brass", BRASS, lathe(tray, 32))]
    for wx, wy, wz in DEEPMALA_WICKS:
        parts.append(("clay", clay, lathe(small, 14, (wx, 0.006, wz))))
        parts.append(("ghee", dict(color=(0.95, 0.72, 0.28), roughness=0.25), disc((wx, 0.022, wz), 0.021, 12)))
        parts.append(("wick", dict(color=(0.92, 0.9, 0.85), roughness=0.9), tube((wx, 0.02, wz), (wx, wy, wz), 0.0028, 5)))
    write_prop("deepmala", parts)

    # The flame the app shows on each deepmala wick: unlit, origin at its base.
    flame_mat = dict(color=(1.0, 0.72, 0.25), unlit=True)
    write_prop("flame", [
        ("flame", flame_mat, sphere((0, 0.016, 0), 0.008, 10, 6, (1, 2.3, 1))),
        ("core", dict(color=(1.0, 0.95, 0.7), unlit=True), sphere((0, 0.012, 0), 0.004, 8, 5, (1, 2.0, 1))),
    ])

    red = dict(color=(0.75, 0.02, 0.03), roughness=0.5)
    hibiscus = [("calyx", dict(color=(0.08, 0.26, 0.04), roughness=0.6), sphere((0, 0.008, 0), 0.012, 10, 6, (1, 0.7, 1)))]
    for k in range(5):
        a = k * 72
        petal = scaled(sphere((0, 0, 0), 1.0, 12, 8), (0.026, 0.004, 0.034))
        petal = moved(petal, rot_y(a) @ rot_x(-22), (0, 0, 0))
        off = (0.03 * math.sin(math.radians(a)), 0.02, 0.03 * math.cos(math.radians(a)))
        hibiscus.append(("petal", red, moved(petal, None, off)))
    hibiscus.append(("stamen", dict(color=(0.85, 0.1, 0.08), roughness=0.6), tube((0, 0.015, 0), (0.0, 0.06, 0.03), 0.0025, 6)))
    hibiscus.append(("pollen", dict(color=(1.0, 0.75, 0.1), roughness=0.6), sphere((0.0, 0.061, 0.031), 0.005, 8, 5)))
    write_prop("hibiscus", hibiscus)

    sweet = dict(color=(0.93, 0.88, 0.76), roughness=0.6)
    modak = [("leaf", dict(color=(0.08, 0.26, 0.04), roughness=0.6), scaled(disc((0, 0.003, 0), 1.0, 20), (0.068, 1, 0.052)))]
    shape = [(0.0, 0.0), (0.02, 0.003), (0.024, 0.012), (0.02, 0.026), (0.011, 0.040), (0.004, 0.05), (0.0, 0.053)]
    for k, (x, z) in enumerate(((-0.03, 0.012), (0.03, 0.012), (0.0, -0.022))):
        p, n, uv, t = lathe(shape, 24, (x, 0.004, z))
        # Pleats: the folds a modak is pinched into.
        theta = np.arctan2(p[:, 0] - x, p[:, 2] - z)
        rise = np.clip((p[:, 1] - 0.004) / 0.05, 0, 1)
        push = 0.0025 * np.cos(theta * 8) * np.sin(rise * math.pi)
        p = p + n * push[:, None]
        modak.append(("modak", sweet, (p, n, uv, t)))
    write_prop("modak", modak)


def main() -> None:
    if not (SOURCES / "textures").exists():
        raise SystemExit("Run fetch_assets.py first: the CC0 textures and models are not in git.")
    load_imported()
    room = build_room()
    write_sanctum(room)
    drop = build_props()
    gx, gy, gz = MARK["ghanta"]
    markers = {k: [round(float(c), 4) for c in v] for k, v in MARK.items()}
    markers["ghanta_pivot"] = [gx, round(gy + drop, 4), gz]
    markers["deepmala_wicks"] = [[round(c, 4) for c in w] for w in DEEPMALA_WICKS]
    markers["lamps"] = room.halos
    (OUT / "markers.json").write_text(json.dumps(markers, indent=2) + "\n")
    print("Wrote", OUT)
    for path in sorted(OUT.rglob("*.glb")):
        print(f"  {path.relative_to(OUT)}  {path.stat().st_size // 1024} KB")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("BUILD FAILED:", error, file=sys.stderr)
        raise
