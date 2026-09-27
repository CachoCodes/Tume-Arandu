# WI-066: Сделать заглушку адаптации понятной

Canon action: direct-edit

## Outcome

Ученик с первого взгляда понимает, какие ошибки будут влиять на уроки и как может измениться практика; экран остаётся заглушкой.

## Scope

- In: визуальная и текстовая переработка существующего экрана, ясный вход из профиля, Guaraní и Spanish, проверка на втором эмуляторе.
- Out: реальные рекомендации, анализ ответов, изменение курса и прогресса.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#adaptive-placeholder`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#navigation`.

## Acceptance

- AC-1: из профиля ясно, зачем открывать экран; переход и возврат работают.
- AC-2: обе языковые версии показывают частоту и серьёзность ошибок как входы, возможные объяснение/повторение/практику как результат и ясно отмечают, что функция ещё не активна.
- AC-3: Android APK собирается, устанавливается на второй эмулятор; GN/ES и ближайшее регрессионное состояние визуально проверены, первый эмулятор не затронут.

## Dependencies

WI-065 создал маршрут и исходную заглушку.

## Risks

Текст Guaraní Jopara следует вычитать с носителем языка перед релизом.

## Result

Готово 2026-09-27.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | [Профиль](../../../../docs/handoff/screenshots/tume-adaptive-explained-profile.png) показывает понятное действие «Ehecha / Ver cómo funciona»; на втором эмуляторе нажатие открыло экран, Android Back вернул в профиль, повторный вход открыл Guaraní. | passed |
| AC-2 | [Guaraní](../../../../docs/handoff/screenshots/tume-adaptive-explained-gn.png) и [Spanish](../../../../docs/handoff/screenshots/tume-adaptive-explained-es.png) показывают два вида ошибок, стрелку к дополнительной практике и явную пометку о неактивной функции. UI dump содержит соответствующие заголовки в обоих языках. | passed |
| AC-3 | `:app:assembleDebug` и `:app:assembleShare` — `BUILD SUCCESSFUL`; `adb -s emulator-5556 install -r` — `Success`. На финальном APK GN/ES проверены; первый `emulator-5554` остался на `Etapa 4`. | passed |

REVIEW: формулировки Guaraní Jopara требуют вычитки носителем перед релизом. После коммита рабочее дерево интеграционного checkout чистое.
