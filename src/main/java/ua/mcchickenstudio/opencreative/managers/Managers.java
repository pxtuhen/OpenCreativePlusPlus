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

package ua.mcchickenstudio.opencreative.managers;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendDebug;

/**
 * <h1>Managers</h1>
 * This class represents registry of managers.
 */
public final class Managers {

    private final Map<Class<?>, Manager> managers = new HashMap<>();

    /**
     * Registers manager, or replaces existing with new.
     *
     * @param type type of manager.
     * @param manager manager instance.
     * @param <T> manager type.
     */
    public <T extends Manager> void register(@NotNull Class<T> type, @NotNull T manager) {
        Manager instance = managers.get(type);
        if (instance != null) {
            sendDebug("[MANAGERS] Replaced manager " + type.getSimpleName() + ": " + instance.getName() + " with " + manager.getName());
            shutdown(type);
        } else {
            sendDebug("[MANAGERS] Registered " + type.getSimpleName() + ": " + manager.getName());
        }
        managers.put(type, manager);
    }

    /**
     * Registers manager only if it still not registered yet.
     *
     * @param type type of manager.
     * @param manager manager instance.
     * @param <T> manager type.
     */
    public <T extends Manager> void registerIfAbsent(@NotNull Class<T> type, @NotNull T manager) {
        if (managers.containsKey(type)) return;
        sendDebug("[MANAGERS] Registered " + type.getSimpleName() + ": " + manager.getName());
        managers.put(type, manager);
    }

    /**
     * Returns instance of manager.
     *
     * @param type type of manager.
     * @return manager instance.
     * @param <T> manager type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Manager> T get(@NotNull Class<T> type) {
        return (T) managers.get(type);
    }

    /**
     * Returns collection of all managers.
     *
     * @return collection of all managers.
     */
    public @NotNull Collection<Manager> all() {
        return managers.values();
    }

    /**
     * Starts specified managers.
     *
     * @param managers managers to start.
     */
    @SafeVarargs
    public final void start(@NotNull Class<? extends Manager>... managers) {
        for (Class<? extends Manager> clazz : managers) {
            Manager manager = this.managers.get(clazz);
            if (!(manager instanceof Startable startable)) continue;
            try {
                startable.start();
            } catch (Exception error) {
                sendCriticalErrorMessage("Failed to start manager: " + manager.getName(), error);
            }
        }
    }

    /**
     * Shutdowns specified managers.
     *
     * @param managers managers to shut down.
     */
    @SafeVarargs
    public final void shutdown(@NotNull Class<? extends Manager>... managers) {
        for (Class<? extends Manager> clazz : managers) {
            Manager manager = this.managers.get(clazz);
            if (!(manager instanceof ShutDownable shutDownable)) continue;
            try {
                shutDownable.shutdown();
            } catch (Exception error) {
                sendCriticalErrorMessage("Failed to shutdown manager: " + manager.getName(), error);
            }
        }
    }

}
