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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Action;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.conditions.playerconditions.PlayerCondition;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.utils.ItemUtils;

import java.util.List;

public final class IsWearingItemCondition extends PlayerCondition {

    public IsWearingItemCondition(Executor executor, Target target, int x, Arguments args, List<Action> actions, List<Action> reactions, boolean isOpposed) {
        super(executor, target, x, args, actions, reactions, isOpposed);
    }

    @Override
    public boolean checkPlayer(@NotNull Player player) {

        ItemStack helmet = getArguments().getItem("helmet", new ItemStack(Material.AIR), this);
        ItemStack chestplate = getArguments().getItem("chestplate", new ItemStack(Material.AIR), this);
        ItemStack leggings = getArguments().getItem("leggings", new ItemStack(Material.AIR), this);
        ItemStack boots = getArguments().getItem("boots", new ItemStack(Material.AIR), this);

        boolean requireAll = getArguments().getBoolean("all", true, this);
        boolean requireAir = getArguments().getBoolean("require-air", false, this);
        boolean ignoreAmount = getArguments().getBoolean("ignore-amount", true, this);
        boolean ignoreName = getArguments().getBoolean("ignore-name", false, this);
        boolean ignoreLore = getArguments().getBoolean("ignore-lore", false, this);
        boolean ignoreEnchantments = getArguments().getBoolean("ignore-enchantments", false, this);
        boolean ignoreFlags = getArguments().getBoolean("ignore-flags", false, this);
        boolean ignoreDamage = getArguments().getBoolean("ignore-damage", false, this);

        PlayerInventory inv = player.getInventory();

        ItemStack[] expected = {helmet, chestplate, leggings, boots};
        ItemStack[] actual = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};

        boolean wearingAnything = false;
        for (int i = 0; i < expected.length; i++) {
            ItemStack expectedItem = expected[i];
            ItemStack actualItem = actual[i];

            if (actualItem == null) actualItem = new ItemStack(Material.AIR);

            boolean expectedAir = expectedItem.getType().isAir();
            boolean actualAir = actualItem.getType().isAir();

            boolean match;

            if (expectedAir) {
                match = !requireAir || actualAir;
            } else {
                match = ItemUtils.checkItemsIgnoreData(
                        expectedItem,
                        actualItem,
                        ignoreAmount,
                        ignoreName,
                        ignoreLore,
                        ignoreFlags,
                        ignoreEnchantments,
                        false,
                        ignoreDamage
                );
            }

            if (requireAll) {
                if (!match) return false;
            } else {
                if (match && !(expectedAir && actualAir)) {
                    wearingAnything = true;
                }
            }
        }

        return requireAll || wearingAnything;
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.IF_PLAYER_IS_WEARING_ITEM;
    }
}
