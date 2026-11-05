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

import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Action;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.repeatactions.RepeatAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.variables.VariableLink;

import java.util.List;

public final class RepeatTimesAction extends RepeatAction {

    public RepeatTimesAction(Executor executor, Target target, int x, Arguments args, List<Action> actions) {
        super(executor, target, x, args, actions);
    }

    @Override
    public boolean checkCanContinue() {
        VariableLink link = getArguments().getVariableLink("variable", this);
        double currentValue = getArguments().getDouble("variable", 0.0d, this);
        if (link == null) {
            /*
             * This will allocate local variable "repeat-times" for this action,
             * if there's no other variable link in argument.
             */
            // OpenCreative++: own counter for every repeat block (OpenCreative+ shared one "repeat-times" for the whole event,
            // so the second "repeat N times" in a line and nested repeats ran 0 times).
            link = new VariableLink("repeat-times-" + getX(), VariableLink.VariableType.LOCAL);
            Object variableValue = getPlanet().getVariables().getVariableValue(link, this);
            if (variableValue != null) {
                currentValue = getArguments().parseObject(variableValue, 0.0d);
            }
        }
        double maxValue = getArguments().getDouble("times", 10.0d, this);
        if (maxValue < 0) {
            maxValue = 0;
        }
        if (maxValue > 50000) {
            maxValue = 50000;
        }
        if (currentValue < maxValue) {
            this.setVarValue(link, currentValue + 1);
            return true;
        }
        if (getArguments().getVariableLink("variable", this) == null) {
            this.setVarValue(link, 0); // OpenCreative++: reset, so the same block can repeat again (functions, cycles)
        }
        return false;
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.REPEAT_TIMES;
    }
}

