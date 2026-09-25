# Техническая карта

Таблица связывает продуктовые области с текущими точками реализации; статусы работ находятся в `specs/BOARD.md`.

| Область ответственности | Код | Прямые проверки | Governing spec |
|---|---|---|---|
| Android entry point / navigation | `app/src/main/kotlin/com/guarani/mathdemo/MainActivity.kt`, `ui/CourseApp.kt` | debug APK install/launch и переходы на эмуляторе | `spec://modules/android/PROP-010-android-demo-architecture#navigation` |
| Course model / static content | `course/Course.kt`, `app/src/main/assets/courses/trigonometry.json` | встроенный loader, course content/schema checks | `spec://modules/android/PROP-010-android-demo-architecture#course-schema` |
| Exercise validation / lesson flow | `app/src/main/kotlin/com/guarani/mathdemo/exercise/ExerciseValidator.kt`, `app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt` | validator behavior in local exercise flow | `spec://modules/learning/FEAT-010-learning-demo#exercises` |
| Local progress | `progress/ProgressRepository.kt` | emulator restart, unlock, one-time XP smoke checks | `spec://modules/android/PROP-010-android-demo-architecture#persistence` |
| AI Tutor routing/providers | Not implemented in P0; planned under `tutor/` and `ai_context/` | Requires a separate P1 work item | `spec://modules/android/PROP-010-android-demo-architecture#ai-boundary` |

`common/` and `modules/` contain canon; `work/` tracks implementation; `examples/` contains teaching samples; `protocols/` define workflow.
