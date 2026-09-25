# @spec spec://modules/learning/FEAT-010-learning-demo#course-map
"""Render Blender platform reference images; Android platforms are Canvas-drawn."""

from math import asin, sqrt, tan
from pathlib import Path

import bpy
from mathutils import Matrix, Vector


ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "reference-renders" / "platforms"
OUTPUT.mkdir(parents=True, exist_ok=True)
WIDTH, HEIGHT = 384, 320
TILE_WIDTH, TILE_HEIGHT = 74, 52  # CourseApp.kt courseMapGrid.
ORTHO_SCALE = 4.05
CAMERA_COMPONENT = 12.0

PALETTES = {
    "current": {
        "top": (0.025, 0.20, 0.46), "side": (0.02, 0.10, 0.25),
        "edge": (1.00, 0.58, 0.18), "face": (0.98, 0.99, 1.00),
        "ink": (0.04, 0.12, 0.27),
    },
    "available": {
        "top": (0.035, 0.23, 0.51), "side": (0.02, 0.11, 0.28),
        "edge": (0.52, 0.83, 1.00), "face": (0.98, 0.99, 1.00),
        "ink": (0.035, 0.12, 0.29),
    },
    "completed": {
        "top": (0.025, 0.34, 0.27), "side": (0.015, 0.17, 0.15),
        "edge": (0.55, 0.96, 0.79), "face": (0.98, 1.00, 0.99),
        "ink": (0.02, 0.18, 0.16),
    },
    "locked": {
        "top": (0.22, 0.31, 0.46), "side": (0.11, 0.17, 0.28),
        "edge": (0.82, 0.88, 0.96), "face": (0.28, 0.39, 0.58),
        "ink": (0.22, 0.30, 0.42),
    },
}


def make_material(name, color, metallic=0.08, roughness=0.28):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = (*color, 1)
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = roughness
    return mat


def aim(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def main():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    ratio = TILE_HEIGHT / TILE_WIDTH
    elevation = asin(ratio)
    camera_height = sqrt(2) * CAMERA_COMPONENT * tan(elevation)
    bpy.ops.object.camera_add(location=(-CAMERA_COMPONENT, -CAMERA_COMPONENT, camera_height))
    camera = bpy.context.object
    camera.name = "Course-map orthographic isometric camera"
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = ORTHO_SCALE
    aim(camera, (0, 0, 0.44))
    bpy.context.view_layer.update()
    basis = camera.matrix_world.to_3x3()
    right = (basis @ Vector((1, 0, 0))).normalized()
    up = (basis @ Vector((0, 1, 0))).normalized()
    back = (basis @ Vector((0, 0, 1))).normalized()

    world = bpy.data.worlds.new("Cool ambient | transparent")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.44, 0.57, 0.75, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.25

    lights = (
        ("Large soft key", 950, (1.00, 0.84, 0.68), -right * 4 + up * 5 + back * 4, 3.4),
        ("Frontal cool fill", 620, (0.62, 0.82, 1.00), right * 4 + up * 1 + back * 4, 4.0),
        ("Rear edge light", 760, (0.38, 0.72, 1.00), -back * 4 + up * 3, 2.8),
    )
    for name, power, color, offset, size in lights:
        bpy.ops.object.light_add(type="AREA", location=offset + Vector((0, 0, 0.4)))
        light = bpy.context.object
        light.name = name
        light.data.energy = power
        light.data.shape = "DISK"
        light.data.size = size
        light.data.color = color
        aim(light, (0, 0, 0.35))

    top = make_material("Lesson platform | upper ceramic", PALETTES["current"]["top"], 0.12)
    side = make_material("Lesson platform | deep blue sides", PALETTES["current"]["side"], 0.12)
    edge = make_material("Lesson platform | bevel inlay", PALETTES["current"]["edge"], 0.16)
    numeral_face = make_material("Raised numeral | enamel", PALETTES["current"]["face"], 0.05, 0.23)
    numeral_side = make_material("Raised numeral | bevel shadow", PALETTES["current"]["ink"], 0.05, 0.3)

    bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 0.22))
    platform = bpy.context.object
    platform.name = "Rounded 3D lesson tile | editable master"
    platform.dimensions = (2.48, 2.48, 0.44)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    platform.data.materials.clear()
    platform.data.materials.append(top)
    platform.data.materials.append(side)
    platform.data.materials.append(edge)
    for face in platform.data.polygons:
        face.material_index = 0 if face.normal.z > 0.9 else 1
    bevel = platform.modifiers.new("Broad highlight-catching chamfer", "BEVEL")
    bevel.width = 0.16
    bevel.segments = 6
    bevel.material = 2
    platform.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")

    font_path = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    font = bpy.data.fonts.load(font_path) if Path(font_path).exists() else bpy.data.fonts.get("Bfont")
    curve = bpy.data.curves.new("Raised centered lesson number", "FONT")
    curve.body = "1"
    curve.font = font
    curve.align_x = "CENTER"
    curve.align_y = "CENTER"
    curve.size = 1.90
    curve.extrude = 0.14
    curve.bevel_depth = 0.026
    curve.bevel_resolution = 3
    curve.materials.append(numeral_face)
    curve.materials.append(numeral_side)
    numeral = bpy.data.objects.new("Lesson number | embossed 1", curve)
    bpy.context.collection.objects.link(numeral)
    numeral.rotation_mode = "QUATERNION"
    numeral.rotation_quaternion = Matrix((right, up, back)).transposed().to_quaternion()
    numeral.location = Vector((0, 0, 0.44 + 0.12)) + back * 0.14

    shadow_curve = curve.copy()
    shadow_curve.name = "Raised numeral | dark extrusion silhouette"
    shadow_curve.extrude = 0.18
    shadow_curve.bevel_depth = 0.018
    shadow_curve.materials.clear()
    shadow_curve.materials.append(numeral_side)
    shadow_number = bpy.data.objects.new("Lesson number | visible extruded edge", shadow_curve)
    bpy.context.collection.objects.link(shadow_number)
    shadow_number.rotation_mode = "QUATERNION"
    shadow_number.rotation_quaternion = numeral.rotation_quaternion.copy()
    shadow_number.location = numeral.location - right * 0.025 - up * 0.075 - back * 0.10

    scene = bpy.context.scene
    scene.world = world
    scene.camera = camera
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 20
    scene.cycles.use_denoising = True
    scene.render.resolution_x = WIDTH
    scene.render.resolution_y = HEIGHT
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.render.image_settings.compression = 100
    scene.render.film_transparent = True
    scene.view_settings.view_transform = "AgX"
    if "AgX - Medium High Contrast" in scene.view_settings.bl_rna.properties["look"].enum_items.keys():
        scene.view_settings.look = "AgX - Medium High Contrast"
    scene.render.filepath = str(OUTPUT / "map_node_current_1.png")

    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "course_map_platforms.blend"))

    for state, colors in PALETTES.items():
        top.diffuse_color = (*colors["top"], 1)
        top.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*colors["top"], 1)
        side.diffuse_color = (*colors["side"], 1)
        side.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*colors["side"], 1)
        edge.diffuse_color = (*colors["edge"], 1)
        edge.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*colors["edge"], 1)
        numeral_face.diffuse_color = (*colors["face"], 1)
        numeral_side.diffuse_color = (*colors["ink"], 1)
        numeral_side.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*colors["ink"], 1)
        for number in range(1, 11):
            curve.body = str(number)
            shadow_curve.body = str(number)
            numeral.name = f"Lesson number | embossed {number} | {state}"
            shadow_number.name = f"Lesson number | extruded edge {number} | {state}"
            numeral.location = Vector((0, 0, 0.56)) + back * 0.14
            if number == 1:
                numeral.location -= right * 0.07
            shadow_number.location = numeral.location - right * 0.025 - up * 0.075 - back * 0.10
            output = OUTPUT / f"map_node_{state}_{number}.png"
            scene.render.filepath = str(output)
            bpy.context.view_layer.update()
            bpy.ops.render.render(write_still=True)
            print(f"Rendered {output.name}")

    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "course_map_platforms.blend"))
    print(f"Saved editable platform scene: {ROOT / 'course_map_platforms.blend'}")


if __name__ == "__main__":
    main()
