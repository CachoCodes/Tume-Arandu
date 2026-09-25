# Tume-Arandu

Tume-Arandu is an offline-first Android app for learning introductory trigonometry in Guaraní, with an optional Spanish translation. The demo includes a seven-lesson course, interactive exercises, prepared solutions, completion and XP, and on-device progress. It does not require an account, backend, API key, network access, or AI service.

The Guaraní course copy is an initial draft. A Guaraní-speaking math teacher should review it before classroom or public educational use.

For the current implementation and scope, the active contracts in `specs/` are authoritative.

## Build the Android app

Requirements: JDK 17, Android SDK Platform 35, and Android Build Tools 36.0.0. The Gradle wrapper downloads the pinned Gradle version and project dependencies.

Android Studio can configure the SDK automatically. For command-line builds, set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or add an untracked `local.properties` with the SDK path.

```sh
git clone https://github.com/CachoCodes/Tume-Arandu.git
cd Tume-Arandu
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Android Studio can also open the project root and launch the `app` configuration on an emulator or device.

## Project layout

- `app/src/main/assets/courses/` — Guaraní and Spanish course content.
- `app/src/main/kotlin/com/guarani/mathdemo/` — course loading, exercise validation, local progress, and Compose UI.
- `tools/math-prop/` — optional Three.js artwork renderer. It requires Node.js, npm, and Google Chrome at the macOS application path configured in `render.mjs`; run `npm ci && npm run render` from this directory.
- `tools/map-decorations/` — Blender source scenes and visual reference assets; these are not bundled into the app.
- `specs/` — product and Android architecture contracts, work history, and spec checks (`python3 tools/spec-lint.py`).

## Contributing

Fork the repository or create a branch, keep changes focused, and open a pull request against `main`. Before submitting Android changes, run `./gradlew :app:assembleDebug`.

## License

Project code and original assets are released under the MIT License. See [LICENSE](LICENSE) and [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) for the bundled Three.js font asset's attribution and license.
