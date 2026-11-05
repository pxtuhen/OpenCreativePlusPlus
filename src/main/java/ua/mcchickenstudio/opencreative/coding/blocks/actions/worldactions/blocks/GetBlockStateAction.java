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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.blocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.variables.VariableLink;

public final class GetBlockStateAction extends WorldAction {
    public GetBlockStateAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {
        if (getPlanet().getLimits().cantCheckBlock(this)) {
            return;
        }

        VariableLink link = getArguments().getVariableLink("variable", this);
        Location location = getArguments().getLocation("location", getPlanet().getTerritory().getSpawnLocation(), this);

        String blockState = location.getBlock().getBlockData().getAsString();
        // Strips block type for ease of editing and so that state can be applied to similar types e.g. slabs
        try {
            blockState = blockState.substring(blockState.indexOf("["));

            blockState = blockState.replace(",", "\",\"");
            blockState = blockState.replace("[", "{\"");
            blockState = blockState.replace("]", "\"}");
            blockState = blockState.replace("=","\":\"");
            setVarValue(link, blockState);
        } catch (Exception StringIndexOutOfBoundsException)
        {
            setVarValue(link, "{}");
        }

    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.WORLD_GET_BLOCK_STATE;
    }
}