# WI-029: Полный и копируемый репозиторий Tume-Arandu

Canon action: none

## Outcome

Репозиторий Tume-Arandu содержит весь Android-проект GuaraniMath, понятные инструкции по сборке и разрешение на копирование и слияние изменений.

## Scope

- In: перенести исходники, ресурсы, Gradle wrapper, проектные документы и инструменты; описать чистую сборку; добавить лицензию проекта и уведомление для стороннего шрифта; подготовить изменения к слиянию в `main`.
- Out: изменение приложения, публикация APK/релиза, CI и новые тестовые наборы.

## Specs

- Governing / constraint: `spec://modules/android/PROP-010-android-demo-architecture#root`, `spec://modules/learning/FEAT-010-learning-demo#root`.

## Acceptance

- AC-1: целевой репозиторий содержит полный исходный Android-проект и относящиеся к нему документы/инструменты, но не локальные кэши, `node_modules`, пользовательские настройки и резервные `.blend1`.
- AC-2: README описывает требования и команды для сборки Android-приложения и необязательного рендера инструмента.
- AC-3: репозиторий содержит MIT-лицензию для проекта и сохраняет атрибуцию и лицензионный текст стороннего шрифта.
- AC-4: на чистом клоне проходит `./gradlew :app:assembleDebug`.
- AC-5: изменения отправлены отдельной веткой и доступны в PR для слияния в `main`.

## Dependencies

Публичный репозиторий `https://github.com/CachoCodes/Tume-Arandu` и доступ для отправки ветки.

## Risks

Исходный каталог не содержит Git-метаданных; сравнение велось с файловым снимком и единственным README в целевом `main`. Проектный copyright указан как Tume-Arandu contributors; внешний файл шрифта сохраняет MIT-атрибуцию Three.js.

## Result

Done — 2026-09-25. Полный проект отправлен в ветку `import/guarani-math-project` и PR [#1](https://github.com/CachoCodes/Tume-Arandu/pull/1) для слияния в `main`. Исходный импорт зафиксирован коммитом `de56bc6`; после финальной отправки рабочее дерево чистое. REVIEW: текст курса на Guaraní остаётся черновым и требует проверки преподавателем-носителем, это отражено в README. Нового техдолга нет.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Ветка содержит полный Android source, курс, Gradle wrapper, спецификации и инструменты; в индексе нет `.gradle`, `build`, `node_modules`, `.blend1`, `local.properties`, `specs/.me` или `.spec-drive`. | passed |
| AC-2 | README описывает требования, `./gradlew :app:assembleDebug`, Android Studio, необязательный renderer и путь для контрибьюции. | passed |
| AC-3 | В корне добавлены `LICENSE` (MIT) и `THIRD-PARTY-NOTICES` с MIT-текстом Three.js для файла шрифта. | passed |
| AC-4 | `./gradlew :app:assembleDebug` в импортированном checkout — `BUILD SUCCESSFUL` (JDK 17, SDK Platform 35, Build Tools 36.0.0). | passed |
| AC-5 | PR https://github.com/CachoCodes/Tume-Arandu/pull/1 открыт из `import/guarani-math-project` в `main`. | passed |
