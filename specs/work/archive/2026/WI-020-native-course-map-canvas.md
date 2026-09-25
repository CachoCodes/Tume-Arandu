# WI-020: Нативная 2.5D-карта курса

Canon action: direct-edit

## Outcome

Карта Android выглядит как цельная игровая карта: уровни идут зигзагом от нижнего края до верхнего, а объёмные платформы и модели sin/cos рисуются средствами Compose, без вставленного Blender-рендера.

## Scope

- In: программная отрисовка фона, маршрута, 3D-платформ и моделей sin/cos; удаление квадратного маркера внутри треугольников; размещение уровней по всей доступной высоте; сохранение блокировки, перехода в урок и accessibility.
- Out: изменение структуры и контента уроков, Tutor, профиля и навигации приложения.

## Specs

- Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#implementation-map`.

## Acceptance

- AC-1: Основная карта полностью рисуется программно через Compose Canvas. Runtime-экран карты не использует Blender-рендер, фоновое изображение сцены или экспортированные PNG-модели.
- AC-2: Восемь уровней распределены от нижней части карты до верхней, образуют читаемый зигзаг и остаются видимыми между шапкой и нижней навигацией на целевом Android-эмуляторе.
- AC-3: Платформы и обе математические модели выглядят объёмными за счёт граней, теней и согласованного света. Внутренний квадратный маркер прямого угла отсутствует; метки 30°/90°, стороны 1/√3/2 и формулы sin 30°/cos 30° остаются читаемыми.
- AC-4: Нажатие на доступный уровень открывает урок, закрытый уровень остаётся недоступным; каждое касание имеет понятное accessibility-описание и достаточный размер.
- AC-5: Debug APK собирается, устанавливается и запускается на Android-эмуляторе; карта просмотрена на устройстве после финальной правки.

## Dependencies

Android Compose Canvas, текущая модель курса и локальный Android SDK.

## Risks

Метки и математические формулы могут стать тесными на узком экране; их размеры и позиции должны вычисляться из Canvas и проверяться на целевой портретной ширине.

## Result

Android-карта полностью перерисована Compose Canvas: маршрут и платформы занимают доступную высоту, а SIN/COS — программно нарисованные объёмные треугольные призмы с подписями. Внутренний квадрат удалён из обеих моделей и Blender reference-сцены; Android больше не использует её рендеры или PNG-модели. Увеличены размеры призм и глубина боковых граней. Debug APK собран, установлен и запущен на Android-эмуляторе 1080×2400; первый уровень открывается, закрытый второй остаётся на карте. Приложение оставлено на карте. Дата: 2026-09-24. Git metadata в каталоге проекта отсутствует. REVIEW / техдолг: нет.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Runtime-компоновка и геометрия реализованы в [CourseApp.kt](/Users/maks/work/projects/guarani/app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt); `tools/map-decorations/README.md` фиксирует Blender как локальный визуальный reference. APK на эмуляторе отображает нативную карту: `/tmp/guarani-native-course-map-final.png`. | passed |
| AC-2 | Скриншот карты `/tmp/guarani-native-course-map-final.png`: уровни 1–8 распределены от низа до верха, SIN/COS расположены по бокам. Скриншот `/tmp/guarani-native-map-lesson1-final.png`: нажатие уровня 1 открыло урок. После возврата через кнопку приложения нажатие закрытого уровня 2 оставило карту открытой (`/tmp/guarani-native-map-locked-final.png`). | passed |
| AC-3 | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r app/build/outputs/apk/debug/app-debug.apk` — `Success`; `adb shell am start -n com.guarani.mathdemo/.MainActivity` — activity started. После установки финальной сборки приложение оставлено на карте. | passed |
| AC-4 | Эмулятор: касание уровня 1 открыло урок (`/tmp/guarani-native-map-lesson1-final.png`); возврат кнопкой приложения и касание закрытого уровня 2 оставили карту открытой (`/tmp/guarani-native-map-locked-final.png`). В [CourseApp.kt](/Users/maks/work/projects/guarani/app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt) каждой платформе назначены семантика заголовка/состояния и hit target размером 48–64 dp. | passed |
| AC-5 | Финальный APK собран, установлен, запущен и осмотрен на эмуляторе; результат: `/tmp/guarani-native-course-map-final.png`. | passed |
