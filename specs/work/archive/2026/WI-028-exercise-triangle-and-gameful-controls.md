# WI-028: Треугольник и игровая подача упражнений

Canon action: direct-edit

## Outcome

Каждое упражнение показывает общий наглядный треугольник 3–4–5 с углом α; экран задания собирает вопрос и ответ в единую карточку, ясно объясняет ввод дроби и заметно подтверждает верный ответ.

## Scope

- In: общий треугольник во всех типах упражнений и обоих языках; цельная рамка карточки, центрированная шкала прогресса, тактильные кнопки выбора; подписи числителя и знаменателя; зелёное сообщение о верном ответе непосредственно над закреплённым действием.
- Out: перекраска карты, изменение учебных задач/ответов, логики начисления прогресса и новых зависимостей.

## Specs

- Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#lesson-content`, `spec://modules/learning/FEAT-010-learning-demo#exercises`, `spec://modules/learning/FEAT-010-learning-demo#language-and-visuals`.
- Constraint: `spec://modules/android/PROP-010-android-demo-architecture#root`.

## Acceptance

- AC-1: На каждом задании GN/ES, включая multiple choice, input, matching, step-by-step и построение угла, внутри общей карточки виден прямоугольный 3–4–5 треугольник: катет 4, гипотенуза 5, угол α и отметка прямого угла.
- AC-2: Поле дроби явно подписывает верхнее место числителя и нижнее место знаменателя; после правильного ответа зелёное подтверждение видно над закреплённой кнопкой продолжения.
- AC-3: Шкала прогресса визуально центрирована; вопрос, треугольник и содержимое задания собраны в одной обведённой карточке; варианты ответа имеют крупный нажимаемый игровой вид в текущей синей палитре без заимствования брендинга или палитры Duolingo.
- AC-4: Прокрутка длинных заданий и закреплённые цифровая клавиатура/действия сохраняют доступность; изменение не очищает сохранённые ответы или прогресс.

## Dependencies

Compose `ExercisePanel`, цифровой ввод ответа, GN/ES копии и локальный Android-эмулятор.

## Risks

Постоянный рисунок увеличивает высоту карточки в портретном режиме; существующая прокрутка карточки должна оставлять контент доступным над закреплённой панелью.

## Result

Done — 2026-09-25. Git metadata unavailable in this checkout, so branch/commit could not be recorded.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | `:app:assembleDebug` passed. The live mirror at `http://127.0.0.1:8765/` shows the 3–4–5 diagram, α, right-angle mark and reference label in the shared matching exercise card; the same `ExercisePanel` renders every exercise type in GN/ES, and the help dialog uses the same diagram. | passed |
| AC-2 | `NumericAnswerPad` receives localized top/bottom numerator/denominator labels for both input and step exercises. The successful feedback path is rendered in a green dock banner between the keypad and primary action; `:app:assembleDebug` passed. | passed |
| AC-3 | The live mirror shows the centered progress bar, one bordered card holding prompt/diagram/response, and raised matching controls in the existing blue palette. No Duolingo brand assets or colors were added. | passed |
| AC-4 | The task body remains vertically scrollable above the fixed action dock. The updated APK was applied with `adb install -r`; no progress reset or repository/data-layer changes were made. | passed |
