# WI-019: Карта Android по Blender-сцене

Canon action: direct-edit

## Outcome

Android-карта визуально повторяет сохранённую Blender-сцену и сохраняет работу уроков: доступность и нажатия на восемь уровней.

## Scope

- In: экспорт карты из сохранённой Blender-сцены; отображение её композиции на Android; совмещение интерактивных уровней с изображением; доступные названия и статусы уроков без экранных карточек над сценой; сборка и запуск на эмуляторе.
- Out: изменение содержимого уроков, Tutor и профиля.

## Specs

- Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#implementation-map`.

## Acceptance

- AC-1: Основная карта в Android использует композицию и ракурс сохранённой Blender-сцены, включая положение и масштаб син/кос пропов.
- AC-2: Все восемь прозрачных областей касания совмещены со своими платформами; доступный уровень открывает урок, закрытый остаётся недоступен; название и статус урока доступны через accessibility без видимых карточек поверх Blender-сцены.
- AC-3: Debug APK собирается, устанавливается и запускается на Android-эмуляторе; карта визуально сверена с Blender-рендером.

## Dependencies

Локальная Blender-сцена и Android SDK проекта.

## Risks

Blender-фон содержит платформы в исходном состоянии; если прогресс изменяет статус уровня, Compose заменяет только эту платформу, сохраняя интерактивность и accessibility-описание.

## Result

Android-карта использует полный рендер из сохранённой `course_map_approval.blend`. Удалены экранные карточки с названиями/статусами, которые закрывали сцену; Compose оставляет прозрачные области нажатия и accessibility-описания. Файл рендера совпадает с Blender review render; син-проп сохранён в сцене с yaw −53°, COS yaw −51.95°, камера 1080×1620. Debug APK собран, установлен и запущен на эмуляторе 1080×2400, 420 dpi; первый урок открывается, закрытый второй остаётся на карте. Приложение оставлено на карте. Дата: 2026-09-24. Git metadata в каталоге проекта отсутствует. REVIEW / техдолг: нет.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Blender background export created `map_course_blender_background.png` at 1080×1620; decoded image comparison with the Blender render returned no differing pixels. Blender MCP confirmed the scene camera and SIN yaw −53° / COS yaw −51.95°. Final Android screenshot: `/tmp/guarani-blender-map-android-final.png`. | passed |
| AC-2 | Emulator interaction: tapping level 1 opened its lesson (`/tmp/guarani-blender-map-final-lesson.png`); Back returned to map; tapping locked level 2 kept map open (`/tmp/guarani-blender-map-final-locked.png`). The Blender scene has no visible lesson cards; Compose targets preserve labels/status in accessibility semantics. | passed |
| AC-3 | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r app/build/outputs/apk/debug/app-debug.apk` — `Success`; `adb shell am start -n com.guarani.mathdemo/.MainActivity` — activity started. Emulator: 1080×2400, 420 dpi. | passed |
