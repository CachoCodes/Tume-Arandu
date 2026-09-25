# WI-002: Рабочий P0 Android demo

Canon action: direct-edit

## Outcome

Собрать устанавливаемое Android demo с коротким Guaraní-курсом тригонометрии, работающими заданиями и локальным прогрессом, пригодное для продолжения работы над AI Tutor позже.

## Scope

- In: один Kotlin/Compose Android app, статический курс из 8 уроков, course map, lesson/theory, все 4 exercise types, deterministic validation, подсказки и пошаговые решения, completion/XP, локальное сохранение, доступные UI labels, debug APK.
- Out: real online/offline AI, API keys, backend, второй курс, аккаунты, аналитика, публикация в магазине, полный production QA.

## Specs

Governing / constraint / affected: `spec://modules/learning/FEAT-010-learning-demo#root` и `spec://modules/android/PROP-010-android-demo-architecture#root`; Tutor (`spec://modules/tutor/FEAT-011-ai-tutor#root`) остаётся draft и вне этого результата. Constraint: `context.md`.

## Acceptance

- AC-1: `./gradlew :app:assembleDebug` создаёт APK, который устанавливается и открывается на Android emulator или физическом Android устройстве на Course Map.
- AC-2: карта содержит один курс из 8 последовательных уроков; первый доступен, следующие открываются после завершения предыдущего.
- AC-3: приложение даёт пройти theory и multiple choice, input, matching, step-by-step упражнения; неверный ответ можно повторить, решения заранее записаны, проверка работает без сети.
- AC-4: результат упражнения, позиция урока, completion, mastery и одноразовые 100 XP сохраняются и переживают перезапуск приложения.
- AC-5: первый завершённый урок открывает следующий; repeated completion не начисляет XP повторно.
- AC-6: UI и учебный контент отображаются на Guaraní с нужными диакритическими знаками; главные действия имеют accessibility labels, ответ не кодируется только цветом.
- AC-7: код содержит `@spec` на активный канон и `python3 tools/spec-lint.py` проходит без ошибок и предупреждений.

## Dependencies

- Android SDK уже установлен под `/Users/maks/Library/Android/sdk`; локально обнаружен API 35.
- JDK 17 установлен для сборки по пути `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`; Android Studio для проекта не требуется.
- Нужна сеть только для первоначальной загрузки Android Build Tools/Gradle/Maven artifacts; курс и ответы находятся в приложении.

## Risks

- Физический Samsung Galaxy S24 не подключён; emulator smoke pass не подтверждает поведение на конкретном устройстве.
- Guaraní учебный текст — первый авторский вариант и требует проверки носителем/преподавателем до публичного demo.

## Result

P0 Android demo собран, установлен на API 35 arm64 emulator, пройден от карты через все четыре типа упражнений до completion, повторный запуск восстановил прогресс. Реальные AI-сервисы и ключи не добавлялись. Внешнее использование ждёт проверки Guaraní преподавателем/носителем; проверка на Galaxy S24 и TalkBack остаётся следующим device gate.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL; `adb install` + MainActivity opened on AVD `onelu` (API 35, arm64). APK: `app/build/outputs/apk/debug/app-debug.apk`. | passed |
| AC-2 | `trigonometry.json` has 8 lessons; emulator map showed first available and following lessons locked; completing lessons 1–2 opened lesson 3. | passed |
| AC-3 | Emulator scenario: wrong then correct choice with retry, numeric input, full matching, both steps; hint and saved solution opened. APK permission dump has no `INTERNET`; course and validators are local. | passed |
| AC-4 | DataStore retained current exercise (`Jejapo 2 / 3`) through force-stop/reopen; two completions retained 200 XP and 2/8 after relaunch. | passed |
| AC-5 | Completing lessons 1–2 unlocked the following lesson; replay of a completed lesson showed `+0 XP`. | passed |
| AC-6 | Guaraní/Unicode visible in emulator; accessibility tree exposed lesson nodes, radio options, back/hint/solution/primary actions with labels. State includes text/icon as well as color. Full TalkBack/large-text review remains a separate device pass. | passed |
| AC-7 | Active-spec `@spec` tags present; `python3 tools/spec-lint.py`: 0 errors, 0 warnings. | passed |

REVIEW / remaining gate: a Guaraní-speaking math teacher should review the draft course before external use; full Galaxy S24, TalkBack, and large-text review remain outside this P0 emulator acceptance. No AI, API key, backend, or internet permission was added.

Checked 2026-09-23. Git commit: unavailable (checkout has no `.git`). Working tree: N/A; files are in the shared project checkout.
