# WI-061: Объединить PR 3 с итоговым Android-курсом

Canon action: none

## Outcome

PR #3 с восемью прежними уроками входит в `main`; его уроки и новые книжечки работают в одном Android-приложении.

## Scope

- In: разрешение конфликта PR #3 с `main`, сохранение восьми уроков и их ID/упражнений, проверка структуры курса, мердж и сборка объединённого APK.
- Out: публикация в магазине и изменение чужой учебной математики без найденной ошибки.

## Specs

- Governing: `spec://modules/learning/PROP-011-course-json-format#package`, `spec://modules/learning/PROP-011-course-json-format#validation`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#progress`, `spec://modules/android/PROP-010-android-demo-architecture#implementation-map`.

## Acceptance

- AC-1: восемь lesson ID и 48 exercise ID из PR #3 присутствуют в объединённом курсе без переименования; формат отвечает текущему валидатору.
- AC-2: конфликт PR разрешён и GitHub показывает PR #3 как merged в `main`.
- AC-3: объединённый APK собран и установлен; начало и поздний урок из PR доступны через карту; прежний DataStore не очищен.

## Dependencies

WI-059 задаёт итоговый порядок 13 тем; WI-060 создаёт компактный APK. PR: https://github.com/CachoCodes/Tume-Arandu/pull/3.

## Risks

PR #3 сейчас конфликтует с `main` в JSON курса и тесте; в PR нет `source` и части обязательных рисунков нового контракта, что надо дополнить при интеграции. Рабочий каталог `/Users/maks/work/projects/guarani` не имеет `.git`; Git checkout есть в соседнем `/Users/maks/work/projects/guarani-pr`.

## Result

В работе.
