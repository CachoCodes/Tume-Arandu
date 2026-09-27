# Правила для ИИ: как делать новый урок

Документ для ИИ-агентов (Claude, Codex и других), которые пишут или меняют уроки GuaraniMath. Обязательный канон — [PROP-011](../specs/modules/learning/PROP-011-course-json-format.md) и раздел упражнений [FEAT-010](../specs/modules/learning/FEAT-010-learning-demo.md#exercises). При расхождении прав канон; этот файл — порядок работы.

## Главный принцип: шаблон, а не новый дизайн

Все виды заданий уже согласованы и лежат в каталоге шаблонов [`app/src/main/assets/templates/exercise-templates.json`](../app/src/main/assets/templates/exercise-templates.json) — по одному заданию `tpl-<visual>` на каждый вид. Новое задание = копия шаблона нужного вида, в которой изменено только содержание. Дизайн, поля вида и код экранов не меняются.

Можно менять: `id`, `prompt`, `hint`, `solutionSteps`, варианты (`options`, `leftItems`, `rightItems`) и ответы (`correctOptionId`, `acceptedAnswers`, `pairs`, `steps`), данные `triangle` (угол и стороны задачи).
Нельзя менять: `type`, `visual`, набор полей шаблона, Kotlin/Compose, цвета и расположение.

## Жёсткие правила

1. **Только JSON.** Урок добавляется в `app/src/main/assets/courses/trigonometry.json` по формату v2 ([пример](course-json-v2.md)).
2. **Происхождение в каждом уроке.** У каждого урока ровно такой блок:
   ```json
   "source": {"madeWith": "Dulce Duro", "basedOn": "MEC Paraguay"}
   ```
   Урок готовится с Dulce Duro и основан на программе по математике MEC (Ministerio de Educación y Ciencias, Парагвай). Тема и уровень — по этой программе. Не выдумывайте названия разделов программы MEC; нужна ссылка на конкретный пункт — спросите владельца.
3. **Рисунок у каждого задания, кроме сопоставления.** Экран только из текста запрещён. У `standard_*` обязательно поле `triangle` со сторонами именно этой задачи. Сопоставление (`*_pairs`) — карточки без треугольника, ровно три в каждом столбце; `triangle` у него запрещён.
4. **Только виды из каталога.** Новый визуальный вид в JSON не придумывается. Его путь: браузерный прототип в `tools/` → согласование владельцем → спека → валидатор и загрузчик → Compose → новый шаблон в каталоге ([PROP-011#new-visuals](../specs/modules/learning/PROP-011-course-json-format.md)). Если ни один вид не подходит — остановитесь и предложите прототип.
5. **Два языка.** Каждая видимая строка — `{"gn-PY": "...", "es": "..."}`, оба непустые. Guaraní — основной язык; обозначения стандартные (`sin α`, `cos α`, `tan α`, `√`, дроби). Английских подписей нет.
6. **ID не трогать.** Существующие ID уроков и заданий не меняются при правке текста или оформления — к ним привязан прогресс. Новый смысл или новый правильный ответ — новый ID.
7. **Задание однозначно.** Ответ выводится из условия и рисунка; неверные варианты правдоподобны, но точно неверны; `hint` подталкивает, не выдавая ответ; `solutionSteps` ведут к ответу. Для формулировок используйте навыки `lesson-tasks` и `ux-writing`, если они доступны.

## Каталог видов

| Шаблон | `type` | `visual` | Как выглядит | Когда брать |
|---|---|---|---|---|
| `tpl-triangle-choice` | `multiple_choice` | `triangle_choice` | треугольник 3–4–5 с α, варианты-дроби вертикально в карточках снизу | выбрать отношение сторон |
| `tpl-angle-builder` | `multiple_choice` | `angle_builder` | луч на ползунке, градусы по центру | построить/выбрать угол |
| `tpl-standard-choice` | `multiple_choice` | `standard_choice` | схема `triangle` задачи, варианты в карточках снизу | выбрать ответ по треугольнику |
| `tpl-fraction-triangle` | `input` | `fraction_triangle` | крупный треугольник 3–4–5, поля числителя и знаменателя, цифровая клавиатура | вписать дробь-отношение |
| `tpl-straight-angle` | `input` | `straight_angle` | прямая линия (180°), поле числа | число о развёрнутом угле |
| `tpl-standard-number` | `input` | `standard_number` | схема `triangle`, поле числа, клавиатура | найти сторону или число |
| `tpl-standard-fraction` | `input` | `standard_fraction` | схема `triangle`, поля дроби, клавиатура | ответ-дробь по треугольнику |
| `tpl-ratio-pairs` | `matching` | `ratio_pairs` | карточки `sin/cos/tan α` ↔ вертикальные дроби сторон | функции ↔ отношения сторон |
| `tpl-angle-pairs` | `matching` | `angle_pairs` | карточки градусов ↔ рисунки углов (`diagram`: `acute`, `right`, `straight`) | градусы ↔ вид угла |
| `tpl-standard-pairs` | `matching` | `standard_pairs` | карточки текста/формул в два столбца | формулы, понятия, данные ↔ смысл |
| `tpl-radical-fraction` | `step_by_step` | `radical_fraction` | ввод `√числитель/знаменатель` | рационализация |
| `tpl-standard-steps` | `step_by_step` | `standard_steps` | схема `triangle`, шаги с полем ответа | задача в несколько шагов |

## Как просить ИИ (для людей)

Опишите задание словами — ИИ подберёт шаблон. Хороший запрос содержит тему, что ученик делает и данные, например:

> Добавь в урок `unknown-side` задание: ученик выбирает длину гипотенузы, если угол 30°, а противолежащий катет 4. Варианты 8, 4√3, 2.

ИИ отвечает, какой шаблон взял (здесь `tpl-standard-choice`), и показывает, что изменил. Если вы хотите конкретный вид — назовите шаблон из таблицы.

## Порядок работы ИИ

1. Прочитайте `AGENTS.md`, `specs/protocols/BOOT.md`, PROP-011 и этот файл.
2. Для каждого задания выберите шаблон из каталога и назовите его пользователю.
3. Скопируйте шаблон в урок, измените только разрешённые поля, дайте новый уникальный `id`. У урока — `source`. Увеличьте `revision` курса.
4. Проверьте:
   ```sh
   python3 tools/validate-course.py app/src/main/assets/courses/trigonometry.json
   python3 tools/test_course_validator.py
   ./gradlew :app:assembleDebug
   ```
   (JDK 17 и Android SDK: при необходимости задайте `JAVA_HOME` и `ANDROID_HOME`.)
5. Установите APK без очистки данных (`adb install -r`) и посмотрите каждое новое задание в режиме просмотра — он не трогает прогресс ученика:
   ```sh
   adb shell am start -n com.guarani.mathdemo/.MainActivity -e preview courses/trigonometry.json -e exercise <id>
   ```
   Эталон вида для сравнения: `-e preview templates/exercise-templates.json -e exercise tpl-<visual>`.
6. Откройте pull request. Математику и перевод на Guaraní проверяет человек — укажите это в описании PR.
