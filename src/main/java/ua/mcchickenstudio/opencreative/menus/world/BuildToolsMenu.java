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

package ua.mcchickenstudio.opencreative.menus.world;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.items.ItemPair;
import ua.mcchickenstudio.opencreative.settings.items.ItemsGroup;
import ua.mcchickenstudio.opencreative.settings.items.SettingsItem;
import ua.mcchickenstudio.opencreative.settings.items.SettingsItemsGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

public final class BuildToolsMenu extends AbstractMenu {

    public BuildToolsMenu() {
        super(ItemsGroup.BUILD_TOOLS.getRows(), getLocaleMessage("menus.build-tools.title"));
    }

    @Override
    public void fillItems(Player player) {
        for (ItemPair pair : ItemsGroup.BUILD_TOOLS.getPairs()) {
            setItem(pair.slot()-1, pair.item().get(player));
        }
        SettingsItemsGroup group = OpenCreative.getSettings().getItemsGroups().get(ItemsGroup.BUILD_TOOLS);
        if (group != null) {
            Map<Integer, SettingsItem> items = group.getItems();
            for (int slot : items.keySet()) {
                ItemStack item = items.get(slot).getItem(player);
                setItem(slot-1, item);
            }
        }
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {

    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_BUILDER_TOOLS.play(event.getPlayer());
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        Sounds.MENU_CLOSE_BUILDER_TOOLS.play(event.getPlayer());
        super.onClose(event);
    }
}
