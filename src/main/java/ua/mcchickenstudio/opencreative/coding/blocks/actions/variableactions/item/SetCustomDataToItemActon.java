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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.variableactions.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.variableactions.VariableAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.variables.VariableLink;

public final class SetCustomDataToItemActon extends VariableAction {
    public SetCustomDataToItemActon(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {
        VariableLink result = getArguments().getVariableLink("result", this);
        ItemStack item = getArguments().getItem("item", new ItemStack(Material.AIR),this);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String key = getArguments().getText("key", "key", this);
        String value = getArguments().getText("value", "default value", this);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(OpenCreative.getPlugin(), "custom_" + key),
                PersistentDataType.STRING,
                value
        );
        item.setItemMeta(meta);
        setVarValue(result, item);
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.VAR_SET_CUSTOM_DATA_TO_ITEM;
    }
}
