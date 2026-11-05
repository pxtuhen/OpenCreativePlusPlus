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

package ua.mcchickenstudio.opencreative.planets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionCategory;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.ExecutorCategory;
import ua.mcchickenstudio.opencreative.coding.menus.layouts.Layout;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.items.ItemsGroup;
import ua.mcchickenstudio.opencreative.utils.MessageUtils;
import ua.mcchickenstudio.opencreative.utils.world.DevPlanetChunkGenerator;
import ua.mcchickenstudio.opencreative.utils.world.cache.ChunkCache;
import ua.mcchickenstudio.opencreative.utils.world.platforms.DevPlatformer;
import ua.mcchickenstudio.opencreative.utils.world.platforms.DevPlatformers;
import ua.mcchickenstudio.opencreative.utils.world.platforms.HasVisibleBorder;
import ua.mcchickenstudio.opencreative.wanders.Wander;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ua.mcchickenstudio.opencreative.utils.BlockUtils.getSignLine;
import static ua.mcchickenstudio.opencreative.utils.BlockUtils.isOutOfBorders;
import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.*;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.getDevPlanetFolder;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.getPlanetConfig;
import static ua.mcchickenstudio.opencreative.utils.ItemUtils.getItemType;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.*;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.setGameRuleIfExists;

/**
 * <h1>DevPlanet</h1>
 * This class represents developer's world, where players
 * can edit and change code with blocks on platform.
 * <p>
 * Platform consists of white, blue and gray stained-glass,
 * and it can't be destroyed. Players can place chests,
 * shulkers, signs and anvils on white stained-glass.
 * On blue stained-glass players should place executor blocks,
 * on gray stained-glass - actions and conditions.
 * </p>
 */
public class DevPlanet {

    private final static Material DEFAULT_EVENT_MATERIAL = Material.BLUE_STAINED_GLASS;
    private final static Material DEFAULT_ACTION_MATERIAL = Material.GRAY_STAINED_GLASS;
    private final static Material DEFAULT_FLOOR_MATERIAL = Material.WHITE_STAINED_GLASS;
    private final Planet planet;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<Location, Layout> openedBlocksMenus = new HashMap<>();
    private final Map<UUID, Set<Location>> selectedExecutors = new HashMap<>();
    private final Set<Location> changedColumns = new HashSet<>();

    private String platformerID = "";
    private Material signMaterial = Material.BIRCH_WALL_SIGN;
    private Material containerMaterial = Material.CHEST;
    private boolean dropItems = true;
    private boolean saveLocation = true;
    private boolean nightVision = true;
    private boolean currentlySavingCode = false;
    private UUID worldID;

    /**
     * Constructor of developer planet, that
     * loads settings from config.
     *
     * @param planet planet, that owns this developer planet.
     */
    public DevPlanet(@NotNull Planet planet) {
        this.planet = planet;
    }

    /**
     * Returns default action block material,
     * used for creating new coding platforms.
     * <p>
     * This material must be solid, so players
     * will be able to place action blocks on it.
     *
     * @return default action block material.
     */
    public static @NotNull Material getDefaultActionMaterial() {
        return DEFAULT_ACTION_MATERIAL;
    }

    /**
     * Returns default event block material,
     * used for creating new coding platforms.
     * <p>
     * This material must be solid, so players
     * will be able to place event blocks on it.
     *
     * @return default event block material.
     */
    public static @NotNull Material getDefaultEventMaterial() {
        return DEFAULT_EVENT_MATERIAL;
    }

    /**
     * Returns default floor block material,
     * used for creating new coding platforms.
     * <p>
     * This material must be solid, so players
     * will be able to place allowed blocks on it.
     *
     * @return default floor block material.
     */
    public static @NotNull Material getDefaultFloorMaterial() {
        return DEFAULT_FLOOR_MATERIAL;
    }

    /**
     * Loads settings of developer planet.
     */
    private void loadInformation() {
        FileConfiguration config = planet.getConfiguration().getConfig();
        try {
            containerMaterial = Material.getMaterial(config.getString("dev.container", "CHEST"));
            if (containerMaterial == null || !containerMaterial.isBlock()) {
                containerMaterial = Material.CHEST;
            }
        } catch (Exception ignored) {
        }
        try {
            signMaterial = Material.getMaterial(config.getString("dev.sign", "BIRCH_WALL_SIGN"));
            if (signMaterial == null || !signMaterial.isBlock()) {
                signMaterial = Material.BIRCH_WALL_SIGN;
            }
        } catch (Exception ignored) {
        }
        dropItems = config.getBoolean("dev.drops", true);
        saveLocation = config.getBoolean("dev.save-location", true);
        nightVision = config.getBoolean("dev.night-vision", true);
        platformerID = config.getString("dev.platformer", "");
    }

    /**
     * Loads developer's world and setups it.
     */
    public @NotNull CompletableFuture<World> load() {
        CompletableFuture<World> future = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        boolean existed = this.exists();
        WorldCreator creator = new WorldCreator(this.getWorldName())
                .type(WorldType.FLAT)
                .generator(new DevPlanetChunkGenerator());
        PlanetTerritory.fixFlatSettings(creator);
        CompletableFuture<World> worldProcess;
        if (existed) {
            worldProcess = OpenCreative.getWorldManager().loadWorld(creator, planet);
        } else {
            worldProcess = OpenCreative.getWorldManager().createWorld(creator, planet);
        }
        worldProcess.thenAccept(world -> {
            if (world == null) {
                sendCriticalErrorMessage("Failed to load Dev planet world " + planet.getId());
                future.complete(null);
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
                loadInformation();
                List<String> savedChanges = planet.getConfiguration().getConfig().getStringList("changed-code-columns");
                if (!savedChanges.isEmpty()) {
                    for (String saved : savedChanges) {
                        String[] coords = saved.split(" ");
                        if (coords.length != 3) continue;
                        try {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int z = Integer.parseInt(coords[2]);
                            changedColumns.add(new Location(world, x, y, z));
                        } catch (Exception ignored) {
                        }
                    }
                }
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    if (existed) {
                        if (world.getBlockAt(4, 0, 4).isEmpty()) {
                            createPlatform(1, 1);
                        }
                    } else {
                        createPlatform(1, 1);
                        world.setTime(12500);
                    }
                    setupWorld(world);
                    long endTime = System.currentTimeMillis();
                    ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Dev planet world " + planet.getId() + " loaded in " + (endTime - startTime) + " ms");
                    future.complete(world);
                });
            });
        }).exceptionally(error -> {
            sendCriticalErrorMessage("Failed to load dev planet " + planet.getId());
            future.completeExceptionally(error);
            return null;
        });
        return future;
    }

    /**
     * Unloads developer's world and teleports
     * all players in it to lobby.
     */
    public @NotNull CompletableFuture<Void> unload() {
        return unload(OpenCreative.getPlugin().isEnabled());
    }

    /**
     * Unloads developer's world and teleports
     * all players in it to lobby.
     *
     * @param asyncSave true - will save world later, false - immediately.
     */
    public @NotNull CompletableFuture<Void> unload(boolean asyncSave) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        World world = getWorld();

        changedColumns.clear();
        if (world == null) {
            future.complete(null);
            return future;
        }

        long startTime = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                teleportToLobby(player);
            }
        }
        if (asyncSave) {
            for (Chunk chunk : world.getLoadedChunks()) {
                world.unloadChunk(chunk.getX(), chunk.getZ(), true);
            }
            try {
                // 1.21+ Content:
                Method saveMethod = world.getClass().getMethod("save", boolean.class);
                saveMethod.invoke(world, false);
            } catch (Exception ignored) {
                world.save();
            }
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
                OpenCreative.getWorldManager().unloadWorld(world, false, planet).whenComplete((result, error) -> {
                    ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Dev planet world " + planet.getId()
                            + " unloaded in " + (System.currentTimeMillis() - startTime) + " ms");
                    worldID = null;
                    future.complete(null);
                });
            }, 40);
        } else {
            OpenCreative.getWorldManager().unloadWorld(world, true, planet).whenComplete((result, error) -> {
                ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Dev planet world " + planet.getId()
                        + " unloaded in " + (System.currentTimeMillis() - startTime) + " ms");
                worldID = null;
                future.complete(null);
            });
        }
        return future;
    }

    /**
     * Setups developer's world, changes spawn location,
     * sets game rules and world border.
     */
    public void setupWorld(@NotNull World world) {
        world.setSpawnLocation(2, 1, 2);
        world.setGameRule(GameRule.DO_LIMITED_CRAFTING, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        setGameRuleIfExists(world, "spawn_mobs", false);
        setGameRuleIfExists(world, "spawn_monsters", false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.GLOBAL_SOUND_EVENTS, false);
        getDevPlatformer().setWorldBorder(this);
    }

    /**
     * Checks whether developer planet was generated before.
     *
     * @return true - exists, false - not created yet.
     */
    public boolean exists() {
        File folder = getDevPlanetFolder(this);
        return folder.exists() && folder.isDirectory();
    }

    public @NotNull Set<Material> getAllCodingBlocksForPlacing() {
        Set<Material> allBlocks = new HashSet<>();
        allBlocks.addAll(getEventsBlocks());
        allBlocks.addAll(getActionsBlocks());
        return allBlocks;
    }

    public @NotNull Set<Material> getEventsBlocks() {
        return new HashSet<>(Arrays.stream(ExecutorCategory.values()).map(ExecutorCategory::getBlock).toList());
    }

    public @NotNull Set<Material> getActionsBlocks() {
        return new HashSet<>(Arrays.stream(ActionCategory.values()).map(ActionCategory::getBlock).toList());
    }

    /**
     * Creates a coding platform with specified X and Z of platform.
     * <p>
     * By default, it generates floor with white stained-glass,
     * and fills executor sections with blue stained-glass,
     * action sections with gray stained-glass.</p>
     *
     * @param platformX X number of platform.
     * @param platformZ Z number of platform.
     * @return true - if successfully created, false - if failed.
     */
    public boolean createPlatform(int platformX, int platformZ) {
        if (platformX >= 30 || platformZ >= 30 || platformX <= 0 || platformZ <= 0) {
            return false;
        }
        getDevPlatformer().buildPlatform(new DevPlatform(getWorld(), getDevPlatformer(), platformX, platformZ),
                DEFAULT_FLOOR_MATERIAL, DEFAULT_EVENT_MATERIAL, DEFAULT_ACTION_MATERIAL);
        return true;
    }

    /**
     * Claims new platform and teleports player to it.
     *
     * @param platform platform to claim.
     * @param player   player, who will be teleported.
     * @return true - claimed coding platform, false - already built and exists.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean claimPlatform(@NotNull DevPlatform platform, @NotNull Player player) {
        if (getDevPlatformer().claimPlatform(this, platform)) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.teleport(platform.getSpawnLocation());
            player.sendMessage(getLocaleMessage("environment.platform.claimed"));
            Sounds.DEV_PLATFORM_CLAIM.play(player);
            Location location = getDevPlatformer().getPlatformBeginLocation(platform);
            Chunk chunk = location.getChunk();
            ChunkCache.load(getWorld(), chunk.getX(), chunk.getZ());
            return true;
        } else {
            return false;
        }
    }

    public @NotNull Set<Material> getIndestructibleBlocks() {
        Set<Material> indestructibleBlocks = new HashSet<>();
        indestructibleBlocks.add(DEFAULT_ACTION_MATERIAL);
        indestructibleBlocks.add(DEFAULT_EVENT_MATERIAL);
        indestructibleBlocks.add(DEFAULT_FLOOR_MATERIAL);
        indestructibleBlocks.add(Material.DIAMOND_ORE);
        indestructibleBlocks.add(Material.GOLD_ORE);
        indestructibleBlocks.add(Material.REDSTONE_ORE);
        indestructibleBlocks.addAll(Arrays.stream(ExecutorCategory.values()).map(ExecutorCategory::getAdditionalBlock).toList());
        indestructibleBlocks.addAll(Arrays.stream(ActionCategory.values()).map(ActionCategory::getAdditionalBlock).toList());
        indestructibleBlocks.remove(Material.PISTON);
        return indestructibleBlocks;
    }

    public @NotNull Set<Material> getAllowedBlocks() {
        Set<Material> allowedBlocks = new HashSet<>();
        allowedBlocks.add(Material.LANTERN);
        allowedBlocks.add(Material.JACK_O_LANTERN);
        allowedBlocks.add(Material.SOUL_LANTERN);
        allowedBlocks.add(Material.TORCH);
        allowedBlocks.add(Material.SOUL_TORCH);
        allowedBlocks.add(Material.BARREL);
        allowedBlocks.add(Material.OAK_SIGN);
        allowedBlocks.add(Material.SPRUCE_SIGN);
        allowedBlocks.add(Material.ACACIA_SIGN);
        allowedBlocks.add(Material.BAMBOO_SIGN);
        allowedBlocks.add(Material.JUNGLE_SIGN);
        allowedBlocks.add(Material.CHERRY_SIGN);
        allowedBlocks.add(Material.WARPED_SIGN);
        allowedBlocks.add(Material.CRIMSON_SIGN);
        allowedBlocks.add(Material.MANGROVE_SIGN);
        allowedBlocks.add(Material.DARK_OAK_SIGN);
        allowedBlocks.add(Material.BIRCH_SIGN);
        allowedBlocks.add(Material.CRAFTING_TABLE);
        allowedBlocks.add(Material.JUKEBOX);
        allowedBlocks.add(Material.STONECUTTER);
        allowedBlocks.add(Material.CARTOGRAPHY_TABLE);
        allowedBlocks.add(Material.SMITHING_TABLE);
        allowedBlocks.add(Material.LOOM);
        allowedBlocks.add(Material.GRINDSTONE);
        allowedBlocks.add(Material.CHEST);
        allowedBlocks.add(Material.ANVIL);
        allowedBlocks.add(Material.CHIPPED_ANVIL);
        allowedBlocks.add(Material.DAMAGED_ANVIL);
        allowedBlocks.add(Material.ENDER_CHEST);
        allowedBlocks.add(Material.SHULKER_BOX);
        allowedBlocks.add(Material.WHITE_SHULKER_BOX);
        allowedBlocks.add(Material.BLACK_SHULKER_BOX);
        allowedBlocks.add(Material.BLUE_SHULKER_BOX);
        allowedBlocks.add(Material.BROWN_SHULKER_BOX);
        allowedBlocks.add(Material.CYAN_SHULKER_BOX);
        allowedBlocks.add(Material.MAGENTA_SHULKER_BOX);
        allowedBlocks.add(Material.GRAY_SHULKER_BOX);
        allowedBlocks.add(Material.GREEN_SHULKER_BOX);
        allowedBlocks.add(Material.LIME_SHULKER_BOX);
        allowedBlocks.add(Material.RED_SHULKER_BOX);
        allowedBlocks.add(Material.ORANGE_SHULKER_BOX);
        allowedBlocks.add(Material.PURPLE_SHULKER_BOX);
        allowedBlocks.add(Material.YELLOW_SHULKER_BOX);
        allowedBlocks.add(Material.LIGHT_BLUE_SHULKER_BOX);
        allowedBlocks.add(Material.LIGHT_GRAY_SHULKER_BOX);
        allowedBlocks.add(Material.PINK_SHULKER_BOX);
        // 1.21+ Content:
        Optional.ofNullable(Material.matchMaterial("PALE_OAK_SIGN"))
                .ifPresent(allowedBlocks::add);
        return allowedBlocks;
    }

    public @NotNull List<Location> getPlacedExecutors(ExecutorCategory category) {
        List<Location> locations = new ArrayList<>();
        for (DevPlatform platform : getPlatforms()) {
            locations.addAll(platform.getPlacedExecutors(category));
        }
        return locations;
    }

    public @NotNull List<Location> getPlacedFunctions() {
        List<Location> locations = new ArrayList<>();
        for (Location location : getPlacedExecutors(ExecutorCategory.FUNCTION)) {
            Block block = location.getBlock();
            String line = getSignLine(block.getRelative(BlockFace.SOUTH).getLocation(), (byte) 3);
            if (line != null && !line.isEmpty()) {
                locations.add(block.getLocation());
            }
        }
        return locations;
    }

    public @NotNull List<Location> getPlacedMethods() {
        List<Location> locations = new ArrayList<>();
        for (Location location : getPlacedExecutors(ExecutorCategory.METHOD)) {
            Block block = location.getBlock();
            String line = getSignLine(block.getRelative(BlockFace.SOUTH).getLocation(), (byte) 3);
            if (line != null && !line.isEmpty()) {
                locations.add(block.getLocation());
            }
        }
        return locations;
    }

    public void updateContainers() {
        if (!isLoaded()) return;
        for (DevPlatform platform : getPlatforms()) {
            platform.setContainerMaterial(containerMaterial);
        }
    }

    public void updateSigns() {
        if (!isLoaded()) return;
        for (DevPlatform platform : getPlatforms()) {
            platform.setSignMaterial(signMaterial);
        }
    }

    /**
     * Returns unique ID of loaded developer world.
     *
     * @return uuid of world, null - if not loaded.
     */
    public @Nullable UUID getWorldUUID() {
        return worldID;
    }

    /**
     * Sets a unique ID of loaded developer world.
     *
     * @param uuid uuid of world, null - if not loaded.
     */
    public void setWorld(@Nullable UUID uuid) {
        worldID = uuid;
    }

    /**
     * Checks whether developer world is loaded.
     *
     * @return true - is loaded, false - unloaded.
     */
    public boolean isLoaded() {
        return getWorld() != null;
    }

    public @Nullable Layout getOpenedMenu(@NotNull Location location) {
        return openedBlocksMenus.get(location);
    }

    public void registerOpenedMenu(@NotNull Location location, @NotNull Layout menu) {
        openedBlocksMenus.put(location, menu);
    }

    public void unregisterOpenedMenu(@NotNull Location location) {
        openedBlocksMenus.remove(location);
    }

    public @NotNull Material getContainerMaterial() {
        return containerMaterial;
    }

    public @NotNull Material getSignMaterial() {
        return signMaterial;
    }

    public boolean isNightVision() {
        return nightVision;
    }

    public void setNightVision(boolean nightVision) {
        this.nightVision = nightVision;
        planet.getConfiguration().set("dev.night-vision", nightVision);
    }

    public boolean isSaveLocation() {
        return saveLocation;
    }

    public void setSaveLocation(boolean saveLocation) {
        this.saveLocation = saveLocation;
        planet.getConfiguration().set("dev.save-location", saveLocation);
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
        planet.getConfiguration().set("dev.drops", dropItems);
    }

    public void setPlatformerID(@NotNull String platformer) {
        this.platformerID = platformer;
        planet.getConfiguration().set("dev.platformer", platformerID);
    }

    public boolean setContainerMaterial(Material containerMaterial) {
        if (containerMaterial == Material.BARREL || containerMaterial == Material.CHEST || containerMaterial.name().endsWith("SHULKER_BOX")) {
            this.containerMaterial = containerMaterial;
            planet.getConfiguration().set("dev.container", containerMaterial.name());
            return true;
        }
        return false;
    }

    public boolean setSignMaterial(Material signMaterial) {
        // 1.21+ Content:
        Material paleSign = Material.PALE_OAK_WALL_SIGN; // OpenCreative++: /env passes wall signs, PALE_OAK_SIGN never matched
        if (signMaterial == Material.OAK_WALL_SIGN || signMaterial == Material.ACACIA_WALL_SIGN ||
                signMaterial == Material.BAMBOO_WALL_SIGN || signMaterial == Material.CHERRY_WALL_SIGN ||
                signMaterial == Material.BIRCH_WALL_SIGN || signMaterial == Material.JUNGLE_WALL_SIGN ||
                (paleSign != null && signMaterial == paleSign)) {
            this.signMaterial = signMaterial;
            planet.getConfiguration().set("dev.sign", signMaterial.name());
            return true;
        }
        return false;
    }

    public @NotNull String getWorldName() {
        return planet.getWorldName() + "dev";
    }

    /**
     * Returns list of existing coding platforms, that
     * can be used to place coding blocks.
     *
     * @return list of developer platforms.
     */
    public @NotNull List<DevPlatform> getPlatforms() {
        return getDevPlatformer().getPlatforms(this);
    }

    /**
     * Returns coding platform by location.
     *
     * @param location location to get platform.
     * @return coding platform - if location contains coding platform, otherwise - null.
     */
    public @Nullable DevPlatform getPlatformInLocation(@NotNull Location location) {
        return getDevPlatformer().getPlatformInLocation(this, location);
    }

    /**
     * Changes world border for all players
     * inside developer world, used when some
     * player joins the developer world.
     */
    public void displayWorldBorders() {
        if (!isLoaded()) return;
        if (getDevPlatformer() instanceof HasVisibleBorder) return;
        for (Player player : getWorld().getPlayers()) {
            WorldBorder border = Bukkit.createWorldBorder();
            border.setCenter(getWorld().getWorldBorder().getCenter());
            border.setSize(getWorld().getWorldBorder().getSize() * 5);
            player.setWorldBorder(border);
        }
    }

    public @NotNull DevPlatformer getDevPlatformer() {
        if (platformerID == null || platformerID.isEmpty()) return OpenCreative.getDevPlatformer();
        DevPlatformer platformer = DevPlatformers.getInstance().getById(platformerID);
        if (platformer == null) return OpenCreative.getDevPlatformer();
        return platformer;
    }

    public @NotNull Map<UUID, Location> getLastLocations() {
        return lastLocations;
    }

    public @NotNull Set<Location> getMarkedExecutors(@NotNull Player player) {
        return selectedExecutors.getOrDefault(player.getUniqueId(), new LinkedHashSet<>());
    }

    /**
     * Adds executor to marked list for player,
     * when they click it with manipulator item.
     *
     * @param player   player, who just marked executor.
     * @param location location of executor block.
     */
    public void markExecutorAsSelected(@NotNull Player player, @NotNull Location location) {
        Set<Location> locations = selectedExecutors.getOrDefault(player.getUniqueId(), new LinkedHashSet<>());
        locations.add(location);
        selectedExecutors.put(player.getUniqueId(), locations);
    }

    /**
     * Removes marked executor for player,
     * who selected it with manipulator item.
     *
     * @param player   player, who marked executor before.
     * @param location location of executor block.
     */
    public void unselectMarkedExecutor(@NotNull Player player, @NotNull Location location) {
        Set<Location> locations = selectedExecutors.getOrDefault(player.getUniqueId(), new LinkedHashSet<>());
        locations.remove(location);
        if (locations.isEmpty()) {
            selectedExecutors.remove(player.getUniqueId());
        } else {
            selectedExecutors.put(player.getUniqueId(), locations);
        }
    }

    /**
     * Removes marked executor for all players,
     * who selected it with manipulator item.
     *
     * @param location location of executor block.
     */
    public void clearMarkedExecutors(@NotNull Location location) {
        for (UUID uuid : new HashSet<>(selectedExecutors.keySet())) {
            Set<Location> locations = selectedExecutors.get(uuid);
            if (locations == null || locations.isEmpty()) continue;
            locations.remove(location);
            selectedExecutors.put(uuid, locations);
        }
    }

    /**
     * Returns whether code was changed after
     * last parsing and saving.
     *
     * @return true - code was changed, false - not changed.
     */
    public boolean isCodeChanged() {
        return !changedColumns.isEmpty();
    }

    /**
     * Checks whether code is currently
     * being saved to file or not.
     *
     * @return true - is busy, false - not.
     */
    public boolean isCurrentlySavingCode() {
        return currentlySavingCode;
    }

    /**
     * Sets the state of saving code.
     * If true, it will disallow to save a code.
     *
     * @param currentlySavingCode true - set busy state, false - allow to save code.
     */
    public void setCurrentlySavingCode(boolean currentlySavingCode) {
        this.currentlySavingCode = currentlySavingCode;
    }

    /**
     * Returns executor location by location.
     *
     * @param location location to get platform.
     * @return executor location - if location is related to actions or its executor location itself.
     */
    public @Nullable Location getCodingLineBeginLocation(@NotNull Location location) {
        return getDevPlatformer().getColumnBeginLocation(this, location);
    }

    /**
     * Adds coding line, that will be parsed on
     * partially code parsing (/play) by getting
     * begin location.
     *
     * @param location location on coding line.
     */
    public void addInsideCodeColumnChange(@NotNull Location location) {
        Location executorLocation = getCodingLineBeginLocation(location);
        if (executorLocation == null) return;
        changedColumns.add(executorLocation);
    }

    /**
     * Adds coding line, that will be parsed on
     * partially code parsing (/play).
     *
     * @param executorLocation location of executor on coding line.
     */
    public void addChangedColumn(@NotNull Location executorLocation) {
        changedColumns.add(executorLocation);
    }

    /**
     * Returns immutable set of executor locations for changed coding lines.
     *
     * @return immutable set of locations with executors.
     */
    public @NotNull Set<Location> getChangedColumns() {
        return new HashSet<>(changedColumns);
    }

    /**
     * Connects player to developer's world.
     *
     * @param player player to connect.
     */
    public void connectPlayer(@NotNull Player player) {
        connectPlayer(player, isVanished(player));
    }

    /**
     * Connects player to developer's planet.
     *
     * @param player     player to connect.
     * @param hidePlayer whether hide player's join message and make him in spectator mode or not.
     */
    public void connectPlayer(@NotNull Player player, boolean hidePlayer) {
        Wander wander = OpenCreative.getWander(player);
        if (wander.isConnectingToPlanet()) {
            player.sendMessage(getLocaleMessage("world.connecting.busy"));
            return;
        }
        wander.setConnectingToPlanet(true);
        player.showTitle(Title.title(
                toComponent(getLocaleMessage("world.dev-mode.connecting.title")), toComponent(getLocaleMessage("world.dev-mode.connecting.subtitle")),
                Title.Times.times(Duration.ofSeconds(15), Duration.ofSeconds(30), Duration.ofSeconds(10))
        ));
        World previousWorld = player.getWorld();
        World world = getWorld();
        if (world == null) {
            load().whenComplete((loadedWorld, error) -> {
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    if (error != null) {
                        String errorMessage = (error.getMessage() == null ? "Unknown error" : error.getMessage());
                        player.sendMessage(getComponentWithPlaceholders("world.dev-mode.connecting.error",
                                player, "id", planet.getId(), "wasloaded", false,
                                "phase", "Loading World", "loaded", isLoaded(), "error", errorMessage)
                                .clickEvent(ClickEvent.suggestCommand(errorMessage))
                                .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                        sendCriticalErrorMessage("Failed to connect the player to planet " + planet.getId()
                                + " (was loaded: true, loaded: " + isLoaded() + ", phase: Loading World)", error);
                        Sounds.PLAYER_ERROR.play(player);
                        wander.setConnectingToPlanet(false); // OpenCreative++: was left "connecting" forever
                        player.clearTitle();
                        return;
                    }
                    if (loadedWorld == null) {
                        wander.setConnectingToPlanet(false);
                        String errorMessage = "Dev planet world is null";
                        player.sendMessage(getComponentWithPlaceholders("world.dev-mode.connecting.error",
                                player, "id", planet.getId(), "wasloaded", false,
                                "phase", "Loading World", "loaded", isLoaded(), "error", errorMessage)
                                .clickEvent(ClickEvent.suggestCommand(errorMessage))
                                .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                        sendCriticalErrorMessage("Failed to connect the player to planet " + planet.getId()
                                + " (was loaded: false, loaded: " + isLoaded() + ", phase: Loading World)", error);
                        Sounds.PLAYER_ERROR.play(player);
                        return;
                    }
                    worldID = loadedWorld.getUID();
                    if (player.isOnline() && previousWorld.equals(player.getWorld())) {
                        handlePlayerConnection(player, loadedWorld, hidePlayer, wander);
                    } else if (planet.getPlayers().isEmpty()) {
                        unload();
                        wander.setConnectingToPlanet(false);
                    } else {
                        wander.setConnectingToPlanet(false);
                    }
                });
            });
            return;
        }
        handlePlayerConnection(player, world, hidePlayer, wander);
    }

    private void handlePlayerConnection(@NotNull Player player, @NotNull World world, boolean hidePlayer, @NotNull Wander wander) {
        world.getSpawnLocation().getChunk().load(true);
        Location lastLocation = this.getLastLocations().get(player.getUniqueId());
        if (!this.isLoaded()) {
            wander.setConnectingToPlanet(false);
            return;
        }
        if (lastLocation != null) {
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
                translateSigns(player, 10);
            }, 5L);
        }
        if (lastLocation == null || !isSaveLocation()) {
            lastLocation = world.getSpawnLocation();
        }
        saveInventory(player);
        ItemStack[] inventoryBefore = player.getInventory().getContents().clone();
        GameMode gameModeBefore = player.getGameMode();
        clearPlayer(player, false);
        player.teleportAsync(lastLocation).thenAccept(success -> {
            if (!success) {
                // OpenCreative++: teleport was cancelled; OpenCreative+ left the player in the build world
                // with an empty inventory, adventure mode and "connecting" flag.
                player.getInventory().setContents(inventoryBefore);
                player.setGameMode(gameModeBefore);
                player.clearTitle();
                wander.setConnectingToPlanet(false);
                return;
            }
            if (success) {
                player.setAllowFlight(true);
                player.setFlying(true);
                if (!hidePlayer) {
                    /*
                     * If player is visiting world normally.
                     */
                    if (planet.getWorldPlayers().canDevelop(player)) {
                        player.sendMessage(getPlayerLocaleMessage("world.dev-mode.help", player));
                        player.setGameMode(GameMode.CREATIVE);
                    } else {
                        player.setGameMode(GameMode.ADVENTURE);
                    }
                    for (Player onlinePlayer : player.getWorld().getPlayers()) {
                        if (!onlinePlayer.equals(player)) {
                            onlinePlayer.sendMessage(MessageUtils.getPlayerLocaleMessage("world.dev-mode.joined", player));
                        }
                    }
                } else {
                    /*
                     * If player is moderator and should be hidden.
                     */
                    player.setGameMode(GameMode.SPECTATOR);
                    for (Player onlinePlayer : player.getWorld().getPlayers()) {
                        onlinePlayer.hidePlayer(OpenCreative.getPlugin(), player);
                    }
                }
                if (isSaveLocation()) getLastLocations().put(player.getUniqueId(), player.getLocation());
                if (isNightVision())
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
                Sounds.DEV_CONNECTED.play(player);
                Sounds.WORLD_MODE_DEV.play(player);
                displayWorldBorders();
                player.showTitle(Title.title(
                        toComponent(getLocaleMessage("world.dev-mode.title")), toComponent(getLocaleMessage("world.dev-mode.subtitle")),
                        Title.Times.times(Duration.ofMillis(750), Duration.ofSeconds(2), Duration.ofMillis(750))
                ));
                wander.setConnectingToPlanet(false);
                ItemsGroup itemsGroup = planet.isOwner(player) ? ItemsGroup.CODING_OWNER : ItemsGroup.CODING;
                itemsGroup.setItemsIfAbsent(player);
            }
        }).exceptionally(error -> {
            wander.setConnectingToPlanet(false);
            String errorMessage = (error.getMessage() == null ? "Unknown error" : error.getMessage());
            player.sendMessage(getComponentWithPlaceholders("world.dev-mode.connecting.error",
                    player, "id", planet.getId(), "wasloaded", true,
                    "phase", "Teleportation", "loaded", isLoaded(), "error", errorMessage)
                    .clickEvent(ClickEvent.suggestCommand(errorMessage))
                    .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
            sendCriticalErrorMessage("Failed to connect the player to planet " + planet.getId()
                    + " (was loaded: true, loaded: " + isLoaded() + ", phase: Teleportation)", error);
            Sounds.PLAYER_ERROR.play(player);
            return null;
        });
    }

    /**
     * Connects player to developer's world, teleports next
     * to block on specified coordinates.
     * <p>
     * Will make block glowing.
     *
     * @param player player to connect.
     * @param x      x coordinate of block.
     * @param y      y coordinate of block.
     * @param z      z coordinate of block.
     */
    public void connectPlayer(@NotNull Player player, double x, double y, double z) {
        connectPlayer(player);
        if (x > 0 && y > 0 && z > 0 && y < 30 && !isOutOfBorders(new Location(getWorld(), x + 1, y, z + 2))) {
            Location location = new Location(this.getWorld(), x + 1, y, z + 2, 180, 5);
            boolean blockExists = !new Location(getWorld(), x, y, z).getBlock().isEmpty();
            player.teleportAsync(location).thenAccept(success -> {
                if (success) {
                    if (blockExists) spawnGlowingBlock(player, new Location(this.getWorld(), x + 0.5, y, z + 0.5));
                    translateSigns(player, 5);
                }
            });
        }
    }

    /**
     * Clears set of changed coding lines, so they will be not parsed.
     */
    public void clearColumnsChanges() {
        changedColumns.clear();
    }

    public void clearMarkedExecutors(@NotNull Player player) {
        selectedExecutors.remove(player.getUniqueId());
    }

    public void clearSavedInventory(@NotNull Player player) {
        savedInventories.remove(player.getUniqueId());
    }

    public @NotNull ItemStack[] getSavedInventory(@NotNull Player player) {
        ItemStack[] items = savedInventories.get(player.getUniqueId());
        if (items == null) {
            return new ItemStack[45];
        }
        return items;
    }

    public void saveInventory(@NotNull Player player) {
        ItemStack[] savedItems = new ItemStack[45];
        int slot = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (getItemType(item).equals("world_settings") || getItemType(item).equals("build_tools"))) {
                savedItems[slot] = null;
            } else {
                savedItems[slot] = item;
            }
            slot++;
        }
        saveInventory(player, savedItems);
    }

    public void saveInventory(@NotNull Player player, @Nullable ItemStack @NotNull[] items) {
        savedInventories.put(player.getUniqueId(), items);
    }

    public World getWorld() {
        return Bukkit.getWorld(worldID);
    }

    public @NotNull Planet getPlanet() {
        return planet;
    }
}
