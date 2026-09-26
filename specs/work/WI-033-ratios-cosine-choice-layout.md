# WI-033: Компоновка первого multiple choice по тригонометрическим отношениям

Canon action: direct-edit

## Outcome

На `ratios-cosine` увеличенная справочная схема и три узнаваемых варианта дробей видны одновременно с нижним действием проверки.

## Scope

- In: только первый `multiple_choice` урока `ratios`, Compose-компоновка и снимок `tools/exercise-gallery-images/multiple_choice.png`.
- Out: содержание/ID/правильный ответ, другие задания и локализация курса.

## Specs

- Governing / affected: `spec://modules/learning/FEAT-010-learning-demo#exercises`.

## Acceptance

- AC-1: На экране `ratios-cosine` схема треугольника увеличена; ответы остаются доступны в трёх центральных карточках над нижней кнопкой проверки.
- AC-2: `3/4`, `4/5`, `5/4` набраны вертикальными дробями без радиокружков; выбор и существующая проверка продолжают работать.
- AC-3: Изменение ограничено `ratios-cosine` и сохраняет текущую синюю палитру приложения.
- AC-4: APK собирается и обновляется без очистки приложения; скриншот gallery показывает `ratios-cosine` на ES; исходный DataStore восстановлен byte-for-byte и SHA-256 совпадает.

## Dependencies

`CourseApp.kt`, Gradle, Android SDK/emulator, `tools/exercise-gallery-images/multiple_choice.png`.

## Risks

Временный snapshot меняет видимый упражненческий экран; перед ним требуется сохранить исходный DataStore файл и hash, после capture — восстановить его и проверить совпадение hash.

## Result

Не выполнено.

| Criterion | Check / evidence | Result |
|---|---|---|
