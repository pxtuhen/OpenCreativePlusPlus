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

package ua.mcchickenstudio.opencreative.utils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerUtils;

import java.io.File;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendDebugError;

/**
 * <h1>WorldUtils</h1>
 * This class contains methods for changing
 * or manipulating worlds, like checking if
 * world is a planet.
 */
public final class WorldUtils {

    private final static NamespacedKey DO_NOT_HURT_ANYONE = new NamespacedKey(OpenCreative.getPlugin(), "oc_do_not_damage");

    /**
     * Returns key, that will be used in entities, that
     * mustn't damage any entity.
     *
     * @return namespaced key of not hurting anyone.
     */
    public static NamespacedKey getDoNotHurtAnyoneKey() {
        return DO_NOT_HURT_ANYONE;
    }

    /**
     * Summons fireworks around players in world.
     * They won't damage players or entities.
     *
     * @param world  world to summon.
     * @param amount amount of fireworks.
     */
    public static void summonFireworks(@NotNull World world, int amount) {
        Random random = new Random();
        for (int i = 0; i < amount; i++) {
            for (Player player : world.getPlayers()) {
                int randomOffsetX = random.nextInt(-5, 6);
                int randomOffsetZ = random.nextInt(-5, 6);
                int detonateTicks = random.nextInt(30, 51);
                Color color = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                Color fadeColor = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                FireworkEffect.Type type = FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)];
                FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(color).withFade(fadeColor).with(type).trail(true).build();
                Location location = player.getLocation();
                location.add(randomOffsetX, 1, randomOffsetZ);
                if (!location.getBlock().isPassable()) {
                    location = location.getWorld().getHighestBlockAt(location).getLocation();
                }
                Firework firework = world.spawn(location, Firework.class);
                firework.getPersistentDataContainer().set(DO_NOT_HURT_ANYONE, PersistentDataType.BYTE, (byte) 1);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(effect);
                firework.setFireworkMeta(meta);
                firework.setTicksToDetonate(detonateTicks);
            }
        }
    }

    /**
     * Summons fireworks around players in world.
     * They won't damage players or entities.
     *
     * @param times  how many times summon fireworks.
     * @param period period of time, that should pass before every time.
     */
    public static void summonFireworks(int times, int period) {
        final int[] summoned = {0};
        Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), (task) -> {
            if (summoned[0] >= times) {
                task.cancel();
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                if (isLobbyWorld(world)) {
                    summonFireworks(world, 5);
                } else if (isDevPlanet(world)) {
                    summonFireworks(world, 4);
                } else if (isPlanet(world)) {
                    summonFireworks(world, 3);
                }
            }
            summoned[0]++;
        }, 0L, period);
    }

    /**
     * Returns planet id from world's name
     * by splitting it and removing path to
     * planets folder.
     *
     * @param world world to get id.
     * @return planet id.
     */
    public static @NotNull String getPlanetIdFromName(@NotNull World world) {
        String name = world.getName();

        String prefix = "./planets/planet";
        if (!name.startsWith(prefix)) {
            return name;
        }

        String id = name.substring(prefix.length());

        if (id.endsWith("dev")) {
            id = id.substring(0, id.length() - 3);
        }

        return id;
    }

    /**
     * Rounds location numbers: x, y, z, yaw, pitch.
     *
     * @param location location to round.
     * @return location with rounded coordinates.
     */
    public static @NotNull Location roundLocation(@NotNull Location location) {
        double x = Math.round(location.getX() * 100.0) / 100.0;
        double y = Math.round(location.getY() * 100.0) / 100.0;
        double z = Math.round(location.getZ() * 100.0) / 100.0;
        float yaw = (float) (Math.round(location.getYaw() * 100.0) / 100.0);
        float pitch = (float) (Math.round(location.getPitch() * 100.0) / 100.0);
        location.set(x, y, z);
        location.setYaw(yaw);
        location.setPitch(pitch);
        return location;
    }

    /**
     * Converts text coordinate to numeric double.
     * <p>
     * If it starts with ~ symbol, it will add specified value
     * to default value.
     * <p>
     * If it fails to get number from text, it will return
     * default value.
     *
     * @param text         text with number to convert.
     * @param defaultValue default value.
     * @return coordinate double value.
     */
    public static double fromTextToCoordinate(@NotNull String text, double defaultValue) {
        try {
            if (text.startsWith("~")) {
                return text.equals("~") ? defaultValue : defaultValue + Double.parseDouble(text.substring(1));
            } else {
                return Double.parseDouble(text);
            }
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * Converts location to map with x, y, z, yaw, pitch values.
     *
     * @param location location to convert to map.
     * @return map with x, y, z, yaw, pitch values.
     */
    public static @NotNull Map<String, Double> fromLocationToMap(@NotNull Location location) {
        Map<String, Double> map = new HashMap<>();
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", (double) location.getYaw());
        map.put("pitch", (double) location.getPitch());
        return map;
    }

    /**
     * Checks if the world is a planet by
     * checking whether it's name contains
     * a typical planets folder path.
     *
     * @param world world to check.
     * @return true - should be a planet, false - not a planet.
     */
    public static boolean isPlanet(@NotNull World world) {
        return isPlanet(world.getName());
    }

    /**
     * Checks if the world is a planet by
     * checking whether it's name contains
     * a typical planets folder path.
     *
     * @param worldName name of world to check.
     * @return true - should be a planet, false - not a planet.
     */
    public static boolean isPlanet(@NotNull String worldName) {
        return worldName.contains("planets/planet");
    }

    /**
     * Checks if the world is a dev planet
     * by checking whether it's name ends
     * with "dev" word.
     *
     * @param worldName world to check.
     * @return true - should be a dev planet, false - not a dev planet.
     */
    public static boolean isDevPlanet(@NotNull String worldName) {
        return worldName.contains("planets/planet") && worldName.endsWith("dev");
    }

    /**
     * Checks if the world is a planet, or
     * dev planet, or lobby world.
     *
     * @param world world to check.
     * @return true - it's spawn, planet, dev planet world, false - not plugin's world.
     */
    public static boolean isOpenCreativeWorld(@NotNull World world) {
        return isLobbyWorld(world) || isPlanet(world) || isDevPlanet(world);
    }

    /**
     * Checks if entity is a hostile one,
     * that can attack player.
     *
     * @param entity entity to check.
     * @return true - entity is angry, false - entity is friendly or null.
     */
    public static boolean isEntityHostile(@Nullable Entity entity) {
        return entity instanceof Enemy || entity instanceof Boss;
    }

    /**
     * Checks if the world is a dev planet
     * by checking whether it's name ends
     * with "dev" word.
     *
     * @param world world to check.
     * @return true - should be a dev planet, false - not a dev planet.
     */
    public static boolean isDevPlanet(@NotNull World world) {
        return isDevPlanet(world.getName());
    }

    /**
     * Checks if the world is a lobby world,
     * where players will be teleported on
     * server connection or by /spawn command.
     *
     * @param world world to check.
     * @return true - it's lobby world, false - not lobby world.
     */
    public static boolean isLobbyWorld(@NotNull World world) {
        return world.equals(PlayerUtils.getLobbyWorld());
    }

    /**
     * Checks and destroys bad containers, that store
     * too many books in one chunk, in a world.
     *
     * @param world                world to check bad containers.
     * @param symbolsPerChunkLimit maximum amount of symbols in books per chunk.
     */
    public static void checkBadContainersInWorld(@NotNull World world, long symbolsPerChunkLimit) {
        try {
            int destroyedShulkers = 0;
            int destroyedContainers = 0;
            long totalSymbolsAmount = 0L;

            for (Chunk loadedChunk : world.getLoadedChunks()) {

                long chunkSymbols = 0L;
                List<BlockEntity> containersInChunk = new ArrayList<>();

                // NMS! using chunks
                ChunkAccess chunk = ((CraftChunk) loadedChunk).getHandle(ChunkStatus.FULL);
                Map<BlockPos, BlockEntity> blockEntities = new HashMap<>(chunk.blockEntities);

                for (BlockEntity blockEntity : blockEntities.values()) {
                    if (blockEntity instanceof RandomizableContainerBlockEntity holder) {
                        containersInChunk.add(blockEntity);
                        for (ItemStack item : holder.getContents()) {
                            chunkSymbols += getItemAllPagesLength(item);
                        }
                    }
                }

                if (chunkSymbols >= symbolsPerChunkLimit) {
                    totalSymbolsAmount += chunkSymbols;

                    for (BlockEntity blockEntity : containersInChunk) {
                        BlockPos position = blockEntity.getBlockPos();

                        if (blockEntity.getType() == BlockEntityType.SHULKER_BOX) {
                            destroyedShulkers++;
                        } else {
                            destroyedContainers++;
                        }

                        world.setBlockData(new Location(world, position.getX(), position.getY(), position.getZ()), Material.AIR.createBlockData());
                    }
                }
            }

            if (destroyedContainers > 0 || destroyedShulkers > 0) {
                OpenCreative.getPlugin().getLogger().warning("[HEAVY: " + getPlanetIdFromName(world)
                        + "] Removed heavy " + (destroyedContainers == 0 ? "" : destroyedContainers
                        + " containers" + (destroyedShulkers == 0 ? " " : ", ")) + (destroyedShulkers == 0 ? "" : destroyedShulkers + " shulkers ") + "containing " + totalSymbolsAmount + " symbols in books.");
            }
        } catch (NoSuchMethodError error) {
            sendDebugError("Failed to check bad containers in world. NMS seems like to be broken on this " + Bukkit.getMinecraftVersion()
                    + " version", error);
        } catch (Exception error) {
            sendDebugError("Failed to check bad containers in world on this " + Bukkit.getMinecraftVersion() + " version.", error);
        }
    }

    /**
     * Checks and destroys bad containers, that store
     * too many books in one chunk, in a world.
     *
     * @param loadedChunk          chunk to check bad containers.
     * @param symbolsPerChunkLimit maximum amount of symbols in books per chunk.
     */
    public static void checkBadContainersInChunk(@NotNull Chunk loadedChunk, long symbolsPerChunkLimit) {
        try {
            int destroyedShulkers = 0;
            int destroyedContainers = 0;
            long totalSymbolsAmount = 0L;


            long chunkSymbols = 0L;
            List<BlockEntity> containersInChunk = new ArrayList<>();

            // NMS! using chunks
            ChunkAccess chunk = ((CraftChunk) loadedChunk).getHandle(ChunkStatus.FULL);
            Map<BlockPos, BlockEntity> blockEntities = new HashMap<>(chunk.blockEntities);

            for (BlockEntity blockEntity : blockEntities.values()) {
                if (blockEntity instanceof RandomizableContainerBlockEntity holder) {
                    containersInChunk.add(blockEntity);
                    for (ItemStack item : holder.getContents()) {
                        chunkSymbols += getItemAllPagesLength(item);
                    }
                }
            }

            if (chunkSymbols >= symbolsPerChunkLimit) {
                totalSymbolsAmount += chunkSymbols;

                for (BlockEntity blockEntity : containersInChunk) {
                    BlockPos position = blockEntity.getBlockPos();

                    if (blockEntity.getType() == BlockEntityType.SHULKER_BOX) {
                        destroyedShulkers++;
                    } else {
                        destroyedContainers++;
                    }

                    loadedChunk.getWorld().setBlockData(new Location(loadedChunk.getWorld(), position.getX(), position.getY(), position.getZ()), Material.AIR.createBlockData());
                }
            }

            if (destroyedContainers > 0 || destroyedShulkers > 0) {
                OpenCreative.getPlugin().getLogger().warning("[HEAVY: " + getPlanetIdFromName(loadedChunk.getWorld())
                        + "] Removed heavy " + (destroyedContainers == 0 ? "" : destroyedContainers
                        + " containers" + (destroyedShulkers == 0 ? " " : ", ")) + (destroyedShulkers == 0 ? "" : destroyedShulkers + " shulkers ") + "containing " + totalSymbolsAmount + " symbols in books.");
            }
        } catch (NoSuchMethodError error) {
            sendDebugError("Failed to check bad containers in world. NMS seems like to be broken on this " + Bukkit.getMinecraftVersion()
                    + " version", error);
        } catch (Exception error) {
            sendDebugError("Failed to check bad containers in world on this " + Bukkit.getMinecraftVersion() + " version.", error);
        }
    }

    /**
     * Returns length of all pages of item if it's a book,
     * or if it's a container it will return length of all pages
     * in nested books.
     *
     * @param item item to check.
     * @return all pages symbols amount, or 0 - if it's not a book, or container.
     */
    private static long getItemAllPagesLength(@NotNull ItemStack item) {
        if (item.isEmpty()) return 0L;
        long count = getBookPagesLength(item);
        ItemContainerContents contents = item.get(DataComponents.CONTAINER);
        if (contents != null) {
            for (ItemStack subItem : contents.nonEmptyItems()) {
                count += getItemAllPagesLength(subItem);
            }
        }
        return count;
    }

    /**
     * Returns length of all pages in book.
     *
     * @param item book with pages.
     * @return all pages symbols amount, or 0 - if item is not a book.
     */
    private static long getBookPagesLength(@NotNull ItemStack item) {
        long symbolsAmount = 0L;
        if (item.is(Items.WRITABLE_BOOK)) {
            WritableBookContent content = item.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (content != null) {
                for (Filterable<String> page : content.pages()) {
                    symbolsAmount += page.raw().length();
                }
            }
        } else if (item.is(Items.WRITTEN_BOOK)) {
            WrittenBookContent content = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                for (var page : content.pages()) {
                    symbolsAmount += page.raw().getString().length();
                }
            }
        }
        return symbolsAmount;
    }

    /**
     * Returns a unique numeric ID for new planet.
     * It gets a last numeric ID from config and
     * increases it while checking is number free.
     * Immediately will replace last numeric ID
     * with result.
     *
     * @return unique numeric ID.
     */
    public static int generateWorldID() {
        int newWorldID = OpenCreative.getPlugin().getConfig().getInt("last-world-id", 1);
        while (true) {
            newWorldID++;
            File planetFolder = new File(FileUtils.getPlanetFolderPath(newWorldID));
            File devPlanetFolder = new File(FileUtils.getDevPlanetFolderPath(newWorldID));
            if (!planetFolder.exists() && !devPlanetFolder.exists()) {
                OpenCreative.getPlugin().getConfig().set("last-world-id", newWorldID);
                OpenCreative.getPlugin().saveConfig();
                return newWorldID;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void setGameRuleIfExists(@NotNull World world, @NotNull String gameRule, boolean value) {
        try {
            GameRule<?> rule = GameRule.getByName(gameRule.toLowerCase());
            if (rule != null) {
                world.setGameRule((GameRule<? super Boolean>) rule, value);
            }
        } catch (Exception ignored) {
        }
    }

}

