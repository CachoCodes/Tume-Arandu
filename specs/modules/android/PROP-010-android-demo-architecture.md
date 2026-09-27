---
status: active
---
# PROP-010: Архитектура Android demo {#root}

## Простыми словами {#plain-language}

Первая реализация — одно Android-приложение с локально встроенным курсом и прогрессом. Compose рисует flow, простая domain-логика проверяет задания, DataStore сохраняет состояние, а TutorService отделяет онлайн-вызов от on-device runtime. Backend и кроссплатформенная архитектура ради будущего не строятся.

## Scope {#scope}

- In: целевой стек и границы компонентов, навигация, models, формат курса, deterministic validation, local persistence, online/offline AI seam, модель на диске, обработка ошибок и план тестирования на Galaxy S24.
- Out: реализованный Android project, сервер, sync, DI framework, Room/CMS, math engine, iOS-клиент и production observability.

## Наблюдаемое состояние репозитория {#repository-state}

При создании спеки checkout содержал `context.md` и файлы Spec Drive, без Android source project и Gradle wrapper. P0 реализация теперь лежит в `app/`; Tutor, его runtime и backend остаются будущей работой.

## Стек и границы {#stack}

- Kotlin, Android single-activity application, Jetpack Compose и Material 3.
- Navigation Compose: `CourseMap`, `Lesson`, `LessonComplete` в P0; `Tutor` относится к P1. Теория и упражнения — состояния одного `Lesson` destination.
- Kotlin Coroutines/Flow для локальных операций и подписки UI на DataStore; P0 не вводит ViewModel без необходимости.
- Простая структура одного app module с пакетами `course`, `progress`, `exercise`, `tutor`, `ui`. Новые модули, DI и слои интерфейсов добавлять только при реальной необходимости.
- Чистые функции Kotlin для проверки ответа и вычисления mastery. UI их не переопределяет; AI в оценивании не участвует.
- Сохранять границу domain/data от Compose, чтобы будущая iOS-реализация была возможна, но не вводить Kotlin Multiplatform сейчас.
- Зафиксированные P0 toolchain: Gradle 9.3.1, Android Gradle Plugin 9.0.1, встроенная поддержка Kotlin AGP 9 с Compose plugin 2.2.10, JDK 17, compile/target SDK 35, minSdk 26, Build Tools 36.0.0. Compose BOM 2025.12.00, Activity Compose 1.10.1, Navigation Compose 2.9.6, DataStore Preferences 1.2.1; версии зафиксированы в Gradle файлах.

## Навигация и состояние {#navigation}

`NavHost` связывает верхнеуровневые вкладки `CourseMap`, `Tutor` и `Profile`, а также конкретный lesson ID и completion. Нижняя навигация остаётся доступна на трёх вкладках; переходы между ними не создают дубликаты back stack. Урок и completion — отдельные destinations, системный Back возвращает по ожидаемой истории, завершение возвращает на карту, повторный вход в `IN_PROGRESS` продолжает сохранённое упражнение. Ошибка Tutor не выводит приложение из карты. Ответ в упражнениях вводится клавишами внутри приложения; системная IME-клавиатура для `input` и `step_by_step` не используется.

Экран читает данные из небольшого repository/ViewModel; JSON курса загружается один раз. Прогресс меняется через repository, а UI подписан на его состояние. Для flow достаточно локальных sealed состояния загрузки/готовности/ошибки, отдельный event bus не нужен.

## Модели и статический курс {#course-schema}

Минимальные domain models:

- `Course(id, locale, title, lessons)`;
- `Lesson(id, title, objective, theoryBlocks, exercises, xpReward)`;
- `TheoryBlock`: `text`, `formula`, `example`;
- `Exercise`: `multiple_choice`, `input`, `matching`, `step_by_step`;
- общий `ExerciseResult`: число попыток при необходимости, final state (`correct` / `solution_viewed`) и последний ответ;
- `ProgressSnapshot`: версия схемы, результаты по ID упражнений, завершённые lesson IDs, текущий lesson/exercise, XP, даты активности и streak.

Курс — единый versioned JSON v2 под `app/src/main/assets/courses/trigonometry.json`. Он содержит обе локализации в объектах `{"gn-PY": "...", "es": "..."}` и явное поле `visual` для каждого упражнения. Отдельного испанского файла нет. Корневые поля: `schemaVersion`, `id`, `revision`, `defaultLocale`, `title`, `lessons`; полная структура и валидация заданы в `spec://modules/learning/PROP-011-course-json-format#root`. Android loader проверяет пакет до показа и выбирает локализованный текст, сохраняя общие ID и ответы. Некорректный встроенный файл показывает безопасное состояние ошибки без сброса прогресса. Прогресс DataStore остаётся в текущей схеме v1, поскольку ID существующих уроков и упражнений не меняются.

Проверка input — нормализация с Unicode-aware trim, удаление внешних и повторных пробелов, сравнение с объявленными aliases. Не использовать `eval`, исполнение выражений или парсер алгебры. Дроби и запятые задаются явными aliases в контенте, а не эвристической математической эквивалентностью.

## Локальное хранение {#persistence}

Для небольшого demo snapshot достаточно Android DataStore Preferences; отдельная база данных не нужна. Хранить versioned значения прогресса и сериализованный компактный набор результатов. Смена структуры должна проверять `schemaVersion`; неизвестная версия не должна молча перезаписывать существующий файл чистым прогрессом.

Запись упражнения выполняется сразу после проверки. Завершение урока атомарно помечает урок, фиксирует XP и переводит доступность следующего урока в вычислимое состояние; повторное сохранение не удваивает награду. Все данные остаются локальными, без аккаунта, analytics SDK, Firebase или cloud sync.

## Граница AI {#ai-boundary}

Общий интерфейс `TutorService.respond(message, context): TutorResult` возвращает текст и источник ответа (`online`/`offline`). `TutorRouter` выбирает сервис по `Auto`/`Online`/`Offline`; UI не импортирует API/LLM SDK напрямую. Для demo достаточно двух реализаций и одного in-memory chat state.

### Online {#online}

Online Tutor использует Gemini Developer API через Cloudflare Worker. Для личного развёртывания выбрана текстовая модель `gemini-3.8-flash`; модель задаётся в конфигурации Worker. Схема — HTTPS `generateContent` API с отдельным вопросом в каждом вызове, ограничением числа выходных токенов, низким уровнем thinking и разбором `candidates[].content.parts[].text` из ответа. Ответ для Android формируется спокойным простым текстом: обычно результат и одно короткое пояснение, не более двух предложений; Worker удаляет сырую Markdown/LaTeX-разметку и ограничивает длину даже при нарушении инструкции моделью. Язык ответа выбирается по явной просьбе ученика, иначе по основному языку и грамматике вопроса: испанская фраза с одним-двумя словами гуарани получает испанский ответ, преимущественно гуаранийская — ответ на простом гуарани; отдельные математические термины не переключают язык. Streaming не обязателен для первой версии; loading state покрывает время запроса. Timeout и HTTP error отображаются отдельными состояниями. Приложение сохраняет ID и вопрос до ответа, повторяет доставку с тем же ID и отдельно запрашивает статус. Worker хранит ключ провайдера как secret, не выводит его и подробности ответов провайдера в логи, ограничивает фактический размер тела запроса даже без `Content-Length`, выдачу и число новых вопросов за календарный месяц, не создаёт второй запрос при повторном ID. Вопрос и ответ удаляются через 24 часа, но бессодержательный ID остаётся для защиты от позднего повтора. До создания job и расходования месячного лимита Worker проверяет явные математические признаки вопроса; при их отсутствии возвращает `422 not_math` без вызова AI. Android показывает, что вопрос не относится к математике, освобождает ввод для следующего вопроса и не повторяет отклонённый запрос. Неоднозначную математическую формулировку пользователь может уточнить. Незавершённое состояние восстанавливается при следующем открытии Tutor; фоновая доставка при закрытом приложении не требуется.

Прямой Gemini API требует Google API key; рабочий ключ проверен прямым запросом к Google и сохранён только как Cloudflare secret. Не добавлять его в исходники, Git, логи или распространяемый APK. Личный Worker требует отдельный токен доступа, который пользователь вводит на устройстве; этот простой механизм не предназначен для публичного многопользовательского APK. До настройки Worker и credentials Online остаётся `unconfigured`; статус сети сам по себе не означает, что API доступен. Ограничение количества запросов Worker не контролирует другие способы использования общего ключа и не заменяет лимит расходов у владельца аккаунта. Одновременные повторы с одним ID не должны повторно расходовать месячный лимит или менять вопрос после первого принятия.

### Offline model {#offline-model}

- Первый кандидат runtime — официальный LiteRT-LM Kotlin API для Android; Google указывает Kotlin API и пример Android. Версию dependency выбрать и зафиксировать при реализации.
- Первый кандидат модели — `Gemma3-1B-IT` int4 `.litertlm`. Запись в проверенном Google AI Edge Gallery allowlist указывает размер 584,417,280 bytes и minimum device memory 6 GB. Google также публикует Gemma 4 E2B как mobile variant; при первом spike сравнить её только если доступен готовый LiteRT-LM artifact с документированными требованиями. Эти записи не гарантируют скорость, качество или свободную RAM в приложении.
- Пользовательский контекст называет Samsung Galaxy S24, но не точную региональную версию, SoC, память и Android build. На самом устройстве сначала проверяется CPU backend; GPU/другой backend включается только после отдельной проверки совместимости и устойчивости. NPU не считать обязательным.
- До фиксации кандидата провести на физическом S24 smoke run, измерить запуск, ответ на короткий math prompt, memory pressure и нагрев, проверить корректный выход при нехватке ресурсов. Отдельно вручную оценить набор Guaraní prompts. Если 1B не помещается или нестабилен, проверить меньшую модель/альтернативный runtime; offline feature может быть снята с demo, не блокируя P0.
- Большая модель не включается в APK. После явного нажатия `Download offline AI` файл сохраняется в app-specific persistent storage, а не в cache. До загрузки показываются размер/свободное место; видны прогресс, ошибка и retry. После загрузки файл проверяется по зафиксированному SHA-256/manifest и только потом помечается готовым. Ошибочный частичный файл не открывается runtime.
- Контекст offline Tutor берётся из небольших локальных Guaraní-файлов по текущему уроку (например, словарь математических терминов и краткая теория курса); это прямой подбор по lesson ID, не RAG/векторная база.
- Engine initialize/inference/download исполняются не на main thread; engine корректно закрывается при окончании сессии/очистке. После установки локальный ответ не требует сети.

### Сетевой выбор {#network-routing}

Наблюдение Android connectivity используется только как подсказка перед запросом: истинный успех подтверждается реальным ответом endpoint. В `Auto` нет сети → offline, если модель готова; есть сеть и credential → попытка online; временный сбой online → offline fallback, если модель готова. Без модели результат — локализованная ошибка с retry. `Online` и `Offline` всегда соблюдают явный выбор пользователя.

## Секреты и данные пользователя {#privacy}

- Никаких ключей пока нет; никаких реальных запросов до появления ключа не отправлять.
- Для Online внешний провайдер получает текст вопроса и минимальный переданный контекст урока. Не отправлять course progress или answer key по умолчанию; UI обозначает сетевой режим.
- Не писать API key, пользовательские сообщения или сгенерированные ответы в лог. Только незавершённый вопрос, его ID и последний ответ временно хранятся в app-private storage для восстановления при плохой связи; остальная история чата остаётся в памяти. Worker удаляет данные по TTL.
- Offline запросы и context pack остаются на устройстве.
- Модель скачивается только после явного выбора; ошибка загрузки не делает её активной.

## Целевой код и проверки {#implementation-map}

Реализованные P0 точки: `app/src/main/kotlin/com/guarani/mathdemo/course/Course.kt`, `exercise/ExerciseValidator.kt`, `progress/ProgressRepository.kt`, `ui/CourseApp.kt`, `MainActivity.kt`; versioned course asset — `app/src/main/assets/courses/trigonometry.json`. P1 Tutor packages/context files пока отсутствуют.

Для внутренней пересылки доступен `assembleShare`: R8 сокращает код и неиспользуемые ресурсы, APK подписан локальным debug-ключом и устанавливается поверх debug-сборки того же разработчика. Для публичной публикации нужен отдельный release key; `share` не является магазинной подписью.

P0 checks: debug APK build, validator cases, install/launch/course flow, persistence/XP/unlock after restart. Galaxy S24 check is still required before device-specific performance claims. P1 checks (repository migration cases, online error path, offline model download/inference) apply only after Tutor implementation.

## Acceptance {#acceptance}

- AC-1: проект собирается в APK для Android; курс доступен offline и exercise validators не делают сетевых вызовов.
- AC-2: холодный старт и возврат в урок восстанавливают один корректный snapshot; повтор completion не начисляет XP второй раз.
- AC-3: Online provider использует `gemini-3.8-flash` через Interactions API, показывает отсутствие credentials и не встраивает secret в распространяемую сборку.
- AC-4: Download/initialize/inference локальной модели не блокирует UI; модель отмечается установленной только после целостной загрузки; базовый ответ проверен в airplane mode на S24.
- AC-5: Auto сообщает источник ответа и применяет правила fallback; при любой ошибке Tutor приложение остаётся пригодным для уроков.
- AC-6: курс и ключевые validator/domain функции не зависят от Compose; фактический scope остаётся одним Android app module без backend.
- AC-7: нематематический вопрос отклоняется Worker до создания job, месячного счётчика и вызова AI; Android сообщает причину и разрешает задать новый вопрос.

## Related {#related}

- Learning product contract: `spec://modules/learning/FEAT-010-learning-demo#root`.
- Tutor behavior: `spec://modules/tutor/FEAT-011-ai-tutor#root`.
- External documentation and implementation sequence: [Implementation Plan](../../IMPLEMENTATION_PLAN.md).

## Внешние технические источники {#sources}

Проверено 2026-09-23; перед добавлением dependency/model повторно сверить актуальные версии и форматы.

- [Google: каталог моделей Gemini](https://ai.google.dev/gemini-api/docs/models).
- [Google: Generate Content API](https://ai.google.dev/gemini-api/docs/generate-content/text-generation).
- [Google: цены Gemini Developer API](https://ai.google.dev/gemini-api/docs/pricing).
- [LiteRT-LM Kotlin API: Android, Gradle и engine lifecycle](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).
- [Официальный AI Edge Gallery model allowlist: Gemma3-1B-IT artifact](https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_15.json).
- [Google Gemma: текущие варианты для mobile и выбор модели](https://ai.google.dev/gemma/docs/get_started).
- [Android Developers: app-specific persistent files](https://developer.android.com/training/data-storage/app-specific).

## History {#history}

- 2026-09-27 — добавлена компактная внутренняя `share`-сборка с R8 и resource shrink; подпись ограничена локальным debug-ключом.

- 2026-09-23 — первая черновая архитектура из `context.md`; код проекта отсутствует, поэтому stack/dependency/model проходят физическую проверку до реализации.
- 2026-09-23 — P0 demo stack pinned and one-app implementation started; AI model/runtime and credentials do not enter P0.
- 2026-09-24 — по отзыву на живой экран карта должна использовать один согласованный ракурс, а Map, Tutor и Profile становятся отдельными destinations; ответы в упражнениях вводятся встроенной клавиатурой.
- 2026-09-26 — для личного Online Tutor выбран Cloudflare Worker с `gemini-3.8-flash`, устойчивым ID запроса и временным хранением ответа.
- 2026-09-26 — перед вызовом AI добавлен локальный по вычислению фильтр явных математических вопросов, с отказом `not_math` и возможностью переформулировать вопрос.
- 2026-09-26 — вызов Gemini переключён на `generateContent` по примеру пользователя; `minimal` заменён на поддерживаемый `low`.
- 2026-09-26 — онлайн-ответ запрашивается простым текстом, чтобы Tutor показывал математические выражения без сырой разметки.
- 2026-09-26 — онлайн-ответ ограничен кратким результатом и одним пояснением; Worker очищает разметку и проверяет фактический размер входного тела без доверия к заголовку.
- 2026-09-26 — рабочий Gemini key подтверждён прямым запросом; одновременные повторы одного ID не должны расходовать месячный лимит повторно.
- 2026-09-26 — язык ответа выбирается по основному языку вопроса, а не по отдельным словам другого языка.
