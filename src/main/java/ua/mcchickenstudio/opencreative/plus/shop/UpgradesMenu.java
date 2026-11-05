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

package ua.mcchickenstudio.opencreative.plus.shop;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.menus.LimitsMenu;
import ua.mcchickenstudio.opencreative.plus.players.PlayerData;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.createItem;
import static ua.mcchickenstudio.opencreative.utils.ItemUtils.replacePlaceholdersInItem;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * Paid upgrades (shop.yml): coding floors and world size for the current own world, world slots for the player.
 */
public final class UpgradesMenu extends AbstractMenu {

    private static final String PATH = "plus.shop.";
    private static final int SLOT_INFO = 4, SLOT_FLOOR = 11, SLOT_SIZE = 13, SLOT_SLOT = 15, SLOT_BACK = 22;
    private final Player player;

    public UpgradesMenu(@NotNull Player player) {
        super(3, getLocaleMessage(PATH + "title", false));
        this.player = player;
    }

    private static @NotNull FileConfiguration config() {
        return OpenCreative.getPlugin().getConfig();
    }

    /**
     * Own world, where player is now, or null.
     */
    private @Nullable Planet ownWorld() {
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        return planet != null && planet.isOwner(player) ? planet : null;
    }

    @Override
    public void fillItems(Player player) {
        for (int slot = 0; slot < 27; slot++) setItem(slot, DECORATION_PANE_ITEM);
        setItem(SLOT_INFO, replacePlaceholdersInItem(createItem(Material.GOLD_INGOT, 1, PATH + "items.balance"),
                new PlaceholderReplacer("balance", Payments.format(Payments.balance(player)))));
        Planet planet = ownWorld();
        int floors = planet == null ? 0 : PlusLimits.bonusFloors(planet);
        int sizes = planet == null ? 0 : PlusLimits.bonusSize(planet) / Math.max(1, config().getInt("shop.upgrades.world-size.step", 32));
        setItem(SLOT_FLOOR, upgrade(Material.COMMAND_BLOCK, "floor", planet != null, floors,
                config().getInt("shop.upgrades.coding-floor.max", 4), config().getLong("shop.upgrades.coding-floor.price", 500),
                planet == null ? 0 : planet.getLimits().getCodingPlatformsLimit()));
        setItem(SLOT_SIZE, upgrade(Material.GRASS_BLOCK, "size", planet != null, sizes,
                config().getInt("shop.upgrades.world-size.max", 4), config().getLong("shop.upgrades.world-size.price", 400),
                planet == null ? 0 : planet.getTerritory().getWorldSize()));
        setItem(SLOT_SLOT, upgrade(Material.MAP, "slot", true, PlusLimits.boughtWorldSlots(player),
                config().getInt("shop.upgrades.world-slot.max", 3), config().getLong("shop.upgrades.world-slot.price", 1500),
                PlusLimits.worldsLimit(player)));
        setItem(SLOT_BACK, createItem(Material.ARROW, 1, PATH + "items.back"));
    }

    private @NotNull ItemStack upgrade(@NotNull Material material, @NotNull String key, boolean available,
                                       int bought, int max, long price, int current) {
        if (!available) return createItem(Material.BARRIER, 1, PATH + "items.need-own-world");
        if (bought >= max) {
            return replacePlaceholdersInItem(createItem(material, 1, PATH + "items." + key + "-max"),
                    new PlaceholderReplacer("current", current, "bought", bought, "max", max));
        }
        return replacePlaceholdersInItem(createItem(material, 1, PATH + "items." + key),
                new PlaceholderReplacer("current", current, "bought", bought, "max", max, "price", Payments.format(price),
                        "step", config().getInt("shop.upgrades.world-size.step", 32)));
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        if (!config().getBoolean("shop.enabled", true)) {
            player.sendMessage(getLocaleMessage(PATH + "disabled"));
            return;
        }
        int slot = event.getRawSlot();
        if (slot == SLOT_BACK) {
            new LimitsMenu(player).open(player);
            return;
        }
        Planet planet = ownWorld();
        switch (slot) {
            case SLOT_FLOOR -> {
                if (planet == null) return;
                int bought = PlusLimits.bonusFloors(planet);
                if (bought >= config().getInt("shop.upgrades.coding-floor.max", 4)) return;
                buy(Material.COMMAND_BLOCK, "floor", config().getLong("shop.upgrades.coding-floor.price", 500),
                        "этаж кодинга, мир " + planet.getId(), () -> planet.getConfiguration().set(PlusLimits.BONUS_FLOORS, bought + 1));
            }
            case SLOT_SIZE -> {
                if (planet == null) return;
                int step = Math.max(1, config().getInt("shop.upgrades.world-size.step", 32));
                int bonus = PlusLimits.bonusSize(planet);
                if (bonus / step >= config().getInt("shop.upgrades.world-size.max", 4)) return;
                buy(Material.GRASS_BLOCK, "size", config().getLong("shop.upgrades.world-size.price", 400),
                        "размер мира, мир " + planet.getId(), () -> {
                            planet.getConfiguration().set(PlusLimits.BONUS_SIZE, bonus + step);
                            if (!planet.getConfiguration().getConfig().contains("size")) {
                                planet.getTerritory().setWorldSize(planet.getTerritory().getWorldSize() + step, false);
                            }
                        });
            }
            case SLOT_SLOT -> {
                int bought = PlusLimits.boughtWorldSlots(player);
                if (bought >= config().getInt("shop.upgrades.world-slot.max", 3)) return;
                buy(Material.MAP, "slot", config().getLong("shop.upgrades.world-slot.price", 1500),
                        "слот мира", () -> PlayerData.setInt(player.getUniqueId(), PlusLimits.WORLD_SLOTS, bought + 1));
            }
            default -> {
            }
        }
    }

    private void buy(@NotNull Material icon, @NotNull String key, long price, @NotNull String reason, @NotNull Runnable apply) {
        String name = getLocaleMessage(PATH + "names." + key, false);
        Payments.confirmPurchase(player, icon, name, price, reason, () -> {
            apply.run();
            player.sendMessage(getLocaleMessage(PATH + "bought").replace("%name%", name).replace("%price%", Payments.format(price)));
        });
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }

    /**
     * /upgrades
     */
    public static final class UpgradesCommand implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (sender instanceof Player player) new UpgradesMenu(player).open(player);
            else sender.sendMessage(getLocaleMessage("only-players"));
            return true;
        }
    }
}
