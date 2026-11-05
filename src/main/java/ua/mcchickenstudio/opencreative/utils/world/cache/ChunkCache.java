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

package ua.mcchickenstudio.opencreative.utils.world.cache;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>ChunkCache</h1>
 * This class saves the state of the chunk in the
 * cache so as not to overload the memory.
 *
 * @author Sasha Kireiko
 */
public class ChunkCache {

    private static final Map<UUID, Map<Long, Boolean>> statusKeeper = new ConcurrentHashMap<>();

    /**
     * Checks whether chunk is generated.
     *
     * @param world world with chunk.
     * @param x     x coordinate of chunk.
     * @param z     z coordinate of chunk.
     * @return true - chunk is generated, false - not generated.
     */
    public static boolean isChunkGenerated(@NotNull World world, int x, final int z) {
        long hash = chunkHash(x, z);
        return statusKeeper
                .computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(hash, k -> world.isChunkGenerated(x, z));
    }

    /**
     * Preloads chunk and saves it's hashcode into map.
     *
     * @param world world with chunk.
     * @param x     x coordinate of chunk.
     * @param z     z coordinate of chunk.
     */
    public static void preLoad(@NotNull World world, int x, int z) {
        statusKeeper
                .computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>())
                .putIfAbsent(chunkHash(x, z), true);
    }

    /**
     * Saves chunk's hashcode into map.
     *
     * @param world world with chunk.
     * @param x     x coordinate of chunk.
     * @param z     z coordinate of chunk.
     */
    public static void load(@NotNull World world, int x, int z) {
        statusKeeper
                .computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>())
                .put(chunkHash(x, z), true);
    }

    /**
     * Marks chunk as unloaded.
     *
     * @param world world with chunk.
     * @param x     x coordinate of chunk.
     * @param z     z coordinate of chunk.
     */
    public static void unload(@NotNull World world, int x, int z) {
        Map<Long, Boolean> chunks = statusKeeper.get(world.getUID());
        if (chunks != null) {
            chunks.putIfAbsent(chunkHash(x, z), false);
        }
    }

    /**
     * Removes all cached chunk hashes for a world.
     */
    public static void unload(@NotNull World world) {
        statusKeeper.remove(world.getUID());
    }

    /**
     * Returns chunk's hashcode.
     *
     * @param x x coordinate of chunk.
     * @param z z coordinate of chunk.
     */
    private static long chunkHash(int x, int z) {
        return (31L * x + z) * 31L;
    }

}
