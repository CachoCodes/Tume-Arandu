# WI-062: Плашка Tume Arandu и переход к Tutor из помощи

Canon action: direct-edit

## Outcome

На каждом учебном экране видна небольшая подпись команды Tume Arandu; после последнего шага помощи ученик может открыть Tutor с подготовленным вопросом о текущем задании.

## Scope

- In: Android Compose заголовок книжечки/урока/повторения, финальный шаг помощи каждого упражнения, переход в существующий Tutor, Guaraní по умолчанию и Spanish при переключении, установка и проверка на эмуляторе.
- Out: автоматическая отправка вопроса, изменение проверки ответов или начисления XP, новая модель AI.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#lesson-content`, `spec://modules/learning/FEAT-010-learning-demo#solutions`.
- Constraint: `spec://modules/tutor/FEAT-011-ai-tutor#behavior`, `spec://modules/learning/FEAT-010-learning-demo#progress`.

## Acceptance

- AC-1: Android Emulator с текущим приложением открыт для просмотра; после обновления показывает новую версию.
- AC-2: компактная подпись «создано с командой Tume Arandu» видна в книжечке, основном уроке и повторении; по умолчанию Guaraní, Spanish после переключения.
- AC-3: в помощи каждого типа упражнения кнопка «Спросить AI Tutor» появляется только на последнем шаге; она открывает существующий Tutor с редактируемым вопросом по текущему уроку и заданию, ничего не отправляет сама и не меняет результат.
- AC-4: из Tutor можно вернуться к уроку; сборка, установка поверх и проверка раннего и позднего урока проходят без сброса прогресса.

## Dependencies

WI-059/061 завершили единый курс. Существующий Tutor доступен через вкладку.

## Risks

При переходе из модального окна можно случайно потерять состояние упражнения или пометить решение просмотренным; проверить на эмуляторе. Проверить формулировку Guaraní с преподавателем.

## Result

Готово 2026-09-27. Реализация в коммите `470df00` (ветка `integration/pr3-13-lessons`); перед итоговой записью отслеживаемое Git-дерево чистое. Компактный APK установлен поверх прежнего приложения в запущенном Android Emulator `emulator-5554`.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `adb devices` показывает `emulator-5554 device`, `dumpsys activity` — `com.guarani.mathdemo/.MainActivity` как top resumed; [карта после установки](../../../../docs/handoff/screenshots/tume-final-map.png). | passed |
| AC-2 | На эмуляторе проверены [книжечка](../../../../docs/handoff/screenshots/tume-credit-book-gn.png), [основной урок](../../../../docs/handoff/screenshots/tume-credit-lesson-gn.png) и [повторение 13](../../../../docs/handoff/screenshots/tume-review13-gn.png); после переключения языка подпись стала «Creado con el equipo Tume Arandu». | passed |
| AC-3 | `python3 tools/check_tutor_handoff.py`: PASS — на первом шаге кнопки AI нет, на последнем есть, Tutor получает редактируемый черновик, возврат оставляет упражнение нерешённым. Вручную повторено на испанском и на уроке 13 с пошаговым заданием; [GN финальный шаг](../../../../docs/handoff/screenshots/tume-help-last-gn.png), [ES финальный шаг](../../../../docs/handoff/screenshots/tume-help-last-es.png), [Tutor GN](../../../../docs/handoff/screenshots/tume-tutor-gn.png). Автоматической отправки нового вопроса не было. | passed |
| AC-4 | `:app:assembleDebug` и `:app:assembleShare` успешны; `adb install -r` — Success. После установки реальный прогресс остался 180 XP и 3/6 первого этапа; Tutor возвращает к тому же заданию. | passed |

REVIEW: формулировки Guaraní Jopara нужно вычитать с носителем/преподавателем. Офлайн-модель Tutor остаётся прежним незавершённым направлением; новый переход использует существующий онлайн-Tutor и не отправляет вопрос без действия ученика.
