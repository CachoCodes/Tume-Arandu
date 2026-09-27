# WI-057: Концепты первых трёх уроков в Android

Canon action: direct-edit

## Outcome

Книжечки первых трёх уроков открывают утверждённые последовательности смысловых блоков с иллюстрациями и встроенными проверками, после которых ученик переходит к упражнениям.

## Scope

- In: формат concept в JSON, три концепта, загрузка/валидация, экран Compose, поэтапный прогресс и визуальная проверка.
- Out: задания вкладки «Типы заданий», изменение существующих упражнений и профиля.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#flow`, `spec://modules/learning/FEAT-010-learning-demo#lesson-content`, `spec://modules/learning/PROP-011-course-json-format#concept`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`, `spec://modules/learning/PROP-011-course-json-format#validation`.

## Acceptance

- AC-1: три концепта из прототипа доступны на книжечках, содержат 8/6/6 блоков на Guaraní и Español и ожидаемые рисунки.
- AC-2: блоки раскрываются последовательно, встроенная проверка удерживает переход до правильного ответа, работают варианты и выбор фигуры.
- AC-3: пройденный блок и завершение концепта сохраняются; существующий прогресс ученика не сбрасывается, XP не меняется.
- AC-4: неверный visual и неверный correctOptionId отклоняются загрузчиком и валидатором; курс без concept продолжает работать.
- AC-5: APK собирается, устанавливается и три концепта проверены на эмуляторе; визуальный результат сопоставлен с прототипом.

## Dependencies

WI-056 добавил книжечки и маршрут. Параллельная задача оптимизации карты завершила запись в общие файлы.

## Risks

Черновой Guaraní требует проверки носителем. Общий каталог без Git checkout: менять файлы узкими патчами, не откатывая чужие изменения.

## Result

2026-09-26: в JSON курса добавлены 8/6/6 блоков трёх концептов с `gn-PY` и `es` (Guaraní — черновик для носителя). Android загружает и проверяет формат, Compose показывает вертикальную ленту, иллюстрации и встроенные проверки; книжечка ведёт к упражнениям. Неразработанный робот-тьютор оставлен как место 52 dp согласно handoff-промпту. Для визуального сравнения открыт `tools/concept-lesson-lab.html`: сохранены порядок блоков, геометрия и цветовая обратная связь; Android использует существующие `ScreenHeader` и `PrimaryAction`. Git checkout отсутствует в этом рабочем каталоге, поэтому PR здесь не создан. Параллельная задача оптимизации карты завершила свои записи до финальной сборки.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `python3 tools/validate-course.py app/src/main/assets/courses/trigonometry.json` → 16 lessons, 42 exercises; `tools/test_course_validator.py` подтверждает 8/6/6 блоков. Первые экраны: `docs/handoff/screenshots/concept-1-first.png`, `concept-2-first.png`, `concept-3-first.png`. | passed |
| AC-2 | Ручной проход трёх концептов на эмуляторе 720×1600: фигура B → ошибка → исправление → фигура A → верно; выбор вариантов и переход на последний блок каждого концепта. Снимки `docs/handoff/screenshots/concept-{1,2,3}-{wrong,correct}.png`; первый и второй концепты проверены до упражнения, третий также прошёл действие «К заданиям». | passed |
| AC-3 | В отдельном preview-хранилище после раскрытия второго блока `bookReveal:{"right-triangle":1}`; поворот экрана сохранил блок (`docs/handoff/screenshots/concept-rotation.png`). После установки APK обычная карта показывает прежние 180 XP и 4/12 узлов (`docs/handoff/screenshots/concept-map-real-final.png`). Концепт XP не начисляет. | passed |
| AC-4 | `python3 tools/test_course_validator.py` → 12 tests OK, включая неверный visual, пустой текст и отсутствующий correctOptionId; шаблонный курс без concept открыл карту 0/9 без книжечек (`docs/handoff/screenshots/concept-no-concept-map.png`) и старое упражнение. | passed |
| AC-5 | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug --max-workers=2 --no-daemon` → BUILD SUCCESSFUL; `adb install -r app/build/outputs/apk/debug/app-debug.apk` → Success; три концепта пройдены на эмуляторе, прототип открыт для визуального сравнения; `python3 tools/spec-lint.py` → 0 errors. | passed |
