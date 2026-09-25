# Implementation Plan: Android learning demo

P0 learning flow и Android architecture active. Рабочий P0 APK собран и пройден на API 35 emulator; evidence и остаточные device/content gates записаны в WI-002. P1 начнётся отдельными WI после снятия существенных вопросов.

Требования: [learning flow](modules/learning/FEAT-010-learning-demo.md), [AI Tutor](modules/tutor/FEAT-011-ai-tutor.md), [Android architecture](modules/android/PROP-010-android-demo-architecture.md).

## P0 — курс работает без сети

1. **Android skeleton / APK — готово:** Kotlin/Compose app, Navigation Compose и pinned toolchain; APK устанавливается и открывается на API 35 arm64 emulator.
2. **Курс и Guaraní content — черновик готов:** 8 уроков в versioned JSON, встроенная структурная проверка и правильное отображение Unicode; native/teacher review остаётся обязательной перед внешней демонстрацией.
3. **Карта курса — готово:** 8 состояний, последовательная разблокировка, mastery и XP.
4. **Lesson engine — готово:** теория, сохранённая позиция урока и упражнения.
5. **Exercise validators — готовы:** choice, exact-alias input, matching и step-by-step; решения статические, проверка локальная.
6. **Progress и completion — готовы:** DataStore snapshot, повторный запуск, completion, mastery, одноразовые 100 XP и unlock следующего урока проверены на emulator.
7. **Device pass перед внешним demo — осталось:** пройти TalkBack/увеличение текста и финальный flow на Galaxy S24. AI не входит в P0.

## P1 — AI Tutor

8. **Tutor UI и контракт:** подключить экран к `TutorService`; реализовать Auto/Online/Offline status и понятное поведение при ненастроенных сервисах.
9. **Online GPT-6 Luna:** после того, как владелец предоставит API project/key и согласует, как безопасно демонстрировать сборку, добавить минимальный Responses вызов `gpt-6-luna`; проверять timeout/auth/provider errors. До этого — отсутствие ключа отражается как `unconfigured`, никаких заглушек, выдаваемых за AI.
10. **Offline spike на устройстве:** проверить стабильную версию LiteRT-LM Kotlin и Gemma3-1B-IT int4 в airplane mode на конкретном S24; измерить запуск, memory/thermal behavior и ответ на короткие Guaraní math prompts. Принять/отклонить модель до UI download work.
11. **Offline install + Auto fallback:** добавить явную загрузку модели, свободное место, progress/retry/verification, app-private persistent storage, корректную инициализацию; проверить, что Auto переходит offline при сетевой ошибке.

## P2 — только если P0/P1 готовы

12. Streak и 3–4 простых achievements, затем недорогие анимации и polish карты. Дополнительный курс не добавлять, пока первый курс не проверен.

## Demo gates

- Не начинать polish/P2, пока S24 не проходит P0 полностью.
- Отсутствие credentials блокирует только real Online AI, не build APK, статический курс, progress или offline work.
- Отсутствие устойчивой локальной модели исключает offline-AI claim из demo; оно не должно сорвать работающий P0 APK.
- Любая зависимость или model artifact фиксируется точной версией, размером, лицензией/источником и проверенной командой установки.
