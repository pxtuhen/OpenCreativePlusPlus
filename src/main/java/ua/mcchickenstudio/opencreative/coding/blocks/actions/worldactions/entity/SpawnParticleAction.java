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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;

import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCodingDebugLog;

public final class SpawnParticleAction extends WorldAction {
    public SpawnParticleAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {
        if (getWorld().getEntities().size() >= getPlanet().getLimits().getEntitiesLimit()) {
            sendCodingDebugLog(getPlanet(), "Too many entities: spawn particles action is cancelled.");
            return;
        }
        Particle particle = getArguments().getParticle("particle", Particle.HEART, this);
        int count = Math.min(30, getArguments().getInt("count", 1, this));
        if (particle == Particle.ELDER_GUARDIAN) {
            count = Math.min(count, 2);
        }
        double offsetX = getArguments().getDouble("offset-x", 0.0d, this);
        double offsetY = getArguments().getDouble("offset-y", 0.0d, this);
        double offsetZ = getArguments().getDouble("offset-z", 0.0d, this);
        List<Location> locations = getArguments().getLocationList("locations", this);
        List<Player> players = getWorld().getPlayers();
        int finalCount = count;
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            for (Location location : locations) {
                for (Player player : players) {
                    ua.mcchickenstudio.opencreative.plus.coding.ParticleData.spawn(player, particle, location, finalCount, offsetX, offsetY, offsetZ); // OpenCreative++: data for 1.21.9+ particles
                }
            }
        });
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.WORLD_SPAWN_PARTICLE;
    }
}
