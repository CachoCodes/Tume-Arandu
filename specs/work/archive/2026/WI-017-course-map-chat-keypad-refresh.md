# WI-017: Карта курса, Tutor и ввод ответа

Canon action: direct-edit

## Outcome

Сделать карту курса читаемой из одного ортографического ракурса, оформить её как цельную изометрическую игровую сцену и исправить геометрию формульных треугольников. Перевести Tutor и профиль в отдельные вкладки; заменить системную клавиатуру в упражнениях на встроенную.

## Scope

- In: обновить активные UX-контракты карты, навигации и ответа; исправить Blender-треугольники/формулы и ассеты; согласовать и облегчить визуальный стиль Android-карты; сделать Tutor и профиль самостоятельными экранами; добавить Compose-клавиши ввода для input и step-by-step.
- Out: реальный сетевой/модельный AI Tutor, новая 3D-зависимость/runtime и изменение правил проверки ответа или прогресса.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#course-map`, `#exercises`.
- Governing: `spec://modules/android/PROP-010-android-demo-architecture#navigation`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#ai-boundary`, `spec://modules/tutor/FEAT-011-ai-tutor#root`.

## Acceptance

- AC-1: `.blend` и воспроизводимый Blender-скрипт задают одну ортографическую изометрическую камеру; прямой угол, дуга 30°, стороны `1`/`√3` и гипотенуза `2` расположены рядом со своими рёбрами без лишних трубок/линий.
- AC-2: карта на устройстве читается с одного общего ракурса: плитки и узлы выглядят объёмными, боковые треугольные ориентиры повёрнуты от фронтального положения, маршрут и подписи не перекрывают друг друга.
- AC-3: Map, Tutor и Profile открываются как отдельные вкладки с общей нижней навигацией; Tutor показывает самостоятельный экран чата и не выдаёт локальные демо-ответы за подключённый AI.
- AC-4: input и step-by-step принимают ответ через встроенные клавиши приложения; системная Android-клавиатура для этих ответов не появляется; детерминированная проверка и подсказки продолжают работать.
- AC-5: debug APK собирается, обновлённое приложение запускается в Android-эмуляторе, а работа карты, трёх вкладок и клавиатуры проверена на самом экране.

## Dependencies

Нет.

## Risks

- Рендеры статичны; камера, размер Blender-пропов и координаты Compose должны сохранять общий изометрический ракурс.
- API key и локальная AI-модель не настроены; чат остаётся демонстрационным UI без сетевого ответа.

## Result

Выполнено 2026-09-24.

REVIEW / техдолг: чат Tutor остаётся детерминированной локальной демонстрацией и явно помечен `DEMO · LOCAL`; подключение внешней AI-модели не входит в эту работу. Карта использует статичные Blender PNG в Compose, без 3D-runtime.

Git: `.git` отсутствует в корне проекта, поэтому commit и git working-tree status недоступны.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Blender 5.2.2 CLI: `/Applications/Blender.app/Contents/MacOS/Blender --background --python tools/map-decorations/generate_blender.py`; визуально проверены `tools/map-decorations/course_map_approval.png` и оба 510×499 PNG. `course_map_approval.blend` сохраняет одну orthographic-камеру (`ortho_scale=26.5`), а формульные треугольники показывают `30°`, `90°`, `1`, `√3` и `2` у своих сторон. | passed |
| AC-2 | На `emulator-5554` установлен текущий APK; `/tmp/guarani-map-final-android.png` проверен после последнего рендера. Пропы стоят по бокам от пути и не перекрывают подписи видимых уроков; полная восьмиузловая композиция проверена на Blender preview. | passed |
| AC-3 | На эмуляторе вручную выполнен переход Map → Tutor → Profile → Map; экраны доступны в `/tmp/guarani-tutor-final.png` и `/tmp/guarani-profile-final.png`. | passed |
| AC-4 | Открыт `InputExercise`, клавишей приложения введено `2`; число появилось в поле без открытия Android IME. Скриншот: `/tmp/guarani-keypad-final-android.png`. Теория свёрнута по умолчанию для input-задач, чтобы вся встроенная панель и проверка были видны сразу. | passed |
| AC-5 | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug` завершилась `BUILD SUCCESSFUL`; APK установлен на Android 35 AVD `onelu` (`emulator-5554`) и `com.guarani.mathdemo/.MainActivity` запущена. | passed |
