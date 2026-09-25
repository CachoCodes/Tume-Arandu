# WI-011: Three.js PNG-проп sin 30°

Canon action: direct-edit

## Outcome

Создать один повторно рендеримый изометрический 3D PNG-проп `sin(30°) = 1/2` локальной Three.js-сценой и вставить ассет в существующую Android-карту через Compose `Image`.

## Scope

- In: `tools/math-prop/` с Three.js исходником и командой рендера через установленный Chrome + `playwright-core`; прозрачный RGBA PNG 768×768; замена текущей Compose-геометрии ассетом; build и итеративная визуальная проверка на карте.
- Out: Blender, отдельный Chromium, React, WebView, 3D runtime в APK, дополнительные объекты и изменение маршрута/уроков.

## Specs

Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#course-map`; visual constraint: `spec://modules/learning/FEAT-010-learning-demo#language-and-visuals`.

## Acceptance

- AC-1: `tools/math-prop/` содержит минимальную HTML/JS-сцену Three.js и точную воспроизводимую команду рендера; рендер запускается установленным Chrome через `playwright-core`, без скачивания Chromium.
- AC-2: сцена строит треугольник `30°–60°–90°` через `Shape` → `ExtrudeGeometry` с глубиной `0.15`, bevelSize/bevelThickness `0.02`, bevelSegments `4`, освещённым `MeshStandardMaterial` и малым объёмным основанием.
- AC-3: `sin 30° =` и stacked fraction `1 / line / 2` выполнены рельефной `TextGeometry` на поверхности основания; PNG имеет прозрачность и контактную тень через `ShadowMaterial`, ровно 768×768 RGBA.
- AC-4: `OrthographicCamera` показывает грани треугольника и основания под общим изометрическим ракурсом; верхне-левый свет и тени визуально соответствуют карте.
- AC-5: карта использует Compose `Image` с PNG из `drawable-nodpi`; другие объекты карты, прокрутка, нажатие на урок и grid-anchor сохранены, наложений нет.
- AC-6: `:app:assembleDebug` успешно; на скриншоте приложения при фактическом размере `Image` формула читается, призма выглядит объёмной, виден контакт с полем, весь объект помещается в сцену.

## Dependencies

- Локальные Node.js, установленный Google Chrome и Android SDK/emulator.
- npm-модули `three` и `playwright-core` только для локального генератора в `tools/math-prop/`.

## Risks

- Browser WebGL/headless может отрисовать прозрачность или `ShadowMaterial` иначе ожидаемого; проверить alpha-канал исходного PNG и скриншот PNG внутри карты.
- Наклон камеры может визуально исказить отношение сторон и перспективу карты; проверить реалистичный 30° силуэт и объём на финальном приложении, затем скорректировать сцену и повторно сгенерировать PNG.
- Мелкий рельеф формулы может потеряться при Compose масштабе около 128dp; проверять читаемость именно в screenshot приложения и итеративно увеличивать читаемые элементы в 3D-сцене.

## Result

Создан `tools/math-prop/` — минимальная HTML/JS-сцена на Three.js 0.186.0 с исходником `scene.js`, шрифтом для `TextGeometry`, локальным HTTP-рендерером `render.mjs` и повторяемыми `npm ci && npm run render`. `playwright-core` 1.63.0 запускает установленный Google Chrome; отдельный Chromium/Blender не устанавливались. Треугольник строится через `Shape` → `ExtrudeGeometry` (height 1, depth 0.15, bevelSize/bevelThickness 0.02, bevelSegments 4), с объёмными передней и боковой гранями. Компактный скруглённый постамент и рельефные `sin 30° =`/`1`/черта/`2` сделаны Three.js-геометрией и освещёнными `MeshStandardMaterial`. Орфографическая камера, верхне-левый свет, тени от `ShadowMaterial` и прозрачный фон сохранены в исходнике.

PNG `app/src/main/res/drawable-nodpi/sin30_sculpture.png` — прозрачный RGBA `768×768`. В `CourseApp.kt` объект заменён обычным Compose `Image` в существующем прокручиваемом grid-слое; нижний центр совмещён с anchor по alpha-bounds рендера. В APK Three.js и WebView не добавлялись. Эмулятор `1080×2400`, `420 dpi`: APK собран и установлен; визуально проверены карта при начальном масштабе, карта после свайпа, открытие доступного урока по нажатию и возврат. Снимки: `/tmp/guarani-sin30-three-map-final.png`, `/tmp/guarani-sin30-three-map-scroll.png`, `/tmp/guarani-sin30-lesson-open.png`.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `tools/math-prop/index.html`, `scene.js`, `render.mjs`, `package-lock.json`, `README.md`; `npm run render` повторно создал PNG в установленном Chrome через Playwright. | passed |
| AC-2 | `scene.js`: Shape с отношением катетов `tan(30°)` и `1`, `ExtrudeGeometry(depth=0.15, bevelSize=0.02, bevelThickness=0.02, bevelSegments=4)`, отдельные светлая передняя и синяя боковая поверхности; скруглённый постамент. | passed |
| AC-3 | `scene.js`: рельефный `TextGeometry` для `sin 30° =` и независимых числителя/черты/знаменателя; `ShadowMaterial` на прозрачном полу. `sips`: `768×768`, PNG, alpha=yes. | passed |
| AC-4 | `scene.js`: `OrthographicCamera` под общим наклонным изометрическим ракурсом, `MeshStandardMaterial(roughness=0.35, metalness=0)`, верхне-левый key light, мягкая контактная тень. На карте призма показывает светлую переднюю и синюю боковую грани. | passed |
| AC-5 | `CourseApp.kt`: `painterResource(R.drawable.sin30_sculpture)` через Compose `Image`, в существующем `verticalScroll`/grid ordering; нижний центр поставлен по alpha-bounds. Первый экран `/tmp/guarani-sin30-three-map-final.png`, после прокрутки `/tmp/guarani-sin30-three-map-scroll.png`; соседние уроки и подписи не перекрываются. Нажатие по доступному узлу открывает урок (`/tmp/guarani-sin30-lesson-open.png`). | passed |
| AC-6 | `JAVA_HOME=... ANDROID_HOME=... ./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`; `adb install -r .../app-debug.apk` — `Success`. На эмуляторе проверены масштаб, читаемость формулы и контакт с полем. | passed |

REVIEW/debt: визуальный проп и его контакт с картой проверены в портретном viewport; вне scope долгов не обнаружено. Git status/commit недоступен: каталог проекта не является Git checkout.
