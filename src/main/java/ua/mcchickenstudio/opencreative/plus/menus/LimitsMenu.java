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

package ua.mcchickenstudio.opencreative.plus.menus;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.groups.Group;
import ua.mcchickenstudio.opencreative.settings.groups.Groups;
import ua.mcchickenstudio.opencreative.settings.groups.LimitType;

import java.util.ArrayList;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * "My limits" menu: everything, that player's group (groups.yml) gives —
 * world size, coding floors, worlds amount, main coding limits, and other groups to compare.
 */
public final class LimitsMenu extends AbstractMenu {

    private static final String PATH = "plus.menus.limits.";
    private static final int SLOT_BACK = 49, SLOT_UPGRADES = 51;

    private final Player player;

    public LimitsMenu(@NotNull Player player) {
        super(6, getLocaleMessage(PATH + "title", false));
        this.player = player;
    }

    @Override
    public void fillItems(Player player) {
        Groups groups = OpenCreative.getSettings().getGroups();
        Group group = groups.getGroup(player);
        int worlds = ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsUsed(player);
        boolean datapacks = player.hasPermission(OpenCreative.getPlugin().getConfig()
                .getString("datapacks.permission", "opencreativepp.datapack"));

        for (int slot = 0; slot < 54; slot++) setItem(slot, DECORATION_PANE_ITEM);

        setItem(4, item(Material.NAME_TAG, "group", "group", group.getName()));
        setItem(19, item(Material.GRASS_BLOCK, "world-size", "size", group.getWorldSize()));
        setItem(21, item(Material.COMMAND_BLOCK, "coding-floors", "floors", group.getCodingPlatformsLimit()));
        setItem(23, item(Material.MAP, "worlds", "worlds", worlds, "limit", ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsLimit(player)));
        setItem(25, item(Material.CHEST, "modules", "limit", group.getModulesLimit()));
        setItem(29, item(Material.ZOMBIE_HEAD, "entities",
                "limit", group.getLimit(LimitType.ENTITIES).limit(),
                "per-player", group.getLimit(LimitType.ENTITIES).modifier()));
        setItem(30, item(Material.WRITABLE_BOOK, "variables",
                "limit", group.getLimit(LimitType.VARIABLES).limit(),
                "per-player", group.getLimit(LimitType.VARIABLES).modifier()));
        setItem(31, item(Material.REPEATER, "code-operations",
                "limit", group.getLimit(LimitType.CODE_OPERATIONS).limit(),
                "per-player", group.getLimit(LimitType.CODE_OPERATIONS).modifier()));
        setItem(32, item(Material.PLAYER_HEAD, "members",
                "builders", group.getLimit(LimitType.BUILDERS_AMOUNT).limit(),
                "developers", group.getLimit(LimitType.DEVELOPERS_AMOUNT).limit()));
        setItem(33, item(datapacks ? Material.KNOWLEDGE_BOOK : Material.BOOK, datapacks ? "datapacks-yes" : "datapacks-no"));
        setItem(40, otherGroups(groups, group));
        setItem(SLOT_BACK, createItem(Material.ARROW, 1, PATH + "items.back"));
        setItem(SLOT_UPGRADES, createItem(Material.EMERALD_BLOCK, 1, "plus.shop.items.open"));
    }

    private @NotNull ItemStack item(@NotNull Material material, @NotNull String key, Object... placeholders) {
        ItemStack item = createItem(material, 1, PATH + "items." + key);
        return placeholders.length == 0 ? item : replacePlaceholdersInItem(item, new PlaceholderReplacer(placeholders));
    }

    /**
     * Every other group with its main values, so player can see what another group gives.
     */
    private @NotNull ItemStack otherGroups(@NotNull Groups groups, @NotNull Group current) {
        ItemStack item = createItem(Material.EMERALD, 1, PATH + "items.other-groups");
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        String format = getLocaleMessage(PATH + "group-line", false);
        for (String name : groups.getNames()) {
            Group group = groups.getGroup(name);
            if (group.getName().equalsIgnoreCase(current.getName())) continue;
            lore.add(toComponent(format
                    .replace("%group%", group.getName())
                    .replace("%size%", String.valueOf(group.getWorldSize()))
                    .replace("%floors%", String.valueOf(group.getCodingPlatformsLimit()))
                    .replace("%worlds%", String.valueOf(group.getWorldsLimit()))));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        if (event.getRawSlot() == SLOT_BACK) {
            new WorldsTabsMenu(player).open(player);
        } else if (event.getRawSlot() == SLOT_UPGRADES) {
            new ua.mcchickenstudio.opencreative.plus.shop.UpgradesMenu(player).open(player);
        }
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }
}
