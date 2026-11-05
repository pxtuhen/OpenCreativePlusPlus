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

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.util.*;

/**
 * Favorite worlds of players, stored in plugins/OpenCreativePP/favorites.yml as {@code uuid: [world ids]}.
 */
public final class FavoriteWorlds {

    private static final PlusLog log = PlusLog.module("Миры");
    private static final Map<UUID, LinkedHashSet<Integer>> favorites = new HashMap<>();
    private static boolean loaded = false;

    private FavoriteWorlds() {
    }

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "favorites.yml");
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        File file = file();
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                favorites.put(UUID.fromString(key), new LinkedHashSet<>(config.getIntegerList(key)));
            } catch (IllegalArgumentException ignored) {
                log.warn("favorites.yml: пропущен неверный UUID " + key);
            }
        }
    }

    private static synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of("Любимые миры игроков: UUID -> список ID миров. Файл ведёт плагин."));
        for (Map.Entry<UUID, LinkedHashSet<Integer>> entry : favorites.entrySet()) {
            if (!entry.getValue().isEmpty()) config.set(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(file());
        } catch (Exception error) {
            log.error("Не удалось сохранить favorites.yml", error);
        }
    }

    public static synchronized boolean isFavorite(@NotNull UUID player, int worldId) {
        load();
        return favorites.getOrDefault(player, new LinkedHashSet<>()).contains(worldId);
    }

    /**
     * Adds or removes world from favorites.
     *
     * @return true if world is favorite now.
     */
    public static synchronized boolean toggle(@NotNull UUID player, int worldId) {
        load();
        LinkedHashSet<Integer> set = favorites.computeIfAbsent(player, uuid -> new LinkedHashSet<>());
        boolean nowFavorite = !set.remove(worldId);
        if (nowFavorite) set.add(worldId);
        save();
        return nowFavorite;
    }

    /**
     * Existing favorite worlds; deleted worlds are skipped.
     */
    public static synchronized @NotNull List<Planet> getPlanets(@NotNull UUID player) {
        load();
        List<Planet> planets = new ArrayList<>();
        for (int id : favorites.getOrDefault(player, new LinkedHashSet<>())) {
            Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(id));
            if (planet != null) planets.add(planet);
        }
        return planets;
    }
}
