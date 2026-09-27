# WI-046: Первые три урока и настоящие повторения

Canon action: direct-edit

## Outcome

Карта ведёт по первым трём темам плана, после каждой открывает два разных урока повторения.

## Scope

- In: порядок и содержание первых девяти узлов, доступность повторений, сохранение старых уроков и прогресса, правило для продолжения курса.
- Out: новые типы упражнений, перетаскивание вершин и отдельные иллюстрации сцен.

## Specs

Governing: `spec://modules/learning/FEAT-010-learning-demo#course-map`, `spec://modules/learning/FEAT-010-learning-demo#lesson-content`; constraint: `spec://modules/learning/PROP-011-course-json-format#package`, `spec://modules/learning/PROP-011-course-json-format#templates`.

## Acceptance

- AC-1: на карте три первые темы плана идут по порядку, между ними и после третьей есть по два открываемых урока повторения.
- AC-2: повторения проверяют материал своей темы новыми вопросами; все новые задания используют шаблоны и проходят валидатор.
- AC-3: старые ID и сохранённый прогресс не удаляются, курс собирается и новые экраны открываются в режиме просмотра.

## Dependencies

Нет.

## Risks

Переход уже начатого курса к новому порядку может показать ранее завершённый старый урок позже на карте; проверяем открытие без очистки прогресса.

## Result

Готово 2026-09-26. Вставлены первые три темы плана и шесть отдельных повторений; прежние семь уроков и все их ID сохранены после них. Карта открывает малые узлы первого этапа как уроки. REVIEW: перевод Guaraní и формулировки задач требуют проверки преподавателя до внешней демонстрации. Git commit отсутствует: каталог проекта не содержит `.git`; рабочие файлы изменены локально.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `python3 tools/test_course_validator.py`; карта первого этапа на эмуляторе, нажатие на первый малый узел открыло «Jehechajey: lado-kuéra» | passed |
| AC-2 | `python3 tools/validate-course.py app/src/main/assets/courses/trigonometry.json` → 16 уроков, 42 задания; `python3 tools/test_course_validator.py` → 10 tests OK | passed |
| AC-3 | `./gradlew :app:assembleDebug` с JDK 17 и Android SDK → BUILD SUCCESSFUL; `adb install -r` → Success; preview `rt-hypotenuse`, `life-ladder`, `measure-ladder-unknown`, старого `angles-choice` открыл экраны без очистки прогресса | passed |
