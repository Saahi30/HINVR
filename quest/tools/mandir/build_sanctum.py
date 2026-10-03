"""Build the 1:1 HINVR sanctum in Blender and export GLBs for the Quest app.

Run: blender --background --python quest/tools/mandir/build_sanctum.py

Layout is in meters. Blender +Y is the garbha griha; the glTF exporter turns that
into headset -Z (forward). The guest stands near the origin, facing the murti.
"""

from __future__ import annotations

import json
import math
import os
import sys
from pathlib import Path

import bpy
import bmesh
from mathutils import Vector

HERE = Path(__file__).resolve().parent
SOURCES = HERE / "sources"
OUT = HERE.parent.parent / "app" / "src" / "main" / "assets" / "mandir"
WORK = HERE / "work"
OUT.mkdir(parents=True, exist_ok=True)
WORK.mkdir(parents=True, exist_ok=True)

# Mandap: 6.4m wide, 7.0m deep, 4.0m high. Door on -Y, garbha griha on +Y.
HALF_W = 3.20
HALF_D = 3.50
HEIGHT = 4.00
WALL = 0.32
DOOR_W = 1.70
DOOR_H = 2.10
GARBHA_W = 2.60
GARBHA_D = 2.40
GARBHA_H = 3.30
NICHE_W = 1.05
NICHE_H = 1.25
NICHE_Z = 1.45
STAND_Y = 0.85
STAND_Z = 0.78


def reset_scene() -> None:
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.unit_settings.system = "METRIC"
    scene.unit_settings.scale_length = 1.0
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 16
    world = bpy.data.worlds.new("SanctumWorld")
    scene.world = world
    world.use_nodes = True
    nodes = world.node_tree.nodes
    links = world.node_tree.links
    nodes.clear()
    env = nodes.new("ShaderNodeTexEnvironment")
    courtyard = SOURCES / "images" / "courtyard_360.jpg"
    if courtyard.exists():
        env.image = bpy.data.images.load(str(courtyard))
    bg = nodes.new("ShaderNodeBackground")
    bg.inputs["Strength"].default_value = 1.6
    out = nodes.new("ShaderNodeOutputWorld")
    links.new(env.outputs["Color"], bg.inputs["Color"])
    links.new(bg.outputs["Background"], out.inputs["Surface"])


def load_image(path: Path) -> bpy.types.Image:
    image = bpy.data.images.load(str(path))
    image.colorspace_settings.name = "sRGB" if "diff" in path.name else "Non-Color"
    return image


def make_pbr(name: str, folder: str, scale: float, metallic: float = 0.0) -> bpy.types.Material:
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    tex = nodes.new("ShaderNodeTexCoord")
    mapping = nodes.new("ShaderNodeMapping")
    mapping.inputs["Scale"].default_value = (scale, scale, scale)
    links.new(tex.outputs["UV"], mapping.inputs["Vector"])
    base = SOURCES / "textures" / folder
    diff = nodes.new("ShaderNodeTexImage")
    diff.image = load_image(next(base.glob("*_diff_2k.jpg")))
    nor = nodes.new("ShaderNodeTexImage")
    nor.image = load_image(next(base.glob("*_nor_gl_2k.jpg")))
    nor.image.colorspace_settings.name = "Non-Color"
    rough = nodes.new("ShaderNodeTexImage")
    rough.image = load_image(next(base.glob("*_rough_2k.jpg")))
    rough.image.colorspace_settings.name = "Non-Color"
    nmap = nodes.new("ShaderNodeNormalMap")
    for node in (diff, nor, rough):
        links.new(mapping.outputs["Vector"], node.inputs["Vector"])
    links.new(diff.outputs["Color"], bsdf.inputs["Base Color"])
    links.new(rough.outputs["Color"], bsdf.inputs["Roughness"])
    links.new(nor.outputs["Color"], nmap.inputs["Color"])
    links.new(nmap.outputs["Normal"], bsdf.inputs["Normal"])
    bsdf.inputs["Metallic"].default_value = metallic
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    return mat


def color_mat(name: str, color: tuple[float, float, float], metallic: float, rough: float, emit: float = 0.0) -> bpy.types.Material:
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    bsdf = nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = rough
    if "Emission Strength" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*color, 1.0)
        bsdf.inputs["Emission Strength"].default_value = emit
    return mat


def add_box(name: str, size: tuple[float, float, float], loc: tuple[float, float, float], mat: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=loc)
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(mat)
    return obj


def add_cyl(name: str, radius: float, depth: float, loc: tuple[float, float, float], mat: bpy.types.Material, verts: int = 24) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, location=loc, vertices=verts)
    obj = bpy.context.active_object
    obj.name = name
    obj.data.materials.append(mat)
    return obj


def add_uv_sphere(name: str, radius: float, loc: tuple[float, float, float], mat: bpy.types.Material, segs: int = 16) -> bpy.types.Object:
    bpy.ops.mesh.primitive_uv_sphere_add(radius=radius, location=loc, segments=segs, ring_count=max(8, segs // 2))
    obj = bpy.context.active_object
    obj.name = name
    obj.data.materials.append(mat)
    return obj


def add_torus(name: str, major: float, minor: float, loc: tuple[float, float, float], mat: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_torus_add(major_radius=major, minor_radius=minor, location=loc, major_segments=28, minor_segments=10)
    obj = bpy.context.active_object
    obj.name = name
    obj.data.materials.append(mat)
    return obj


def smart_uv(obj: bpy.types.Object, island: float = 0.12) -> None:
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.uv.smart_project(island_margin=island)
    bpy.ops.object.mode_set(mode="OBJECT")
    obj.select_set(False)


def join_objects(name: str, objects: list[bpy.types.Object]) -> bpy.types.Object:
    if len(objects) == 1:
        objects[0].name = name
        return objects[0]
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = objects[0]
    bpy.ops.object.join()
    objects[0].name = name
    return objects[0]


def shade_smooth(obj: bpy.types.Object, angle: float = 0.7) -> None:
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.shade_smooth()
    mesh = obj.data
    if hasattr(mesh, "use_auto_smooth"):
        mesh.use_auto_smooth = True
        mesh.auto_smooth_angle = angle
    obj.select_set(False)


def build_architecture(floor: bpy.types.Material, wall: bpy.types.Material, pillar: bpy.types.Material, garbha: bpy.types.Material, ceiling: bpy.types.Material) -> list[bpy.types.Object]:
    parts: list[bpy.types.Object] = []
    floor_w = (HALF_W + WALL) * 2
    floor_d = (HALF_D + WALL) * 2 + GARBHA_D
    parts.append(add_box("floor", (floor_w, floor_d, 0.12), (0, GARBHA_D / 2, -0.06), floor))
    # Side walls
    parts.append(add_box("wall_east", (WALL, floor_d, HEIGHT), (HALF_W + WALL / 2, GARBHA_D / 2, HEIGHT / 2), wall))
    parts.append(add_box("wall_west", (WALL, floor_d, HEIGHT), (-HALF_W - WALL / 2, GARBHA_D / 2, HEIGHT / 2), wall))
    # Door wall on -Y: two jambs and a lintel
    jamb = (HALF_W + WALL) - DOOR_W / 2
    door_y = -HALF_D - WALL / 2
    parts.append(add_box("door_left", (jamb, WALL, HEIGHT), (-(DOOR_W / 2 + jamb / 2), door_y, HEIGHT / 2), wall))
    parts.append(add_box("door_right", (jamb, WALL, HEIGHT), (DOOR_W / 2 + jamb / 2, door_y, HEIGHT / 2), wall))
    parts.append(add_box("door_lintel", (DOOR_W, WALL, HEIGHT - DOOR_H), (0, door_y, DOOR_H + (HEIGHT - DOOR_H) / 2), wall))
    # Threshold
    parts.append(add_box("threshold", (DOOR_W + 0.2, 0.42, 0.08), (0, -HALF_D, 0.04), pillar))
    # Garbha front wall with opening, plus three closed walls
    gy = HALF_D + GARBHA_D / 2
    gx = GARBHA_W / 2 + WALL / 2
    parts.append(add_box("garbha_east", (WALL, GARBHA_D + WALL, GARBHA_H), (gx, gy, GARBHA_H / 2), garbha))
    parts.append(add_box("garbha_west", (WALL, GARBHA_D + WALL, GARBHA_H), (-gx, gy, GARBHA_H / 2), garbha))
    parts.append(add_box("garbha_back", (GARBHA_W + WALL * 2, WALL, GARBHA_H), (0, HALF_D + GARBHA_D + WALL / 2, GARBHA_H / 2), garbha))
    opening = NICHE_W + 0.18
    side = (GARBHA_W - opening) / 2
    front_y = HALF_D + WALL / 2
    parts.append(add_box("garbha_front_l", (side, WALL, GARBHA_H), (-(opening / 2 + side / 2), front_y, GARBHA_H / 2), garbha))
    parts.append(add_box("garbha_front_r", (side, WALL, GARBHA_H), (opening / 2 + side / 2, front_y, GARBHA_H / 2), garbha))
    parts.append(add_box("garbha_front_top", (opening, WALL, GARBHA_H - 2.4), (0, front_y, 2.4 + (GARBHA_H - 2.4) / 2), garbha))
    parts.append(add_box("garbha_front_sill", (opening, WALL, 0.55), (0, front_y, 0.275), garbha))
    parts.append(add_box("garbha_ceiling", (GARBHA_W + WALL * 2, GARBHA_D + WALL, WALL), (0, gy, GARBHA_H + WALL / 2), ceiling))
    # Niche frame around the darshan window
    ny = HALF_D + GARBHA_D - 0.18
    parts.append(add_box("niche_plinth", (NICHE_W + 0.28, 0.55, 0.22), (0, ny, 0.72), pillar))
    # Mandap ceiling
    parts.append(add_box("ceiling", (floor_w, (HALF_D + WALL) * 2, WALL), (0, 0, HEIGHT + WALL / 2), ceiling))
    # Four carved-looking pillars
    for i, (x, y) in enumerate(((-1.85, -1.55), (1.85, -1.55), (-1.85, 1.45), (1.85, 1.45))):
        col = add_cyl(f"pillar_{i}", 0.22, HEIGHT - 0.2, (x, y, (HEIGHT - 0.2) / 2), pillar, verts=12)
        base = add_box(f"pillar_base_{i}", (0.55, 0.55, 0.22), (x, y, 0.11), pillar)
        cap = add_box(f"pillar_cap_{i}", (0.52, 0.52, 0.16), (x, y, HEIGHT - 0.18), pillar)
        parts.extend([col, base, cap])
    # Pradakshina step
    parts.append(add_box("pradakshina", (GARBHA_W + 1.6, 0.90, 0.08), (0, HALF_D + 0.2, 0.04), floor))
    for obj in parts:
        smart_uv(obj, 0.04)
    return parts


def build_lingam(stone: bpy.types.Material, brass: bpy.types.Material) -> bpy.types.Object:
    y = HALF_D + GARBHA_D - 0.70
    yoni = add_cyl("yoni", 0.38, 0.16, (0, y, 0.86), stone, verts=20)
    yoni.scale = (1.15, 0.95, 1.0)
    bpy.ops.object.transform_apply(scale=True)
    lingam = add_uv_sphere("lingam", 0.16, (0, y, 1.12), stone, segs=18)
    lingam.scale = (0.85, 0.85, 1.55)
    bpy.ops.object.transform_apply(scale=True)
    snake = add_torus("naga", 0.18, 0.025, (0, y, 1.18), brass)
    snake.rotation_euler = (math.radians(18), 0, 0)
    bpy.ops.object.transform_apply(rotation=True)
    joined = join_objects("lingam", [yoni, lingam, snake])
    shade_smooth(joined)
    return joined


def build_stand(stone: bpy.types.Material) -> bpy.types.Object:
    top = add_box("stand_top", (1.15, 0.48, 0.06), (0, STAND_Y, STAND_Z), stone)
    leg = add_box("stand_leg", (0.22, 0.22, STAND_Z), (0, STAND_Y, STAND_Z / 2), stone)
    joined = join_objects("offering_stand", [top, leg])
    smart_uv(joined, 0.08)
    return joined


def build_peti(wood: bpy.types.Material, brass: bpy.types.Material) -> bpy.types.Object:
    body = add_box("peti_body", (0.42, 0.28, 0.32), (1.55, 1.35, 0.16), wood)
    lid = add_box("peti_lid", (0.44, 0.30, 0.04), (1.55, 1.35, 0.34), wood)
    slot = add_box("peti_slot", (0.18, 0.04, 0.02), (1.55, 1.35, 0.37), brass)
    joined = join_objects("daan_peti", [body, lid, slot])
    smart_uv(joined, 0.08)
    return joined


def build_ghanta(brass: bpy.types.Material) -> bpy.types.Object:
    bell = add_uv_sphere("ghanta_body", 0.11, (0, 0, 0.08), brass, segs=18)
    bell.scale = (1.0, 1.0, 1.15)
    bpy.ops.object.transform_apply(scale=True)
    bpy.context.view_layer.objects.active = bell
    bpy.ops.object.mode_set(mode="EDIT")
    bm = bmesh.from_edit_mesh(bell.data)
    dead = [v for v in bm.verts if v.co.z < -0.02]
    bmesh.ops.delete(bm, geom=dead, context="VERTS")
    bmesh.update_edit_mesh(bell.data)
    bpy.ops.object.mode_set(mode="OBJECT")
    rim = add_torus("ghanta_rim", 0.11, 0.012, (0, 0, 0.0), brass)
    cap = add_cyl("ghanta_cap", 0.03, 0.04, (0, 0, 0.20), brass, verts=12)
    clapper = add_uv_sphere("ghanta_clapper", 0.018, (0, 0, -0.02), brass, segs=10)
    joined = join_objects("ghanta", [bell, rim, cap, clapper])
    shade_smooth(joined)
    return joined


def build_diya(brass: bpy.types.Material, flame: bpy.types.Material) -> bpy.types.Object:
    bowl = add_uv_sphere("diya_bowl", 0.055, (0, 0, 0.02), brass, segs=16)
    bowl.scale = (1.15, 1.15, 0.55)
    bpy.ops.object.transform_apply(scale=True)
    bpy.context.view_layer.objects.active = bowl
    bpy.ops.object.mode_set(mode="EDIT")
    bm = bmesh.from_edit_mesh(bowl.data)
    dead = [v for v in bm.verts if v.co.z > 0.03]
    bmesh.ops.delete(bm, geom=dead, context="VERTS")
    bmesh.update_edit_mesh(bowl.data)
    bpy.ops.object.mode_set(mode="OBJECT")
    wick = add_cyl("diya_wick", 0.006, 0.03, (0, 0, 0.04), color_mat("wick", (0.25, 0.18, 0.08), 0, 0.8), verts=8)
    fire = add_uv_sphere("diya_flame", 0.018, (0, 0, 0.07), flame, segs=10)
    fire.scale = (0.7, 0.7, 1.4)
    bpy.ops.object.transform_apply(scale=True)
    joined = join_objects("diya", [bowl, wick, fire])
    shade_smooth(joined)
    return joined


def build_thali(brass: bpy.types.Material) -> bpy.types.Object:
    plate = add_cyl("thali_plate", 0.13, 0.012, (0, 0, 0.006), brass, verts=28)
    rim = add_torus("thali_rim", 0.13, 0.008, (0, 0, 0.012), brass)
    handle = add_cyl("thali_handle", 0.012, 0.16, (0, 0, 0.10), brass, verts=10)
    joined = join_objects("aarti_thali", [plate, rim, handle])
    shade_smooth(joined)
    return joined


def build_agarbatti(wood: bpy.types.Material, ash: bpy.types.Material) -> bpy.types.Object:
    stick = add_cyl("agarbatti_stick", 0.004, 0.22, (0, 0, 0.11), wood, verts=6)
    tip = add_uv_sphere("agarbatti_tip", 0.007, (0, 0, 0.22), ash, segs=8)
    return join_objects("agarbatti", [stick, tip])


def build_stand_incense(brass: bpy.types.Material) -> bpy.types.Object:
    bowl = add_cyl("incense_bowl", 0.05, 0.04, (0, 0, 0.02), brass, verts=16)
    sand = add_cyl("incense_sand", 0.045, 0.012, (0, 0, 0.038), color_mat("sand", (0.55, 0.42, 0.22), 0, 0.9), verts=12)
    return join_objects("incense_stand", [bowl, sand])


def build_marigold() -> bpy.types.Object:
    mat = color_mat("marigold", (0.92, 0.62, 0.08), 0.0, 0.55, emit=0.05)
    head = add_uv_sphere("marigold_head", 0.045, (0, 0, 0.03), mat, segs=12)
    head.scale = (1.0, 1.0, 0.7)
    bpy.ops.object.transform_apply(scale=True)
    return join_objects("marigold", [head])


def build_lotus() -> bpy.types.Object:
    mat = color_mat("lotus", (0.86, 0.42, 0.52), 0.0, 0.5)
    head = add_uv_sphere("lotus_head", 0.05, (0, 0, 0.03), mat, segs=12)
    head.scale = (1.1, 0.9, 0.55)
    bpy.ops.object.transform_apply(scale=True)
    return join_objects("lotus", [head])


def build_coconut() -> bpy.types.Object:
    mat = color_mat("coconut", (0.42, 0.26, 0.12), 0.0, 0.65)
    nut = add_uv_sphere("coconut", 0.055, (0, 0, 0.055), mat, segs=14)
    shade_smooth(nut)
    return nut


def build_laddoo() -> bpy.types.Object:
    mat = color_mat("laddoo", (0.78, 0.48, 0.12), 0.0, 0.7)
    sweet = add_uv_sphere("laddoo", 0.035, (0, 0, 0.035), mat, segs=12)
    return sweet


def build_kumkum(brass: bpy.types.Material) -> bpy.types.Object:
    bowl = add_uv_sphere("kumkum_bowl", 0.04, (0, 0, 0.02), brass, segs=12)
    bowl.scale = (1.1, 1.1, 0.5)
    bpy.ops.object.transform_apply(scale=True)
    powder = add_cyl("kumkum_powder", 0.032, 0.01, (0, 0, 0.03), color_mat("kumkum", (0.72, 0.08, 0.06), 0, 0.9, 0.04), verts=12)
    return join_objects("kumkum", [bowl, powder])


def build_coin(brass: bpy.types.Material) -> bpy.types.Object:
    coin = add_cyl("coin", 0.018, 0.003, (0, 0, 0.002), brass, verts=16)
    return coin


def import_gltf(folder: str) -> bpy.types.Object:
    path = next((SOURCES / "models" / folder).glob("*.gltf"))
    before = set(bpy.data.objects)
    bpy.ops.import_scene.gltf(filepath=str(path))
    imported = [obj for obj in bpy.data.objects if obj not in before and obj.type == "MESH"]
    if not imported:
        raise RuntimeError(f"no mesh in {folder}")
    for obj in imported:
        bpy.ops.object.select_all(action="DESELECT")
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    joined = join_objects(folder, imported)
    # Poly Haven models are millimetres.
    if max(joined.dimensions) > 5:
        joined.scale = (0.001, 0.001, 0.001)
        bpy.ops.object.select_all(action="DESELECT")
        joined.select_set(True)
        bpy.context.view_layer.objects.active = joined
        bpy.ops.object.transform_apply(scale=True)
    return joined


def fit_to_height(obj: bpy.types.Object, height: float) -> None:
    dim = max(obj.dimensions.z, 0.001)
    obj.scale = (height / dim,) * 3
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.transform_apply(scale=True)


def origin_to_bottom(obj: bpy.types.Object) -> None:
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.origin_set(type="ORIGIN_GEOMETRY", center="BOUNDS")
    lowest = min((obj.matrix_world @ Vector(c) for c in obj.bound_box), key=lambda v: v.z)
    obj.location.z -= lowest.z
    bpy.ops.object.transform_apply(location=True)


def duplicate_at(obj: bpy.types.Object, name: str, loc: tuple[float, float, float], rot_z: float = 0.0) -> bpy.types.Object:
    copy = obj.copy()
    copy.data = obj.data.copy()
    copy.name = name
    bpy.context.collection.objects.link(copy)
    copy.location = loc
    copy.rotation_euler = (0, 0, rot_z)
    bpy.ops.object.select_all(action="DESELECT")
    copy.select_set(True)
    bpy.context.view_layer.objects.active = copy
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
    return copy


def add_lights() -> None:
    bpy.ops.object.light_add(type="AREA", location=(0, HALF_D + GARBHA_D - 0.4, 2.4))
    garbha = bpy.context.active_object
    garbha.data.energy = 80
    garbha.data.size = 0.6
    garbha.data.color = (1.0, 0.72, 0.38)
    garbha.rotation_euler = (math.radians(110), 0, 0)
    bpy.ops.object.light_add(type="AREA", location=(0, -HALF_D - 1.2, 2.6))
    door = bpy.context.active_object
    door.data.energy = 220
    door.data.size = 2.4
    door.data.color = (1.0, 0.88, 0.7)
    door.rotation_euler = (math.radians(75), 0, 0)
    bpy.ops.object.light_add(type="POINT", location=(0, 0.2, 3.1))
    hang = bpy.context.active_object
    hang.data.energy = 18
    hang.data.color = (1.0, 0.62, 0.28)
    hang.data.shadow_soft_size = 0.4


def export_glb(objects: list[bpy.types.Object], path: Path) -> None:
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(path),
        export_format="GLB",
        use_selection=True,
        export_apply=True,
        export_texcoords=True,
        export_normals=True,
        export_materials="EXPORT",
        export_image_format="JPEG",
        export_jpeg_quality=80,
        export_lights=True,
        export_cameras=False,
        export_yup=True,
    )


def hide_for_export(objects: list[bpy.types.Object], hide: bool) -> None:
    for obj in objects:
        obj.hide_set(hide)
        obj.hide_render = hide


def main() -> None:
    reset_scene()
    floor = make_pbr("floor", "granite_tile", 4.0)
    walls = make_pbr("walls", "red_sandstone_pavement", 2.4)
    pillars = make_pbr("pillars", "sandstone_cracks", 1.6)
    garbha = make_pbr("garbha", "red_sandstone_tiles", 2.2)
    ceiling = make_pbr("ceiling", "marble_01", 2.0)
    brass = color_mat("brass", (0.72, 0.52, 0.18), 1.0, 0.28, emit=0.02)
    wood = color_mat("wood", (0.28, 0.16, 0.08), 0.0, 0.7)
    flame = color_mat("flame", (1.0, 0.72, 0.22), 0.0, 0.4, emit=6.0)
    ash = color_mat("ash", (0.22, 0.18, 0.14), 0.0, 0.9, emit=1.4)

    architecture = build_architecture(floor, walls, pillars, garbha, ceiling)
    lingam = build_lingam(pillars, brass)
    stand = build_stand(pillars)
    peti = build_peti(wood, brass)
    incense_stand = build_stand_incense(brass)
    incense_stand.location = (0.38, STAND_Y, STAND_Z + 0.03)

    lantern = import_gltf("brass_diya_lantern")
    fit_to_height(lantern, 0.42)
    origin_to_bottom(lantern)
    hanging = [
        duplicate_at(lantern, "lantern_c", (0.0, 0.15, 3.15)),
        duplicate_at(lantern, "lantern_l", (-1.35, 0.9, 3.05)),
        duplicate_at(lantern, "lantern_r", (1.35, 0.9, 3.05)),
    ]
    bpy.data.objects.remove(lantern, do_unlink=True)

    kalash = import_gltf("brass_pot_01")
    fit_to_height(kalash, 0.32)
    origin_to_bottom(kalash)
    pots = [
        duplicate_at(kalash, "kalash_l", (-0.85, -HALF_D + 0.28, 0.0)),
        duplicate_at(kalash, "kalash_r", (0.85, -HALF_D + 0.28, 0.0)),
    ]
    bpy.data.objects.remove(kalash, do_unlink=True)

    lota = import_gltf("brass_vase_03")
    fit_to_height(lota, 0.16)
    origin_to_bottom(lota)
    lota.location = (-0.38, STAND_Y, STAND_Z + 0.03)
    lota.name = "lota"

    bananas = import_gltf("bananas")
    fit_to_height(bananas, 0.12)
    origin_to_bottom(bananas)
    pom = import_gltf("food_pomegranate_01")
    fit_to_height(pom, 0.08)
    origin_to_bottom(pom)

    add_lights()

    walls = join_objects("sanctum_shell", architecture)
    furniture = join_objects("sanctum_furniture", [lingam, stand, peti, incense_stand, lota] + hanging + pots)
    static = [walls, furniture]
    print("Exporting sanctum…", flush=True)
    export_glb(static, OUT / "sanctum.glb")

    # Procedural grabbables at the origin, each as its own file
    props = {
        "ghanta": build_ghanta(brass),
        "diya": build_diya(brass, flame),
        "aarti_thali": build_thali(brass),
        "agarbatti": build_agarbatti(wood, ash),
        "marigold": build_marigold(),
        "lotus": build_lotus(),
        "coconut": build_coconut(),
        "laddoo": build_laddoo(),
        "kumkum": build_kumkum(brass),
        "coin": build_coin(brass),
        "bananas": bananas,
        "pomegranate": pom,
    }
    for name, obj in props.items():
        origin_to_bottom(obj)
        export_glb([obj], OUT / "props" / f"{name}.glb")

    courtyard_src = SOURCES / "images" / "courtyard_360.jpg"
    courtyard_dst = OUT / "courtyard.jpg"
    if courtyard_src.exists():
        image = bpy.data.images.load(str(courtyard_src))
        image.scale(2048, 1024)
        image.filepath_raw = str(courtyard_dst)
        image.file_format = "JPEG"
        image.save()

    markers = {
        "spawn": [0.0, 0.0, 1.55],
        "garbha": [0.0, 0.0, - (HALF_D + GARBHA_D - 0.7)],
        "darshan": [0.0, NICHE_Z, -(HALF_D + GARBHA_D - 0.22)],
        "stand": [0.0, STAND_Z, -STAND_Y],
        "ghanta": [0.85, 1.55, -0.55],
        "peti": [1.55, 0.16, -1.35],
        "feet": [0.0, 0.82, -(HALF_D + 0.35)],
        "teleport_mandap": [0.0, 0.0, 1.55],
        "teleport_threshold": [0.0, 0.0, -2.05],
        "teleport_pradakshina": [1.55, 0.0, -0.4],
        "door": [0.0, DOOR_H / 2, HALF_D + 0.1],
    }
    (OUT / "markers.json").write_text(json.dumps(markers, indent=2))
    print("Wrote", OUT)
    for path in sorted(OUT.rglob("*")):
        if path.is_file():
            print(f"  {path.relative_to(OUT)}  {path.stat().st_size}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("BUILD FAILED:", error, file=sys.stderr)
        raise
