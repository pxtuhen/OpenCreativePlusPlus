<div align="center">

# OpenCreative++

**Build. Code. Play.**

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
![Paper](https://img.shields.io/badge/Paper-1.21.11-white)
![Java](https://img.shields.io/badge/Java-21-orange)
[![Based on OpenCreative+](https://img.shields.io/badge/Based%20on-OpenCreative%2B-purple)](https://gitlab.com/eagles-creative/opencreative)

# Let your players create the worlds

Minecraft plugin for PaperMC servers that allows players to create their own worlds, build in them,
code mini-games with blocks and play them with friends.

### [Original project](https://gitlab.com/eagles-creative/opencreative) - [Installation](#installation) - [Commands](#commands) - [Permissions](#permissions) - [Changelog](CHANGELOG)

![Screenshot](https://i.imgur.com/4jzB4F9.png)

</div>

> **OpenCreative++ is an unofficial fork** of [OpenCreative+](https://gitlab.com/eagles-creative/opencreative)
> by McChicken Studio (based on 6.0.0 Pre-release 9). Please do not ask the original authors for support of this fork.

## Features

Everything OpenCreative+ can do is still here:

- An **unique gamemode**, where **players can build** and even **create mini-games with coding**.
  It's a completely new experience compared to classic Creative servers with plots.

- **A lot of options for world generation**: terrain (Flat, Empty, Ocean, Normal) and environment
  (Overworld, Nether, The End), plus custom generators in `worlds.yml`.

- **Players can easily change their worlds**: time, weather, game rules, name, description, icon, category and custom ID.

- **It's better to create together**: owners add builders, developers, a whitelist or a ban list.

- **Coding on blocks** with 120+ events, 320+ actions, 90 conditions, values, variables and modules.

- **Built-in security**: redstone limiter, bad items fixer, lobby protection, chat filter.

- **Translatable**: every message, item, menu and sound can be changed.

- **Player groups** with custom limits, cooldowns, permissions, advertisement price and rating reward.

And this is what **OpenCreative++ adds**:

### 🪙 Own currency (no Vault needed)
- Coins are stored inside the plugin (`balances.yml`) with an operations log.
- `/balance`, `/pay`, `/baltop` for players and `/eco give|take|set|log` for admins.
- Ways to earn coins, each configurable or switchable off: a daily login bonus, playtime in other players' worlds
  (AFK time doesn't count), coins for world owners when new players visit, ratings, contest prizes and market sales.
- Already have an economy? Set `economy.type: vault` or `auto` and Vault will be used instead.

### ⭐ Star ratings instead of likes
- Rate worlds from 1 to 5 stars with `/rate` or a five-star menu. `/like` = 5★, `/dislike` = 1★.
- Worlds are sorted by a **weighted (Bayesian) rating**, so a world with a single 5★ vote won't overtake a world
  with a hundred 4.8★ votes.
- Old likes are migrated as 5★ and dislikes as 1★ automatically.

### ⬆ Paid world upgrades
- `/upgrades`: buy extra coding floors, a bigger world size and more world slots for coins.
- Prices, steps and maximums are in `shop.yml`. "My limits" shows *group + bought*.

### 💾 World backups and rollback
- Automatic backups on a timer (only for worlds that changed), manual backups with a cooldown.
- `/backups`: list, create, restore. A *"before restore"* backup is made before every rollback.
- Admins can back up and restore any world.

### 🛒 Code marketplace
- `/market`: share code modules for free or sell them for coins.
- A buyer gets a **license** and can install the module in any of their worlds; the author stays the author.
- The server takes a configurable commission. Paid modules can't be installed without a license.
- Modules are rated with stars after installation.

### 🏆 Build contests
- Admins create a contest with a theme, world generator, size, durations and rewards.
- Phases: **Building → Voting → Results**. Every participant gets a **separate world** that doesn't use their world slots.
- After building ends, contest worlds are locked; players vote with stars and see their progress ("rated 7 of 12").
- Winners get coins (and optional commands), their worlds can go to the recommended list.
- The contest is shown in the compass, the lobby sidebar and chat announcements.

### 🌐 Server website
- Built-in web page (default port `22913`): best worlds, playing now, new worlds, world pages, the current contest.
- JSON API: `/api/worlds` and `/api/contest`.

### 🧭 New compass and menus
- One menu with four tabs: **Recommended / My worlds / Favorites / Search**.
- "Continue" button (last world), category filter, "playing now" sort, search by description.
- **My limits**: world size, coding floors, worlds count and what your group gives.
- **Profile** `/profile [player]`: worlds, average rating, favorites, recent worlds, "Invite to my world" button.
- World name, author and rating are shown above the hotbar when you join a world.

### 🎨 And more
- **Colored chat and signs**: `&a`, `&l`, HEX `&#RRGGBB`, each style has its own permission. Signs keep their codes when edited again.
- **Datapacks in worlds (Premium)**: admins build a datapack library, owners enable packs with `/datapack`.
  Functions run in play mode and from code ("Run datapack function" world action). Each pack is validated first.
- **Lobby sidebar and TAB** with online, worlds count, balance, group and the current contest.
- **Five extra world types**: desert, snowy plains, beach, stone and nether flats.
- **Discord webhook**: new worlds, contests and market sales.
- **Split config**: `config.yml`, `groups.yml`, `worlds.yml`, `items.yml`, `chat.yml`, `datapacks.yml`, `shop.yml`, `sounds.yml`.
  A broken file is renamed instead of being reset, and new options are added automatically after updates.
- **Short logs**: startup fits in one summary line, details are available with `debug: true`.
- **Migration from OpenCreative+**: data from `plugins/OpenCreative` is copied automatically.
- **Many bugs of the original fixed**: coding, worlds, menus and chat. See [CHANGELOG](CHANGELOG) and [MODLOG.md](MODLOG.md).

## Usage

> Screenshots and videos below are from [OpenCreative+](https://gitlab.com/eagles-creative/opencreative) by McChicken Studio.
> Menus of OpenCreative++ look a bit different (new compass tabs, star ratings, upgrades).

### Creating a world
Players create worlds from the compass with one of the generators (plains, flat, ocean, void and five extra flats).
World size depends on the player's group in `groups.yml` and can be increased with `/upgrades`.

![](https://i.imgur.com/sID0dxv.png)

▶ [World creation video](https://gitlab.com/eagles-creative/opencreative/-/wikis/uploads/82d2118f4aee556c146a22b1419ed0df/WorldCreation.mp4)

### Browsing worlds
Open the compass (hotbar slot 5). Left click — join, right click — add to favorites, shift + left click — moderation (admins).
In the Search tab click the tab again: left click — by name and description, right click — by ID, shift + left click — by owner.
Any chat input request expires after 60 seconds; type `cancel` to cancel it.

![](https://i.imgur.com/mL6FS4o.png)

### World's settings
Every owner can change the name, description, category, ID, flags and icon of the world.
Backups, upgrades and datapacks are also available from the world settings.

![](https://i.imgur.com/iMJjFh0.png)

▶ [World settings video](https://gitlab.com/eagles-creative/opencreative/-/wikis/uploads/9a1cfb155d1391f4cba17be23a1fc13a/WorldSettings.mp4)

### Coding
Players build code with coding blocks in the developer world (`/dev`) and run it with `/play`.
Code is saved into `codeScript.yml` of the world. Finished code can be saved as a module and shared on `/market`.

![](https://i.imgur.com/1SkIjhv.png)

▶ [Developer world video](https://gitlab.com/eagles-creative/opencreative/-/wikis/uploads/1eda4a8c0ee3dcdb4b9a23954fa6d7e6/DevelopersWorld.mp4)

Coding blocks are parsed into `codeScript.yml`. Example: send a message "Hello World" on join.

```yaml
code:
  blocks:
    exec_block_4_1:
      category: EVENT_PLAYER
      type: PLAYER_JOIN
      actions:
        action_block1:
          category: PLAYER_ACTION
          type: PLAYER_SEND_MESSAGE
          arguments:
            type:
              type: TEXT
              value: join-spaces
            messages:
              type: LIST
              value:
                '1':
                  type: TEXT
                  value:
                    name: Hello
                '2':
                  type: TEXT
                  value:
                    name: World
```

## Installation

1. Server **Paper 1.21.11**, **Java 21**.
2. Put `OpenCreativePP-<version>.jar` into `plugins/` and start the server.
3. Configure files in `plugins/OpenCreativePP/`:

| File | Contents |
|---|---|
| `config.yml` | lobby, messages, coding, protection, logs, sidebar, economy, backups, contests, website, Discord |
| `groups.yml` | player groups: world size, coding floors, worlds count, limits, cooldowns |
| `worlds.yml` | recommended worlds, world types, deprecated worlds removal |
| `items.yml` | lobby and world items |
| `chat.yml` | chat and sign colors, word filter |
| `datapacks.yml` | datapack library for worlds |
| `shop.yml` | upgrade prices and code marketplace settings |
| `sounds.yml`, `filter.yml`, `locales/` | sounds, forbidden words, texts (`ru`, `en`, `ua`) |

Language is chosen by the admin for the whole server: `config.yml → messages.locale` (`ru`, `en` or `ua`).
Reload without restart: `/oc reload`.

Optional plugins:
- [Vault](https://www.spigotmc.org/resources/vault.34315/) — only if you want an external economy instead of the built-in one.
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — placeholders in messages.
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) or PacketEvents — coding chest animations, glowing blocks.
- [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81/) — entity disguise action.
- WorldEdit / FastAsyncWorldEdit.

### Moving from OpenCreative+
Replace OpenCreative+ with OpenCreative++. On the first start data from `plugins/OpenCreative` is copied to
`plugins/OpenCreativePP` and the old single `config.yml` is split into the new files. Values are kept,
the old folder stays untouched.

## Commands

### Players

| Command | Description |
|---|---|
| compass or `/games` | worlds menu: Recommended / My worlds / Favorites / Search, "Continue", My limits, profile |
| `/profile [player]` | player profile: worlds, rating, favorites, recent worlds, invite |
| `/own` | "My worlds" tab |
| `/join [ID]` | join a world by ID |
| `/jointo [player]` | join the world where the player is |
| `/spawn` | back to the lobby |
| `/cc [message]` or `!message` | global chat; `/cc off`, `/cc on` |
| `/locate [player]` | which world the player is in |
| `/rate [1-5]` | rate the world with stars (menu without a number); `/like` = 5★, `/dislike` = 1★ |
| `/ad` | advertise your world |
| `/build`, `/play`, `/dev` | build mode, play mode, developer world |
| `/env` | developer world settings: `vars list`, `vars size`, `debug`, ... |
| `/world` | world settings: `info`, `delete`, `deletemobs`, `kick/ban/unban`, `whitelist/unwhitelist` |
| `/balance [player]` | coins balance |
| `/pay [player] [amount]` | send coins |
| `/baltop` | richest players |
| `/upgrades` | buy coding floors, world size, world slots |
| `/backups` | backups of your world: create, restore |
| `/market` | code marketplace: buy, take for free, sell your modules |
| `/contest` | build contest: join, go to your entry, vote |
| `/datapack` | **Premium**: datapacks of your world |
| `/give`, `/tp`, `/gamemode`, `/time`, `/weather`, `/playsound`, `/stopsound` | in your world — owner, developers and builders |

### Admins

| Command | Permission |
|---|---|
| `/oc reload`, `/oc setspawn`, `/oc resetlocale` | `opencreative.reload`, `.setspawn`, `.resetlocale` |
| `/oc list`, `/oc deprecated [months]`, `/oc moderate [ID]` | `opencreative.list.*`, `opencreative.moderation.menu` |
| `/oc info / load / unload [ID]`, `/oc recommend / unrecommend [ID]` | `opencreative.world.*` |
| `/oc setowner [ID] [player]`, `/oc setsize [ID] [size]` | `opencreative.world.setowner / setsize` |
| `/oc maintenance start [sec] / end / restart` | `opencreative.maintenance` |
| `/eco give / take / set [player] [amount]`, `/eco log [player]` | `opencreativepp.economy.admin` |
| `/backups [ID] [create / restore <file>]` | `opencreativepp.backup.admin` |
| `/contest create [name]`, `theme`, `generator`, `size`, `time [build min] [vote min]`, `reward [place] [coins]`, `start`, `vote`, `finish`, `cancel`, `info` | `opencreativepp.contest.admin` |
| `/datapack list / reload` | `opencreative.datapacks.list / reload` |

The full list is in [COMMANDS.md](COMMANDS.md).

## Permissions

| Permission | Default | Gives |
|---|---|---|
| `opencreativepp.player` | everyone | colored chat and signs |
| `opencreativepp.premium` | nobody | `premium` group from `groups.yml` and datapacks |
| `opencreativepp.admin` | OP | all `/oc` commands, moderation, bypasses, and the three below |
| `opencreativepp.economy.admin` | OP | `/eco` |
| `opencreativepp.backup.admin` | OP | backups and rollback of any world |
| `opencreativepp.contest.admin` | OP | creating and running contests |

## Development

```bash
./build.sh
```

The jar appears in `build/libs/OpenCreativePP-<version>.jar`. Java 21 is required, Gradle is downloaded automatically.
Without the script: `./gradlew build -x javadoc`.

New fork code lives in `ua.mcchickenstudio.opencreative.plus`; changes in the original code are marked with
`// OpenCreative++:` comments. Pull requests are welcome.

## License

This plugin is licensed under **GNU AGPL v3**, like the original (see [LICENSE](LICENSE)). If you run a modified version
on your server, AGPL requires you to give your players access to its source code.

[![AGPLv3 Logo](https://www.gnu.org/graphics/agplv3-with-text-136x68.png)](https://www.gnu.org/licenses/agpl-3.0.txt)

<div align="center">

## Credits

### _**OpenCreative+**_

Made by McChicken Studio 2017-2026.

McChicken Team, onn512, pawsashatoy, LWJENNI, HACKERPRO17, liventmst, logiklop, 70N77U, kalienda, Renzotom

Translation: Nagibator6000LoL, initzero, DrakesWeb, kogtyv, Senmpai333

Source code: <https://gitlab.com/eagles-creative/opencreative>

### _**OpenCreative++**_

Fork with bug fixes and new features on top of OpenCreative+ 6.0.0 Pre-release 9.

### _**Special Thanks**_

PaperMC Team, developers of Vault, ProtocolLib, PacketEvents, PlaceholderAPI and LibsDisguises,
Creative+ Wiki, [DiamondFire](https://mcdiamondfire.com) for the inspiration.

**❤️ Every server that uses this plugin.**

</div>
