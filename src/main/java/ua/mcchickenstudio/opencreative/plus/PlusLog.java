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

package ua.mcchickenstudio.opencreative.plus;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Console log of OpenCreative++ with module prefix: {@code [OpenCreativePP] [Миры] Загружено 12 миров}.
 * <p>
 * Debug lines are printed only with {@code debug: true} in config.yml.
 */
public final class PlusLog {

    private static volatile boolean debug = false;

    private final String prefix;

    private PlusLog(@NotNull String module) {
        this.prefix = module.isEmpty() ? "" : "[" + module + "] ";
    }

    public static @NotNull PlusLog module(@NotNull String module) {
        return new PlusLog(module);
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    public static boolean isDebug() {
        return debug;
    }

    private static @NotNull Logger logger() {
        var plugin = Bukkit.getPluginManager().getPlugin("OpenCreativePP");
        return plugin != null ? plugin.getLogger() : Logger.getLogger("OpenCreativePP");
    }

    public void info(@NotNull String message) {
        logger().info(prefix + message);
    }

    public void warn(@NotNull String message) {
        logger().warning(prefix + message);
    }

    public void error(@NotNull String message) {
        logger().severe(prefix + message);
    }

    /**
     * Error with short cause; full stack trace only in debug mode.
     */
    public void error(@NotNull String message, @Nullable Throwable error) {
        if (error == null) {
            error(message);
            return;
        }
        String cause = error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage());
        if (debug) {
            logger().log(Level.SEVERE, prefix + message + " — " + cause, error);
        } else {
            logger().severe(prefix + message + " — " + cause + " (подробности: debug: true в config.yml)");
        }
    }

    public void debug(@NotNull String message) {
        if (debug) logger().info(prefix + "[debug] " + message);
    }
}
