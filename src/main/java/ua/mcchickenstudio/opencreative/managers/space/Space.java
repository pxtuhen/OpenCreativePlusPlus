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

package ua.mcchickenstudio.opencreative.managers.space;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.planet.PlanetDeletionEvent;
import ua.mcchickenstudio.opencreative.events.planet.PlanetRegisterEvent;
import ua.mcchickenstudio.opencreative.events.planet.PlanetSharingChangeEvent;
import ua.mcchickenstudio.opencreative.events.planet.PlanetCreationEvent;
import ua.mcchickenstudio.opencreative.managers.Startable;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.wanders.OfflineWander;
import ua.mcchickenstudio.opencreative.wanders.Wander;
import ua.mcchickenstudio.opencreative.menus.world.WorldMenu;
import ua.mcchickenstudio.opencreative.planets.DevPlanet;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.utils.ErrorUtils;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerUtils;
import ua.mcchickenstudio.opencreative.utils.world.generators.WorldGenerator;
import ua.mcchickenstudio.opencreative.utils.world.generators.WorldTemplate;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.*;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.createWorldSettings;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isPlanet;

public final class Space implements PlanetsManager, Startable {

    private final Int2ObjectMap<Planet> planets = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Planet> corruptedPlanets = new Int2ObjectOpenHashMap<>();
    private final Object planetsLock = new Object();

    @Override
    public void start() {
        FileUtils.loadPlanets();
    }

    @Override
    public void shutdown() {
        ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").info("Выгрузка миров...");
        try {
            for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                if (planet.isLoaded()) {
                    ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Unloading planet " + planet.getId() + "...");
                    planet.getTerritory().unload();
                } else if (planet.getDevPlanet().isLoaded()) {
                    ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Unloading planet dev " + planet.getId() + "...");
                    planet.getDevPlanet().unload(false);
                }
            }
            OpenCreative.getPlanetsManager().getPlanets().clear();
        } catch (Exception error) {
            sendCriticalErrorMessage("Error while unloading worlds.", error);
        }
        try {
            OpenCreative.getSettings().getDeprecatedWorldsSettings().cancelTask();
        } catch (Exception ignored) {
        }
        planets.clear();
    }

    @Override
    public boolean isWorking() {
        return true;
    }

    @Override
    public @NotNull Set<Planet> getPlanets() {
        return new HashSet<>(planets.values());
    }

    @Override
    public @NotNull Set<Planet> getCorruptedPlanets() {
        return new HashSet<>(corruptedPlanets.values());
    }

    @Override
    public void registerPlanet(@NotNull Planet planet) {
        try {
            synchronized (planetsLock) {
                if (planet.isCorrupted()) {
                    corruptedPlanets.put(planet.getId(), planet);
                } else {
                    planets.put(planet.getId(), planet);
                }
            }
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                new PlanetRegisterEvent(planet).callEvent();
            });
        } catch (Exception error) {
            sendCriticalErrorMessage("Failed to register planet " + planet.getId(), error);
        }
    }

    @Override
    public void unregisterPlanet(@NotNull Planet planet) {
        planets.remove(planet.getId());
        corruptedPlanets.remove(planet.getId());
        clearOnceMessages(planet);
    }

    @Override
    public void createPlanet(@NotNull Player owner, int id, @NotNull WorldGenerator generator) {
        createPlanet(owner, id, generator, World.Environment.NORMAL, new Random().nextInt(), false, "");
    }

    @Override
    public void createPlanet(@NotNull Player owner, int id, @NotNull WorldTemplate template) {
        Wander wander = OpenCreative.getWander(owner);
        wander.setConnectingToPlanet(true);
        owner.showTitle(Title.title(
                toComponent(getLocaleMessage("creating-world.title")), toComponent(getLocaleMessage("creating-world.subtitle")),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(30), Duration.ofSeconds(2))
        ));
        long startTime = System.currentTimeMillis();
        OpenCreative.getPlugin().getLogger().info("Copying template folder (" + template.getID() + ") for new planet " + id + " by " + owner.getName() + "...");
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            File worldTemplateFolder = new File(OpenCreative.getPlugin().getDataPath()
                    + File.separator + "templates" + File.separator + template.getFolderName());
            if (!worldTemplateFolder.exists() || !worldTemplateFolder.isDirectory()) {
                sendPlayerErrorMessage(owner, "Failed to create world by template " + template.getID() + ", because folder doesn't exist.");
                sendCriticalErrorMessage("Failed to create world for planet " + id + " by " + owner.getName() + ". Folder " + template.getFolderName()
                        + " doesn't exist, or it's not directory.");
                wander.setConnectingToPlanet(false);
                return;
            }
            File devTemplateFolder = new File(worldTemplateFolder.getPath() + "dev");
            File planetFolder = new File(Bukkit.getWorldContainer().getPath() + File.separator + "planets" + File.separator + "planet" + id + File.separator);
            File planetDevFolder = new File(planetFolder.getPath() + "dev");
            FileUtils.copyFilesToDirectory(worldTemplateFolder, planetFolder);
            if (devTemplateFolder.exists() && devTemplateFolder.isDirectory()) {
                FileUtils.copyFilesToDirectory(devTemplateFolder, planetDevFolder);
            }
            if (!owner.isOnline()) {
                FileUtils.deleteFolder(planetFolder);
                return;
            }
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                wander.setConnectingToPlanet(false);
                owner.showTitle(Title.title(
                        toComponent(getLocaleMessage("creating-world.title")), toComponent(getLocaleMessage("creating-world.subtitle")),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(30), Duration.ofSeconds(2))
                ));
                ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").info("Создаётся мир " + id + " (владелец " + owner.getName() + ")...");

                createWorldSettings(id, owner, World.Environment.NORMAL, template.getID());
                Planet planet = new Planet(id);

                CompletableFuture<World> worldCreation = planet.getTerritory().generateWorld(template, World.Environment.NORMAL,
                        0, false, "");
                worldCreation.thenAccept(world -> {
                    Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                        long endTime = System.currentTimeMillis();
                        ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").info("Мир " + id + " создан за " + (endTime - startTime) + " мс");
                        new PlanetCreationEvent(planet, owner, template, World.Environment.NORMAL, 0, false).callEvent();
                        planet.connectPlayer(owner);
                    });
                }).exceptionally(error -> {
                    String errorMessage = (error.getMessage() == null ? "Failed to create the world" : error.getMessage());
                    owner.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                            owner, "id", id, "wasloaded", false,
                            "phase", "Creation", "loaded", false, "error", errorMessage)
                            .clickEvent(ClickEvent.suggestCommand(errorMessage))
                            .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                    sendCriticalErrorMessage("Failed to create the world " + id, error);
                    Sounds.PLAYER_ERROR.play(owner);
                    return null;
                });
            });
        });
    }

    @Override
    public void createPlanet(@NotNull Player owner, int id, @NotNull WorldGenerator generator,
                             World.@NotNull Environment environment, long seed, boolean generateStructures,
                             @NotNull String biome) {
        long startTime = System.currentTimeMillis();

        owner.showTitle(Title.title(
                toComponent(getLocaleMessage("creating-world.title")), toComponent(getLocaleMessage("creating-world.subtitle")),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(30), Duration.ofSeconds(2))
        ));
        ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").info("Создаётся мир " + id + " (владелец " + owner.getName() + ")...");

        createWorldSettings(id, owner, environment, generator.getID());
        Planet planet = new Planet(id);

        CompletableFuture<World> worldCreation = planet.getTerritory().generateWorld(generator, environment, seed, generateStructures, biome);
        worldCreation.thenAccept(world -> {
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                long endTime = System.currentTimeMillis();
                ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").info("Мир " + id + " создан за " + (endTime - startTime) + " мс");
                new PlanetCreationEvent(planet, owner, generator, environment, seed, generateStructures).callEvent();
                planet.connectPlayer(owner);
            });
        }).exceptionally(error -> {
            String errorMessage = (error.getMessage() == null ? "Failed to create the world" : error.getMessage());
            owner.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                    owner, "id", id, "wasloaded", false,
                    "phase", "Creation", "loaded", false, "error", errorMessage)
                    .clickEvent(ClickEvent.suggestCommand(errorMessage))
                    .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
            sendCriticalErrorMessage("Failed to create the world " + id, error);
            Sounds.PLAYER_ERROR.play(owner);
            return null;
        });
    }

    public @NotNull Set<Planet> getPlanetsByOwner(@NotNull Player player) {
        return getPlanetsByOwner(player.getName());
    }

    private void deleteFilesWhenUnloaded(@NotNull Planet planet, int attempt) {
        boolean unloaded = Bukkit.getWorld(planet.getWorldName()) == null
                && Bukkit.getWorld(planet.getDevPlanet().getWorldName()) == null;
        if (!unloaded && attempt < 10) {
            if (Bukkit.getWorld(planet.getWorldName()) != null) {
                OpenCreative.getWorldManager().unloadWorld(Bukkit.getWorld(planet.getWorldName()), false, planet);
            }
            if (Bukkit.getWorld(planet.getDevPlanet().getWorldName()) != null) {
                OpenCreative.getWorldManager().unloadWorld(Bukkit.getWorld(planet.getDevPlanet().getWorldName()), false, planet);
            }
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> deleteFilesWhenUnloaded(planet, attempt + 1), 20L);
            return;
        }
        if (!unloaded) {
            ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").warn("Мир " + planet.getId()
                    + " не выгрузился за 10 секунд, его файлы не удалены — удали папку мира вручную.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            FileUtils.deleteFolder(FileUtils.getPlanetFolder(planet));
            FileUtils.deleteFolder(FileUtils.getDevPlanetFolder(planet.getDevPlanet()));
            FileUtils.deleteWorldFoldersInPlugins(planet.getId());
        });
    }

    @Override
    public boolean deletePlanet(@NotNull Planet planet) {
        new PlanetDeletionEvent(planet).callEvent();
        try {
            planet.getTerritory().setIgnoreUnloading(true);
            for (Player p : planet.getPlayers()) {
                PlayerUtils.teleportToLobby(p);
                if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof WorldMenu) {
                    p.closeInventory();
                }
            }
            PlanetSharingChangeEvent planetEvent = new PlanetSharingChangeEvent(planet, planet.getSharing(), Planet.Sharing.PUBLIC);
            planetEvent.callEvent();
            if (!planetEvent.isCancelled()) {
                planet.setSharing(Planet.Sharing.CLOSED);
            }
            unregisterPlanet(planet);
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
                if (planet.isLoaded()) {
                    OpenCreative.getWorldManager().unloadWorld(planet.getWorld(), false, planet);
                }
                if (planet.getDevPlanet().isLoaded()) {
                    OpenCreative.getWorldManager().unloadWorld(planet.getDevPlanet().getWorld(), false, planet);
                }
                // OpenCreative++: stop runnables, boss bars, scoreboards of the world and delete files only after
                // both worlds are really unloaded (Bukkit refuses to unload while players are still there).
                try {
                    planet.getTerritory().clearData();
                } catch (Exception ignored) {
                }
                deleteFilesWhenUnloaded(planet, 0);
            }, 5L);
            return true;
        } catch (Exception error) {
            planet.getTerritory().setIgnoreUnloading(false);
            ErrorUtils.sendCriticalErrorMessage("Error while deleting world " + planet.getId(), error);
            return false;
        }
    }

    @Override
    public @NotNull List<Planet> getDeprecatedPlanets(int months) {
        if (months < 1) months = 1;
        long currentTime = System.currentTimeMillis();
        long monthsInMillis = 2592000000L * months;
        List<Planet> deprecatedWorlds = new ArrayList<>();
        List<Planet> recommended = getRecommendedPlanets();
        for (Planet planet : getPlanets()) {
            if (currentTime - planet.getCreationTime() > monthsInMillis && !recommended.contains(planet)) {
                OfflinePlayer planetOwner = Bukkit.getOfflinePlayer(planet.getOwner());
                if (!planetOwner.isOnline()) {
                    long lastSeen = Math.max(planetOwner.getLastSeen(), planetOwner.getLastLogin());
                    if (lastSeen == 0 || currentTime - lastSeen > monthsInMillis) {
                        deprecatedWorlds.add(planet);
                    }
                }
            }
        }
        return deprecatedWorlds;
    }

    @Override
    public @NotNull CompletableFuture<Integer> deleteDeprecatedPlanets(int months) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            List<Planet> deprecated = getDeprecatedPlanets(months);
            if (deprecated.isEmpty()) {
                future.complete(0);
                return;
            }
            long totalSize = 0L;
            for (Planet planet : deprecated) {
                totalSize += FileUtils.getFolderSize(FileUtils.getPlanetFolder(planet));
                totalSize += FileUtils.getFolderSize(FileUtils.getDevPlanetFolder(planet.getDevPlanet()));
            }
            double totalSizeMB = totalSize / (1024.0 * 1024.0);
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                int deleted = 0;
                for (Planet planet : deprecated) {
                    if (deletePlanet(planet)) {
                        deleted++;
                    }
                }
                OpenCreative.getPlugin().getLogger().info("Auto-deleted " + deleted + " deprecated worlds (" +
                        String.format(Locale.US, "%.2f", totalSizeMB) + " MB freed).");
                future.complete(deleted);
            });
        });
        return future;
    }

    @Override
    public @NotNull List<Planet> getRecommendedPlanets() {
        List<Planet> featuredPlanets = new ArrayList<>();
        Set<Integer> featuredIds = OpenCreative.getSettings().getRecommendedWorldsIDs();
        for (int id : featuredIds) {
            Planet planet = getPlanetById(String.valueOf(id));
            if (planet != null) {
                featuredPlanets.add(planet);
            }
        }
        return featuredPlanets;
    }

    @Override
    public @NotNull Set<Planet> getFavoritePlanets(@NotNull OfflineWander wander) {
        Set<Planet> favoritePlanets = new LinkedHashSet<>();
        Set<Integer> favoriteIds = wander.getFavoriteWorlds();
        for (int id : favoriteIds) {
            Planet planet = getPlanetById(String.valueOf(id));
            if (planet != null) {
                favoritePlanets.add(planet);
            }
        }
        return favoritePlanets;
    }

    public @NotNull Set<Planet> getPlanetsContainingName(@NotNull String worldName) {
        Set<Planet> foundPlanets = new HashSet<>();
        for (Planet planet : planets.values()) {
            if (planet.getInformation().getDisplayName().toLowerCase().contains(worldName.toLowerCase())) {
                foundPlanets.add(planet);
            }
        }
        return foundPlanets;
    }

    @Override
    public @NotNull Set<Planet> getPlanetsContainingID(@NotNull String worldID) {
        Set<Planet> foundPlanets = new HashSet<>();
        for (Planet planet : planets.values()) {
            if (planet.getInformation().getCustomID().toLowerCase().contains(worldID.toLowerCase())) {
                foundPlanets.add(planet);
            }
        }
        return foundPlanets;
    }

    @Override
    public @NotNull Set<Planet> getPlanetsByOwner(@NotNull String owner) {
        Set<Planet> foundPlanets = new HashSet<>();
        for (Planet planet : planets.values()) {
            if (planet.isOwner(owner)) {
                foundPlanets.add(planet);
            }
        }
        return foundPlanets;
    }

    @Override
    public Planet getPlanetByPlayer(@NotNull Player player) {
        World world = player.getWorld();
        if (!isPlanet(world)) return null;
        String worldID = world.getName()
                .replace("./planets/planet", "")
                .replace("dev", "");
        return getPlanetById(worldID);
    }

    @Override
    public DevPlanet getDevPlanet(@NotNull Player player) {
        if (!isDevPlanet(player.getWorld())) return null;
        Planet planet = getPlanetByPlayer(player);
        return planet != null ? planet.getDevPlanet() : null;
    }

    @Override
    public DevPlanet getDevPlanet(@NotNull World world) {
        if (!isDevPlanet(world)) return null;
        String worldID = world.getName()
                .replace("./planets/planet", "")
                .replace("dev", "");
        Planet planet = getPlanetById(worldID);
        if (planet == null) return null;
        return planet.getDevPlanet();
    }

    @Override
    public Planet getPlanetByWorld(@NotNull World world) {
        if (!isPlanet(world) && !isDevPlanet(world)) return null;
        int id = getNumberFromWorldName(world.getName());
        if (id == -1) return null;
        Planet planet = planets.get(id);
        if (planet == null) {
            planet = corruptedPlanets.get(id);
        }
        return planet;
    }

    @Override
    public Planet getPlanetByWorldName(@NotNull String worldName) {
        if (!isPlanet(worldName) && !isDevPlanet(worldName)) return null;
        int id = getNumberFromWorldName(worldName);
        if (id == -1) return null;
        Planet planet = planets.get(id);
        if (planet == null) {
            planet = corruptedPlanets.get(id);
        }
        return planet;
    }

    @Override
    public Planet getPlanetById(@NotNull String id) {
        try {
            int planetId = Integer.parseInt(id);
            return planets.get(planetId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int getNumberFromWorldName(@NotNull String name) {
        int start = 16; // ./planets/planet
        int end = name.endsWith("dev")
                ? name.length() - 3 // dev
                : name.length();
        int id = 0;
        for (int i = start; i < end; i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            id = id * 10 + (c - '0');
        }
        return id;
    }

    @Override
    public @Nullable Planet getPlanetByAnyID(@NotNull String id) {
        Planet found = getPlanetById(id);
        if (found != null) return found;
        return getPlanetByCustomID(id);
    }

    @Override
    public Planet getPlanetByCustomID(@NotNull String customID) {
        for (Planet planet : planets.values()) {
            if (planet.getInformation().getCustomID().equalsIgnoreCase(customID)) {
                return planet;
            }
        }
        return null;
    }

    @Override
    public boolean isStableConnection() {
        // True, because it depends only on file system
        return true;
    }

    @Override
    public @NotNull String getName() {
        return "Planet Manager";
    }
}
