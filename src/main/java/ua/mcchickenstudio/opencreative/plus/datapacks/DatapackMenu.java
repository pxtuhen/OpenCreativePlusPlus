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

package ua.mcchickenstudio.opencreative.plus.datapacks;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * Menu of datapacks for the world, where player is: click switches datapack on or off.
 */
public final class DatapackMenu extends AbstractMenu {

    private static final String PATH = "plus.datapacks.menu.";
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final Player player;
    private final Planet planet;
    private final Map<Integer, String> slots = new HashMap<>();

    public DatapackMenu(@NotNull Player player, @NotNull Planet planet) {
        super(5, getLocaleMessage(PATH + "title", false));
        this.player = player;
        this.planet = planet;
    }

    @Override
    public void fillItems(Player player) {
        slots.clear();
        for (int slot = 0; slot < 45; slot++) setItem(slot, DECORATION_PANE_ITEM);
        int index = 0;
        for (Datapacks.Entry entry : Datapacks.getLibrary()) {
            if (index >= SLOTS.length) break;
            setItem(SLOTS[index], icon(entry));
            slots.put(SLOTS[index], entry.id());
            index++;
        }
        if (index == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(toComponent(getLocaleMessage(PATH + "empty", false)));
            empty.setItemMeta(meta);
            setItem(22, empty);
        }
    }

    private @NotNull ItemStack icon(@NotNull Datapacks.Entry entry) {
        Material material = Material.matchMaterial(entry.icon());
        if (material == null || !material.isItem()) material = Material.KNOWLEDGE_BOOK;
        boolean enabled = Datapacks.isEnabledIn(planet, entry.id());
        ItemStack item = new ItemStack(entry.valid() ? material : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(entry.name()));
        List<Component> lore = new ArrayList<>();
        for (String line : entry.description()) lore.add(toComponent(line));
        lore.add(Component.empty());
        if (!entry.valid()) {
            lore.add(toComponent(getLocaleMessage(PATH + "status-invalid", false)));
            lore.add(toComponent("&8" + entry.problem()));
        } else {
            lore.add(toComponent(getLocaleMessage(PATH + (enabled ? "status-on" : "status-off"), false)));
            lore.add(toComponent(getLocaleMessage(PATH + (enabled ? "click-off" : "click-on"), false)));
        }
        lore.add(toComponent("&8ID: " + entry.id()));
        meta.lore(lore);
        if (enabled) meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        String id = slots.get(event.getRawSlot());
        if (id == null) return;
        Datapacks.ToggleResult result = Datapacks.toggle(player, id);
        DatapackCommand.sendResult(player, result, id);
        if (result == Datapacks.ToggleResult.ENABLED || result == Datapacks.ToggleResult.DISABLED) {
            Sounds.MENU_NEXT_CHOICE.play(player);
            fillItems(player);
        } else {
            Sounds.PLAYER_FAIL.play(player);
        }
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }
}
