# WI-060: Компактный APK для пересылки

Canon action: direct-edit

## Outcome

Проект даёт отдельный уменьшенный APK для внутренней пересылки по слабой сети без потери основных экранов и курса.

## Scope

- In: измерение состава текущего APK, штатное сжатие/сокращение Android build, сравнение размеров и smoke-проверка.
- Out: смена стека, удаление учебного контента, публикация в магазине.

## Specs

- Governing: `spec://modules/android/PROP-010-android-demo-architecture#implementation-map`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#flow`, `spec://modules/learning/FEAT-010-learning-demo#progress`.

## Acceptance

- AC-1: размер базового и уменьшенного APK измерен; уменьшенный вариант существенно меньше.
- AC-2: уменьшенный APK собирается, устанавливается и открывает карту, книжечку, урок и повторение.
- AC-3: оптимизация не удаляет курс/ресурсы и не меняет сохранённый прогресс; способ подписи и предел использования явно указан.

## Dependencies

Финальная проверка после WI-059, чтобы измерять собранный полный курс.

## Risks

R8/resource shrink может удалить динамически используемый ресурс; нужен запуск APK. Отладочная подпись годится только для внутренней пересылки.

## Result

Готово 2026-09-27. Отдельный `share` APK содержит тот же курс и экраны, но собирается штатными R8 и resource shrink. Финальный файл: `/Users/maks/Downloads/Tume-Arandu-13-lessons.apk`, SHA-256 `402a2ec28ece5ae3ffbd4da1f8e1ba0d073e7b25226c710c2b922bdcc8c8920e`.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `stat`: debug 13 810 547 байт; share 1 894 480 байт (на 86,3% меньше). | passed |
| AC-2 | `:app:assembleShare` успешна; `adb install -r` — Success. На установленном APK открыты [карта](../../../../docs/handoff/screenshots/linear-map-stage7.png), [книжечка](../../../../docs/handoff/screenshots/linear-concept13.png), [урок](../../../../docs/handoff/screenshots/linear-lesson13.png), [повторение](../../../../docs/handoff/screenshots/linear-review13.png). | passed |
| AC-3 | Валидатор видит весь курс 26/89; после обновления реальные 180 XP и 3/6 не изменились. `apksigner verify --verbose`: подпись v2 действительна. `share` подписан локальным debug-ключом и предназначен для внутренней пересылки, не для Play Store. | passed |

REVIEW: для публичного релиза нужна постоянная release-подпись владельца приложения.
