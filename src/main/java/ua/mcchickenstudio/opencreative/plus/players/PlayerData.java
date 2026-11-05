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
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Small per-player data of OpenCreative++ (plugins/OpenCreativePP/players.yml):
 * bought world slots, recent worlds and so on. {@code <uuid>.<key>: value}.
 */
public final class PlayerData {

    private static YamlConfiguration config;

    private PlayerData() {
    }

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "players.yml");
    }

    private static synchronized @NotNull YamlConfiguration config() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file());
        return config;
    }

    private static synchronized void save() {
        try {
            config().save(file());
        } catch (Exception error) {
            PlusLog.module("Игроки").error("Не удалось сохранить players.yml", error);
        }
    }

    public static synchronized int getInt(@NotNull UUID player, @NotNull String key) {
        return config().getInt(player + "." + key, 0);
    }

    public static synchronized void setInt(@NotNull UUID player, @NotNull String key, int value) {
        config().set(player + "." + key, value);
        save();
    }

    public static synchronized @NotNull List<Integer> getIntList(@NotNull UUID player, @NotNull String key) {
        return new ArrayList<>(config().getIntegerList(player + "." + key));
    }

    public static synchronized void setIntList(@NotNull UUID player, @NotNull String key, @NotNull List<Integer> value) {
        config().set(player + "." + key, value);
        save();
    }
}
