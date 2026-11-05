/*
 * OpenCreative++, fork of OpenCreative+ (C) 2022-2026, McChicken Studio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.plus.rating;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.List;
import java.util.function.IntConsumer;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * Generic "choose 1–5 stars" menu (used for code modules on the market and in contests).
 */
public final class StarPickerMenu extends AbstractMenu {

    private static final int FIRST_STAR = 11;
    private final int current;
    private final IntConsumer onPick;

    public StarPickerMenu(@NotNull String title, int current, @NotNull IntConsumer onPick) {
        super(3, title);
        this.current = current;
        this.onPick = onPick;
    }

    @Override
    public void fillItems(Player player) {
        for (int slot = 0; slot < 27; slot++) setItem(slot, DECORATION_PANE_ITEM);
        for (int value = 1; value <= 5; value++) {
            ItemStack star = new ItemStack(value <= current ? Material.NETHER_STAR : Material.FIREWORK_STAR, value);
            ItemMeta meta = star.getItemMeta();
            meta.displayName(toComponent(Ratings.stars(value)));
            meta.lore(List.of(toComponent(getLocaleMessage("plus.rating.click", false).replace("%value%", String.valueOf(value)))));
            if (value == current) meta.setEnchantmentGlintOverride(true);
            star.setItemMeta(meta);
            setItem(FIRST_STAR + value - 1, star);
        }
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        int value = event.getRawSlot() - FIRST_STAR + 1;
        if (value < 1 || value > 5) return;
        event.getWhoClicked().closeInventory();
        onPick.accept(value);
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }
}
