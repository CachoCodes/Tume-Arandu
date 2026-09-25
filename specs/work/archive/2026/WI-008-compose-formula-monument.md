# WI-008: Compose 2.5D FormulaMonument карты курса

Canon action: direct-edit

## Outcome

Показать в существующей прокручиваемой изометрической карте один повторно используемый Compose-native монумент с формулой `sin(30°) = 1/2`, визуально встроенный в синюю ортографическую сцену.

## Scope

- In: проверка существующей карты и визуальных токенов; компонент FormulaMonument с общим вектором экструзии для таблички и основания, нативной дробью и Compose Canvas/Path; grid-якорь справа от маршрута между уроками 2 и 3; сборка, установка и визуальная проверка на локальном Android-эмуляторе.
- Out: переработка карты, новые уроки/поведение прогресса, дополнительные объекты/формулы, PNG/WebP, Blender, WebView, 3D-движок, зависимости и API-ключи.

## Specs

Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`; visual constraint: `spec://modules/learning/FEAT-010-learning-demo#language-and-visuals`.

## Acceptance

- AC-1: существующие layout, scroll, состояния, подписи и действия уроков сохранены; карта компилируется.
- AC-2: отображается ровно один неинтерактивный `sin(30°) = 1/2` FormulaMonument, размещённый изометрическими координатами справа от маршрута между уроками 2 и 3 внутри того же scroll content.
- AC-3: Compose Canvas/Path рисует скруглённые табличку и основание с видимыми гранями и мягкой тенью; все поверхности используют согласованный вектор экструзии `(8dp, 7dp)`, совпадающий с синим стилем карты.
- AC-4: формула отображается нативным Compose Text; `1/2` — вертикально сложенная дробь с чертой; компонент повторно используем для другого текста формулы через данные.
- AC-5: на портретном эмуляторе с пропорциями Samsung Galaxy S24 объект не пересекает уроки, подписи, камни маршрута или границы карты и естественно прокручивается с миром.
- AC-6: `:app:assembleDebug` успешно собирает Debug APK, устанавливает его и открывает существующую карту; затронутый экран визуально проверен.

## Dependencies

- Активная Compose-карта в `app/src/main/kotlin/com/guarani/mathdemo/ui/CourseApp.kt`.
- Локальный Android SDK и доступный портретный эмулятор API 35.

## Risks

- Изменение действующего правила «декорации только pre-rendered ассеты» на пользовательское требование Compose-геометрии зафиксировать в FEAT-010 до реализации.
- Вертикально высокая табличка может упереться в узел или обрезаться у viewport; расположение и полное отображение подтвердить на S24-подобном размере 411×914 dp.

## Result

Добавлен переиспользуемый `FormulaMonumentFormula` и неинтерактивный Compose `FormulaMonument`: нативные `Text` формируют выражение и вертикальную дробь; Canvas строит мягкую тень, скруглённую табличку, общий `(8dp, 7dp)` рельеф, опору и пьедестал в синей палитре. Единственный объект карты закреплён bottom-center на промежуточной координате справа от маршрута между уроками 2 и 3. Остальная карта, её порядок, labels, прокрутка и переходы уроков сохранены.

Проверено 2026-09-23 на portrait Android emulator: 1080×2400 px при 420 dpi (примерно 411×914 dp, размер Samsung Galaxy S24). `:app:assembleDebug` завершился успешно; APK установлен, карта открылась. Ручной сценарий: формула видна рядом с узлами 2 и 3; свайп вниз двигает её вместе с доской и показывает следующие уровни; свайп назад возвращает к уроку 1; нажатие на урок 1 открывает его экран, Back возвращает на карту. Финальный снимок: `/tmp/guarani-formula-monument-verified.png`. Отдельный снимок прокрутки: `/tmp/guarani-formula-final-scroll.png`.

REVIEW/debt: нет незакрытых пунктов в пределах scope. Git commit недоступен: checkout без `.git`; состояние working tree недоступно.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Debug APK установлен и открыт; экран карты перед/после свайпа и переход урок 1 → Back: `/tmp/guarani-formula-monument-verified.png`, `/tmp/guarani-formula-final-scroll.png`, `/tmp/guarani-formula-final-lesson.png`, `/tmp/guarani-formula-final-map.png`. | passed |
| AC-2 | `CourseApp.kt`: создаётся единственный `formulaMonumentPoint` из средней точки между lesson points 1 и 2 с изометрическим смещением вправо; он входит в общий scroll content и scene depth sort. Визуальная позиция видна на `/tmp/guarani-formula-monument-verified.png`. | passed |
| AC-3 | `FormulaMonument.kt`: Canvas/Path строит скруглённые грани и подставку с общими `FormulaExtrusionX/Y = 8/7dp`, мягкой овальной тенью и синими цветами. Визуальное подтверждение: `/tmp/guarani-formula-monument-verified.png`. | passed |
| AC-4 | `FormulaMonumentFormula` хранит левую часть и числитель/знаменатель; Compose `Text` рисует символы, Column и отдельная линия формируют вертикальную дробь. Экран: `/tmp/guarani-formula-monument-verified.png`. | passed |
| AC-5 | `adb shell wm size` = `1080x2400`, `adb shell wm density` = `420`; `CoursePath` ширина монумента = `min(172dp, maxWidth * .45)`. На S24-размере объект не пересекает уроки 2/3, подписи, путь и края; свайп меняет его экранную позицию вместе с уроками (`/tmp/guarani-formula-final-scroll.png`). | passed |
| AC-6 | `:app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r app/build/outputs/apk/debug/app-debug.apk` — `Success`; `am start -W` — `Status: ok`; ручной экранный сценарий завершён и финальная карта визуально осмотрена. | passed |
