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

package ua.mcchickenstudio.opencreative.listeners.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.events.world.other.LimitReachedRedstoneEvent;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.planets.Planet;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.sendMessageOnce;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;

public final class RedstoneListener implements Listener {

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (isDevPlanet(event.getBlock().getWorld())) {
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
        Location location = event.getBlock().getLocation();
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(location.getWorld());
        if (planet != null) {
            if (!planet.getLimits().canRedstoneWork(event.getBlock().getLocation())) {
                event.setNewCurrent(event.getOldCurrent());
                sendMessageOnce(planet, "world.redstone-limit",
                        new PlaceholderReplacer("count", planet.getLimits().getRedstoneOperationsLimit()),
                        null, null, 5);
                Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> location.getBlock().setType(Material.AIR), 1L);
                new LimitReachedRedstoneEvent(planet).callEvent();
                return;
            }
            new ua.mcchickenstudio.opencreative.coding.blocks.events.world.blocks.BlockRedstoneEvent(planet, event).callEvent();
        }

    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent event) {
        if (isDevPlanet(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

}
