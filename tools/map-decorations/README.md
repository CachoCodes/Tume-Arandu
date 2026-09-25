# Blender course-map assets

Android's course map is drawn at runtime by Kotlin/Compose Canvas in `app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt`. Its grid, route, raised lesson platforms and SIN/COS triangular prisms are programmatic; Android does not read Blender renders or exported map PNGs. Lesson targets remain Compose controls with accessibility descriptions.

`course_map_approval.blend`, `generate_blender.py`, `generate_platforms.py` and `export_approval_background.py` are kept as local visual references. Any images they produce now go under `reference-renders/`, outside the Android source set.

Render the Blender reference scene on macOS from the project root with Blender 5.2.2:

```sh
"/Applications/Blender.app/Contents/MacOS/Blender" --background tools/map-decorations/course_map_approval.blend --python tools/map-decorations/export_approval_background.py
```

Regenerate numbered platform reference images separately with `generate_platforms.py`. These are not used by the app. The Android map does not bundle Blender, Three.js, WebView or a separate 3D runtime.
