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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.planet.PlanetConnectPlayerEvent;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Last world of every player (plugins/OpenCreativePP/last-worlds.yml), for "Continue" button and profile.
 */
public final class LastWorlds implements Listener {

    private static final Map<UUID, Integer> lastWorlds = new HashMap<>();
    private static boolean loaded = false;

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "last-worlds.yml");
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file());
        for (String key : config.getKeys(false)) {
            try {
                lastWorlds.put(UUID.fromString(key), config.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of("Последний мир игроков: UUID -> ID мира. Файл ведёт плагин."));
        for (Map.Entry<UUID, Integer> entry : lastWorlds.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file());
        } catch (Exception error) {
            PlusLog.module("Миры").error("Не удалось сохранить last-worlds.yml", error);
        }
    }

    public static synchronized @Nullable Planet get(@NotNull UUID player) {
        load();
        Integer id = lastWorlds.get(player);
        return id == null ? null : OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(id));
    }

    @EventHandler
    public void onConnect(PlanetConnectPlayerEvent event) {
        if (event.getPlayer() == null) return;
        synchronized (LastWorlds.class) {
            load();
            Integer previous = lastWorlds.put(event.getPlayer().getUniqueId(), event.getPlanet().getId());
            if (previous == null || previous != event.getPlanet().getId()) save();
        }
    }
}
