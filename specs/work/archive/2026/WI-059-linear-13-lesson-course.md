# WI-059: Линейный курс из 13 уроков

Canon action: direct-edit

## Outcome

Карта Android ведёт по 13 последовательным циклам «книжка → урок → повторение»; каждый основной урок имеет содержательный концепт и одно повторение по плану пользователя.

## Scope

- In: порядок курса, карта Compose, 13 концептов и 13 повторений, загрузка/валидация, сохранение прежнего прогресса и экранная проверка.
- Out: новые 3D-модели, изменения Tutor/профиля, редактирование исходного DOCX.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#course-map`, `spec://modules/learning/FEAT-010-learning-demo#lesson-content`, `spec://modules/learning/PROP-011-course-json-format#package`, `spec://modules/learning/PROP-011-course-json-format#concept`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`, `spec://modules/learning/PROP-011-course-json-format#validation`.

## Acceptance

- AC-1: карта показывает без ветвлений 13 циклов B→L→R, все узлы открывают соответствующие элементы, путь не лежит на объектах сцены.
- AC-2: 13 основных уроков следуют темам DOCX, каждый имеет последовательный локализованный концепт с рисунками и проверкой.
- AC-3: после каждого урока ровно одно отдельное повторение с новыми заданиями и корректными ответами.
- AC-4: курс и шаблоны проходят валидатор; существующие ID/завершения сохраняют смысл, APK собирается и обновляется поверх установленного приложения без сброса прогресса.
- AC-5: на эмуляторе проверены карта, концепт, урок и повторение на раннем и позднем этапе; ближайшие экраны Tutor/профиля остаются доступны.

## Dependencies

WI-056/057 создали книжечки и экран концепта. DOCX содержит 11 нумерованных тем при требовании 13 уроков; ожидается предпочтение пользователя о разбиении широких тем.

## Risks

Текст Guaraní Jopara требует проверки носителем/преподавателем. Общий каталог без Git checkout: узкие правки и повторная сверка при параллельной записи. Изменение порядка не должно обнулять DataStore.

## Result

Готово 2026-09-27. Курс содержит 13 основных уроков и 13 отдельных повторений (89 заданий), перед каждым основным уроком — книжечка с последовательным концептом. План DOCX содержит 11 нумерованных тем; для 13 циклов отдельно выделены пропорции, а практические сюжеты разделены на дом и природу. Существующий прогресс не сброшен. Merge-коммит курса: `60762df`.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | UI sweep на эмуляторе открыл все 39 узлов B/L/R на 7 этапах, `FAILURES: []`; [этап 1](../../../../docs/handoff/screenshots/linear-map-stage1.png), [этап 7](../../../../docs/handoff/screenshots/linear-map-stage7.png). | passed |
| AC-2 | `python3 tools/validate-course.py app/src/main/assets/courses/trigonometry.json`: 26 entries / 89 exercises; у 13 основных уроков 60 визуальных блоков концепта и 16 встроенных проверок. [Концепт 13](../../../../docs/handoff/screenshots/linear-concept13.png). | passed |
| AC-3 | У каждого из 13 циклов одно повторение с двумя отдельными заданиями; `python3 tools/test_course_validator.py`: 12 tests OK. Проверены ответы и числовые углы схем; [повторение 8](../../../../docs/handoff/screenshots/linear-review8-cosine.png), [повторение 13](../../../../docs/handoff/screenshots/linear-review13.png). | passed |
| AC-4 | Валидатор, тесты и `:app:assembleDebug :app:assembleShare` прошли. `adb install -r` сохранил 180 XP, завершения 3/6 и текущий урок; [профиль](../../../../docs/handoff/screenshots/linear-profile-progress.png). | passed |
| AC-5 | На установленном APK открыты книжечка, урок 13 и повторение 13; Tutor и профиль доступны. [Урок 13](../../../../docs/handoff/screenshots/linear-lesson13.png). | passed |

REVIEW: формулировки Guaraní Jopara требуют редакторской проверки носителем/преподавателем; это не блокирует техническую приёмку.

Состояние перед итоговой записью: `git status --short` пуст; проверенный код и курс — коммит `8fc2fa0`.
