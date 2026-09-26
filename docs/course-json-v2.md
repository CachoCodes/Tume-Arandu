# Формат курса v2

Встроенный курс хранится в одном файле [`trigonometry.json`](../app/src/main/assets/courses/trigonometry.json). Новый урок добавляется в массив `lessons` этого файла; текущая карта поддерживает 1–10 уроков. Android выбирает текст по `gn-PY` или `es`; ID и правильные ответы общие для обоих языков.

```json
{
  "schemaVersion": 2,
  "id": "trigonometry-guarani",
  "revision": 1,
  "defaultLocale": "gn-PY",
  "title": {"gn-PY": "Trigonometría", "es": "Trigonometría"},
  "lessons": [{
    "id": "example-lesson",
    "title": {"gn-PY": "Ko mbo'epy", "es": "Esta lección"},
    "objective": {"gn-PY": "Jejapo", "es": "Objetivo"},
    "theory": [{"type": "text", "body": {"gn-PY": "Ñemyesakã", "es": "Explicación"}}],
    "xpReward": 100,
    "exercises": [{
      "id": "example-choice",
      "type": "multiple_choice",
      "visual": "standard_choice",
      "prompt": {"gn-PY": "Mboy?", "es": "¿Cuánto?"},
      "hint": {"gn-PY": "Ejesareko", "es": "Observa"},
      "solutionSteps": [{"gn-PY": "Peteĩ", "es": "Uno"}],
      "options": [
        {"id": "one", "text": {"gn-PY": "1", "es": "1"}},
        {"id": "two", "text": {"gn-PY": "2", "es": "2"}}
      ],
      "correctOptionId": "one"
    }]
  }]
}
```

Допустимые сочетания:

| `type` | `visual` | Поля ответа |
|---|---|---|
| `multiple_choice` | `standard_choice`, `triangle_choice`, `angle_builder` | `options`, `correctOptionId` |
| `input` | `standard_number`, `standard_fraction`, `fraction_triangle`, `straight_angle` | `acceptedAnswers` |
| `matching` | `standard_pairs`, `ratio_pairs`, `angle_pairs` | `leftItems`, `rightItems`, `pairs` |
| `step_by_step` | `standard_steps`, `radical_fraction` | `steps` |

Для `angle_pairs` каждый правый элемент содержит `diagram`: `acute`, `right` или `straight`. `ratio_pairs` использует математические имена `sin α`, `cos α`, `tan α`; правая карточка хранит числитель и знаменатель одной строкой с `/` и показывает их вертикально. У `step_by_step` каждый шаг содержит локализованные `prompt` и `explanation`, а также `acceptedAnswers`.

Сохраняйте ID существующих уроков и упражнений при изменении оформления или перевода: к ним привязан локальный прогресс. Если меняется смысл задания или правильный ответ, дайте упражнению новый ID. Поле `revision` увеличивайте при редакции пакета. Перед PR выполните:

```sh
python3 tools/validate-course.py app/src/main/assets/courses/trigonometry.json
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:assembleDebug
```

Валидатор проверяет структуру, локализации и ссылки между ответами. Математическую правильность и качество перевода проверяют автор и преподаватель. Формат пока предназначен для встроенного курса; импорт файлов в приложении не реализован.

Для задач по тригонометрии добавляйте схему текущего задания, например:

```json
"triangle": {"angleDegrees": 30, "angleLabel": "30°", "base": "√3", "opposite": "1", "hypotenuse": "2"}
```

Схема показывается над ответом. Искомую сторону обозначайте `?`, не подставляйте рисунок 3–4–5 в задачу о других сторонах. Поле поддерживается всеми типами; для matching с абстрактными формулами и упражнений с лучами оно не требуется.
