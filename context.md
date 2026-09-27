# PROJECT_CONTEXT — Android Math Learning Demo with Guaraní + AI

## 1. Что мы строим

Нужно создать демонстрационное Android-приложение для хакатона.

Концепция:

> Duolingo/Chess.com-подобное приложение для изучения математики на Guaraní, с интерактивными уроками, прогрессом и AI-репетитором, который умеет работать как онлайн, так и локально без интернета.

## Правило порядка уроков (2026-09-26)

Основные уроки 1, 2 и далее дают новые знания в порядке плана `Plan_de_lecciones_Trigonometrias_Guarani_Jopara.docx`. После каждого основного урока идут 2–3 отдельных урока повторения: они закрепляют уже данный материал новыми вопросами и ситуациями, а не дословно повторяют исходные задания. Первые три темы плана: прямоугольный треугольник и его стороны; треугольники в жизни; известные и искомые стороны при измерении.

Это **демо/MVP**, а не production-продукт.

Главные приоритеты:

1. приложение реально запускается на Android;
2. выглядит как законченный современный продукт;
3. уроки реально работают;
4. прогресс реально работает;
5. online AI реально работает;
6. offline AI желательно реально работает;
7. весь основной UX должен быть на Guaraní;
8. проект должен быть достаточно простым, чтобы собрать рабочую версию за 1–2 дня.

Не нужно строить сложную инфраструктуру.

---

# 2. Платформа

Основная платформа:

* Android;
* основной тестовый девайс: Samsung Galaxy S24;
* результат должен собираться в APK.

iOS сейчас не требуется.

Но архитектура не должна намеренно делать будущий iOS-клиент невозможным. Это вторичный приоритет.

---

# 3. Предпочтительный стек

По умолчанию использовать:

* Kotlin;
* Jetpack Compose;
* Material 3;
* Navigation Compose;
* Kotlin Coroutines;
* локальное хранение состояния через DataStore или другой максимально простой Android-native механизм.

Не добавлять сложную архитектуру ради архитектуры.

Не нужны:

* микросервисы;
* Firebase;
* аккаунты;
* авторизация;
* полноценный backend;
* cloud database;
* синхронизация между устройствами.

Если есть веская техническая причина изменить стек — сначала описать ее в спецификации.

---

# 4. Главный UX

Приложение должно ощущаться как полноценное небольшое learning-приложение.

Основная структура:

```text
Home / Course Map
        ↓
Lesson
        ↓
Exercises
        ↓
Lesson Completed
        ↓
Progress / next lesson
```

Параллельно пользователь может открыть:

```text
AI Tutor
```

---

# 5. Course Map

Главный экран должен показывать карту курса.

Визуальная идея:

* вертикальный путь;
* движение снизу вверх;
* lesson nodes расположены зигзагом;
* визуально можно ориентироваться на learning path из Duolingo / Chess.com, но НЕ копировать их дизайн один в один.

Уроки должны иметь состояния:

```text
LOCKED
AVAILABLE
IN_PROGRESS
COMPLETED
```

На карте должны визуально отличаться:

* завершенные уроки;
* текущий урок;
* заблокированные будущие уроки.

Также показать:

* общий mastery/progress;
* streak, если его реализация простая;
* небольшое количество XP/очков или аналогичного показателя прогресса.

XP НЕ тратятся на AI.

---

# 6. Контент

Для demo достаточно одного курса:

```text
Trigonometry
```

Примерно:

```text
5–10 lessons
```

Если останется время, можно добавить второй небольшой курс, но это НЕ приоритет.

Весь образовательный интерфейс и основная теория должны быть на Guaraní.

Контент может быть статическим и храниться внутри приложения.

Например:

```text
assets/
  courses/
    trigonometry.json
```

Не нужно делать CMS или генератор курсов.

---

# 7. Структура урока

Каждый урок должен состоять примерно из:

```text
Lesson
├── title
├── short theory blocks
├── examples
├── exercises
└── completion state
```

Урок должен ощущаться интерактивным, а не просто страницей текста.

---

# 8. Типы упражнений

Нужно поддержать несколько типов.

Минимум:

### Multiple Choice

Пользователь выбирает один правильный вариант.

### Numeric / Formula Answer

Пользователь вводит число или простую формулу.

Не требуется полноценная symbolic math engine.

Можно использовать заранее заданные допустимые варианты ответа.

Например:

```json
{
  "acceptedAnswers": [
    "0.5",
    "1/2",
    "0,5"
  ]
}
```

### Matching

Например:

```text
sin(30°) → 1/2
cos(60°) → 1/2
tan(45°) → 1
```

### Step-by-step exercise

Некоторые упражнения должны иметь несколько последовательных шагов.

---

# 9. Проверка ответов

AI НЕ должен использоваться для основной проверки заданий.

Проверка должна быть deterministic.

Например:

```text
user answer
      ↓
local validation
      ↓
correct / incorrect
```

Если ответ неправильный:

* показать, что он неправильный;
* разрешить попробовать снова;
* при необходимости показать объяснение.

Если правильный:

* показать положительное состояние;
* перейти дальше.

---

# 10. Full Solution / Hint

Это одна из важных функций.

У упражнений должна быть кнопка типа:

```text
Hint
```

или:

```text
Show solution
```

При открытии должна показываться нормальная пошаговая логика решения.

Пример:

```text
sin(30°) = ?

1. Рассматриваем стандартный угол 30°.
2. Для треугольника 30°–60°–90° отношения сторон известны.
3. Применяем определение синуса к углу 30°.
4. Поэтому sin(30°) = 1/2.
```

В финальном приложении текст должен быть на Guaraní.

Решения желательно хранить заранее вместе с exercise data.

Не генерировать их каждый раз через AI.

---

# 11. Progress

Прогресс хранится полностью локально.

Минимум сохранять:

```text
completed lessons
current lesson
exercise completion
mastery
XP / points
last activity date
streak
```

Mastery можно вычислять простой формулой.

Например:

```text
correct answers / total answers
```

Не требуется сложная learning science модель.

---

# 12. Achievements

Не являются обязательными.

Если остаётся время, добавить 3–4 простых achievement.

Например:

```text
First Lesson
Perfect Lesson
3 Lessons Completed
Course Completed
```

Не тратить на эту функцию много времени.

---

# 13. AI Tutor

В приложении должен быть отдельный AI Tutor / AI Chat.

Его задача:

* объяснять математические понятия;
* отвечать на вопросы по математике;
* помогать разобраться в уроке;
* при необходимости давать подсказки;
* отвечать преимущественно на Guaraní.

AI Tutor НЕ является основным механизмом проверки упражнений.

---

# 14. Языковое поведение AI

Основной язык:

```text
Guaraní
```

Правило:

```text
если пользователь пишет на Guaraní
→ отвечать на Guaraní

если пользователь явно пишет на другом языке
→ можно отвечать на языке пользователя
```

Guaraní является критически важной частью demo.

---

# 15. Online AI

Для online AI планируется использовать:

```text
Luna 6
```

Это модель, которую команда хочет использовать в demo.

ВАЖНО:

* не придумывать API endpoint;
* не придумывать model ID;
* перед интеграцией проверить актуальную документацию провайдера;
* название модели и API-конфигурацию сделать заменяемыми.

Пример:

```text
ONLINE_AI_MODEL=<configured Luna 6 model id>
```

AI integration должна быть изолирована за интерфейсом примерно такого типа:

```kotlin
interface TutorService {
    suspend fun sendMessage(
        message: String,
        context: TutorContext
    ): TutorResponse
}
```

И дальше:

```text
OnlineTutorService
OfflineTutorService
```

---

# 16. API key

Для hackathon demo API key может подаваться через локальную конфигурацию разработчика.

Ключ:

* НЕ коммитить в Git;
* НЕ хранить прямо строкой в исходниках;
* использовать local properties / environment configuration.

Production-вариант позже должен использовать backend proxy.

Сейчас production infrastructure строить не нужно.

---

# 17. Offline AI

Это экспериментальная, но важная часть demo.

Требование:

> После загрузки локальной модели AI Tutor должен иметь возможность дать хотя бы базовый ответ без подключения к интернету.

Основной target:

```text
Samsung Galaxy S24
```

Нужна максимально маленькая on-device LLM.

Стартовый кандидат:

```text
Gemma 3 1B или другая сопоставимая маленькая instruct-модель
```

Но агент должен проверить:

* Android compatibility;
* RAM usage;
* model size;
* inference speed;
* доступный Android inference runtime;
* реальную возможность запуска на Samsung S24.

Не выбирать модель только потому, что она популярная.

---

# 18. Offline AI не обязан быть таким же хорошим, как Online AI

Offline режим может быть ограниченным.

Главное, чтобы он мог показать:

```text
internet disabled
        ↓
local AI
        ↓
basic math explanation
```

Это demo capability.

Можно явно отображать в UI:

```text
Online AI
```

и:

```text
Offline AI
```

---

# 19. AI Mode

Предпочтительный UX:

```text
AI Mode:

Auto
Online
Offline
```

Поведение Auto:

```text
internet available
→ online Luna 6

internet unavailable
→ local model
```

Пользователь также может вручную переключить режим.

---

# 20. Offline Guaraní

Это потенциально самая сложная часть проекта.

Маленькая локальная модель может плохо знать Guaraní.

Поэтому разрешается использовать небольшой локальный context pack.

Например:

```text
assets/
  ai_context/
    guarani_math_terms.json
    trigonometry_context_guarani.txt
    common_explanations_guarani.json
```

Перед запросом локальной модели можно добавлять релевантный Guaraní-контекст.

Например:

```text
SYSTEM:
You are a mathematics tutor.

Use primarily Guaraní.

Here are relevant Guaraní mathematical terms:
...

Current lesson:
Trigonometric ratios.

Relevant educational context:
...
```

Это не полноценный RAG.

Для demo достаточно простого context injection.

---

# 21. Model download

Большую локальную модель НЕ обязательно включать внутрь APK.

Предпочтительно:

```text
install APK
    ↓
first launch
    ↓
Download offline AI model
    ↓
model stored locally
```

Для hackathon demo модель можно заранее скачать на устройство.

UI должен показывать:

```text
Offline AI model installed
```

или:

```text
Download offline AI
```

---

# 22. UI / Design Direction

Приложение должно выглядеть современно и визуально законченно.

Не делать generic developer UI.

Нужны:

* хороший spacing;
* крупная типографика;
* приятные карточки;
* хороший progress visualization;
* анимации переходов там, где они дешёвые;
* хороший visual feedback после ответа;
* выразительный Course Map.

Можно вдохновляться:

* Duolingo;
* Chess.com learning;
* современными mobile EdTech apps.

Но не делать pixel-perfect копию.

Не копировать:

* персонажей;
* иллюстрации;
* branding;
* UI assets;
* тексты Duolingo.

Математический контент для demo лучше создать самостоятельно.

---

# 23. Основные экраны

Минимальный список:

```text
1. Course Map
2. Lesson Intro / Theory
3. Exercise
4. Lesson Completed
5. AI Tutor
```

Опционально:

```text
6. Simple Progress/Profile screen
```

Не добавлять лишние экраны.

---

# 24. Lesson Completion

После завершения урока показать красивый completion screen.

Например:

```text
Lesson Complete

Mastery: 85%
Correct: 8 / 10
+100 XP

Continue
```

После Continue пользователь возвращается на Course Map, где следующий lesson становится доступным.

---

# 25. Data model

Примерное направление:

```kotlin
data class Course(
    val id: String,
    val title: String,
    val lessons: List<Lesson>
)

data class Lesson(
    val id: String,
    val title: String,
    val theory: List<TheoryBlock>,
    val exercises: List<Exercise>
)

sealed interface Exercise

data class MultipleChoiceExercise(...)
data class InputExercise(...)
data class MatchingExercise(...)
data class StepExercise(...)
```

Это только направление.

Агент должен сам определить финальную модель в technical spec.

---

# 26. Пример структуры проекта

Ориентировочно:

```text
app/
├── data/
│   ├── course/
│   └── progress/
│
├── domain/
│   ├── Course.kt
│   ├── Lesson.kt
│   └── Exercise.kt
│
├── ai/
│   ├── TutorService.kt
│   ├── OnlineTutorService.kt
│   ├── OfflineTutorService.kt
│   └── TutorRouter.kt
│
├── ui/
│   ├── coursemap/
│   ├── lesson/
│   ├── exercise/
│   ├── completion/
│   └── tutor/
│
└── assets/
    ├── courses/
    └── ai_context/
```

Не считать эту структуру обязательной, если существует более простой хороший вариант.

---

# 27. Что НЕ нужно делать

Не делать сейчас:

```text
authentication
accounts
payments
subscriptions
cloud sync
social features
leaderboards
admin panel
CMS
complex backend
microservices
iOS app
web app
full symbolic math engine
AI-generated course system
advanced analytics
production monitoring
```

Это hackathon demo.

---

# 28. Приоритеты

Если приходится выбирать между функциями, использовать такой порядок:

```text
P0
- Android APK реально запускается
- Course Map
- минимум 5 уроков
- теория
- упражнения
- правильная проверка ответов
- Show Solution
- local progress
- хороший UI

P1
- Online Luna 6 AI Tutor
- Guaraní AI responses
- Offline AI proof-of-concept
- Auto online/offline switching

P2
- streak
- achievements
- extra animations
- второй курс
- дополнительные exercise types
```

Не жертвовать P0 ради P2.

---

# 29. Главное ограничение

Срок:

```text
1–2 дня
```

Поэтому выбирать самое простое решение, которое:

```text
works
looks good
can be demonstrated
```

Не overengineer.

---

# 30. Что нужно сделать агенту ПЕРЕД написанием основной реализации

Не начинать хаотично писать код.

Сначала на основании этого документа создать внутри проекта короткую техническую спецификацию.

Например:

```text
docs/
  PRODUCT_SPEC.md
  TECH_SPEC.md
  IMPLEMENTATION_PLAN.md
```

## PRODUCT_SPEC.md

Зафиксировать:

* пользовательский flow;
* экраны;
* функциональность каждого экрана;
* exercise types;
* progress mechanics;
* AI behaviour;
* offline behaviour;
* acceptance criteria.

## TECH_SPEC.md

Зафиксировать:

* stack;
* application architecture;
* navigation;
* data models;
* local persistence;
* course JSON schema;
* AI abstraction;
* Luna 6 integration;
* offline inference runtime;
* offline model;
* network detection;
* model download/storage;
* error handling.

Особенно внимательно проверить реальную документацию для:

```text
Luna 6
offline Android inference
chosen local model
```

Не придумывать отсутствующие API.

## IMPLEMENTATION_PLAN.md

Разбить работу на небольшие вертикальные этапы.

Предпочтительно:

```text
1. Project skeleton
2. Design system
3. Static course data
4. Course Map
5. Lesson engine
6. Exercise validation
7. Progress persistence
8. Completion UX
9. Online AI
10. Offline AI
11. Polish
12. APK verification
```

После составления этих документов переходить к реализации.

---

# 31. Definition of Done для demo

Demo считается успешным, если можно выполнить такой сценарий:

```text
1. Установить APK на Samsung S24.

2. Открыть приложение.

3. Увидеть красивую карту курса Trigonometry.

4. Открыть первый урок.

5. Прочитать теорию на Guaraní.

6. Выполнить несколько разных типов заданий.

7. Получить correct / incorrect feedback.

8. Нажать Show Solution и увидеть пошаговое решение.

9. Завершить урок.

10. Увидеть mastery / progress.

11. Вернуться на карту.

12. Увидеть открывшийся следующий lesson.

13. Открыть AI Tutor.

14. Задать математический вопрос на Guaraní.

15. Получить ответ через Luna 6.

16. Переключиться в offline mode.

17. Без интернета получить базовый ответ от локальной модели.
```

Если пункты 1–15 работают хорошо, а offline AI имеет ограниченное качество, demo всё равно может считаться пригодным.

Главное для offline AI — доказать реальный on-device inference.

---

# 32. Принцип принятия решений

Когда есть несколько технических вариантов, выбирать вариант с минимальным:

```text
implementation time
number of dependencies
number of moving parts
risk of demo failure
```

При этом UI и основные пользовательские flows должны выглядеть как настоящий продукт, а не технический prototype.

Сначала спецификация.

Потом реализация.
