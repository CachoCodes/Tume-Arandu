# WI-007: Ортографическая изометрическая карта курса

Canon action: direct-edit

## Outcome

Заменить перспективную имитацию карты общей ортографической изометрической сценой, в которой поле, уроки, маршрут и будущие PNG/WebP-декорации используют одни grid-координаты и одну проекцию.

## Scope

- In: ромбическая изометрическая сетка, координатная проекция мира в экран, размещение платформ и пути через grid-ячейки, поддержка PNG/WebP-декораций с bottom-center anchor, единый порядок отрисовки по экранной глубине.
- Out: создание или рендеринг пользовательских арт-ассетов, изменение курса/прогресса/уроков, новые зависимости.

## Specs

Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`; visual constraint: `spec://modules/learning/FEAT-010-learning-demo#language-and-visuals`.

## Acceptance

- AC-1: мировые координаты проецируются только формулами `screenX = originX + (gridX - gridY) * tileWidth / 2` и `screenY = originY + (gridX + gridY) * tileHeight / 2`; индивидуальных rotation/skew/perspective-модификаторов нет.
- AC-2: поле использует ортографические ячейки постоянного размера и ромбическую изометрическую сетку без сужения к горизонту и масштабирования по экранной высоте.
- AC-3: платформы и камни маршрута ставятся по grid-координатам; размеры платформ остаются постоянными, zigzag снизу вверх сохраняется.
- AC-4: PNG/WebP-декорации загружаются из каталога ассетов карты, используют bottom-center grid anchor и отображаются без изменения камеры/перспективы; отсутствие будущего ассета не ломает экран.
- AC-5: элементы отрисовываются от дальних/верхних к ближним/нижним по `gridX + gridY`, чтобы нижние перекрывали верхние при пересечении.
- AC-6: названия и состояния остаются под своими платформами, доступность уроков и hit targets сохраняются.
- AC-7: Debug APK собирается, устанавливается и визуально проверяется на портретном AVD API 35.

## Dependencies

- Курс Trigonometry из `app/src/main/assets/courses/trigonometry.json`.
- Локальный Android SDK и AVD `onelu` (API 35, arm64).
- Декоративные PNG/WebP ассеты будут предоставлены отдельно; реализация поддерживает их до получения файлов.

## Risks

- Ортографическая сцена потребует другой вертикальной прокрутки, чем предыдущая псевдоперспектива; проверить, что первый доступный урок виден у нижнего края.
- Отсутствие готовых ассетов не позволяет визуально подтвердить их фактическую камеру/тени; до получения файлов проверить безопасную загрузку и якорь в коде.

## Result

Заменена трапециевидная псевдоперспектива на ортографическую ромбическую сетку с общей grid-проекцией. Платформы, paver-камни и будущие декорации получают позиции из одних `(gridX, gridY)` координат; размеры не уменьшаются по высоте экрана. Декорации подхватываются из `assets/map-decorations` в PNG/WebP, bottom-center ставится на grid-anchor, Compose не применяет к изображению ракурсные трансформы. Painter order сортируется по `gridX + gridY`. Самих пользовательских PNG/WebP пока нет: экран пропускает такие декорации до добавления файлов.

Проверены Debug APK и открытие карты на AVD `onelu` API 35; первый урок и его подпись остаются у нижней части видимой области. Учебная навигация и accessibility semantics платформ сохранены в коде.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `CourseApp.kt`: `projectIsoPoint` реализует заданные X/Y уравнения. Поиск `graphicsLayer`, rotation, camera distance, Matrix и skew в компоненте карты — совпадений нет. | passed |
| AC-2 | `/tmp/guarani-isometric-course-map-final.png`: ромбические клетки имеют постоянный размер; сцена не сужается и не масштабируется по высоте. | passed |
| AC-3 | Тот же экран показывает нижний доступный урок, два следующих узла и пaver-маршрут на одной сетке; позиции строятся через `lessonGridPoint` / `projectIsoPoint`. | passed |
| AC-4 | `IsoMapDecoration` фиксирует `Alignment.BottomCenter`; `loadMapDecoration` ищет WebP и PNG и безопасно пропускает отсутствующий файл. Контракт каталога и ассетов описан в `app/src/main/assets/map-decorations/README.md`. Пользовательских файлов пока нет, поэтому их художественный результат не входит в visual review. | passed |
| AC-5 | `CourseSceneItem` упорядочивается по `point.depth` (`gridX + gridY`) до рисования платформ, декораций и path stones. | passed |
| AC-6 | `/tmp/guarani-isometric-course-map-final.png`: названия и состояния расположены под видимыми платформами. `PlatformNode` сохраняет clickable и semantics/disabled состояния. | passed |
| AC-7 | `:app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r` — `Success`; приложение foreground на AVD `onelu` API 35. | passed |

`python3 tools/spec-lint.py` — 0 errors, 0 warnings (structure only). Checked 2026-09-23. Git commit unavailable: checkout has no `.git`; working tree status unavailable.
