"""Render a Blender scene reference; the Android map is drawn in Compose Canvas."""
from pathlib import Path
import bpy

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "tools/map-decorations/reference-renders/course_map_android_reference.png"
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
scene = bpy.context.scene
if scene.camera is None:
    raise RuntimeError("The approval scene needs its orthographic map camera.")

render = scene.render
old = (render.filepath, render.resolution_x, render.resolution_y, render.resolution_percentage,
       render.image_settings.file_format, render.image_settings.color_mode,
       render.image_settings.color_depth)
try:
    render.filepath = str(OUTPUT)
    render.resolution_x, render.resolution_y, render.resolution_percentage = 1080, 1620, 100
    render.image_settings.file_format = "PNG"
    render.image_settings.color_mode = "RGBA"
    bpy.ops.render.render(write_still=True)
finally:
    (render.filepath, render.resolution_x, render.resolution_y, render.resolution_percentage,
     render.image_settings.file_format, render.image_settings.color_mode,
     render.image_settings.color_depth) = old
print(f"Wrote {OUTPUT}")
