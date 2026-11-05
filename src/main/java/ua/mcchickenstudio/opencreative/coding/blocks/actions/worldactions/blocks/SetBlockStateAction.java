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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;

import java.util.List;

public final class SetBlockStateAction extends WorldAction {
    public SetBlockStateAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {

        String blockState = getArguments().getText("state", "", this);
        List<Location> locations = getArguments().getLocationList("locations", this);
        if (locations.isEmpty()) return;

        if (blockState.isEmpty()) {
            for (Location location : locations) {
                if (getPlanet().getLimits().cantModifyBlock(this)) {
                    return;
                }
                location.getBlock().setBlockData(location.getBlock().getType().createBlockData());
            }
            return;
        }

        blockState = blockState
            .replace("\",\"", ",")
            .replace("{\"", "[")
            .replace("\"}", "]")
            .replace("\":\"", "=")
            .replace("\"", "")
            .replace(" ", "");

        StringBuilder builder = new StringBuilder(blockState);
        if (!blockState.startsWith("[")) {
            builder.insert(0, "[");
        }
        if (!blockState.endsWith("]")) {
            builder.append("]");
        }

        for (Location location : locations) {
            if (getPlanet().getLimits().cantModifyBlock(this)) {
                return;
            }
            builder.insert(0, "minecraft:" + location.getBlock().getType().name().toLowerCase());
            try {
                location.getBlock().setBlockData(Bukkit.createBlockData(builder.toString()));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.WORLD_SET_BLOCK_STATE;
    }
}
