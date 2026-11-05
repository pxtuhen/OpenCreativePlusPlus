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

package ua.mcchickenstudio.opencreative.listeners.entity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.wanders.Wander;

import java.util.*;

public final class WitherCreationListener implements Listener {

    private final Map<WitherKey, UUID> recentSkullPlacers = new HashMap<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.WITHER_SKELETON_SKULL) return;
        if (event.getBlockAgainst().getType() != Material.SOUL_SAND) return;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(event.getBlock().getWorld());
        if (planet == null) return;
        WitherKey key = new WitherKey(planet.getId(), event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ());
        recentSkullPlacers.put(key, event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
            recentSkullPlacers.remove(key, event.getPlayer().getUniqueId());
        }, 15L);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.WITHER) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER) return;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(event.getLocation().getWorld());
        if (planet == null) return;
        WitherKey key = new WitherKey(planet.getId(), event.getLocation().getChunk().getX(),
                event.getLocation().getChunk().getZ());
        UUID uuid = recentSkullPlacers.remove(key);
        if (uuid != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Wander wander = OpenCreative.getWander(player);
                wander.getGriefStats().addWithersSummonsAmount(1);
            }
        }
    }

    private record WitherKey(int planetId, int chunkX, int chunkZ) {}

}
