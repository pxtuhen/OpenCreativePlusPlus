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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.world;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;

public final class AddMerchantRecipeAction extends WorldAction {
    public AddMerchantRecipeAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void execute() {
        arguments.requireArguments(this, "result");

        String name = getArguments().getText("name", "custom", this);
        ItemStack first = getArguments().getItem("first", new ItemStack(Material.EMERALD), this);
        ItemStack second = getArguments().getItem("second", new ItemStack(Material.DIAMOND), this);
        ItemStack result = getArguments().getItem("result", new ItemStack(Material.APPLE), this);

        int uses = getArguments().getInt("uses", 0, this);
        int maxUses = getArguments().getInt("max-uses", Integer.MAX_VALUE, this);
        boolean experience = getArguments().getBoolean("player-exp", false, this);
        int villagerExperience = getArguments().getInt("villager-exp", 0, this);
        int discount = -getArguments().getInt("discount", 0, this);
        MerchantRecipe recipe = new MerchantRecipe(result, uses, maxUses, experience, villagerExperience, 0, 0, discount);
        recipe.addIngredient(first);
        if (getArguments().pathExists("second")) {
            recipe.addIngredient(second);
        }

        NamespacedKey key = new NamespacedKey(OpenCreative.getPlugin(), "oc_recipe_"
                + getPlanet().getId() + "_" + name);

        getPlanet().getTerritory().getRecipes().addRecipe(key, recipe);
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.WORLD_ADD_MERCHANT_RECIPE;
    }
}
