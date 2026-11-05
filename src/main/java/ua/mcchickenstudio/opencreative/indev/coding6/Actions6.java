/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.indev.coding6;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.coding.blocks.DisplayableIcon;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionCategory;
import ua.mcchickenstudio.opencreative.coding.menus.MenusCategory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.BlockUtils.getSignLine;
import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendDebug;

public final class Actions6 {

    private static Actions6 instance;
    private final List<Action6> actions = new LinkedList<>();

    /**
     * Returns instance of actions controller class.
     *
     * @return instance of actions.
     */
    public synchronized static @NotNull Actions6 getInstance() {
        if (instance == null) {
            instance = new Actions6();
            instance.registerDefaults();
        }
        return instance;
    }

    /**
     * Registers action, that will be replaced in coding.
     *
     * @param action action to register.
     */
    public void registerAction6(@NotNull Action6 action) {
        Action6 existing = getById(action.getID());
        if (existing != null) {
            sendDebug("[ACTIONS] Can't register action " + action.getName() + " (from " + action.getExtensionId() + "), "
                    + "because there's already registered action " + existing.getName() + " (from " + existing.getExtensionId() + ") "
                    + "with same ID: " + action.getID());
            return;
        }
        sendDebug("[ACTIONS] Registered action: " + action.getName() + " (from " + action.getExtensionId() + ")");
        actions.add(action);
    }

    /**
     * Registers actions, that will be replaced in coding.
     *
     * @param actions actions to register.
     */
    public void registerAction(@NotNull Action6... actions) {
        for (Action6 action : actions) {
            registerAction6(action);
        }
    }

    /**
     * Unregisters action if list contains it.
     *
     * @param action action to unregister.
     */
    @SuppressWarnings("unused")
    public void unregisterAction(@NotNull Action6 action) {
        actions.remove(action);
    }

    /**
     * Returns a copy of list that contains all registered actions.
     *
     * @return actions list.
     */
    public @NotNull List<Action6> getActions() {
        return new ArrayList<>(actions);
    }

    private void registerDefaults() {
    }

    /**
     * Returns list of actions, that have same menu category.
     *
     * @param actionCategory action category.
     * @param menusCategory  menu category.
     * @return list of actions with specified menu category.
     */
    public @NotNull List<Action6> getByCategories(@NotNull ActionCategory actionCategory, @NotNull MenusCategory menusCategory) {
        List<Action6> list = new LinkedList<>();
        for (Action6 action : actions) {
            if (action.getBlockCategory() == actionCategory) {
                if (action instanceof DisplayableIcon icon) {
                    if (icon.getCategory() == menusCategory) {
                        list.add(action);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Returns list of all menu categories of specified action category..
     *
     * @param actionCategory action category.
     * @return list of menu categories.
     */
    public @NotNull List<MenusCategory> getCategories(@NotNull ActionCategory actionCategory) {
        List<MenusCategory> list = new LinkedList<>();
        for (Action6 action : actions) {
            if (action.getBlockCategory() == actionCategory && action instanceof DisplayableIcon icon) {
                if (list.contains(icon.getCategory())) continue;
                list.add(icon.getCategory());
            }
        }
        return list;
    }

    /**
     * Checks if action with specified ID exists in registry.
     *
     * @param id id of action.
     * @return true - exists, false - not exists.
     */
    public boolean exists(@NotNull String id) {
        return getById(id) != null;
    }

    /**
     * Checks if action with specified class exists in registry.
     *
     * @param clazz class of action.
     * @return true - exists, false - not exists.
     */
    public boolean exists(@NotNull Class<? extends Action6> clazz) {
        return getByClass(clazz) != null;
    }

    /**
     * Returns action from registry by specified class
     * if it exists, otherwise will return null.
     *
     * @param clazz class to get action.
     * @return action - if exists, or null - not exists.
     */
    public @Nullable Action6 getByClass(@NotNull Class<? extends Action6> clazz) {
        for (Action6 eventValue : actions) {
            if (eventValue.getClass().equals(clazz)) {
                return eventValue;
            }
        }
        return null;
    }

    /**
     * Returns action from registry by specified id
     * if it exists, otherwise will return null.
     *
     * @param id id to get action.
     * @return action - if exists, or null - not exists.
     */
    public @Nullable Action6 getById(@NotNull String id) {
        for (Action6 eventValue : actions) {
            if (eventValue.getID().equals(id)) {
                return eventValue;
            }
        }
        return null;
    }

    /**
     * Returns action from registry by specified block
     * if it exists, otherwise will return null.
     *
     * @param block block to get action.
     * @return action - if exists, or null - not exists.
     */
    public @Nullable Action6 getByBlock(@NotNull Block block) {
        if (block.getType() == Material.LAPIS_ORE) {
            //return LAUNCH_FUNCTION;
        }
        if (block.getType() == Material.EMERALD_ORE) {
            //return LAUNCH_METHOD;
        }
        Block signBlock = block.getRelative(BlockFace.SOUTH);
        String signLine = getSignLine(signBlock.getLocation(), (byte) 3);
        if (block.getType() == ActionCategory.SELECTION_ACTION.getBlock()) {
            signLine = getSignLine(signBlock.getLocation(), (byte) 4);
        }
        if (block.getType() == ActionCategory.REPEAT_ACTION.getBlock()) {
            // Repeat while <---
            // If player          Repeat
            // Is sitting         For numbers
            // Target
            String text = getSignLine(signBlock.getLocation(), (byte) 1);
            if (text != null && !text.isEmpty()) {
                signLine = getSignLine(signBlock.getLocation(), (byte) 1);
            }
        }
        if (signLine != null) {
            if (signLine.equalsIgnoreCase("entity_set_display_item")) {
                signLine = "entity_set_item";
            }
            for (Action6 action : getActions()) {
                if (action.getID().equals(signLine)) {
                    return action;
                }
            }
        }
        return null;
    }

}
