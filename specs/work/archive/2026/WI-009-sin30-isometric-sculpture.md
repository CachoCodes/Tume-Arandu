# WI-009: Изометрическая скульптура sin 30°

Canon action: direct-edit

## Outcome

Заменить FormulaMonument-табличку на одну цельную небольшую Compose-native треугольную математическую скульптуру, которая органично использует геометрию и освещение узлов курса.

## Scope

- In: сверка PlatformNode и текущего экрана; замена одного объекта на стоящую объёмную прямоугольную треугольную призму с дугой 30°, поднятым `sin 30°`, отдельной сложенной `1/2` и компактной общей изометрической платформой; сборка, установка и ручная visual QA на S24-размере.
- Out: переработка карты и уроков, другие декорации, изображения/PNG, billboard/card, дополнительные зависимости или 3D-движок.

## Specs

Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`; visual constraint: `spec://modules/learning/FEAT-010-learning-demo#language-and-visuals`.

## Acceptance

- AC-1: формульная табличка, отдельный декоративный треугольный знак и прежнее пьедестальное оформление удалены; рядом с путём остаётся один неинтерактивный объект размером около 100–150×90–140dp.
- AC-2: один общий Compose-native объект визуально выражает `sin(30°) = 1/2`: заметная треугольная призма, встроенная дуга 30°, поднятый `sin 30°` и отдельная вертикальная дробь на общей платформе; нет прямоугольной формульной карточки/таблички.
- AC-3: объект повторяет перспективу PlatformNode: общая ортографическая геометрия, мягкий свет сверху-слева и тень вправо-вниз; общий вектор объёма около `(7dp, 6dp)`, небольшие голубые боковые грани и скруглённые фаски, без тяжёлой экструзии.
- AC-4: объект остаётся в существующем scroll content справа от маршрута между уроками 2 и 3; не пересекает узлы, подписи, path stones или viewport и естественно движется при прокрутке.
- AC-5: `:app:assembleDebug` успешен; APK установлен и карта вручную просмотрена/прокручена на portrait эмуляторе около 411×914dp.

## Dependencies

- Текущие `PlatformNode` и `CoursePath` в `app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt`.
- Активный визуальный контракт карты FEAT-010.
- Android SDK и portrait emulator.

## Risks

- Композиция сложной формы может стать мелкой или читаться как разрозненные знаки; проверить силуэт, контакт основания и читаемость на S24-подобной ширине.
- Дуга и дробь могут сдвинуться относительно треугольника на узкой ширине; использовать адаптивные ограничения и проверить край viewport.

## Result

Удалён прежний `FormulaMonument.kt` с табличной композицией. Вместо него `Sin30Decoration.kt` рисует одной Compose-сценой изометрическую платформу, стоящую объёмную прямоугольную треугольную призму с интегрированной дугой `30°`, приподнятую подпись `sin 30°` и отдельную сложенную дробь `1/2`. Объект размещён bottom-center якорем в существующем слое сортировки карты между уроками 2 и 3; кнопкой он не является.

Проверено 2026-09-23 на portrait Android emulator: `1080×2400` при `420 dpi` (примерно `411×914 dp`, формат Samsung S24). `:app:assembleDebug` завершился успешно, APK установлен и запущен. Вручную проверены экран карты, свайп карты вверх по уровням, открытие доступного урока и возврат Back на карту. Снимки: `/tmp/guarani-sin30-sculpture-final.png`, `/tmp/guarani-sin30-sculpture-scroll.png`, `/tmp/guarani-sin30-sculpture-lesson.png`, `/tmp/guarani-sin30-sculpture-map.png`.

REVIEW/debt: дополнительных проблем в пределах scope не обнаружено. Git commit и состояние working tree проверить нельзя: каталог проекта не является Git checkout.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `FormulaMonument.kt` удалён; на карте используется единственный `Sin30Decoration`. Визуальная проверка итоговой карты: `/tmp/guarani-sin30-sculpture-final.png`. | passed |
| AC-2 | `Sin30Decoration.kt`: Canvas рисует общую платформу, стоящую треугольную призму и встроенную дугу; Compose Text рисует поднятые `sin 30°` и вертикальную дробь `1/2`. Визуально проверено на `/tmp/guarani-sin30-sculpture-final.png`. | passed |
| AC-3 | `Sin30Decoration.kt` использует общий вектор объёма `(7dp, 6dp)`, светлую верхнюю грань, приглушённые боковые грани, верхне-левый свет и мягкую тень вниз-вправо. Визуальная проверка на S24-подобном viewport: `/tmp/guarani-sin30-sculpture-final.png`. | passed |
| AC-4 | `CourseApp.kt`: один grid-якорь справа от маршрута между уроками 2 и 3, компонент сортируется вместе с остальными scene items и движется в том же scroll content. Экран до/после прокрутки: `/tmp/guarani-sin30-sculpture-final.png`, `/tmp/guarani-sin30-sculpture-scroll.png`. | passed |
| AC-5 | `:app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r app/build/outputs/apk/debug/app-debug.apk` — `Success`; ручной сценарий: карта → свайп → доступный урок → Back на карту. Экраны: `/tmp/guarani-sin30-sculpture-lesson.png`, `/tmp/guarani-sin30-sculpture-map.png`. | passed |
