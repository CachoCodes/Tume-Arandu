# WI-001: Первая спецификация Android demo

Canon action: new-spec

## Outcome

Зафиксировать первую связную продуктовую и техническую основу Android demo по `context.md`, чтобы команда могла перейти к реализации без повторного сбора требований.

## Scope

- In: продуктовый сценарий и функции, AI Tutor, Android architecture/data contracts, план вертикальной реализации, связи и карта канона.
- Out: исходный код приложения, готовый учебный курс с переведёнными материалами, создание API key, настройка аккаунта провайдера, backend, покупка/публикация APK.

## Specs

Governing / constraint / affected: `spec://modules/learning/FEAT-010-learning-demo#root`, `spec://modules/tutor/FEAT-011-ai-tutor#root`, `spec://modules/android/PROP-010-android-demo-architecture#root`; constraint — `context.md`.

## Acceptance

- AC-1: FEAT-010 описывает аудиторию, первый flow, карту, контент уроков, все четыре типа упражнений, deterministic checking, решения, прогресс, локализацию, ошибки и наблюдаемые acceptance.
- AC-2: FEAT-011 описывает режимы Auto/Online/Offline, поведение Tutor и ошибки; отделяет подтверждённые провайдером факты от ограничений demo и явно оставляет отсутствующие credentials отсутствующими.
- AC-3: PROP-010 описывает Android стек, навигацию, границы кода, модели/формат курса, локальное хранение, AI boundary, скачивание/загрузку модели, безопасность и ошибки с явными неизвестными.
- AC-4: `specs/IMPLEMENTATION_PLAN.md` превращает канон в короткую последовательность проверяемых вертикальных этапов без расширения scope до production.
- AC-5: `specs/common/main.md`, `specs/common/structure.md`, `specs/SPEC-MAP.md` и BOARD согласованы с созданным каноном; `python3 tools/spec-lint.py` проходит.

## Dependencies

- `context.md` — исходные продуктовые требования.
- Актуальная документация OpenAI API и Google AI Edge / Android Developers — внешние технические факты, зафиксированные ссылками и датой проверки.

## Risks

- Checkout не содержит Android source project и не содержит Git metadata; технические границы реализации являются целевым проектом, а не наблюдаемым кодом.
- Для live Online AI нет API key. Публичный APK с встроенным provider secret недопустим; нужен локальный прототипный секрет или backend relay до демонстрации внешним пользователям.
- Локальная модель, производительность и качество на Guaraní зависят от конкретной версии/варианта Samsung Galaxy S24 и должны быть подтверждены на устройстве.

## Result

Готова первая спецификационная версия: создано три draft-спеки, план реализации и карта продукта; ключи не создавались, исходники приложения не менялись. Проведён ручной проход покрытия и проверены внешние первичные источники. Checkout не является Git worktree, поэтому commit и git working-tree status недоступны. Дата: 2026-09-23.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Ручная проверка `specs/modules/learning/FEAT-010-learning-demo.md`: flow, карта, теория, 4 вида заданий, deterministic checking, решения, прогресс, локализация, ошибки и 7 acceptance criteria | passed |
| AC-2 | Ручная проверка `specs/modules/tutor/FEAT-011-ai-tutor.md`; сверка GPT-6 Luna model ID и API через официальные OpenAI docs; ключ не создавался | passed |
| AC-3 | Ручная проверка `specs/modules/android/PROP-010-android-demo-architecture.md`; сверка LiteRT-LM, Gemma model artifact и Android storage по официальным источникам; отсутствие кода обозначено явно | passed |
| AC-4 | `specs/IMPLEMENTATION_PLAN.md`: этапы P0/P1/P2, gates и условия отсутствующих credentials/model | passed |
| AC-5 | `python3 tools/spec-lint.py` — `0 errors, 0 warnings`; ссылки в карте и структуре указывают на draft-канон | passed |
