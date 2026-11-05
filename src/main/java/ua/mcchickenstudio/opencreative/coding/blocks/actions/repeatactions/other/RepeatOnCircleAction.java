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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.repeatactions.other;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Action;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.repeatactions.RepeatAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.variables.ValueType;
import ua.mcchickenstudio.opencreative.coding.variables.VariableLink;

import java.util.List;

import static java.lang.Math.*;

public final class RepeatOnCircleAction extends RepeatAction {

    public RepeatOnCircleAction(Executor executor, Target target, int x, Arguments args, List<Action> actions) {
        super(executor, target, x, args, actions);
    }

    @Override
    public boolean checkCanContinue() {
        arguments.requireArguments(this, "consumer", "radius", "points");

        final int index = super.arguments.getInt("index", 0, this);
        final VariableLink consumer = arguments.getVariableLink("consumer", this);
        final Location center = arguments.getLocation("center", super.getPlanet().getTerritory().getSpawnLocation(), this);
        final double radius = arguments.getDouble("radius", 0d, this);
        final Vector normal = arguments.getVector("normal", new Vector(0, 1, 0), this).normalize();
        final int points = arguments.getInt("points", 0, this);

        if (consumer == null || index >= points || radius <= 0 || points <= 0) return false;

        Vector xAxis = (abs(normal.getY()) > .99f) ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        xAxis = xAxis.crossProduct(normal).normalize();
        Vector yAxis = normal.clone().crossProduct(xAxis).normalize();
        // x and y axes are arbitrary, as a base for further circle point
        double angle = index * ((2*PI) / points);
        double x2d = cos(angle) * radius, y2d = sin(angle) * radius;
        // 2d non-rotated circle point is created here
        Vector xOff = xAxis.clone().multiply(x2d), yOff = yAxis.clone().multiply(y2d);
        // rotating created earlier circle point
        Location result = center.clone().add(xOff.add(yOff));
        // applying computed point to given location

        arguments.setArgumentValue("index", ValueType.NUMBER, index+1);
        setVarValue(consumer, result);
        return true;
    }

    @Override
    public @NotNull ActionType getActionType() { return ActionType.REPEAT_ON_CIRCLE; }
}

// Signed-By: kalienda (vlad)