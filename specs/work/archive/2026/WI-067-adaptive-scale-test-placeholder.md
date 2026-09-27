# WI-067: Шкала сложности и кнопка теста в заглушке адаптации

Canon action: direct-edit

## Outcome

Экран адаптации показывает демонстрационный уровень сложности по шкале 1–10 и кнопку входа в будущий тест.

## Scope

- In: статическая шкала, двуязычная кнопка и локальный отклик о том, что тест ещё готовится, проверка на втором эмуляторе.
- Out: реальное вычисление сложности, оценивание, задания теста, изменение прогресса.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#adaptive-placeholder`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`.

## Acceptance

- AC-1: GN/ES показывают шкалу 1–10 с демонстрационным значением, которое явно не выдано за результат ученика.
- AC-2: кнопка «пройти тест» на обоих языках нажимается и объясняет, что тест в подготовке; прогресс не меняется.
- AC-3: сборка и установка на второй эмулятор успешны; проверены шкала, действие кнопки и прежняя схема адаптации, первый эмулятор не затронут.

## Dependencies

WI-066 оформил понятную схему будущей адаптации.

## Risks

Текст Guaraní Jopara требует вычитки носителем языка.

## Result

Готово 2026-09-27.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | [Guaraní](../../../../docs/handoff/screenshots/tume-adaptive-scale-gn.png) и [Spanish](../../../../docs/handoff/screenshots/tume-adaptive-scale-es.png): десять сегментов, `6 / 10` и явная подпись, что число служит примером. | passed |
| AC-2 | [Нажатая кнопка](../../../../docs/handoff/screenshots/tume-adaptive-scale-test-gn.png) показывает сообщение о подготовке теста; Spanish проверен через UI dump. Обработчик меняет только локальное состояние экрана. | passed |
| AC-3 | `:app:assembleDebug` и `:app:assembleShare` — `BUILD SUCCESSFUL`; share APK установлен на `emulator-5556` и экран проверен. `emulator-5554` остался на `Etapa 4`. | passed |

REVIEW: формулировки Guaraní Jopara требуют вычитки носителем перед релизом.
