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

package ua.mcchickenstudio.opencreative.coding.blocks.events;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.PlanetExecutors;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.ArrayList;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isPlanet;

/**
 * <h1>WorldEvent</h1>
 * This class represents event in Creative's planet.
 */
public abstract class WorldEvent {

    protected final World world;
    protected List<Entity> selection = new ArrayList<>();

    public WorldEvent(@NotNull Planet planet, @NotNull List<Entity> selection) {
        this.world = planet.getTerritory().getWorld();
        this.selection = selection;
    }

    public WorldEvent(@NotNull Planet planet) {
        this.world = planet.getTerritory().getWorld();
        if (world != null) {
            this.selection.addAll(world.getPlayers());
        }
    }

    public WorldEvent(@NotNull Planet planet, @NotNull Block block) {
        this.world = block.getWorld();
        if (planet.getTerritory().getWorld() != null) {
            this.selection.addAll(planet.getTerritory().getWorld().getPlayers());
        }
    }

    public WorldEvent(@NotNull Entity entity) {
        world = entity.getWorld();
        selection.add(entity);
    }

    public List<Entity> getSelection() {
        return selection;
    }

    public World getWorld() {
        return world;
    }

    public Planet getPlanet() {
        if (getWorld() == null) return null;
        return OpenCreative.getPlanetsManager().getPlanetByWorld(getWorld());
    }

    public boolean callEvent() {
        if (canActivate()) {
            PlanetExecutors.activate(this);
        }
        if (this instanceof Cancellable cancellable) {
            return !cancellable.isCancelled();
        }
        return true;
    }

    /**
     * Checks if world event can activate executors or not.
     * World event must:
     * <p>
     * <ul>
     * <li>Be in planet's build world
     * <li>Have entities selection in planet's build world
     * <li>Happen while planet is in play mode
     * <li>Happen while planet is loaded
     * </ul>
     *
     * @return true - if possible, false - disallowed.
     */
    public boolean canActivate() {
        if (getPlanet() == null) return false;
        if (getWorld() == null) return false;
        if (!isPlanet(getWorld())) return false;
        if (isDevPlanet(getWorld())) return false;
        if (!getPlanet().isLoaded()) return false;
        if (getPlanet().getMode() != Planet.Mode.PLAYING) return false;
        if (!getPlanet().getTerritory().getScript().hasCode()) return false;
        if (!getSelection().isEmpty()) {
            for (Entity entity : getSelection()) {
                if (!entity.getWorld().equals(getPlanet().getWorld())) {
                    return false;
                }
            }
        }
        return true;
    }
}
