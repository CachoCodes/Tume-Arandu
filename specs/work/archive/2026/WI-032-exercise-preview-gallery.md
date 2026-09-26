# WI-032: Локальная галерея скриншотов заданий

Canon action: direct-edit

## Outcome

На текущем localhost есть минимальный индекс семи типов заданий, который открывает реальные снимки экранов Android APK и возвращает к живому экрану эмулятора.

## Scope

- In: кнопка с живого экрана, минимальный индекс семи типов на `127.0.0.1:8765/gallery`, снимки актуальных испанских Android-экранов и возврат к live view.
- Out: HTML-перерисовка упражнений, debug-навигация приложения, команды эмулятору из галереи, изменение прогресса, API/AI-генерация и публикация сайта.

## Specs

- Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#exercises`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#navigation`.

## Acceptance

- AC-1: С текущей страницы localhost кнопка открывает индекс `/gallery`, а кнопка возврата снова показывает живой экран.
- AC-2: Индекс предлагает `multiple_choice`, построение угла, числовой и дробный `input`, обычный и угловой `matching`, `step_by_step`; выбор каждого пункта показывает соответствующий скриншот реального Android APK.
- AC-3: Все семь скриншотов сняты с испанских экранов APK; галерея не рисует упражнение в HTML, не отправляет команды эмулятору и не меняет DataStore.
- AC-4: Индекс и выбранный снимок читаются на настольной и узкой ширине; существующие нажатия и прокрутка живого экрана продолжают работать.

## Dependencies

Локальный Python preview server и семь снимков экранов актуального APK в `tools/exercise-gallery-images/`.

## Risks

Снимки могут устареть после изменения Android-экранов; галерея должна использовать только снимки реального APK и не выдавать веб-перерисовку за нативный интерфейс.

## Result

Done — 2026-09-25. `/gallery` стал минимальным индексом семи реальных экранов установленного APK. `python3 tools/local-preview.py --check` прошёл: проверяет ссылку с live view, возврат, семь HTML mappings, семь PNG HTTP routes, размер/сигнатуру/уникальность снимков и 404 неизвестного image ID. `python3 tools/spec-lint.py` прошёл (0 ошибок, 0 предупреждений). Снимки проверены визуально capture-агентом: испанские экраны, 1080×2400, 420 dpi; точный mapping указан ниже. Capture-агент восстановил исходный DataStore, SHA-256 до/после совпал (`f2b6cb5161920ed6fc5eb7638fdd0274f86117fd9d57203e7a5e79c978f59730`), эмулятор оставлен на `ratios-match`. В IAB подтверждены переход `/` → `/gallery` → `/`, смена matching-снимка и его загрузка; при viewport 390×844 индекс прокручивается по горизонтали, выбранный matching-снимок помещается без горизонтального переполнения. REVIEW/техдолг: после будущих правок экранов Android снимки нужно переснять. Git metadata отсутствуют, поэтому состояние working tree не определить.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `python3 tools/local-preview.py --check`; IAB: с `/` ссылка открыла `/gallery`, возврат открыл живой экран `/` | passed |
| AC-2 | HTTP-check подтвердил семь PNG routes (>50 KB, PNG signature, разные SHA-256); capture-agent визуально сопоставил: `multiple_choice`→`ratios-cosine`, `angle_builder`→`angles-choice`, `numeric_input`→`angles-input`, `fraction_input`→`ratios-sine`, `matching`→`ratios-match`, `angle_matching`→`angles-match`, `step_by_step`→`notable-rationalization`; IAB подтвердил смену matching-снимка | passed |
| AC-3 | Все семь файлов сняты с установленного APK в ES (1080×2400, 420 dpi); IAB подтвердил экран matching без HTML-перерисовки. SHA-256 DataStore до/после захвата совпал; галерея не вызывает `/input` и не пишет в DataStore | passed |
| AC-4 | IAB: viewport 390×844 показывает горизонтальную прокрутку индекса и полный matching-снимок без горизонтального переполнения; обработчики live view сохранены и возврат проверен | passed |
