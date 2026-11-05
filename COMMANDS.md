# OpenCreative++ — команды и права

Форк OpenCreative+ 6.0.0 (C) McChicken Studio, GNU AGPL v3. Paper 1.21.11.

## Роли

| Право | Кому по умолчанию | Что даёт |
|---|---|---|
| `opencreativepp.player` | всем | цветной чат и таблички (`opencreativepp.chat.*`, `opencreativepp.sign.*` кроме `&k`) |
| `opencreativepp.premium` | никому (выдать через LuckPerms) | группа `premium` из `groups.yml` (мир 256×256, 4 мира…) + датапаки |
| `opencreativepp.admin` | OP | все команды `/oc`, модерация, обход ограничений лобби и миров, датапаки, `&k` |

```
/lp user Ник permission set opencreativepp.premium
/lp group admin permission set opencreativepp.admin
```

## Игрокам

| Команда | Что делает |
|---|---|
| компас (центр хотбара) или `/games` | меню миров: Рекомендуемые / Мои / Любимые / Поиск; «Продолжить», категории, сортировка, «Мои лимиты», профиль |
| `/profile [ник]` | профиль: группа, миры, лайки, любимые, последний мир |
| `/own` | сразу вкладка «Мои миры» |
| `/join [ID]` | зайти в мир по ID |
| `/jointo [игрок]` | зайти в мир, где сейчас игрок |
| `/spawn` | в лобби |
| `/cc [текст]` или `!текст` | глобальный чат; `/cc off`, `/cc on` |
| `/locate [игрок]` | в каком мире игрок |
| `/rate [1-5]` | оценить мир звёздами (без числа — меню); `/like` = 5★, `/dislike` = 1★ |
| `/balance [ник]` (`/bal`, `/монеты`) | баланс монет |
| `/pay [ник] [сумма]` | перевести монеты (минимум и кулдаун — `config.yml → economy.pay`) |
| `/baltop` | самые богатые игроки |
| `/upgrades` (`/улучшения`) | купить за монеты этажи кодинга, размер мира, слот мира (`shop.yml`) |
| `/backups` (`/бэкапы`) | резервные копии своего мира: создать, восстановить (перед откатом делается копия «до отката») |
| `/market` (`/маркет`) | маркет модулей кода: купить (лицензия на все свои миры), взять бесплатный, выставить свой |
| `/contest` (`/конкурс`) | конкурс построек: участвовать (отдельный мир, не занимает слот), голосовать |
| `/ad` | прорекламировать свой мир |
| `/build` | режим строительства (PvP выключен, креатив владельцу и строителям) |
| `/play` | режим игры, запускает код |
| `/dev` | мир кода |
| `/env` | настройки мира кода: `vars list`, `vars size`, `debug`, `barrel`, `event/action/floor [материал]` |
| `/world` | настройки мира: `info`, `delete`, `deletemobs`, `kick/ban/unban [игрок]`, `whitelist/unwhitelist [игрок]` |
| `/datapack` (`/dp`) | **Premium**: датапаки своего мира — меню; `/datapack [id]` — включить/выключить |
| `/give`, `/tp`, `/gamemode`, `/time set`, `/weather`, `/playsound`, `/stopsound` | в своём мире — владельцу, разработчикам и строителям |

Меню миров: **ЛКМ** — войти (во «Моих» — настройки доступа), **ПКМ** — в любимые/убрать, **Shift+ЛКМ** — модерация (у админа).
Поиск: открой вкладку «Поиск» и нажми на неё ещё раз: ЛКМ — по названию и описанию, ПКМ — по ID, Shift+ЛКМ — по нику владельца.
Любой запрос ввода в чат (поиск, название мира…) живёт 60 секунд; отменить — написать `отмена` или `cancel`.

В коде: действие мира «Запустить функцию датапака» — функция `namespace:путь` из датапака, включённого в этом мире.

## Цвета

`&0-&9 &a-&f` — цвета, `&l &o &n &m` — стили, `&k` — шум, `&#RRGGBB` — HEX. Работают в чате мира, в `/cc` и на табличках
(обе стороны). Повторно открытая табличка показывает коды, а не готовый цвет. Включение и выключение — `chat.yml`.

| Право | По умолчанию |
|---|---|
| `opencreativepp.chat.color`, `.hex`, `.format` | все |
| `opencreativepp.chat.obfuscated` | OP |
| `opencreativepp.sign.color`, `.hex`, `.format` | все |
| `opencreativepp.sign.obfuscated` | OP |

Старые права OpenCreative+ (`opencreative.chat.color`, `opencreative.creative-chat.bold`…) тоже работают.

## Администраторам (`/oc`, `/creative`)

| Команда | Право |
|---|---|
| `/oc reload` — перечитать все конфиги | `opencreative.reload` |
| `/oc setspawn` — точка спавна лобби | `opencreative.setspawn` |
| `/oc resetlocale` — сбросить тексты | `opencreative.resetlocale` |
| `/oc ignoremessage / unignoremessage [путь]` | `opencreative.resetlocale.add-ignored / remove-ignored` |
| `/oc list [игрок]`, `/oc created` | `opencreative.list.loaded / players / all` |
| `/oc deprecated [месяцев]`, `/oc deprecated delete [месяцев]` | `opencreative.list.deprecated`, `opencreative.delete.deprecated` |
| `/oc moderate [ID]` — меню модерации | `opencreative.moderation.menu` |
| `/oc info / load / unload [ID]`, `/oc unload all` | `opencreative.world.info / load / unload / unload.all` |
| `/oc recommend / unrecommend [ID]` — рекомендуемые миры | `opencreative.world.recommend / unrecommend` |
| `/oc setowner [ID] [игрок]`, `/oc setsize [ID] [размер]` | `opencreative.world.setowner / setsize` |
| `/oc maintenance start [сек] / end / restart` | `opencreative.maintenance` |
| `/oc groups info / edit / remove [группа]` | `opencreative.groups.*` |
| `/oc items get / set / reset [набор] [ячейка]` | `opencreative.items.*` |
| `/oc sounds [тема]`, `/oc sound [звук]` | `opencreative.sounds.theme / play` |
| `/oc spy on/off` — чат других миров | `opencreative.spy` |
| `/oc stability` | — |
| `/datapack list` — библиотека датапаков и ошибки проверок | `opencreative.datapacks.list` |
| `/datapack reload` — перечитать `datapacks.yml` | `opencreative.datapacks.reload` |

| `/eco give / take / set [ник] [сумма]`, `/eco log [ник]` | `opencreativepp.economy.admin` |
| `/backups [ID]`, `/backups [ID] create`, `/backups [ID] restore [файл]` — копии любого мира | `opencreativepp.backup.admin` |
| `/contest create [название]`, `theme`, `generator`, `size`, `time [стройка мин] [голосование мин]`, `reward [место] [монет]`, `start`, `vote`, `finish`, `cancel`, `info` | `opencreativepp.contest.admin` |

Все три права входят в `opencreativepp.admin`.

`/datapack` перекрывает ванильную команду; ванильная доступна как `/minecraft:datapack`.

Обходы: `opencreative.maintenance.bypass`, `opencreative.stability.bypass`, `opencreative.world.private.bypass`,
`opencreative.world.banned.bypass`, `opencreative.world.kick.bypass`, `opencreative.world.ban.bypass`,
`opencreative.lobby.*.bypass` — всё входит в `opencreativepp.admin`.
