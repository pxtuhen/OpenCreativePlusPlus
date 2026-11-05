# MODLOG — журнал OpenCreative++

## Версии
- Основа: OpenCreative+ `e9624de` (main, 2026-10-29), версия 6.0.0 Pre-release 9, AGPL-3.0.
- Цель: Paper `1.21.11-R0.1-SNAPSHOT`, Java Temurin 21.0.13, Gradle 9.8.2, paperweight userdev 2.0.0-beta.24, shadow 9.6.1.

## Окружение
- JDK: `~/.jdks/jdk-21.0.13+9`, Gradle-дистрибутив: `~/.jdks/gradle-9.8.2` (нужен только для генерации wrapper).
- Баг машины: `java.io.IOException: Unable to establish loopback connection` / `SocketException: Invalid argument: connect`.
  Причина — AF_UNIX-сокет в TEMP с коротким путём `ADMINI~1`. Решение: `-Djdk.net.unixdomain.tmpdir` и `-Djava.io.tmpdir`
  на нормальный путь (см. `build.sh`).

## Шаг 1. Перевод сборки на 1.21.11
- Аддоны (ProtocolLib, LibsDisguises и др.) транзитивно тянули `spigot-api 1.21.4` и `org.lz4:lz4-java` — конфликт capability
  с Paper 1.21.11. Исключены в `configurations.configureEach`.
- 92 ошибки компиляции, три причины:
  - `Attribute.GENERIC_*` → `Attribute.*` (переименование в 1.21.3), 19 файлов;
  - `Biome` стал интерфейсом — `switch` по биому в `FlatGenerator`, `AbstractFlatGenerator` переписан на `if`;
  - `EntityType.BOAT` + `Boat.Type` удалены — `SpawnVehicleAction` теперь берёт `EntityType` прямо из материала
    (OAK_BOAT, OAK_CHEST_BOAT…). Поведение то же.
- Удалено: `Main.java` (окно Swing/JavaFX), `.gitlab-ci.yml` (публикация в реестр апстрима), modrinth и maven-publish.
- Результат: `BUILD SUCCESSFUL`, `build/libs/OpenCreativePP.jar`.

## Шаг 1.1. Первый запуск на Paper 1.21.11 #132
- Тестовый сервер: `../test-server` (Paper 1.21.11 build 132, sha256 5ffef465…, offline-mode, EULA принята пользователем).
  Прогон: `../test-server/smoke.sh "команда" ...`.
- Плагин грузится и выгружается, `/oc` работает.
- Баг оригинала: `filter.yml` не парсился (`\p{L}` в двойных кавычках YAML — неизвестный escape), фильтр чата
  в OpenCreative 6.0.0 не загружался никогда. 6 регулярок переведены в одинарные кавычки. После — старт без ERROR.

## Шаг 2. Конфиг из нескольких файлов, права, логи
- `plus/config/SplitConfig`: в памяти один конфиг (весь код OpenCreative работает как раньше через getConfig()),
  на диске — config/groups/worlds/items/sounds/chat/datapacks.yml. Сохранение меняет только значения → комментарии живут.
  Переопределены getConfig/reloadConfig/saveConfig/saveDefaultConfig в OpenCreative.
- Миграция: старый одиночный config.yml OpenCreative раскладывается по файлам в saveDefaults() (значения пользователя
  + русские комментарии шаблона), перенесённые секции удаляются из config.yml. Проверено: size 200 / рекомендуемый мир 77 /
  тема christmas переехали. Папка plugins/OpenCreative копируется в plugins/OpenCreativePP при первом запуске.
- Дрейф: скрипт сравнил все ключи/значения старого config.yml с новыми файлами — отличия только задуманные.
- Плагин переименован в OpenCreativePP (data folder plugins/OpenCreativePP), `provides: [OpenCreative]`.
- Права: opencreativepp.player (все), opencreativepp.premium (группа premium + датапаки), opencreativepp.admin (OP),
  opencreativepp.chat.*/sign.*, opencreativepp.datapack. Группа premium в groups.yml теперь по праву opencreativepp.premium.
- Логи: `PlusLog` с модулями, запуск — 1 строка-сводка; баннеры и автопроверка обновлений Hangar выключены
  (logs.startup-banner, logs.check-updates). Сообщение авторов в onLoad оставлено как есть.

## Шаг 3. Цветной чат и таблички
- Оригинал уже умел цвета в чате (`ChatCommand.formatPlayerChatInput`), но по необъявленным правам opencreative.chat.color и т.д.
  → фактически только OP. Теперь `plus/chat/ColorFormatter`: права opencreativepp.chat.* (default true) ИЛИ старые
  opencreative.<chat|creative-chat>.*, плюс переключатели chat.yml → chat.messages. &k — только OP по умолчанию.
- Таблички: `plus/chat/SignColorListener` (SignChangeEvent HIGH, после фильтра оригинала). Сырой текст с кодами хранится в PDC
  таблички по стороне; при PlayerOpenSignEvent (кроме установки) строки временно показываются с кодами.
- Статус: собрано. В игре НЕ проверено (нет клиента).

## Шаг 4. Компас: 4 вкладки и «Мои лимиты»
- `plus/menus/WorldsTabsMenu` (ListBrowserMenu): вкладки внизу 46 Рекомендуемые (открывается первой) / 47 Мои / 48 Любимые /
  49 Поиск; 45/53 страницы, 50 сортировка (онлайн/лайки/новые), 51 «Мои лимиты», 52 «Создать мир» на вкладке «Мои».
  Клик: ЛКМ — войти (во «Моих» — меню доступа мира, как в оригинале), ПКМ — любимые, Shift+ЛКМ — модерация (opencreative.moderation.menus).
  Поиск — тот же ввод в чат, что у оригинала (ЛКМ имя / ПКМ ID / Shift+ЛКМ владелец), результаты — во вкладке «Поиск».
- `plus/players/FavoriteWorlds`: favorites.yml (uuid → ID миров). Не зависит от эксперимента wanders.
- `plus/menus/LimitsMenu`: группа, размер мира, этажи кодинга, миры X из Y, модули, существа/переменные/вызовы (+за игрока),
  строители/разработчики, датапаки (Premium), другие группы для сравнения. Значения — из groups.yml.
- Все входы (предмет «worlds», /games, «own-worlds», /own, кнопки «назад» старых меню) ведут в новое меню.
- Тексты: locales/*.yml → секция plus (ru, en; ua пока английским текстом — TODO перевод).
- Статус: собрано, сервер стартует, ключи локализации дописываются. Меню в игре НЕ проверено (нет клиента).

## Шаг 5. Датапаки (Premium)
- `plus/datapacks/Datapacks`: библиотека из datapacks.yml. Проверки: ID [a-z0-9_], файл/папка/.zip есть, pack.mcmeta есть
  (и не во вложенной папке архива), data/ есть, JSON валиден, pack.pack_format или min_format есть, функции есть в
  data/ns/function/ (подсказка, если лежат в старой functions/). Валидные копируются в world/datapacks/creativeplus_<id>
  и включаются через Paper DatapackManager; лишние creativeplus_* выключаются.
- Включение в мире: `/datapack` (меню) и `/datapack [id]`: владелец мира (или opencreativepp.admin), право datapacks.permission
  (opencreativepp.datapack ⊂ opencreativepp.premium), право пакета (если задано), лимит max-per-world. Хранение: datapacks-worlds.yml.
- Запуск: каждый тик для миров в режиме PLAYING: load-function при входе в PLAYING, tick-function каждый тик. Через NMS
  ServerFunctionManager.execute с getGameLoopSender() (уровень 2, без вывода) + withLevel(мир)+withPosition(спавн).
  После max-tick-errors ошибок подряд пакет выключается в мире, владелец получает сообщение.
- Проверено на сервере: 6 типов ошибок дают понятные строки (включая строку/колонку битого JSON), рабочий пакет
  виден Minecraft как file/creativeplus_good и включён, `function cptest:load` выполняется.
- НЕ проверено: запуск функций внутри мира игрока в /play (нужен клиент, чтобы создать мир).
- Не сделано: действие кодинга «Запустить функцию датапака».
- /datapack перекрывает ванильную /datapack (осталась /minecraft:datapack).

## Шаг 6. Сборка и локальный сервер
- Итоговый jar: `../out/OpenCreativePP-1.0.0.jar` (sha256 0e106e7a…fa0c), сборка воспроизводима. Тонкий jar теперь `-plain`
  (на Windows он перезаписывал shaded jar из-за регистра имени).
- Локальный сервер для ручной проверки: `../server` (Paper 1.21.11 #132, 127.0.0.1:25565, offline-mode, OP: pxtuhen),
  запуск `start.bat`. Тестовый датапак test_pack (load: say + скорборд cptest, tick: +1 в cptest).
- Замечено: «No key layers in MapLike[{}]» при создании миров — поведение оригинала (WorldType.FLAT без настроек),
  миры создаются нормально.

## Шаг 7. Исправления багов оригинала (аудит 3 агентами, ключевые перепроверены)
- Кодинг: перепутанные green/blue; COLOR-переменные терялись ((int) Long); условие/повтор/функция без целей обрывали строку;
  общий счётчик «Повторить N»; убийца/жертва из последнего события; действие после «Ждать» выполнялось после /build;
  большой скрипт грузился; исполнители регистрировались в async; «код сохраняется» навсегда после ошибки; правки кода терялись
  при выгрузке до /play; числа float→double; частицы с данными (1.21.9+); переменные — атомарная запись, пропуск битых,
  автосохранение 5 мин; словари в переменных; бледный дуб в /env.
- Миры: залипание «подключение»; dev-мир при ошибке оставлял пустой инвентарь; полёт между мирами; проверка телепорта по
  размеру не того мира; новый ранг не менял границу; выкидывание в лобби при входе во время выгрузки; сброс флагов после
  выгрузки; спавн на y=0; ошибка загрузки блокировала мир; удаление невыгруженного мира; сохранение при autosave off; иконка
  в async; координаты лобби; мигание скина; NPE в ChangedWorld; ERROR «No key layers».
- Меню: shift-клик/двойной клик/перетаскивание клали свои предметы в меню; иконка мира съедала стопку; «Кикнуть всех» кикал
  владельца; блокирующие запросы профилей; компас во второй руке; build_tools на земле после смерти; кулдаун компаса; clamp.
- Чат/язык/команды: запрос ввода не истекал (теперь 60 с + «отмена»); второй запрос не заменял первый; ввод значений в async;
  первый запуск на английском; resetlocale удалял свой язык; имя модуля в чат; ванильный синтаксис /give /tp /gamemode у админов;
  /time set night; NPE /oc dev из консоли.
- Не трогали: лимит вызовов исполнителей через глубину стека (PlanetExecutors:126), порядок при нескольких целях.

## Шаг 8. Новое
- Компас: «Продолжить» (последний мир, last-worlds.yml), фильтр категорий (ЛКМ дальше, ПКМ — все), сортировка «сейчас играют»,
  профиль, рекомендуемые ⭐ сверху + все остальные миры, подсказка во вкладке поиска, поиск по описанию.
- /profile [ник] (ProfileMenu). Панель лобби и TAB (config.yml → lobby.sidebar / lobby.tablist, LobbyDisplay).
- 5 типов плоских миров по умолчанию (worlds.yml → custom-flats). recommended-worlds по умолчанию [].
- Действие кодинга WORLD_RUN_DATAPACK_FUNCTION (только функции включённых в мире датапаков, индекс функций при загрузке).
- Уведомление владельцу о лайке. Предупреждение о @a/@e без distance= в датапаках. Украинский перевод меню plus.
- Проверено на server: старт/reload без ERROR, загрузка/выгрузка мира, новые ключи конфигов и локализаций дописываются;
  чистая установка сразу на русском. В игре НЕ проверено: меню, кодинг, чат, панель лобби, профиль.

## Шаг 9. Экономика, звёзды, улучшения, бэкапы, маркет, конкурсы, сайт
- 0: plus/economy — своя валюта (PlusEconomy реализует Economy оригинала, balances.yml, журнал), economy.type
  internal|auto|vault|none; /balance /pay /baltop /eco; награды: ежедневный бонус, игра в чужих мирах (AFK не считается),
  новые посетители.
- A: plus/rating — players.stars.<uuid> в settings.yml мира, миграция liked→5★, disliked→1★; байесовский рейтинг,
  getReputation() = рейтинг×10 для старых сортировок; /rate, RateMenu, /like = 5★, /dislike = 1★.
- B: plus/shop — shop.yml, бонусы мира bonus.coding-floors / bonus.world-size, слоты миров в players.yml;
  PlusLimits.worldsUsed/worldsLimit (конкурсные миры не считаются) вместо сравнений в 8 местах; /upgrades.
- C: plus/backups — zip мира и мира кода, авто по таймеру (только при активности), хранится keep копий; откат
  с копией «до отката», выгрузка мира, распаковка с защитой от zip-slip, PlanetConfig.discard(). Проверено на server.
- D: plus/market — market.yml (цена, продавец, продажи, лицензии), комиссия, ConfirmationMenu, блок
  ModuleInstallationEvent без лицензии; оценка модулей звёздами только после установки.
- E: plus/contest — contests.yml, фазы DRAFT/BUILDING/VOTING/FINISHED, отдельный мир через createPlanet, блокировка
  блоков после стройки, голосование StarPickerMenu, награды и рекомендуемые; кнопка в компасе, строка в панели лобби.
- F: plus/web — HttpServer на web.site.port, /, /world/<id>, /api/worlds, /api/contest; снимок данных на главном потоке
  (первый через 100 тиков, потом каждые 600). Проверено в браузере.
- G: строка над хотбаром при входе в мир, недавние миры и «Пригласить» в профиле, Discord-вебхук (новый мир, конкурс, продажа).
- H: тексты ru/en/ua (plus.*), ревизия локализации 3, 7 страниц в книжке изменений, COMMANDS.md, README.
- Проверено на server: старт и /oc reload без ERROR, команды из консоли, бэкап и откат, сайт. В игре НЕ проверено:
  меню улучшений, маркета, конкурса, оценки, приглашения.

## Отклонения от оригинала
- Язык по умолчанию ru, брендинг OpenCreative++, prefix «Миры», глобальный чат «Глобальный».
- Компас в лобби перенесён в ячейку ID 4 (items.lobby.5), «Мои миры» — ячейка 9. При миграции сохраняются старые ячейки.
- Группа premium выдаётся по opencreativepp.premium (было opencreative.group.premium). Старый конфиг при миграции сохраняет своё значение.
- Автопроверка обновлений OpenCreative выключена по умолчанию (форк).
- Все исправления шага 7 — исправления багов, механика не менялась.
- Рекомендуемые миры: при >21 мире оригинал показывал категории, теперь — список с листанием и сортировкой.
- Цвета в чате доступны всем игрокам по умолчанию (в оригинале — только OP из-за необъявленных прав).
- Экономика по умолчанию встроенная (`economy.type: internal`), Vault не нужен.
- Лайки/дизлайки заменены звёздами 1–5; репутация мира считается из звёзд.
- `filter.yml` теперь загружается (в оригинале падал при каждом старте) — исправление краша, не механики.

## Следующий шаг
- Ручная проверка в игре на ../server (компас, лимиты, чат, таблички, /datapack + /play). Перевод ua, действие кодинга для датапаков.
