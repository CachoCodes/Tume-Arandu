# @spec spec://modules/learning/FEAT-010-learning-demo#course-map
"""Build reference Blender models; the Android map is drawn in CourseApp.kt."""

from math import asin, cos, radians, sin, sqrt, tan
from pathlib import Path

import bpy
from mathutils import Matrix, Vector


ROOT = Path(__file__).resolve().parent
RESOURCE_DIR = ROOT / "reference-renders"
RESOURCE_DIR.mkdir(parents=True, exist_ok=True)

WIDTH = 768
HEIGHT = 512
# Keep these in sync with CourseApp.kt's courseMapGrid (74×52 dp). At 45°
# azimuth, sin(camera elevation) equals the diamond's half-height/half-width.
MAP_TILE_WIDTH = 74
MAP_TILE_HEIGHT = 52
TRIANGLE = ((-1.62, 1.00), (1.62, 1.00), (1.62, 2.87))
TRIANGLE_YAW = radians(28)
PREVIEW_PATH = Path("/tmp/guarani-triangle-formulas-preview.png")
MAP_SCENE_PATH = ROOT / "course_map_approval.blend"
MAP_RENDER_PATH = ROOT / "course_map_approval.png"
ASSET_BORDER = {
    "xmin": 86 / 512,
    "xmax": 426 / 512,
    "ymin": 1 - 333 / 342,
    "ymax": 1.0,
}


def material(name, color, metallic=0.0, roughness=0.3):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    shader = next(node for node in mat.node_tree.nodes if node.type == "BSDF_PRINCIPLED")
    shader.inputs["Base Color"].default_value = (*color, 1)
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = roughness
    return mat


def background_node(world):
    return next(node for node in world.node_tree.nodes if node.type == "BACKGROUND")


def rounded_box(name, location, dimensions, mat, bevel):
    bpy.ops.mesh.primitive_cube_add(size=1, location=location)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(mat)
    edge = obj.modifiers.new("Large clean light-catching bevels", "BEVEL")
    edge.width = bevel
    edge.segments = 6
    obj.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")
    return obj


def curve_line(name, points, mat, thickness=0.035):
    curve = bpy.data.curves.new(name, "CURVE")
    curve.dimensions = "3D"
    curve.resolution_u = 1
    curve.bevel_depth = thickness
    curve.bevel_resolution = 3
    spline = curve.splines.new("POLY")
    spline.points.add(len(points) - 1)
    for point, (x, y, z) in zip(spline.points, points):
        point.co = (x, y, z, 1)
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat)
    return obj


def parent_keep_world(obj, parent):
    bpy.context.view_layer.update()
    world = obj.matrix_world.copy()
    obj.parent = parent
    obj.matrix_parent_inverse = parent.matrix_world.inverted()
    obj.matrix_world = world


def make_text(name, body, location, size, mat, angle=0.0, font=None):
    curve = bpy.data.curves.new(name, "FONT")
    curve.body = body
    if font:
        curve.font = font
    curve.align_x = "CENTER"
    curve.align_y = "CENTER"
    curve.size = size
    curve.extrude = 0.045
    curve.bevel_depth = 0.009
    curve.bevel_resolution = 3
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    # The text faces the camera; the second angle rotates its baseline within
    # the XZ triangle face while keeping its normal toward the viewer (-Y).
    obj.rotation_euler = (radians(90), radians(-angle), 0)
    obj.data.materials.append(mat)
    return obj


def set_text_to_fit(obj, max_width):
    bpy.context.view_layer.update()
    width = obj.dimensions.x
    if width > max_width:
        obj.scale *= max_width / width
        bpy.context.view_layer.update()


def make_triangle(name, face_mat, edge_mat, highlight_mat, ink_mat, font, highlight):
    before = {obj.as_pointer() for obj in bpy.context.scene.objects}
    left, right, top = TRIANGLE
    depth = 1.05
    front = [(x, -depth / 2, z) for x, z in TRIANGLE]
    back = [(x, depth / 2, z) for x, z in TRIANGLE]
    mesh = bpy.data.meshes.new(f"{name} | solid 30-60-90 wedge mesh")
    mesh.from_pydata(front + back, [], [
        (0, 1, 2), (5, 4, 3), (3, 4, 1, 0), (4, 5, 2, 1), (5, 3, 0, 2),
    ])
    mesh.materials.append(face_mat)
    mesh.materials.append(edge_mat)
    for polygon in mesh.polygons:
        polygon.material_index = 0 if polygon.index == 0 else 1
    wedge = bpy.data.objects.new(f"{name} | beveled perspective triangle", mesh)
    bpy.context.collection.objects.link(wedge)
    bevel = wedge.modifiers.new("Wide rounded silhouette catches key light", "BEVEL")
    bevel.width = 0.055
    bevel.segments = 5
    wedge.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")

    line_y = -depth / 2 - 0.018
    base_points = [(left[0], line_y, left[1]), (right[0], line_y, right[1])]
    vertical_points = [(right[0], line_y, right[1]), (top[0], line_y, top[1])]
    hypotenuse_points = [(top[0], line_y, top[1]), (left[0], line_y, left[1])]

    active = {"sin": "opposite", "cos": "base"}[highlight]
    active_points = {"base": base_points, "opposite": vertical_points}[active]
    curve_line(f"{name} | highlighted {active} leg", active_points, highlight_mat, 0.05)

    # The arc follows only the 30° wedge between the base and hypotenuse.
    origin_x, origin_z = left
    arc_radius = 0.46
    arc = [
        (origin_x + arc_radius * cos(radians(i * 3)), line_y - 0.018,
         origin_z + arc_radius * sin(radians(i * 3)))
        for i in range(11)
    ]
    curve_line(f"{name} | 30 degree angle arc", arc, highlight_mat, 0.034)
    bisector = radians(15)
    label_radius = 0.78
    angle_label = (
        origin_x + label_radius * cos(bisector),
        line_y - 0.06,
        origin_z + label_radius * sin(bisector),
    )
    make_text(f"{name} | raised 30 degree label", "30°", angle_label, 0.52, ink_mat, angle=15, font=font)

    # Keep the 90° label at the corner without drawing a square over the face.
    corner = right
    make_text(
        f"{name} | raised 90 degree label", "90°",
        (corner[0] + 0.30, line_y - 0.06, corner[1] + 0.16), 0.36, ink_mat, font=font,
    )

    # Side lengths sit inside and follow their corresponding triangle edges.
    text_y = line_y - 0.045
    make_text(f"{name} | adjacent side sqrt 3", "√3", (0.0, text_y, 1.28), 0.58, ink_mat, font=font)
    make_text(f"{name} | opposite side 1", "1", (1.35, text_y, 2.38), 0.58, ink_mat, font=font)
    inward = (sin(radians(30)), -cos(radians(30)))
    hypotenuse_fraction = 0.73
    two_position = (
        left[0] + (top[0] - left[0]) * hypotenuse_fraction + inward[0] * 0.55,
        text_y,
        left[1] + (top[1] - left[1]) * hypotenuse_fraction + inward[1] * 0.55,
    )
    make_text(f"{name} | hypotenuse 2", "2", two_position, 0.64, ink_mat, angle=30, font=font)

    # A slight yaw reveals the prism depth while keeping the shared map camera.
    triangle_root = bpy.data.objects.new(f"{name} | angled triangle group", None)
    bpy.context.collection.objects.link(triangle_root)
    triangle_root.location = (0, 0, (left[1] + top[1]) / 2)
    bpy.context.view_layer.update()
    inverse = triangle_root.matrix_world.inverted()
    for obj in bpy.context.scene.objects:
        if obj.as_pointer() in before or obj is triangle_root:
            continue
        if obj.type not in {"MESH", "CURVE", "FONT"}:
            continue
        world = obj.matrix_world.copy()
        obj.parent = triangle_root
        obj.matrix_parent_inverse = inverse
        obj.matrix_world = world
    triangle_root.rotation_euler.z = TRIANGLE_YAW
    return triangle_root


def find_font():
    candidates = (
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial.ttf",
    )
    for path in candidates:
        if Path(path).exists():
            return bpy.data.fonts.load(path)
    return bpy.data.fonts.get("Bfont")


def create_ornament(name, equation, highlight, ink, ceramic, body_mat, trim_mat, font):
    before = {obj.as_pointer() for obj in bpy.context.scene.objects}
    rounded_box(f"{name} | raised deep-blue formula pedestal", (0, 0, 0.55), (4.80, 0.90, 0.90), body_mat, 0.19)
    rounded_box(f"{name} | bright ceramic formula face", (0, -0.478, 0.55), (4.52, 0.12, 0.62), ceramic, 0.115)
    formula = make_text(f"{name} | fully extruded 3D formula", equation, (0, -0.555, 0.55), 0.74, ink, font=font)
    set_text_to_fit(formula, 4.15)

    make_triangle(name, ceramic, body_mat, trim_mat, ink, font, highlight)
    return [
        obj for obj in bpy.context.scene.objects
        if obj.as_pointer() not in before and obj.parent is None
    ]


def aim(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def camera_axes(camera):
    bpy.context.view_layer.update()
    basis = camera.matrix_world.to_3x3()
    return (
        (basis @ Vector((1, 0, 0))).normalized(),
        (basis @ Vector((0, 1, 0))).normalized(),
        (basis @ Vector((0, 0, 1))).normalized(),
    )


def set_studio_lights(target, right, up, back):
    lights = {
        "Large warm key softbox | upper left": (target - right * 4.0 + up * 5.0 + back * 4.0),
        "Bright frontal cool fill | readable text": (target + right * 4.5 + up * 1.0 + back * 4.0),
        "Cool rear rim | visible prism depth": (target - back * 4.0 + up * 4.0),
    }
    for name, location in lights.items():
        light = bpy.data.objects[name]
        light.location = location
        aim(light, target)


def create_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablock in bpy.data.materials:
        bpy.data.materials.remove(datablock)

    deep_blue = material("01 | midnight cobalt enamel", (0.012, 0.055, 0.19), 0.35, 0.23)
    ceramic = material("02 | luminous glacier ceramic", (0.63, 0.86, 0.98), 0.10, 0.24)
    ink = material("03 | dark raised typography", (0.008, 0.035, 0.105), 0.0, 0.44)
    gold = material("04 | warm amber anodized detail", (1.0, 0.39, 0.045), 0.30, 0.20)
    font = find_font()

    sine_parts = create_ornament("SIN 30", "sin 30° = 1/2", "sin", ink, ceramic, deep_blue, gold, font)
    sine = bpy.data.objects.new("SIN 30 | render collection root", None)
    bpy.context.collection.objects.link(sine)
    for obj in sine_parts:
        obj.parent = sine
    cosine_parts = create_ornament("COS 30", "cos 30° = √3/2", "cos", ink, ceramic, deep_blue, gold, font)
    cosine = bpy.data.objects.new("COS 30 | render collection root", None)
    bpy.context.collection.objects.link(cosine)
    for obj in cosine_parts:
        obj.parent = cosine
    world = bpy.data.worlds.new("Balanced cool studio | transparent render")
    world.use_nodes = True
    background = background_node(world)
    background.inputs["Color"].default_value = (0.21, 0.28, 0.40, 1)
    background.inputs["Strength"].default_value = 0.19

    camera_component = 12.0
    map_elevation = asin(MAP_TILE_HEIGHT / MAP_TILE_WIDTH)
    camera_height = sqrt(2) * camera_component * tan(map_elevation)
    bpy.ops.object.camera_add(location=(-camera_component, -camera_component, camera_height))
    camera = bpy.context.object
    camera.name = "Course-map orthographic isometric camera"
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 15.0
    aim(camera, (0, 0, 0))
    right, up, back = camera_axes(camera)

    # Keep the vertical equation face readable toward the user camera; its
    # ground support remains on the same world-XY diamond plane as map tiles.
    forward = -back
    prop_rotation = Matrix((right, forward, up)).transposed().to_quaternion()
    for prop, horizontal_offset in ((sine, -3.25), (cosine, 3.25)):
        prop.rotation_mode = "QUATERNION"
        prop.rotation_quaternion = prop_rotation
        prop.location = right * horizontal_offset + Vector((0, 0, 0.28))
        tile_center = (prop.location.x, prop.location.y)
        base = rounded_box(
            f"{prop.name} | isometric navy map-stone",
            (*tile_center, 0.16), (3.40, 3.40, 0.32), deep_blue, 0.12,
        )
        top = rounded_box(
            f"{prop.name} | pale diamond top face",
            (*tile_center, 0.36), (3.12, 3.12, 0.08), ceramic, 0.06,
        )
        parent_keep_world(base, prop)
        parent_keep_world(top, prop)

    bpy.ops.object.light_add(type="AREA", location=(0, 0, 0))
    key = bpy.context.object
    key.name = "Large warm key softbox | upper left"
    key.data.energy = 1200
    key.data.shape = "DISK"
    key.data.size = 4.0
    key.data.color = (1.0, 0.84, 0.67)

    bpy.ops.object.light_add(type="AREA", location=(0, 0, 0))
    fill = bpy.context.object
    fill.name = "Bright frontal cool fill | readable text"
    fill.data.energy = 700
    fill.data.shape = "RECTANGLE"
    fill.data.size = 5.0
    fill.data.size_y = 3.0
    fill.data.color = (0.56, 0.82, 1.0)

    bpy.ops.object.light_add(type="AREA", location=(0, 0, 0))
    rim = bpy.context.object
    rim.name = "Cool rear rim | visible prism depth"
    rim.data.energy = 950
    rim.data.shape = "DISK"
    rim.data.size = 3.2
    rim.data.color = (0.32, 0.72, 1.0)
    set_studio_lights(Vector((0, 0, 0)), right, up, back)

    scene = bpy.context.scene
    scene.world = world
    scene.camera = camera
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 40
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
    scene.render.filepath = str(RESOURCE_DIR / "map_sin30_formula.png")
    return scene, camera, sine, cosine, font


def render_ornament(scene, camera, shown, hidden, output):
    shown.hide_render = False
    hidden.hide_render = True
    for child in shown.children_recursive:
        child.hide_render = False
    for child in hidden.children_recursive:
        child.hide_render = True
    right, up, back = camera_axes(camera)
    target = shown.location + up * 0.45
    camera.data.ortho_scale = 7.4
    camera.location = target + back * 14.0
    aim(camera, target)
    set_studio_lights(target, right, up, back)
    scene.render.use_border = True
    scene.render.use_crop_to_border = True
    scene.render.border_min_x = ASSET_BORDER["xmin"]
    scene.render.border_max_x = ASSET_BORDER["xmax"]
    scene.render.border_min_y = ASSET_BORDER["ymin"]
    scene.render.border_max_y = ASSET_BORDER["ymax"]
    scene.render.filepath = str(output)
    bpy.context.view_layer.update()
    bpy.ops.render.render(write_still=True)
    scene.render.use_border = False
    scene.render.use_crop_to_border = False


def render_pair_preview(scene, camera, right, up, back):
    target = Vector((0, 0, 1.0))
    camera.data.ortho_scale = 15.0
    camera.location = target + back * 19.0
    aim(camera, target)
    right, up, back = camera_axes(camera)
    set_studio_lights(target, right, up, back)

    scene.render.resolution_x = 1200
    scene.render.resolution_y = 760
    scene.render.use_border = False
    scene.render.use_crop_to_border = False
    scene.render.film_transparent = False
    background = background_node(scene.world)
    background.inputs["Color"].default_value = (0.035, 0.065, 0.10, 1)
    background.inputs["Strength"].default_value = 1.0
    scene.render.filepath = str(PREVIEW_PATH)
    bpy.context.view_layer.update()
    bpy.ops.render.render(write_still=True)

    scene.render.resolution_x = WIDTH
    scene.render.resolution_y = HEIGHT
    scene.render.use_border = False
    scene.render.use_crop_to_border = False
    scene.render.film_transparent = True
    background.inputs["Color"].default_value = (0.21, 0.28, 0.40, 1)
    background.inputs["Strength"].default_value = 0.19


def ground_point_for_screen(right, up, screen_x, screen_y):
    determinant = right.x * up.y - right.y * up.x
    world_x = (screen_x * up.y - right.y * screen_y) / determinant
    world_y = (right.x * screen_y - screen_x * up.x) / determinant
    return Vector((world_x, world_y, 0))


def create_checkerboard(body_mat, light_mat, dark_mat):
    count = 52
    pitch = 1.5
    tile_size = 1.46
    bottom, top = -0.055, 0.045
    start = -(count - 1) * pitch / 2
    vertices = []
    faces = []
    material_indices = []
    face_specs = (
        ((3, 2, 1, 0), 2), ((4, 5, 6, 7), None),
        ((0, 1, 5, 4), 2), ((1, 2, 6, 5), 2),
        ((2, 3, 7, 6), 2), ((3, 0, 4, 7), 2),
    )
    half = tile_size / 2
    for row in range(count):
        center_y = start + row * pitch
        for column in range(count):
            center_x = start + column * pitch
            first = len(vertices)
            vertices.extend((
                (center_x - half, center_y - half, bottom),
                (center_x + half, center_y - half, bottom),
                (center_x + half, center_y + half, bottom),
                (center_x - half, center_y + half, bottom),
                (center_x - half, center_y - half, top),
                (center_x + half, center_y - half, top),
                (center_x + half, center_y + half, top),
                (center_x - half, center_y + half, top),
            ))
            light_square = (row + column) % 2 == 0
            for indices, face_material in face_specs:
                faces.append(tuple(first + index for index in indices))
                if face_material is None:
                    material_indices.append(0 if light_square else 1)
                else:
                    material_indices.append(face_material)

    mesh = bpy.data.meshes.new("52 by 52 frost checker | beveled square slabs")
    mesh.from_pydata(vertices, [], faces)
    mesh.materials.append(light_mat)
    mesh.materials.append(dark_mat)
    mesh.materials.append(body_mat)
    for polygon, material_index in zip(mesh.polygons, material_indices):
        polygon.material_index = material_index
    board = bpy.data.objects.new("Map floor | continuous icy checkerboard", mesh)
    bpy.context.collection.objects.link(board)
    bevel = board.modifiers.new("Small rounded ice tile edges", "BEVEL")
    bevel.width = 0.018
    bevel.segments = 2
    board.modifiers.new("Weighted ice tile normals", "WEIGHTED_NORMAL")
    rounded_box(
        "Map floor | deep-blue foundation slab", (0, 0, -0.19),
        (count * pitch, count * pitch, 0.27), body_mat, 0.12,
    )
    return board


def create_map_number(name, number, point, mat, font):
    curve = bpy.data.curves.new(name, "FONT")
    curve.body = str(number)
    curve.font = font
    curve.align_x = "CENTER"
    curve.align_y = "CENTER"
    curve.size = 0.78
    curve.extrude = 0.035
    curve.bevel_depth = 0.008
    curve.bevel_resolution = 3
    label = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(label)
    label.location = point
    label.rotation_euler.z = radians(-45)
    label.data.materials.append(mat)
    return label


def create_map_node(number, screen_point, materials, font):
    point = ground_point_for_screen(*screen_point)
    body, top, ink = materials
    pedestal = rounded_box(
        f"Lesson {number} | raised cobalt square platform",
        (point.x, point.y, 0.30), (2.15, 2.15, 0.50), body, 0.18,
    )
    surface = rounded_box(
        f"Lesson {number} | pale ice top face",
        (point.x, point.y, 0.605), (1.86, 1.86, 0.13), body, 0.10,
    )
    surface.data.materials.append(top)
    for polygon in surface.data.polygons:
        if polygon.normal.z > 0.9:
            polygon.material_index = 1
    create_map_number(
        f"Lesson {number} | embossed course number", number,
        (point.x, point.y, 0.675), ink, font,
    )
    return point


def create_approval_map(scene, camera, sine, cosine, font, back):
    bpy.ops.object.select_all(action="DESELECT")
    ice_grout = material("10 | deep frozen tile seams", (0.035, 0.13, 0.23), 0.12, 0.52)
    ice_light = material("11 | pale blue frost squares", (0.29, 0.60, 0.75), 0.08, 0.40)
    ice_dark = material("12 | shaded blue frost squares", (0.19, 0.43, 0.60), 0.08, 0.43)
    node_body = material("13 | cobalt lesson platform sides", (0.025, 0.12, 0.25), 0.24, 0.29)
    node_top = material("14 | porcelain lesson faces", (0.70, 0.88, 0.96), 0.05, 0.32)
    active_top = material("15 | available lesson mint face", (0.42, 0.80, 0.42), 0.05, 0.34)
    map_ink = material("16 | dark embossed lesson numerals", (0.015, 0.08, 0.17), 0.0, 0.42)
    route_mat = material("17 | bright cyan route inlay", (0.055, 0.52, 0.83), 0.18, 0.24)
    create_checkerboard(ice_grout, ice_light, ice_dark)

    right, up, _ = camera_axes(camera)
    route = []
    for index in range(8):
        x = -3.65 if index % 2 == 0 else 3.65
        y = -8.75 + index * 2.5
        route.append((x, y))
    map_points = [ground_point_for_screen(right, up, x, y) for x, y in route]
    route_points = [(point.x, point.y, 0.075) for point in map_points]
    curve_line("Map route | single rising zigzag inlay", route_points, route_mat, 0.07)

    for number, screen_point in enumerate(route, start=1):
        top_mat = active_top if number == 1 else node_top
        create_map_node(number, (right, up, *screen_point), (node_body, top_mat, map_ink), font)

    prop_rotation = Matrix((right, -back, up)).transposed().to_quaternion()
    for prop, screen_x, screen_y in ((sine, -6.30, -6.25), (cosine, 6.30, 6.25)):
        prop.rotation_mode = "QUATERNION"
        prop.rotation_quaternion = prop_rotation
        prop.scale = (0.78, 0.78, 0.78)
        point = ground_point_for_screen(right, up, screen_x, screen_y)
        prop.location = point + Vector((0, 0, 0.28))
        prop.hide_render = False
        for child in prop.children_recursive:
            child.hide_render = False

    target = Vector((0, 0, 0.85))
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 26.5
    camera.location = target + back * 32.0
    aim(camera, target)
    right, up, back = camera_axes(camera)
    lights = {
        "Large warm key softbox | upper left": (target - right * 7.0 + up * 10.0 + back * 15.0, 10500, 15.0),
        "Bright frontal cool fill | readable text": (target + right * 8.0 + up * 6.0 + back * 13.0, 7000, 13.0),
        "Cool rear rim | visible prism depth": (target - back * 14.0 + up * 8.0, 9000, 12.0),
    }
    for name, (location, energy, size) in lights.items():
        light = bpy.data.objects[name]
        light.location = location
        light.data.energy = energy
        light.data.size = size
        aim(light, target)

    background = background_node(scene.world)
    background.inputs["Color"].default_value = (0.035, 0.075, 0.13, 1)
    background.inputs["Strength"].default_value = 0.55
    scene.render.film_transparent = False
    scene.render.use_border = False
    scene.render.use_crop_to_border = False
    scene.render.resolution_x = 1080
    scene.render.resolution_y = 1620
    scene.cycles.samples = 32
    scene.render.filepath = str(MAP_RENDER_PATH)
    bpy.context.view_layer.update()
    bpy.ops.render.render(write_still=True)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(MAP_SCENE_PATH))
    print(f"Saved approval map scene: {MAP_SCENE_PATH}")
    print(f"Rendered approval map: {MAP_RENDER_PATH}")


def main():
    scene, camera, sine, cosine, font = create_scene()
    render_ornament(scene, camera, sine, cosine, RESOURCE_DIR / "map_sin30_formula.png")
    render_ornament(scene, camera, cosine, sine, RESOURCE_DIR / "map_cos30_formula.png")

    for ornament in (sine, cosine):
        ornament.hide_render = False
        for child in ornament.children_recursive:
            child.hide_render = False
    camera.data.ortho_scale = 15.0
    pair_target = Vector((0, 0, 1.0))
    camera.location = pair_target + camera_axes(camera)[2] * 19.0
    aim(camera, pair_target)
    right, up, back = camera_axes(camera)
    set_studio_lights(pair_target, right, up, back)
    render_pair_preview(scene, camera, right, up, back)
    scene.render.filepath = str(RESOURCE_DIR / "map_sin30_formula.png")
    bpy.context.view_layer.update()
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "course_map_formulas.blend"))
    print(f"Saved Blender scene: {ROOT / 'course_map_formulas.blend'}")
    print(f"Rendered static PNGs: {RESOURCE_DIR / 'map_sin30_formula.png'} and {RESOURCE_DIR / 'map_cos30_formula.png'}")
    create_approval_map(scene, camera, sine, cosine, font, back)


if __name__ == "__main__":
    main()
