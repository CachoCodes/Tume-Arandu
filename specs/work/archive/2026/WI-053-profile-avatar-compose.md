# WI-053: Профиль и редактор аватара в Compose

Canon action: none

## Outcome

Новый профиль, общее нижнее меню и редактор аватара в Android-приложении соответствуют утверждённым эталонам и сохраняют образ без потери прогресса.

## Scope

- In: Compose UI профиля, редактора и нижнего меню; AvatarView; правила открытия; хранение образа; ресурсы Nunito; сборка и визуальная проверка.
- Out: уроки, поведение Tutor и карта за пределами общего меню; изменение содержимого курса.

## Specs

- Governing: `spec://modules/android/PROP-010-android-demo-architecture#navigation`, `spec://modules/android/PROP-010-android-demo-architecture#persistence`.
- Constraint: `spec://modules/learning/FEAT-010-learning-demo#course-map`, `spec://modules/learning/FEAT-010-learning-demo#progress`.
- Design: `docs/design/profile-avatar/PROMPT.md`, `docs/design/profile-avatar/source/`, `docs/design/profile-avatar/spec/`.

## Acceptance

- AC-1: Профиль и общее меню работают на карте, у Tutor и в профиле, а профиль соответствует эталонным состояниям при открытии и в конце прокрутки.
- AC-2: Редактор показывает шесть вкладок, цветовые варианты и закрытые вещи по правилам, случайный образ выбирает открытые вещи, отмена и сохранение работают.
- AC-3: Единый AvatarView соответствует SVG-эталону в профиле, меню, баннере и плитках.
- AC-4: Старый прогресс сохраняется при обновлении, новый образ переживает перезапуск приложения.
- AC-5: APK собран, установлен и сделаны сравнительные скриншоты для указанных состояний.

## Dependencies

Эмулятор Android 1080×2400 и исходники дизайна в репозитории.

## Risks

Различие CSS и Compose в рендеринге текста и SVG; проверить снимки рядом с эталонами. DataStore миграция должна сохранить все поля snapshot.

## Result

Готово 2026-09-26. Реализованы профиль, общее меню и редактор в Compose; APK установлен на эмулятор 1080×2400. В рабочей папке нет `.git`, поэтому commit и состояние working tree через Git недоступны.

REVIEW: эталонные PNG сняты в браузере без системных панелей и местами с запасным шрифтом. На Android видны status/navigation bars и отличаются метрики/сглаживание Nunito и тени; верхняя карточка профиля примерно на 19 dp выше эталонной нижней границы. В эталоне зафиксированы 100 XP, 1/9 и кепка, а проверка использует сохранённые 180 XP, 3/9 и снятую шапку. Эти расхождения перечислены в отчёте пользователю. TODO: данные серии дней, реальные id уроков для тематических наград, имя и handle пользователя.

| Criterion | Check / evidence | Result |
|---|---|---|
| AC-1 | Открыты карта, Tutor и профиль на эмуляторе 1080×2400; проверено общее меню и прокрутка до конца: `docs/design/profile-avatar/screenshots/actual-map-after-restart.png`, `actual-tutor-menu.png`, `actual-profile-after-restart.png`, `actual-profile-full-scroll.png`. Верх коллекции на первом экране совпадает с верхом меню. | passed |
| AC-2 | Вручную открыты все шесть категорий, нажата закрытая «Corona», проверены цветовые кружки и случайный образ; после «✕» черновик не сохранился. Редактор открыт и через карандаш, и через «Personalizar avatar». Скриншоты `actual-avatar-editor*.png`; правило случайного образа фильтрует вещи через `unlocked()` в `AvatarCatalog.kt`. | passed |
| AC-3 | `python3 tools/check-avatar-design-assets.py` подтвердил 109 слоёв в порядке SVG и точную копию правил; AvatarView проверен визуально в профиле, меню, баннере и плитках на снимках рядом с эталонами. | passed |
| AC-4 | До сохранения в DataStore не было ключа `avatar`: 180 XP и 3 завершённых урока. После «Guardar» сохранено `hat=none`; после `am force-stop` и повторного запуска DataStore содержит 180 XP, те же 3 id уроков и `hat=none`, профиль и меню показывают новый вид. | passed |
| AC-5 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/Users/maks/Library/Android/sdk ./gradlew :app:assembleDebug` → BUILD SUCCESSFUL; `adb install -r app/build/outputs/apk/debug/app-debug.apk` → Success; снимки в `docs/design/profile-avatar/screenshots/`; `python3 tools/spec-lint.py` → 0 errors, 0 warnings. | passed |
