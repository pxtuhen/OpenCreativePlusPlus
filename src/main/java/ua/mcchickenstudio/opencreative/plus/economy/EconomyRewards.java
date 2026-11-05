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

package ua.mcchickenstudio.opencreative.plus.economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.planet.PlanetConnectPlayerEvent;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * Ways to earn the built-in currency (config.yml → economy.rewards):
 * daily bonus, time in other players' worlds (not AFK), new visitors of your world.
 */
public final class EconomyRewards implements Listener {

    private static BukkitTask task;
    private static final Map<UUID, Location> lastLocations = new HashMap<>();
    private static final Map<UUID, Integer> activeMinutes = new HashMap<>();

    private static @NotNull FileConfiguration config() {
        return OpenCreative.getPlugin().getConfig();
    }

    private static @NotNull String today() {
        return LocalDate.now().toString();
    }

    public static void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), EconomyRewards::everyMinute, 1200L, 1200L);
    }

    public static void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlusEconomy economy = PlusEconomy.get();
        long bonus = config().getLong("economy.rewards.daily-bonus", 50);
        if (economy == null || bonus <= 0) return;
        Player player = event.getPlayer();
        String key = "daily." + player.getUniqueId();
        if (today().equals(economy.data(key))) return;
        economy.data(key, today());
        economy.add(player.getUniqueId(), bonus, "ежедневный бонус");
        Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
            if (player.isOnline()) player.sendMessage(getLocaleMessage("plus.economy.daily")
                    .replace("%amount%", PlusEconomy.format(bonus))
                    .replace("%balance%", PlusEconomy.format(economy.balance(player.getUniqueId()))));
        }, 60L);
    }

    @EventHandler
    public void onVisit(PlanetConnectPlayerEvent event) {
        PlusEconomy economy = PlusEconomy.get();
        long reward = config().getLong("economy.rewards.new-visitor", 3);
        Player visitor = event.getPlayer();
        Planet planet = event.getPlanet();
        if (economy == null || reward <= 0 || visitor == null || planet.isOwner(visitor)) return;
        String key = "visit." + planet.getId() + "." + visitor.getUniqueId();
        if (!economy.data(key).isEmpty()) return;
        economy.data(key, "1");
        economy.add(planet.getOwner(), reward, "новый посетитель мира " + planet.getId());
        Player owner = Bukkit.getPlayer(planet.getOwner());
        if (owner != null) owner.sendActionBar(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent(
                getLocaleMessage("plus.economy.visitor", false).replace("%amount%", PlusEconomy.format(reward))
                        .replace("%player%", visitor.getName())));
    }

    /**
     * Pays for every N active (moving) minutes in other players' worlds, up to a daily limit.
     */
    private static void everyMinute() {
        PlusEconomy economy = PlusEconomy.get();
        int minutes = config().getInt("economy.rewards.playtime.minutes", 10);
        long amount = config().getLong("economy.rewards.playtime.amount", 5);
        long dailyLimit = config().getLong("economy.rewards.playtime.daily-limit", 100);
        if (economy == null || minutes <= 0 || amount <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location now = player.getLocation();
            Location before = lastLocations.put(player.getUniqueId(), now);
            Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            boolean moved = before != null && before.getWorld() == now.getWorld() && before.distanceSquared(now) > 4;
            if (planet == null || planet.isOwner(player) || !moved) continue;
            int active = activeMinutes.merge(player.getUniqueId(), 1, Integer::sum);
            if (active < minutes) continue;
            activeMinutes.put(player.getUniqueId(), 0);
            String key = "playtime." + player.getUniqueId();
            String[] stored = economy.data(key).split(":");
            long earnedToday = stored.length == 2 && stored[0].equals(today()) ? Long.parseLong(stored[1]) : 0;
            long give = dailyLimit > 0 ? Math.min(amount, dailyLimit - earnedToday) : amount;
            if (give <= 0) continue;
            economy.data(key, today() + ":" + (earnedToday + give));
            economy.add(player.getUniqueId(), give, "игра в мирах");
            player.sendActionBar(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent(
                    getLocaleMessage("plus.economy.playtime", false).replace("%amount%", PlusEconomy.format(give))));
        }
        lastLocations.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        activeMinutes.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
}
