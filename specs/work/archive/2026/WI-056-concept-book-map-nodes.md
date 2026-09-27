# WI-056: Книжечки перед первыми тремя уроками на карте

Canon action: direct-edit

## Outcome

На Android-карте перед каждым из первых трёх номерных уроков есть отдельная книжечка: ученик открывает объяснение, затем номерной урок и его повторения.

## Scope

- In: узлы-книжечки на карте, экран объяснения из имеющейся теории урока, сохранение прохождения книжечки, доступность и обратная совместимость прогресса.
- Out: перенос всех блоков и проверок браузерного прототипа, изменение упражнений и профиля.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#course-map`, `spec://modules/learning/FEAT-010-learning-demo#flow`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`, `spec://modules/android/PROP-010-android-demo-architecture#persistence`.

## Acceptance

- AC-1: карта показывает книжечку перед уроками 1, 2, 3; её платформа чуть меньше номерной и больше повторения, номерные уроки и повторения сохраняют порядок и ID.
- AC-2: книжечка открывает объяснение, завершение открывает упражнения своего урока; следующий узел доступен только после соответствующего шага.
- AC-3: прежние завершённые уроки не блокируются новым шагом; прогресс сохраняется без очистки и перечитывается после перезапуска.
- AC-4: APK собирается, устанавливается поверх приложения и затронутый экран проверяется на эмуляторе вместе с соседним состоянием.

## Dependencies

Другой Codex-чат одновременно оптимизирует `CourseMap3D.kt`; перед его правкой необходимо сверить актуальную версию файла.

## Risks

Общий рабочий каталог без `.git`: не перезаписывать файл карты целиком и повторно проверять его содержимое перед узкой правкой.

## Result

2026-09-26: книжечки добавлены перед первыми тремя основными уроками; объяснение использует существующую локализованную теорию урока. Полный перенос 20 блоков и встроенных проверок браузерного прототипа остаётся вне scope этого WI и описан в `docs/handoff/prompt-concepts-to-android.md`. Git checkout в рабочем каталоге отсутствует. Параллельный чат завершил свои правки `CourseMap3D.kt` до правки книжечек; итоговая сборка включила обе задачи.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `docs/handoff/screenshots/book-map.png`: три платформы-книжечки перед уроками 1–3; радиусы в `CourseMap3D.kt` 27/30/21, координаты проверены в установленном APK. | passed |
| AC-2 | Ручной проход в отдельном preview-прогрессе: `docs/handoff/screenshots/book-lesson-1.png` → `docs/handoff/screenshots/book-to-exercise.png`; после возврата на карту книжечка жёлтая, урок 1 голубой. | passed |
| AC-3 | `adb exec-out run-as com.guarani.mathdemo cat files/datastore/exercise_preview.preferences_pb` содержит `completedBookIds:["right-triangle"]`; после `adb install -r` обычный режим показывает прежние 180 XP и 4/12 пройденных узлов: `docs/handoff/screenshots/book-legacy-progress.png`. | passed |
| AC-4 | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug --max-workers=2 --no-daemon` → BUILD SUCCESSFUL; `adb install -r app/build/outputs/apk/debug/app-debug.apk` → Success; экран карты и переход к уроку проверены на эмуляторе. | passed |
