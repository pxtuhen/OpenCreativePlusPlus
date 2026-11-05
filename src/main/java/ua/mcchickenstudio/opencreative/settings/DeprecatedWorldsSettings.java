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

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import ua.mcchickenstudio.opencreative.OpenCreative;

/**
 * <h1>DeprecatedWorldsSettings</h1>
 * This class represents a settings of deprecated worlds auto-deletion.
 */
public final class DeprecatedWorldsSettings {

    private boolean autoDelete = false;
    private int months = 5;
    private boolean deleteOnStartup = false;
    private int checkIntervalHours = 24;
    private BukkitTask autoDeleteTask;

    /**
     * Loads settings of deprecated worlds from configuration.
     */
    public void load() {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        ConfigurationSection section = config.getConfigurationSection("deprecated-worlds");
        if (section == null) {
            section = config.createSection("deprecated-worlds");
        }
        autoDelete = section.getBoolean("auto-delete", false);
        months = Math.max(1, section.getInt("months", 5));
        deleteOnStartup = section.getBoolean("delete-on-startup", false);
        checkIntervalHours = Math.max(0, section.getInt("check-interval-hours", 24));
        startAutoDeleteTask();
    }

    /**
     * Starts or cancels periodic task for deleting deprecated worlds.
     */
    public void startAutoDeleteTask() {
        if (autoDeleteTask != null) {
            autoDeleteTask.cancel();
            autoDeleteTask = null;
        }
        if (autoDelete && checkIntervalHours > 0) {
            long intervalTicks = checkIntervalHours * 72000L;
            autoDeleteTask = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), () -> {
                OpenCreative.getPlanetsManager().deleteDeprecatedPlanets(months);
            }, intervalTicks, intervalTicks);
        }
    }

    /**
     * Cancels periodic auto-deletion task if running.
     */
    public void cancelTask() {
        if (autoDeleteTask != null) {
            autoDeleteTask.cancel();
            autoDeleteTask = null;
        }
    }

    /**
     * Checks whether automatic deletion of deprecated worlds is enabled.
     *
     * @return true - enabled, false - disabled.
     */
    public boolean isAutoDelete() {
        return autoDelete;
    }

    /**
     * Returns inactivity period in months before a world is considered deprecated.
     *
     * @return months amount.
     */
    public int getMonths() {
        return months;
    }

    /**
     * Checks whether deprecated worlds should be deleted on server startup.
     *
     * @return true - delete on startup, false - do not delete.
     */
    public boolean isDeleteOnStartup() {
        return deleteOnStartup;
    }

    /**
     * Returns interval in hours between periodic checks for deprecated worlds.
     *
     * @return interval in hours.
     */
    public int getCheckIntervalHours() {
        return checkIntervalHours;
    }
}
