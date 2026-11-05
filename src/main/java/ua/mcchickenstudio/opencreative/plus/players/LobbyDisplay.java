/*
 * OpenCreative++, fork of OpenCreative+ (C) 2022-2026, McChicken Studio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.plus.players;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.utils.world.WorldUtils;

import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * OpenCreative++: side panel in the lobby and TAB header/footer for everybody (config.yml → lobby.sidebar, lobby.tablist).
 * <p>
 * Placeholders: %player% %online% %worlds% %loaded% %group% %my-worlds% %worlds-limit% %world%.
 */
public final class LobbyDisplay {

    private static final Map<UUID, Scoreboard> boards = new HashMap<>();
    private static BukkitTask task;

    private LobbyDisplay() {
    }

    public static void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), LobbyDisplay::update, 20L, 40L);
    }

    public static void stop() {
        if (task != null) task.cancel();
        task = null;
        boards.clear();
    }

    private static @NotNull String apply(@NotNull String text, @NotNull Player player, int loaded) {
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        var group = OpenCreative.getSettings().getGroups().getGroup(player);
        return text
                .replace("%contest%", ua.mcchickenstudio.opencreative.plus.contest.Contests.isActive()
                        ? OpenCreative.getPlugin().getConfig().getString("lobby.sidebar.contest-line", "&6Конкурс: &f%name% &7(%time%)")
                        .replace("%name%", ua.mcchickenstudio.opencreative.plus.contest.Contests.name()).replace("%time%", ua.mcchickenstudio.opencreative.plus.contest.Contests.timeLeft()) : "")
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%worlds%", String.valueOf(OpenCreative.getPlanetsManager().getPlanets().size()))
                .replace("%loaded%", String.valueOf(loaded))
                .replace("%group%", group.getName())
                .replace("%my-worlds%", String.valueOf(ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsUsed(player)))
                .replace("%worlds-limit%", String.valueOf(ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsLimit(player)))
                .replace("%balance%", OpenCreative.getEconomy() != null && OpenCreative.getEconomy().isWorking()
                        ? (OpenCreative.getEconomy() instanceof ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy
                        ? ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy.format(OpenCreative.getEconomy().getBalance(player).longValue())
                        : String.valueOf(OpenCreative.getEconomy().getBalance(player))) : "-")
                .replace("%world%", planet == null
                        ? OpenCreative.getPlugin().getConfig().getString("lobby.tablist.lobby-name", "Лобби")
                        : planet.getInformation().getDisplayName());
    }

    private static void update() {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        boolean sidebar = config.getBoolean("lobby.sidebar.enabled", true);
        boolean tablist = config.getBoolean("lobby.tablist.enabled", true);
        if (!sidebar && !tablist) return;
        int loaded = 0;
        for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
            if (planet.isLoaded()) loaded++;
        }
        String title = config.getString("lobby.sidebar.title", "&b&lOpenCreative++");
        List<String> lines = config.getStringList("lobby.sidebar.lines");
        String header = String.join("\n", config.getStringList("lobby.tablist.header"));
        String footer = String.join("\n", config.getStringList("lobby.tablist.footer"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (tablist) {
                player.sendPlayerListHeaderAndFooter(toComponent(apply(header, player, loaded)), toComponent(apply(footer, player, loaded)));
            }
            if (!sidebar) continue;
            boolean inLobby = WorldUtils.isLobbyWorld(player.getWorld());
            Scoreboard board = boards.get(player.getUniqueId());
            if (!inLobby) {
                // Worlds use own scoreboards (coding); OpenCreative+ resets the scoreboard on world change.
                if (board != null && player.getScoreboard() == board) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
                continue;
            }
            if (board == null) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
                boards.put(player.getUniqueId(), board);
            }
            Objective objective = board.getObjective("cp_lobby");
            if (objective != null) objective.unregister();
            objective = board.registerNewObjective("cp_lobby", Criteria.DUMMY, toComponent(apply(title, player, loaded)));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(NumberFormat.blank());
            int score = lines.size();
            for (int i = 0; i < lines.size() && i < 15; i++) {
                Component line = toComponent(apply(lines.get(i), player, loaded));
                var entry = objective.getScore("line" + i);
                entry.setScore(score--);
                entry.customName(line);
            }
            if (player.getScoreboard() != board) player.setScoreboard(board);
        }
        boards.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
}
