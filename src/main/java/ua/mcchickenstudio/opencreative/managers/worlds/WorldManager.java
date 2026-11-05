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

package ua.mcchickenstudio.opencreative.managers.worlds;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.managers.Manager;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.concurrent.CompletableFuture;

/**
 * <h1>WorldManager</h1>
 * This interface represents a manager, that loads
 * and unloads worlds.
 */
public interface WorldManager extends Manager {

    /**
     * Creates a new world.
     * <p>
     * If specified world is already loaded, it will return that world.
     *
     * @param creator world creator with world info.
     * @param planet planet of world.
     * @return future with world, or error - if failed to load.
     */
    @NotNull CompletableFuture<World> createWorld(@NotNull WorldCreator creator, @NotNull Planet planet);

    /**
     * Loads world.
     * <p>
     * If specified world is already loaded, it will return that world.
     *
     * @param creator world creator with world info.
     * @param planet planet of world.
     * @return future with world, or error - if failed to load.
     */
    @NotNull CompletableFuture<World> loadWorld(@NotNull WorldCreator creator, @NotNull Planet planet);

    /**
     * Unloads world.
     * <p>
     * <b>NOTE:</b> When using async methods, please check {@code OpenCreative.getPlugin().isEnabled()}
     * to make sure if plugin can schedule async tasks or not (when server is shutting down).
     *
     * @param world world to unload.
     * @param save save or don't save.
     * @param planet planet of world.
     * @return future.
     */
    @NotNull CompletableFuture<Void> unloadWorld(@Nullable World world, boolean save, @NotNull Planet planet);

}
