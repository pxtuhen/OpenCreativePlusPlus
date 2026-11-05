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

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooManyRepeatsException;

import static java.lang.Math.*;

public final class SpawnParticlesCircleAction extends WorldAction {
    public SpawnParticlesCircleAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {
        arguments.requireArguments(this, "radius", "points", "particle");

        final Location center = arguments.getLocation("center", getPlanet().getTerritory().getSpawnLocation(), this);
        final double radius = arguments.getDouble("radius", 0d, this);
        final Vector normal = arguments.getVector("normal", new Vector(0, 1, 0), this).normalize();
        final int points = arguments.getInt("points", 0, this);
        final Particle particle = arguments.getParticle("particle", Particle.ANGRY_VILLAGER, this);

        if (radius <= 0 || points <= 0) return;
        if (points > getPlanet().getLimits().getRepeatsAmountLimit()) throw new RuntimeException(
            "Too many particles spawned in one asynchronous tick! Please spawn particles manually"+
            " by utilizing \"Repeat on circle\" action, and adding \"Wait\" block.",
            new TooManyRepeatsException()
        );

        // for explanation check ua.mcchickenstudio.opencreative.coding.blocks.actions.repeatactions.other.RepeatOnCircleAction
        // all below is pretty much the same
        Vector xAxis = (abs(normal.getY()) > .99f) ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        xAxis = xAxis.crossProduct(normal).normalize();
        Vector yAxis = normal.clone().crossProduct(xAxis).normalize();

        for (int i = 0; i < points; i++) {
            double ang = i * ((2*PI) / points);
            double x2d = cos(ang) * radius, y2d = sin(ang) * radius;
            Vector xOff = xAxis.clone().multiply(x2d), yOff = yAxis.clone().multiply(y2d);
            Location result = center.clone().add(xOff.add(yOff));

            result.getWorld().spawnParticle(particle, result, 1);
        }
    }

    @Override
    public @NotNull ActionType getActionType() { return ActionType.WORLD_SPAWN_PARTICLES_CIRCLE; }
}

// Singed-By: kalienda (vlad)