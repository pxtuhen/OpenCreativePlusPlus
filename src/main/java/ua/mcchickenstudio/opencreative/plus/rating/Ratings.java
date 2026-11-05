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

package ua.mcchickenstudio.opencreative.plus.rating;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;

/**
 * 1–5 star ratings of worlds (replace likes/dislikes of OpenCreative+).
 * <p>
 * Stored in world's settings.yml as {@code players.stars.<uuid>: 1..5}. Old likes become 5 stars, dislikes 1 star.
 * Sorting uses a Bayesian average, so a world with one 5-star vote doesn't beat a world with fifty 4.8 votes.
 */
public final class Ratings {

    /** Weight of the prior: how many "average" votes every world starts with. */
    private static final double PRIOR_VOTES = 3;
    private static final double PRIOR_AVERAGE = 3.0;

    private static final Map<Integer, Map<UUID, Integer>> stars = new ConcurrentHashMap<>();
    /** Worlds whose stars came from migrated likes and are not written yet. */
    private static final Set<Integer> migrated = ConcurrentHashMap.newKeySet();

    private Ratings() {
    }

    /**
     * Called by PlanetInfo when world information is loaded.
     */
    public static void load(@NotNull Planet planet, @NotNull FileConfiguration config) {
        Map<UUID, Integer> votes = new ConcurrentHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("players.stars");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    votes.put(UUID.fromString(key), Math.max(1, Math.min(5, section.getInt(key))));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } else {
            for (String key : config.getStringList("players.liked")) {
                try {
                    votes.put(UUID.fromString(key), 5);
                } catch (IllegalArgumentException ignored) {
                }
            }
            for (String key : config.getStringList("players.disliked")) {
                try {
                    votes.put(UUID.fromString(key), 1);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!votes.isEmpty()) migrated.add(planet.getId());
        }
        stars.put(planet.getId(), votes);
    }

    public static int votes(@NotNull Planet planet) {
        return stars.getOrDefault(planet.getId(), Map.of()).size();
    }

    public static double average(@NotNull Planet planet) {
        Map<UUID, Integer> votes = stars.getOrDefault(planet.getId(), Map.of());
        if (votes.isEmpty()) return 0;
        return votes.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /**
     * Bayesian average, used for sorting.
     */
    public static double weighted(@NotNull Planet planet) {
        Map<UUID, Integer> votes = stars.getOrDefault(planet.getId(), Map.of());
        double sum = votes.values().stream().mapToInt(Integer::intValue).sum();
        return (PRIOR_VOTES * PRIOR_AVERAGE + sum) / (PRIOR_VOTES + votes.size());
    }

    /**
     * Value for OpenCreative+ "reputation": weighted rating × 10 (10..50), so old sorting by reputation works.
     */
    public static int reputation(@NotNull Planet planet) {
        return (int) Math.round(weighted(planet) * 10);
    }

    public static int vote(@NotNull Planet planet, @NotNull UUID player) {
        return stars.getOrDefault(planet.getId(), Map.of()).getOrDefault(player, 0);
    }

    /**
     * "★ 4.6 (23)" or "no ratings".
     */
    public static @NotNull String format(@NotNull Planet planet) {
        int votes = votes(planet);
        if (votes == 0) return getLocaleMessage("plus.rating.none", false);
        return getLocaleMessage("plus.rating.format", false)
                .replace("%stars%", String.format(Locale.ROOT, "%.1f", average(planet)))
                .replace("%votes%", String.valueOf(votes));
    }

    /**
     * Rates world, with all checks of OpenCreative+ likes (world age, own world, cooldown is in commands).
     *
     * @return true if vote was saved.
     */
    public static boolean rate(@NotNull Player player, @NotNull Planet planet, int value) {
        if (value < 1 || value > 5) return false;
        if (planet.isOwner(player)) {
            player.sendMessage(getLocaleMessage("plus.rating.own-world"));
            Sounds.PLAYER_CANCEL.play(player);
            return false;
        }
        long createdSeconds = (System.currentTimeMillis() - planet.getCreationTime()) / 1000;
        long minSeconds = OpenCreative.getSettings().getRequirements().getWorldReputationMinSeconds();
        if (minSeconds > createdSeconds) {
            Sounds.PLAYER_CANCEL.play(player);
            player.sendMessage(getPlayerLocaleMessage("world.cant-rate", player).replace("%time%", convertTime((minSeconds - createdSeconds) * 1000)));
            return false;
        }
        Map<UUID, Integer> votes = stars.computeIfAbsent(planet.getId(), id -> new ConcurrentHashMap<>());
        Integer previous = votes.put(player.getUniqueId(), value);
        if (migrated.remove(planet.getId())) {
            for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
                planet.getConfiguration().set("players.stars." + entry.getKey(), entry.getValue());
            }
        } else {
            planet.getConfiguration().set("players.stars." + player.getUniqueId(), value);
        }
        planet.getInformation().setPlanetReputation(reputation(planet));
        Sounds.WORLD_LIKED.play(player);
        player.sendMessage(getLocaleMessage(previous == null ? "plus.rating.rated" : "plus.rating.changed")
                .replace("%stars%", stars(value)).replace("%rating%", format(planet)));
        if (previous == null) notifyAndReward(player, planet, value);
        return true;
    }

    private static void notifyAndReward(@NotNull Player player, @NotNull Planet planet, int value) {
        Player owner = Bukkit.getPlayer(planet.getOwner());
        if (owner != null && !owner.equals(player)) {
            owner.sendMessage(getLocaleMessage("plus.rating.owner-notify").replace("%player%", player.getName())
                    .replace("%stars%", stars(value)).replace("%world%", planet.getInformation().getDisplayName())
                    .replace("%rating%", format(planet)));
        }
        if (value < 4 || OpenCreative.getEconomy() == null || !OpenCreative.getEconomy().isWorking()) return;
        double reward = planet.getGroup().getLikeReward();
        if (reward <= 0) return;
        PlusEconomy economy = PlusEconomy.get();
        if (economy != null) {
            String key = "rate-reward." + planet.getId() + "." + player.getUniqueId();
            if (!economy.data(key).isEmpty()) return;
            economy.data(key, "1");
            economy.add(planet.getOwner(), (long) reward, "оценка мира " + planet.getId());
        } else {
            OpenCreative.getEconomy().depositMoney(Bukkit.getOfflinePlayer(planet.getOwner()), reward);
        }
    }

    public static @NotNull String stars(int value) {
        return "&e" + "★".repeat(value) + "&8" + "★".repeat(5 - value);
    }
}
