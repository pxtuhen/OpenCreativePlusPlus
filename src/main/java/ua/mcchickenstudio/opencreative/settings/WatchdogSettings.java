/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ua.mcchickenstudio.opencreative.OpenCreative;

public final class WatchdogSettings {

    private boolean enabled;
    private boolean limitOperationsWhenUnstable;
    private boolean unloadWorldsWhenUnstable;

    /**
     * Loads settings of watchdog from configuration.
     */
    public void load() {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        ConfigurationSection section = config.getConfigurationSection("watchdog");
        if (section == null) {
            return;
        }
        enabled = section.getBoolean("enabled", false);
        limitOperationsWhenUnstable = section.getBoolean("limit-operations-when-unstable", false);
        unloadWorldsWhenUnstable = section.getBoolean("unload-worlds-when-unstable", false);
    }

    /**
     * Checks whether watchdog is enabled.
     *
     * @return true - enabled, false - disabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks whether players must be prevented from
     * creating new worlds and connecting to worlds
     * when stability is bad.
     *
     * @return true - should limit, false - will not limit.
     */
    public boolean shouldLimitOperationsWhenUnstable() {
        return limitOperationsWhenUnstable;
    }

    /**
     * Checks whether worlds should be unloaded
     * when stability is bad.
     *
     * @return true - should be unloaded, false - not.
     */
    public boolean shouldUnloadWorldsWhenUnstable() {
        return unloadWorldsWhenUnstable;
    }
}
