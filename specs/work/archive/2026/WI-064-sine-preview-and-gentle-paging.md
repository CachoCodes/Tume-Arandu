# WI-064: Просмотр у синуса и мягкая прокрутка карты

Canon action: none

## Outcome

Android-эмулятор открывает карту так, будто ученик дошёл до урока 7 «SENO», а один жест карты переносит максимум на один этап.

## Scope

- In: временный режим просмотра с предыдущими уроками пройденными, существующая локальная правка свайпа карты, сборка и установка на эмулятор.
- Out: настоящий прогресс ученика, содержимое уроков и порядок карты.

## Specs

- Governing: `spec://modules/learning/FEAT-010-learning-demo#course-map`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`.

## Acceptance

- AC-1: запуск preview с `reached sine` сразу показывает этап 4, пройденные предыдущие уроки и доступный урок 7.
- AC-2: короткий свайп не меняет этап, длинный переводит ровно на соседний; урок 7 открывается.
- AC-3: APK собирается и устанавливается поверх текущего, реальный прогресс не меняется.

## Dependencies

Режим preview уже использует отдельное хранилище; правка прокрутки была сделана параллельно в исходной папке и переносится без перезаписи её файлов.

## Risks

Параллельный агент может обновить карту; перед переносом перепроверить файл и не перезаписывать его исходную копию.

## Result

Готово 2026-09-27.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `adb shell am start -n com.guarani.mathdemo/.MainActivity -e preview courses/trigonometry.json -e reached sine`: UI dump содержит `Etapa 4` и `Lección 7: 7. SENO`; [снимок карты](../../../../docs/handoff/screenshots/tume-reached-sine-map.png). | passed |
| AC-2 | На установленном APK свайп на 55 px оставил `Etapa 4`, длинный свайп перевёл на `Etapa 5`, обратный вернул на 4; нажатие открыло `7. SENO` и Back вернуло карту. [Снимок урока](../../../../docs/handoff/screenshots/tume-sine-lesson.png). | passed |
| AC-3 | `:app:assembleDebug` и `:app:assembleShare` успешны, `adb install -r` — `Success`. Без preview карта показывает настоящий прогресс 180 XP и 3/6 первого этапа; [снимок](../../../../docs/handoff/screenshots/tume-real-progress-after-sine-preview.png). | passed |

Рабочее дерево интеграционного checkout после коммита чистое. Параллельный пользователь подтвердил, что сейчас другие агенты файлы не меняют.
