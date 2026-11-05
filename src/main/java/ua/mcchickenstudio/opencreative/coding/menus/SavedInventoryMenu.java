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

package ua.mcchickenstudio.opencreative.coding.menus;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.planets.DevPlanet;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

public final class SavedInventoryMenu extends AbstractMenu {

    private final Player player;
    private final DevPlanet devPlanet;
    private boolean changed = false;

    public SavedInventoryMenu(@NotNull Player player, @NotNull DevPlanet devPlanet) {
        super(5, getLocaleMessage("menus.saved-inventory.title"));
        this.player = player;
        this.devPlanet = devPlanet;
    }

    @Override
    public void fillItems(Player player) {
        int slot = 0;
        for (ItemStack item : devPlanet.getSavedInventory(player)) {
            setItem(slot, item);
            slot++;
        }
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        changed = true;
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_SAVED_INVENTORY.play(player);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (changed) devPlanet.saveInventory(player, getInventory().getContents());
        Sounds.MENU_CLOSE_SAVED_INVENTORY.play(player);
        destroy();
    }
}
