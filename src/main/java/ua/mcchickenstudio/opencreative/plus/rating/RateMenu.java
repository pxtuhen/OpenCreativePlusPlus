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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * Menu with five stars to rate the world, where player is. Also /rate [1-5].
 */
public final class RateMenu extends AbstractMenu {

    private static final int FIRST_STAR = 11;
    private final Player player;
    private final Planet planet;

    public RateMenu(@NotNull Player player, @NotNull Planet planet) {
        super(3, getLocaleMessage("plus.rating.menu-title", false));
        this.player = player;
        this.planet = planet;
    }

    @Override
    public void fillItems(Player player) {
        for (int slot = 0; slot < 27; slot++) setItem(slot, DECORATION_PANE_ITEM);
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(toComponent("&f" + planet.getInformation().getDisplayName()));
        infoMeta.lore(List.of(toComponent("&7" + Ratings.format(planet))));
        info.setItemMeta(infoMeta);
        setItem(4, info);
        int current = Ratings.vote(planet, player.getUniqueId());
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
        if (Ratings.rate(player, planet, value)) {
            player.closeInventory();
        }
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }

    /**
     * /rate — menu; /rate 1..5 — rate at once. /like = 5 stars, /dislike = 1 star (see Like/DislikeCommand).
     */
    public static final class RateCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getLocaleMessage("only-players"));
                return true;
            }
            Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet == null) {
                player.sendMessage(getLocaleMessage("only-in-world"));
                return true;
            }
            // contest entry during voting: /rate votes in the contest
            if (ua.mcchickenstudio.opencreative.plus.contest.Contests.phase() == ua.mcchickenstudio.opencreative.plus.contest.Contests.Phase.VOTING && ua.mcchickenstudio.opencreative.plus.contest.Contests.isCurrentEntry(planet)) {
                if (args.length == 0) {
                    new StarPickerMenu(getLocaleMessage("plus.contest.rate-title", false), ua.mcchickenstudio.opencreative.plus.contest.Contests.vote(planet, player.getUniqueId()),
                            value -> ua.mcchickenstudio.opencreative.plus.contest.Contests.rate(player, planet, value)).open(player);
                } else {
                    try {
                        ua.mcchickenstudio.opencreative.plus.contest.Contests.rate(player, planet, Integer.parseInt(args[0]));
                    } catch (NumberFormatException error) {
                        player.sendMessage(getLocaleMessage("plus.rating.usage"));
                    }
                }
                return true;
            }
            if (args.length == 0) {
                new RateMenu(player, planet).open(player);
                return true;
            }
            try {
                Ratings.rate(player, planet, Integer.parseInt(args[0]));
            } catch (NumberFormatException error) {
                player.sendMessage(getLocaleMessage("plus.rating.usage"));
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return args.length == 1 ? List.of("1", "2", "3", "4", "5") : List.of();
        }
    }
}
