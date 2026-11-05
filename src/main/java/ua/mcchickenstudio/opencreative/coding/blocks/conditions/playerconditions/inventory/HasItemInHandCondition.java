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

package ua.mcchickenstudio.opencreative.coding.blocks.conditions.playerconditions.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Action;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.conditions.playerconditions.PlayerCondition;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.utils.ItemUtils;

import java.util.List;

public final class HasItemInHandCondition extends PlayerCondition {

    public HasItemInHandCondition(Executor executor, Target target, int x, Arguments args, List<Action> actions, List<Action> reactions, boolean isOpposed) {
        super(executor, target, x, args, actions, reactions, isOpposed);
    }

    @Override
    public boolean checkPlayer(@NotNull Player player) {
        List<ItemStack> items = getArguments().getItemList("items", this);
        if (items.isEmpty()) return false;

        String hand = getArguments().getText("hand", "main-hand", this);
        boolean ignoreAmount = getArguments().getBoolean("ignore-amount", true, this);
        boolean ignoreName = getArguments().getBoolean("ignore-name", false, this);
        boolean ignoreLore = getArguments().getBoolean("ignore-lore", false, this);
        boolean ignoreEnchantments = getArguments().getBoolean("ignore-enchantments", false, this);
        boolean ignoreFlags = getArguments().getBoolean("ignore-flags", false, this);
        boolean ignoreMaterial = getArguments().getBoolean("ignore-material", false, this);
        boolean ignoreDamage = getArguments().getBoolean("ignore-damage", false, this);

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        for (ItemStack checkItem : items) {
            boolean result = switch (hand) {
                case "main-hand" -> ItemUtils.checkItemsIgnoreData(itemInMainHand, checkItem, ignoreAmount,
                        ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage);
                case "off-hand" -> ItemUtils.checkItemsIgnoreData(itemInOffHand, checkItem, ignoreAmount,
                        ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage);
                case "main-or-off-hands" -> ItemUtils.checkItemsIgnoreData(itemInMainHand, checkItem, ignoreAmount,
                        ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage) ||
                        ItemUtils.checkItemsIgnoreData(itemInOffHand, checkItem, ignoreAmount,
                                ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage);
                default -> ItemUtils.checkItemsIgnoreData(itemInMainHand, checkItem, ignoreAmount,
                        ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage) &&
                        ItemUtils.checkItemsIgnoreData(itemInOffHand, checkItem, ignoreAmount,
                                ignoreName, ignoreLore, ignoreFlags, ignoreEnchantments, ignoreMaterial, ignoreDamage);
            };
            if (result) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.IF_PLAYER_HAS_ITEM_IN_HAND;
    }
}
