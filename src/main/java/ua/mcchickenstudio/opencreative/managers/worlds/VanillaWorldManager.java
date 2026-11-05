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

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.concurrent.CompletableFuture;

/**
 * <h1>VanillaWorldManager</h1>
 * This class represents a manager, that loads and unloads
 * worlds using Paper methods.
 */
public final class VanillaWorldManager implements WorldManager {

    @Override
    public @NotNull String getName() {
        return "Vanilla World Manager";
    }

    @Override
    public @NotNull CompletableFuture<World> createWorld(@NotNull WorldCreator creator, @NotNull Planet planet) {
        return loadWorld(creator, planet);
    }

    @Override
    public @NotNull CompletableFuture<World> loadWorld(@NotNull WorldCreator creator, @NotNull Planet planet) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
            try {
                World world = Bukkit.createWorld(creator);
                future.complete(world);
            } catch (Exception error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> unloadWorld(@Nullable World world, boolean save, @NotNull Planet planet) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (world == null) {
            future.complete(null);
            return future;
        }
        if (OpenCreative.getPlugin().isEnabled()) {
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                try {
                    Bukkit.unloadWorld(world, save);
                    future.complete(null);
                } catch (Exception error) {
                    future.completeExceptionally(error);
                }
            });
        } else {
            try {
                Bukkit.unloadWorld(world, save);
                future.complete(null);
            } catch (Exception error) {
                future.completeExceptionally(error);
            }
        }
        return future;
    }
}
