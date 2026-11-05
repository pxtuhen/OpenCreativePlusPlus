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

package ua.mcchickenstudio.opencreative.indev;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;

import java.util.*;

public class MobsAtLoadedChunkLimiter {

    private BukkitTask mobSpawnerTask;
    private final Deque<Long> lastMobSpawns = new ArrayDeque<>();
    private final Map<Location, Entity> entitiesToSpawnQueue = new HashMap<>();

    public void onEventSpawn(Entity entity) {
        if (entity.hasMetadata("oc_ignore_spawn_event")) {
            return;
        }
    }

    /**
     * Checks if world has a lot of spawned mobs.
     * Useful to prevent "too many spawns" lags.
     *
     * @return true - if it's disallowed to spawn a mob right now,
     * so it will be spawned after some time, false - it's allowed.
     */
    public boolean isTooManyMobSpawns(@NotNull Entity entity, @NotNull Location location) {

        long now = System.currentTimeMillis();

        // Removes time from list, if it's more than 1 second.
        while (!lastMobSpawns.isEmpty() && (now - lastMobSpawns.peek()) > 1000) {
            lastMobSpawns.poll();
        }

        if (lastMobSpawns.size() >= 50) {
            if (entitiesToSpawnQueue.containsKey(location)) {
                return false;
            }
            entitiesToSpawnQueue.put(location, entity);
            return true;
        } else {
            lastMobSpawns.add(now);
            return false;
        }

    }

    public void startMobSpawnerTask() {
        if (mobSpawnerTask != null) {
            mobSpawnerTask.cancel();
        }
        mobSpawnerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(OpenCreative.getPlugin(), () -> {
            if (entitiesToSpawnQueue.isEmpty()) return;
            /*if (!planet.isLoaded()) {
                mobSpawnerTask.cancel();
                return;
            }*/
            int spawns = 0;
            Iterator<Map.Entry<Location, Entity>> spawnQueue = entitiesToSpawnQueue.entrySet().iterator();
            while (spawnQueue.hasNext()) {
                Map.Entry<Location, Entity> spawnTarget = spawnQueue.next();
                Entity entity = spawnTarget.getValue().copy();
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    entity.setMetadata("oc_ignore_spawn_event", new FixedMetadataValue(OpenCreative.getPlugin(), 1));
                    entity.spawnAt(spawnTarget.getKey());
                });
                spawnQueue.remove();
                spawns++;
                if (spawns >= 10) break;
            }
        }, 15L, 3L);
    }

}
