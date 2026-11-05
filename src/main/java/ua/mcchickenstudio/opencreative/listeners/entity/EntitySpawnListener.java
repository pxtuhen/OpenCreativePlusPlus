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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.events.world.other.LimitReachedEntitiesEvent;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.utils.ItemUtils;
import ua.mcchickenstudio.opencreative.wanders.Wander;

import java.util.Arrays;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.BlockUtils.isOutOfBorders;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleComponent;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.sendMessageOnce;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isEntityInDevPlanet;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isLobbyWorld;

public final class EntitySpawnListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        switch (entity) {
            case Item item -> {
                ItemStack newItem = ItemUtils.fixItem(item.getItemStack());
                if (newItem.getType().isAir()) {
                    event.setCancelled(true);
                } else {
                    item.setItemStack(newItem);
                }
            }
            case InventoryHolder holder -> {
                for (ItemStack insideItem : holder.getInventory().getContents()) {
                    if (insideItem == null) continue;
                    ItemUtils.fixItem(insideItem);
                }
            }
            case CommandMinecart minecart -> {
                if (OpenCreative.getSettings().getItemFixerSettings().isClearCommandBlocksData()) {
                    minecart.setCommand(null);
                    minecart.customName(Component.text(""));
                }
            }
            default -> {
            }
        }
        Component customName = entity.customName();
        if (customName != null) {
            String text = PlainTextComponentSerializer.plainText().serialize(customName);
            int limit = OpenCreative.getSettings().getItemFixerSettings().getMaxEntityNameLength();
            if (text.length() > limit) {
                entity.customName(PlainTextComponentSerializer.plainText()
                        .deserialize(text.substring(0, limit)));
            }
        }
        World world = event.getLocation().getWorld();
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(world);
        if (planet != null) {
            if (isOutOfBorders(event.getLocation())) {
                event.setCancelled(true);
                return;
            }
            if (event.getEntity().getEntitySpawnReason().name().contains("SPAWNER")
                    && planet.getLimits().isTooManyMobSpawnsBySpawner()) {
                event.setCancelled(true);
                return;
            }
            if (event.getEntity() instanceof EnderDragon ignored) {
                int dragonsLimit = planet.getLimits().getEnderDragonsLimit();
                if (world.getEntitiesByClass(EnderDragon.class).size() > dragonsLimit) {
                    event.setCancelled(true);
                    if (planet.getInformation().getAsyncOnline() < 1) return;
                    sendMessageOnce(planet, "world.ender-dragon-limit",
                            new PlaceholderReplacer("count", dragonsLimit),
                            "/world deletemobs", null, 3);
                    return;
                }
            } else if (event.getEntity() instanceof Display ignored) {
                int displaysLimit = planet.getLimits().getDisplayEntitiesLimit();
                if (world.getEntitiesByClass(EnderDragon.class).size() > displaysLimit) {
                    event.setCancelled(true);
                    if (planet.getInformation().getAsyncOnline() < 1) return;
                    sendMessageOnce(planet, "world.display-limit",
                            new PlaceholderReplacer("count", displaysLimit),
                            "/world deletemobs", null, 3);
                    return;
                }
            }
            int limit = planet.getLimits().getEntitiesLimit();
            int count = 0;
            if (planet.getDevPlanet().isLoaded()) {
                if (isDevPlanet(world)) {
                    if (event.getEntityType() != EntityType.ITEM) {
                        event.setCancelled(true);
                    }
                    World buildWorld = planet.getWorld();
                    if (buildWorld != null) {
                        count += buildWorld.getEntityCount();
                    }
                    count += world.getEntityCount();
                } else {
                    count += world.getEntityCount();
                    count += planet.getDevPlanet().getWorld().getEntityCount();
                }
            } else {
                count += world.getEntityCount();
            }
            if (count > limit) {
                event.setCancelled(true);
                if (planet.getInformation().getAsyncOnline() < 1) return;
                sendMessageOnce(planet, "world.entity-limit",
                        new PlaceholderReplacer("count", limit),
                        "/world deletemobs", null, 3);
                new LimitReachedEntitiesEvent(planet).callEvent();
                return;
            }
            if (event.isCancelled()) return;
            new ua.mcchickenstudio.opencreative.coding.blocks.events.entity.entities.EntitySpawnEvent(event).callEvent();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        World world = event.getBlock().getWorld();
        if (event.getEntity() instanceof Minecart minecart && OpenCreative.getSettings().getWorldFixerSettings().shouldFixMultipleVehiclePlacement()) {
            List<Entity> nearEntities = minecart.getNearbyEntities(1, 1, 1);
            int minecarts = 0;
            for (Entity entity : nearEntities) {
                if (entity instanceof Minecart) {
                    minecarts++;
                }
                if (minecarts >= 1) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(world);
        if (planet != null) {
            if (event.getEntity() instanceof EnderDragon ignored) {
                int dragonsLimit = planet.getLimits().getEnderDragonsLimit();
                if (world.getEntitiesByClass(EnderDragon.class).size() > dragonsLimit) {
                    event.setCancelled(true);
                    if (planet.getInformation().getAsyncOnline() < 1) return;
                    sendMessageOnce(planet, "world.ender-dragon-limit",
                            new PlaceholderReplacer("count", dragonsLimit),
                            "/world deletemobs", null, 3);
                    return;
                }
            } else if (event.getEntity() instanceof Display ignored) {
                int displaysLimit = planet.getLimits().getDisplayEntitiesLimit();
                if (world.getEntitiesByClass(EnderDragon.class).size() > displaysLimit) {
                    event.setCancelled(true);
                    if (planet.getInformation().getAsyncOnline() < 1) return;
                    sendMessageOnce(planet, "world.display-limit",
                            new PlaceholderReplacer("count", displaysLimit),
                            "/world deletemobs", null, 3);
                    return;
                }
            }
            int limit = planet.getLimits().getEntitiesLimit();
            if (world.getEntityCount() > limit) {
                event.setCancelled(true);
                if (planet.getInformation().getAsyncOnline() < 1) return;
                sendMessageOnce(planet, "world.entity-limit",
                        new PlaceholderReplacer("count", limit),
                        "/world deletemobs", null, 3);
                new LimitReachedEntitiesEvent(planet).callEvent();
            } else {
                Player player = event.getPlayer();
                if (player != null) {
                    switch (event.getEntityType()) {
                        case TNT_MINECART -> {
                            Wander wander = OpenCreative.getWander(player);
                            wander.getGriefStats().addTntPlacementsAmount(1);
                        }
                        case CREEPER -> {
                            Wander wander = OpenCreative.getWander(player);
                            wander.getGriefStats().addCreeperSummonsAmount(1);
                        }
                        case WITHER -> {
                            Wander wander = OpenCreative.getWander(player);
                            wander.getGriefStats().addWithersSummonsAmount(1);
                        }
                        case ENDER_DRAGON -> {
                            Wander wander = OpenCreative.getWander(player);
                            wander.getGriefStats().addDragonsSummonsAmount(1);
                        }
                    }
                }
            }
        } else if (isDevPlanet(world)) {
            event.setCancelled(true);
        } else if (isLobbyWorld(world)) {
            Player player = event.getPlayer();
            if (player == null) return;
            if (OpenCreative.getSettings().getLobbySettings().isSpawningMobsDisallowed() && !player.hasPermission("opencreative.lobby.spawning-mobs.bypass")) {
                event.setCancelled(true);
                player.sendActionBar(getLocaleComponent("not-for-lobby"));
            }
        }
    }

    @EventHandler
    public void onVehicleCreation(VehicleCreateEvent event) {
        Entity entity = event.getVehicle();
        if (isEntityInDevPlanet(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(world);
        if (planet == null) return;
        if (isDevPlanet(world)) return;
        Entity[] entities = event.getChunk().getEntities();
        if (world.getEntityCount() < planet.getLimits().getEntitiesLimit() / 2) {
            return;
        }
        for (Entity entity : entities) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        ItemStack item = event.getItemStack();
        if (item == null) return;
        if (event.getEntity() instanceof ItemFrame frame && item.getPersistentDataContainer().has(ItemUtils.getItemEntityInvisible())) {
            frame.setInvisible(true);
        }
    }


}
