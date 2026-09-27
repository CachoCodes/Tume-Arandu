# WI-065: Заглушка адаптации уроков из профиля

Canon action: direct-edit

## Outcome

Из профиля открывается отдельный экран-заглушка будущей адаптации уроков по ошибкам с Guaraní и Spanish.

## Scope

- In: переход из профиля, статический экран, переключение GN/ES, второй Android-эмулятор, сборка и проверка.
- Out: анализ ошибок, персональные рекомендации, изменение уроков или прогресса.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#adaptive-placeholder`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#navigation`.

## Acceptance

- AC-1: профиль показывает вход на новый экран, переход и возврат работают.
- AC-2: экран по умолчанию на Guaraní, переключается на Spanish и ясно обозначен как заглушка без аналитики.
- AC-3: APK собирается и устанавливается на второй эмулятор; первый остаётся на прежнем экране и с прежним прогрессом.

## Dependencies

Используются существующие Compose-компоненты профиля и навигация приложения.

## Risks

Текст Guaraní Jopara требует будущей проверки носителем языка.

## Result

Готово 2026-09-27.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | На `emulator-5556` профиль показывает [вход](../../../../docs/handoff/screenshots/tume-adaptive-profile-entry.png); нажатие открывает новый экран, Android Back возвращает в профиль. | passed |
| AC-2 | [Guaraní](../../../../docs/handoff/screenshots/tume-adaptive-placeholder-gn.png) открыт по умолчанию; кнопка ES показывает [Spanish](../../../../docs/handoff/screenshots/tume-adaptive-placeholder-es.png), GN возвращает Guaraní. Оба текста говорят о частоте и серьёзности ошибок и о том, что функция в подготовке; аналитика не запускается. | passed |
| AC-3 | `:app:assembleDebug :app:assembleShare` — `BUILD SUCCESSFUL`; `adb -s emulator-5556 install -r` — `Success`. На первом `emulator-5554` остались `Etapa 4` и урок `7. SENO`; установка туда не выполнялась. | passed |

REVIEW: формулировку Guaraní Jopara следует вычитать с носителем языка перед реальным запуском функции. После коммита рабочее дерево интеграционного checkout чистое.
