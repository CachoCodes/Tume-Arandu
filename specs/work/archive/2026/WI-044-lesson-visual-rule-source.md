# WI-044: Визуальная часть в каждом задании, источник урока и правила для ИИ

Canon action: direct-edit

## Outcome

Каждое задание урока имеет рисунок; каждый урок помечен `source` (Dulce Duro, MEC Paraguay); у ИИ-агентов есть однозначные правила создания уроков; текстовые сопоставления получают согласованный визуальный вариант.

## Scope

- In: PROP-011/FEAT-010, `tools/validate-course.py` и тесты, `CourseLoader`, `trigonometry.json` (`source`), `docs/lesson-authoring-ai.md`, `docs/course-json-v2.md`, прототип `tools/triangle-pairs-prototype.html`.
- Out: перенос `triangle_pairs` в Compose до согласования прототипа; смена ID и ответов.

## Specs

- Governing: `spec://modules/learning/PROP-011-course-json-format#visual-rule`, `#lesson-source`, `#new-visuals`.

## Acceptance

- AC-1: валидатор и загрузчик отклоняют урок без `source`; `standard_*` без `triangle` отклоняется; у сопоставления `triangle` запрещён.
- AC-2: `docs/lesson-authoring-ai.md` описывает шаблонный процесс и правила для ИИ; AGENTS.md на него ссылается.
- AC-3: каталог `templates/exercise-templates.json` покрывает все пары `type/visual` и открывается в режиме просмотра без изменения прогресса.
- AC-4: все 21 задание курса отображаются согласованными экранами; доска сопоставления из двух пар заполняет высоту как доска из трёх.

## Dependencies

Согласование прототипа владельцем.

## Risks

Режим просмотра доступен через adb-интент в любой сборке; пишет только в отдельное хранилище `exercise_preview`.

## Result

Готово 2026-09-26. По решению владельца сопоставление остаётся карточками без треугольника (прототип `triangle_pairs` удалён). Проверки: validate-course курса (7/21) и каталога (4/12), test_course_validator 8 OK, testDebugUnitTest и assembleDebug OK, spec-lint 0. Эмулятор `guarani-ui-review`: снимки всех 21 заданий курса и 12 шаблонов в режиме просмотра; после обычного запуска прогресс ученика прежний.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | test_course_validator: source, triangle у standard_*, запрет triangle у matching | passed |
| AC-2 | docs/lesson-authoring-ai.md, AGENTS.md | passed |
| AC-3 | test_templates_cover_every_visual; 12 снимков preview | passed |
| AC-4 | снимки 21 задания; identity-match/app-match заполняют высоту | passed |
