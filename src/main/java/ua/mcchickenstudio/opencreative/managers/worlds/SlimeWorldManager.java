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

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.Startable;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.utils.FileUtils;

import java.io.*;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;

/**
 * <h1>SlimeWorldManager</h1>
 * This class represents an advanced slime world manager, that
 * requires using AdvancedSlimePaper instead of PaperMC.
 */
public final class SlimeWorldManager implements WorldManager, Startable {

    private AdvancedSlimePaperAPI slime;

    @Override
    public void start() {
        slime = AdvancedSlimePaperAPI.instance();
    }

    @Override
    public @NotNull CompletableFuture<World> createWorld(@NotNull WorldCreator creator, @NotNull Planet planet) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            SlimePropertyMap properties = createProperties(creator.name(), planet);
            File path = getFolder(creator.name(), planet);
            try {
                SlimeWorld slimeWorld = slime.createEmptyWorld(creator.name(), false, properties, new FileLoader(path));
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    SlimeWorldInstance instance = slime.loadWorld(slimeWorld, true);
                    future.complete(instance.getBukkitWorld());
                });
            } catch (Exception error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<World> loadWorld(@NotNull WorldCreator creator, @NotNull Planet planet) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            SlimePropertyMap properties = createProperties(creator.name(), planet);
            File path = getFolder(creator.name(), planet);
            SlimeWorld slimeWorld;
            File worldSlime = new File(path, "world.slime");
            if (worldSlime.exists()) {
                // world.slime exists
                try {
                    slimeWorld = slime.readWorld(new FileLoader(path), creator.name(), false, properties);
                } catch (Exception error) {
                    future.completeExceptionally(error);
                    return;
                }
            } else {
                // world.slime doesn't exist, try to convert vanilla world to it
                try {
                    slimeWorld = slime.readVanillaWorld(path, creator.name(), new FileLoader(path));
                } catch (Exception error) {
                    future.completeExceptionally(error);
                    return;
                }
            }
            SlimeWorld finalSlimeWorld = slimeWorld;
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                try {
                    SlimeWorldInstance instance = slime.loadWorld(finalSlimeWorld, true);
                    future.complete(instance.getBukkitWorld());
                } catch (Exception error) {
                    future.completeExceptionally(error);
                }
            });

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
        SlimeWorldInstance slimeWorld = slime.getLoadedWorld(world.getName());
        if (slimeWorld == null) {
            future.complete(null);
            return future;
        }
        if (OpenCreative.getPlugin().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
                try {
                    slime.saveWorld(slimeWorld);
                    Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                        Bukkit.unloadWorld(world, save);
                        future.complete(null);
                    });
                } catch (Exception error) {
                    future.completeExceptionally(error);
                }
            });
        } else {
            try {
                slime.saveWorld(slimeWorld);
                Bukkit.unloadWorld(world, save);
                future.complete(null);
            } catch (Exception error) {
                future.completeExceptionally(error);
            }
        }
        return future;
    }

    private @NotNull File getFolder(@NotNull String worldName, @NotNull Planet planet) {
        return isDevPlanet(worldName) ? FileUtils.getDevPlanetFolder(planet.getDevPlanet()) : FileUtils.getPlanetFolder(planet);
    }

    private @NotNull SlimePropertyMap createProperties(@NotNull String worldName, @NotNull Planet planet) {
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DRAGON_BATTLE, false);
        if (isDevPlanet(worldName)) {
            // Development world
            properties.setValue(SlimeProperties.SPAWN_X, 2);
            properties.setValue(SlimeProperties.SPAWN_Y, 1);
            properties.setValue(SlimeProperties.SPAWN_Z, 2);
            properties.setValue(SlimeProperties.PVP, false);
            properties.setValue(SlimeProperties.ALLOW_ANIMALS, false);
            properties.setValue(SlimeProperties.ALLOW_MONSTERS, false);
            properties.setValue(SlimeProperties.DIFFICULTY, "peaceful");
            properties.setValue(SlimeProperties.WORLD_TYPE, "FLAT");
            properties.setValue(SlimeProperties.ENVIRONMENT, "normal");
        } else {
            // Build world
            properties.setValue(SlimeProperties.ENVIRONMENT, planet.getTerritory().getEnvironment().name().toLowerCase());
            Location spawn = planet.getTerritory().getSpawnLocation();
            properties.setValue(SlimeProperties.SPAWN_X, spawn.getBlockX());
            properties.setValue(SlimeProperties.SPAWN_Y, spawn.getBlockY());
            properties.setValue(SlimeProperties.SPAWN_Z, spawn.getBlockZ());
            properties.setValue(SlimeProperties.SPAWN_YAW, spawn.getYaw());
            properties.setValue(SlimeProperties.DIFFICULTY, "normal");
            properties.setValue(SlimeProperties.WORLD_TYPE, "FLAT");
            properties.setValue(SlimeProperties.ALLOW_ANIMALS, true);
            properties.setValue(SlimeProperties.ALLOW_MONSTERS, true);
        }
        return properties;
    }

    @Override
    public @NotNull String getName() {
        return "Advanced Slime World Manager";
    }

    private record FileLoader(File worldDir) implements SlimeLoader {

            private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");

            private FileLoader(File worldDir) {
                this.worldDir = worldDir;

                if (worldDir.exists() && !worldDir.isDirectory()) {
                    OpenCreative.getPlugin().getLogger().warning("A file named " + worldDir.getName() + " has been deleted, as this is the name used for the worlds directory.");
                    if (!worldDir.delete())
                        throw new IllegalStateException("Failed to delete the file named '" + worldDir.getName() + "'.");
                }

                if (!worldDir.exists() && !worldDir.mkdirs())
                    throw new IllegalStateException("Failed to create the worlds directory.");
            }

            @Override
            public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
                if (!worldExists(worldName)) {
                    throw new UnknownWorldException(worldName);
                }

                try (FileInputStream fis = new FileInputStream(new File(worldDir, "world.slime"))) {
                    return fis.readAllBytes();
                }
            }

            @Override
            public boolean worldExists(String worldName) {
                return new File(worldDir, "world.slime").exists();
            }

            @Override
            public List<String> listWorlds() throws NotDirectoryException {
                String[] worlds = worldDir.list(WORLD_FILE_FILTER);

                if (worlds == null) {
                    throw new NotDirectoryException(worldDir.getPath());
                }

                return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
            }

            @Override
            public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
                try (FileOutputStream fos = new FileOutputStream(new File(worldDir, "world.slime"))) {
                    fos.write(serializedWorld);
                }
            }

            @Override
            public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
                if (!worldExists(worldName)) {
                    throw new UnknownWorldException(worldName);
                } else {
                    if (!new File(worldDir, "world.slime").delete()) {
                        throw new IOException("Failed to delete the world file. File#delete() returned false.");
                    }
                }
            }
        }

}
