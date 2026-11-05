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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.entityactions.other;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.entityactions.EntityAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.UnsupportedEntityException;

import java.util.ArrayList;
import java.util.List;

public final class SetVillagerRecipesAction extends EntityAction {
    public SetVillagerRecipesAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void executeEntity(@NotNull Entity entity) {
        if (!(entity instanceof Merchant villager)) {
            throw new UnsupportedEntityException(Villager.class, entity);
        }
        List<String> recipes = getArguments().getTextList("recipes", this);
        List<MerchantRecipe> merchantRecipes = new ArrayList<>();
        for (String key : recipes) {
            if (merchantRecipes.size() > 12) {
                break;
            }
            Recipe recipe = getPlanet().getTerritory().getRecipes().getRecipe(key);
            if (recipe == null) continue;
            if (recipe instanceof MerchantRecipe merchant) {
                merchantRecipes.add(merchant);
            }
        }
        villager.setRecipes(merchantRecipes);
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.ENTITY_SET_VILLAGER_RECIPES;
    }
}
