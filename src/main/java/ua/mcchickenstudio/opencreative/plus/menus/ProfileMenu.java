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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.ListBrowserMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.players.FavoriteWorlds;
import ua.mcchickenstudio.opencreative.plus.players.LastWorlds;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * OpenCreative++: player profile — group, worlds, likes of all their worlds, favorites, last world,
 * and the list of their worlds (click to join).
 */
public final class ProfileMenu extends ListBrowserMenu<Planet> {

    private static final String PATH = "plus.menus.profile.";
    private static final int[] ELEMENTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] CHARMS = {45, 49, 53, 2, 6};
    private static final int[] DECORATION = {0, 1, 3, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 50, 51, 52};
    private static final int SLOT_INFO = 4, SLOT_BACK = 49, SLOT_RECENT = 2, SLOT_INVITE = 6;
    private static final java.util.Map<java.util.UUID, Long> inviteCooldowns = new java.util.HashMap<>();

    private final String nickname;
    private final List<Planet> planets;

    public ProfileMenu(@NotNull Player viewer, @NotNull String nickname) {
        super(viewer, getLocaleMessage(PATH + "title", false).replace("%player%", nickname),
                ELEMENTS, CHARMS, DECORATION, 31, 45, 53);
        this.nickname = nickname;
        this.planets = new ArrayList<>(OpenCreative.getPlanetsManager().getPlanetsByOwner(nickname));
        this.planets.sort(Comparator.comparingInt((Planet planet) -> planet.getInformation().getReputation()).reversed());
    }

    @Override
    public List<Planet> getElements() {
        return new ArrayList<>(planets);
    }

    @Override
    protected ItemStack getElementIcon(Planet planet) {
        return planet.getInformation().getIcon();
    }

    @Override
    protected void fillOtherItems() {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(nickname);
        // average stars of rated worlds of the player
        double sum = 0;
        int rated = 0;
        for (Planet planet : planets) {
            if (ua.mcchickenstudio.opencreative.plus.rating.Ratings.votes(planet) == 0) continue;
            sum += ua.mcchickenstudio.opencreative.plus.rating.Ratings.average(planet);
            rated++;
        }
        String likes = rated == 0 ? "-" : String.format(java.util.Locale.ROOT, "★ %.1f", sum / rated);
        Player online = target.getPlayer();
        String group = online != null ? OpenCreative.getSettings().getGroups().getGroup(online).getName()
                : getLocaleMessage(PATH + "offline", false);
        Planet last = LastWorlds.get(target.getUniqueId());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(target);
            head.setItemMeta(skull);
        }
        setItem(SLOT_INFO, replacePlaceholdersInItem(createItem(head, PATH + "items.info"), new PlaceholderReplacer(
                "player", nickname,
                "group", group,
                "worlds", planets.size(),
                "likes", likes,
                "favorites", FavoriteWorlds.getPlanets(target.getUniqueId()).size(),
                "last", last == null ? getLocaleMessage(PATH + "no-last", false) : last.getInformation().getDisplayName())));
        setItem(SLOT_BACK, createItem(Material.ARROW, 1, PATH + "items.back"));
        // recent worlds of the profile owner
        List<Planet> recent = ua.mcchickenstudio.opencreative.plus.players.Conveniences.recent(target.getUniqueId());
        ItemStack recentItem = createItem(Material.CLOCK, 1, "plus.profile.recent");
        org.bukkit.inventory.meta.ItemMeta recentMeta = recentItem.getItemMeta();
        List<net.kyori.adventure.text.Component> recentLore = new ArrayList<>();
        for (Planet planet : recent) {
            recentLore.add(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent("&7• &f" + planet.getInformation().getDisplayName()
                    + " &8(" + planet.getInformation().getCustomID() + ")"));
        }
        if (recent.isEmpty()) recentLore.add(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent("&8-"));
        else recentLore.add(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent(getLocaleMessage("plus.profile.recent-click", false)));
        recentMeta.lore(recentLore);
        recentItem.setItemMeta(recentMeta);
        setItem(SLOT_RECENT, recentItem);
        Planet viewerWorld = OpenCreative.getPlanetsManager().getPlanetByPlayer(getPlayer());
        boolean canInvite = online != null && !online.equals(getPlayer()) && viewerWorld != null && viewerWorld.isOwner(getPlayer());
        setItem(SLOT_INVITE, canInvite ? createItem(Material.WRITABLE_BOOK, 1, "plus.profile.invite") : DECORATION_PANE_ITEM);
    }

    @Override
    protected void onCharmsBarClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == SLOT_BACK) {
            new WorldsTabsMenu(getPlayer()).open(getPlayer());
        } else if (event.getRawSlot() == SLOT_RECENT) {
            @SuppressWarnings("deprecation")
            List<Planet> recent = ua.mcchickenstudio.opencreative.plus.players.Conveniences.recent(Bukkit.getOfflinePlayer(nickname).getUniqueId());
            if (!recent.isEmpty()) WorldsTabsMenu.connect(getPlayer(), recent.getFirst());
        } else if (event.getRawSlot() == SLOT_INVITE) {
            invite();
        }
    }

    /**
     * Sends the profile owner a clickable invitation to viewer's world.
     */
    private void invite() {
        Player viewer = getPlayer();
        Player target = Bukkit.getPlayerExact(nickname);
        Planet world = OpenCreative.getPlanetsManager().getPlanetByPlayer(viewer);
        if (target == null || world == null || !world.isOwner(viewer)) return;
        long last = inviteCooldowns.getOrDefault(viewer.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < 30_000L) {
            viewer.sendMessage(getLocaleMessage("plus.profile.invite-cooldown"));
            return;
        }
        inviteCooldowns.put(viewer.getUniqueId(), System.currentTimeMillis());
        String command = "/join " + world.getInformation().getCustomID();
        target.sendMessage(ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent(getLocaleMessage("plus.profile.invite-received")
                        .replace("%player%", viewer.getName()).replace("%world%", world.getInformation().getDisplayName()))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(command))));
        viewer.sendMessage(getLocaleMessage("plus.profile.invite-sent").replace("%player%", target.getName()));
        viewer.closeInventory();
    }

    @Override
    protected void onElementClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        String worldID = getPersistentData(item, getItemIdKey());
        if (worldID.isEmpty()) return;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetById(worldID);
        if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(worldID);
        if (planet != null) WorldsTabsMenu.connect(getPlayer(), planet);
    }

    @Override
    protected ItemStack getNextPageButton() {
        return replacePlaceholdersInItem(createItem(Material.SPECTRAL_ARROW, getCurrentPage() + 1, "menus.all-worlds.items.next-page"),
                new PlaceholderReplacer("page", getCurrentPage() + 1));
    }

    @Override
    protected ItemStack getPreviousPageButton() {
        return replacePlaceholdersInItem(createItem(Material.ARROW, Math.max(1, getCurrentPage() - 1), "menus.all-worlds.items.previous-page"),
                new PlaceholderReplacer("page", getCurrentPage() - 1));
    }

    @Override
    protected ItemStack getNoElementsButton() {
        return createItem(Material.BARRIER, 1, PATH + "items.no-worlds");
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLDS_BROWSER.play(event.getPlayer());
    }
}
