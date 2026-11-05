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
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.ListBrowserMenu;
import ua.mcchickenstudio.opencreative.menus.world.WorldAccessMenu;
import ua.mcchickenstudio.opencreative.menus.world.WorldGenerationMenu;
import ua.mcchickenstudio.opencreative.menus.world.WorldModerationMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.planets.PlanetInfo;
import ua.mcchickenstudio.opencreative.plus.players.FavoriteWorlds;
import ua.mcchickenstudio.opencreative.plus.players.LastWorlds;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.utils.MessageUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerConfirmation;

import java.time.Duration;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;

/**
 * Compass menu of OpenCreative++: one menu with four tabs in the bottom row —
 * Recommended worlds (opens first), Player's worlds, Favorite worlds, Search.
 * <p>
 * Row above the tabs: Continue (last world), category filter, profile.
 * Built on OpenCreative+ browsers: same world icons, clicks and checks.
 */
public final class WorldsTabsMenu extends ListBrowserMenu<Planet> {

    public enum Tab {
        RECOMMENDED(Material.NETHER_STAR),
        OWN(Material.REPEATING_COMMAND_BLOCK),
        FAVORITES(Material.GOLDEN_APPLE),
        SEARCH(Material.SPYGLASS);

        private final Material icon;

        Tab(Material icon) {
            this.icon = icon;
        }

        String key() {
            return name().toLowerCase();
        }
    }

    private enum Sort {ONLINE, LIKES, NEW, PLAYING}

    private static final String PATH = "plus.menus.worlds.";
    private static final int[] ELEMENTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int SLOT_CONTINUE = 37, SLOT_CATEGORY = 39, SLOT_PROFILE = 41, SLOT_CONTEST = 43;
    private static final int[] CHARMS = {45, 46, 47, 48, 49, 50, 51, 52, 53, SLOT_CONTINUE, SLOT_CATEGORY, SLOT_PROFILE, SLOT_CONTEST};
    private static final int[] DECORATION = {36, 38, 40, 42, 44};
    private static final int SLOT_PREVIOUS = 45, SLOT_NEXT = 53, SLOT_EMPTY = 22;
    private static final int SLOT_SORT = 50, SLOT_LIMITS = 51, SLOT_CREATE = 52;
    private static final Map<Integer, Tab> TAB_SLOTS = Map.of(46, Tab.RECOMMENDED, 47, Tab.OWN, 48, Tab.FAVORITES, 49, Tab.SEARCH);

    /**
     * Last search of every player: query and found world ids.
     */
    private static final Map<UUID, Map.Entry<String, List<Integer>>> searches = new HashMap<>();

    private final Tab tab;
    private Sort sort = Sort.ONLINE;
    private @Nullable PlanetInfo.Category category = null;
    private Set<Integer> recommendedIds = Set.of();

    public WorldsTabsMenu(@NotNull Player player) {
        this(player, Tab.RECOMMENDED);
    }

    public WorldsTabsMenu(@NotNull Player player, @NotNull Tab tab) {
        super(player, getLocaleMessage(PATH + "title." + tab.key(), false),
                ELEMENTS, CHARMS, DECORATION, SLOT_EMPTY, SLOT_PREVIOUS, SLOT_NEXT);
        this.tab = tab;
    }

    /**
     * Opens Search tab with results; called after player typed the query in chat.
     */
    public static void openSearchResults(@NotNull Player player, @NotNull Collection<Planet> found, @NotNull String query) {
        searches.put(player.getUniqueId(), Map.entry(query, found.stream().map(Planet::getId).toList()));
        new WorldsTabsMenu(player, Tab.SEARCH).open(player);
    }

    @Override
    public List<Planet> getElements() {
        List<Planet> planets = switch (tab) {
            case RECOMMENDED -> {
                // Admin's recommendations first, then all other worlds (in OpenCreative+ there was
                // a separate "All worlds" button for them).
                List<Planet> recommended = new ArrayList<>(OpenCreative.getPlanetsManager().getRecommendedPlanets());
                recommendedIds = new HashSet<>();
                for (Planet planet : recommended) recommendedIds.add(planet.getId());
                List<Planet> others = new ArrayList<>();
                for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                    if (!recommendedIds.contains(planet.getId())) others.add(planet);
                }
                recommended = filter(recommended);
                others = filter(others);
                recommended.sort(comparator());
                others.sort(comparator());
                recommended.addAll(others);
                yield recommended;
            }
            case OWN -> filter(new ArrayList<>(OpenCreative.getPlanetsManager().getPlanetsByOwner(getPlayer())));
            case FAVORITES -> filter(FavoriteWorlds.getPlanets(getPlayer().getUniqueId()));
            case SEARCH -> {
                List<Planet> found = new ArrayList<>();
                var search = searches.get(getPlayer().getUniqueId());
                if (search != null) {
                    for (int id : search.getValue()) {
                        Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(id));
                        if (planet != null) found.add(planet);
                    }
                }
                yield filter(found);
            }
        };
        if (tab != Tab.RECOMMENDED) planets.sort(comparator());
        return planets;
    }

    private @NotNull List<Planet> filter(@NotNull List<Planet> planets) {
        List<Planet> result = new ArrayList<>();
        for (Planet planet : planets) {
            if (category != null && planet.getInformation().getCategory() != category) continue;
            if (sort == Sort.PLAYING && planet.getInformation().getAsyncOnline() <= 0) continue;
            result.add(planet);
        }
        return result;
    }

    private @NotNull Comparator<Planet> comparator() {
        return switch (sort) {
            case LIKES -> Comparator.comparingInt((Planet planet) -> planet.getInformation().getReputation()).reversed();
            case NEW -> Comparator.comparingLong(Planet::getCreationTime).reversed();
            default -> Comparator.comparingInt((Planet planet) -> planet.getInformation().getAsyncOnline()).reversed();
        };
    }

    @Override
    protected ItemStack getElementIcon(Planet planet) {
        ItemStack icon = planet.getInformation().getIcon().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(Objects.requireNonNull(meta.lore()));
        if (tab == Tab.RECOMMENDED && recommendedIds.contains(planet.getId())) {
            lore.addFirst(toComponent(getLocaleMessage(PATH + "hint-recommended", false)));
        }
        boolean favorite = FavoriteWorlds.isFavorite(getPlayer().getUniqueId(), planet.getId());
        lore.add(toComponent(getLocaleMessage(PATH + (favorite ? "hint-unfavorite" : "hint-favorite"), false)));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    protected void fillOtherItems() {
        for (Map.Entry<Integer, Tab> entry : TAB_SLOTS.entrySet()) {
            setItem(entry.getKey(), tabItem(entry.getValue()));
        }
        setItem(SLOT_SORT, replacePlaceholdersInItem(createItem(Material.HOPPER, 1, PATH + "items.sort"),
                new PlaceholderReplacer("sort", getLocaleMessage(PATH + "sort." + sort.name().toLowerCase(), false))));
        setItem(SLOT_LIMITS, createItem(Material.BOOK, 1, PATH + "items.limits"));
        setItem(SLOT_CREATE, tab == Tab.OWN ? createWorldItem() : DECORATION_PANE_ITEM);

        Planet last = LastWorlds.get(getPlayer().getUniqueId());
        setItem(SLOT_CONTINUE, last == null ? DECORATION_PANE_ITEM
                : replacePlaceholdersInItem(createItem(Material.ENDER_PEARL, 1, PATH + "items.continue"),
                new PlaceholderReplacer("world", last.getInformation().getDisplayName(), "id", last.getId())));
        setItem(SLOT_CATEGORY, replacePlaceholdersInItem(
                createItem(category == null ? Material.CHEST : category.getMaterial(), 1, PATH + "items.category"),
                new PlaceholderReplacer("category", category == null
                        ? getLocaleMessage(PATH + "category-all", false) : category.getLocaleName())));
        setItem(SLOT_PROFILE, createItem(Material.PLAYER_HEAD, 1, PATH + "items.profile"));
        setItem(SLOT_CONTEST, ua.mcchickenstudio.opencreative.plus.contest.Contests.isActive()
                ? replacePlaceholdersInItem(createItem(Material.GOLDEN_HELMET, 1, "plus.contest.compass"),
                new PlaceholderReplacer("name", ua.mcchickenstudio.opencreative.plus.contest.Contests.name(), "time", ua.mcchickenstudio.opencreative.plus.contest.Contests.timeLeft()))
                : DECORATION_PANE_ITEM);

        if (elements.isEmpty()) setItem(SLOT_EMPTY, getNoElementsButton());
    }

    private @NotNull ItemStack tabItem(@NotNull Tab target) {
        ItemStack item = createItem(target.icon, 1, PATH + "tabs." + target.key());
        if (target == Tab.SEARCH) {
            var search = searches.get(getPlayer().getUniqueId());
            item = replacePlaceholdersInItem(item, new PlaceholderReplacer("query",
                    search == null ? getLocaleMessage(PATH + "no-query", false) : search.getKey()));
        }
        if (target == tab) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createWorldItem() {
        if (OpenCreative.getSettings().isWorldGenerationUnavailable()) {
            return createItem(Material.BARRIER, 1, PATH + "items.creation-disabled");
        }
        int amount = ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsUsed(getPlayer());
        int limit = ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsLimit(getPlayer());
        if (amount >= limit) {
            return replacePlaceholdersInItem(createItem(Material.RED_CONCRETE, 1, "menus.own-worlds.items.limit"),
                    new PlaceholderReplacer("limit", limit, "planets", amount));
        }
        return createItem(Material.LIME_CONCRETE, 1, "menus.own-worlds.items.create-world");
    }

    private void refresh() {
        updateElements();
        setCurrentPage(1);
        fillElements(getCurrentPage());
        fillArrowsItems(getCurrentPage());
        fillOtherItems();
    }

    @Override
    protected void onCharmsBarClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        Player player = getPlayer();
        Tab target = TAB_SLOTS.get(slot);
        if (target != null) {
            if (target == Tab.SEARCH && tab == Tab.SEARCH) {
                startSearch(event);
            } else if (target != tab) {
                new WorldsTabsMenu(player, target).open(player);
            }
            return;
        }
        switch (slot) {
            case SLOT_SORT -> {
                sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
                refresh();
                Sounds.MENU_WORLDS_BROWSER_SORT.play(player);
            }
            case SLOT_CATEGORY -> {
                PlanetInfo.Category[] categories = PlanetInfo.Category.values();
                if (event.isRightClick()) {
                    category = null;
                } else {
                    category = category == null ? categories[0]
                            : category.ordinal() + 1 < categories.length ? categories[category.ordinal() + 1] : null;
                }
                refresh();
                Sounds.MENU_WORLDS_BROWSER_CATEGORY.play(player);
            }
            case SLOT_CONTINUE -> {
                Planet last = LastWorlds.get(player.getUniqueId());
                if (last != null) connect(player, last);
            }
            case SLOT_PROFILE -> new ProfileMenu(player, player.getName()).open(player);
            case SLOT_CONTEST -> {
                if (ua.mcchickenstudio.opencreative.plus.contest.Contests.isActive()) new ua.mcchickenstudio.opencreative.plus.contest.ContestMenu(player).open(player);
            }
            case SLOT_LIMITS -> new LimitsMenu(player).open(player);
            case SLOT_CREATE -> {
                if (tab == Tab.OWN) createWorld(player);
            }
            default -> {
            }
        }
    }

    /**
     * Same input as in OpenCreative+ compass: left — by name (and description), right — by ID, shift + left — by owner.
     */
    private void startSearch(@NotNull InventoryClickEvent event) {
        PlayerConfirmation request = switch (event.getClick()) {
            case LEFT -> PlayerConfirmation.FIND_PLANETS_BY_NAME;
            case RIGHT, SHIFT_RIGHT -> PlayerConfirmation.FIND_PLANETS_BY_ID;
            case SHIFT_LEFT -> PlayerConfirmation.FIND_PLANETS_BY_OWNER;
            default -> null;
        };
        if (request == null) return;
        Player player = getPlayer();
        player.closeInventory();
        String searchQuery = request == PlayerConfirmation.FIND_PLANETS_BY_NAME ? "world-name"
                : request == PlayerConfirmation.FIND_PLANETS_BY_ID ? "id" : "owner";
        String what = getLocaleMessage("menus.all-worlds.items.search." + searchQuery);
        player.showTitle(Title.title(
                Component.text(getLocaleMessage("menus.all-worlds.items.search.title").replace("%search%", what)),
                Component.text(getLocaleMessage("menus.all-worlds.items.search.subtitle").replace("%search%", what)),
                Title.Times.times(Duration.ofMillis(750), Duration.ofSeconds(20), Duration.ofMillis(750))
        ));
        player.sendMessage(MessageUtils.getPlayerLocaleMessage("menus.all-worlds.items.search.usage", player).replace("%search%", what));
        Sounds.MENU_WORLD_SEARCH.play(player);
        PlayerConfirmation.setConfirmation(player, request);
    }

    /**
     * Same checks as OpenCreative+ "Create world" button in own worlds menu.
     */
    private void createWorld(@NotNull Player player) {
        if (OpenCreative.getSettings().isWorldGenerationUnavailable()) return;
        int amount = ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsUsed(player);
        int limit = ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.worldsLimit(player);
        if (amount >= limit) {
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        long minSeconds = OpenCreative.getSettings().getRequirements().getWorldCreationMinSeconds();
        long playedSeconds = (System.currentTimeMillis() - player.getFirstPlayed()) / 1000;
        if (playedSeconds < minSeconds) {
            Sounds.PLAYER_CANCEL.play(player);
            player.closeInventory();
            player.sendMessage(getPlayerLocaleMessage("creating-world.not-enough-played", player)
                    .replace("%time%", convertTime((minSeconds - playedSeconds) * 1000)));
            return;
        }
        new WorldGenerationMenu(player).open(player);
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
        if (planet == null) return;
        Player player = getPlayer();
        switch (event.getClick()) {
            case RIGHT, SHIFT_RIGHT -> toggleFavorite(player, planet);
            case SHIFT_LEFT -> {
                if (player.hasPermission("opencreative.moderation.menus")) {
                    new WorldModerationMenu(planet).open(player);
                } else {
                    connect(player, planet);
                }
            }
            default -> {
                if (tab == Tab.OWN) {
                    new WorldAccessMenu(planet).open(player);
                } else {
                    connect(player, planet);
                }
            }
        }
    }

    private void toggleFavorite(@NotNull Player player, @NotNull Planet planet) {
        boolean favorite = FavoriteWorlds.toggle(player.getUniqueId(), planet.getId());
        player.sendMessage(getLocaleMessage(favorite ? "world.favorites.added" : "world.favorites.removed")
                .replace("%id%", String.valueOf(planet.getId())));
        Sounds.MENU_NEXT_CHOICE.play(player);
        if (tab == Tab.FAVORITES) {
            refresh();
        } else {
            fillElements(getCurrentPage());
            fillArrowsItems(getCurrentPage());
        }
    }

    static void connect(@NotNull Player player, @NotNull Planet planet) {
        if (planet.equals(OpenCreative.getPlanetsManager().getPlanetByPlayer(player))) {
            player.sendMessage(getPlayerLocaleMessage("same-world", player));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        player.closeInventory();
        planet.connectPlayer(player);
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
        if (tab == Tab.SEARCH && !searches.containsKey(getPlayer().getUniqueId())) {
            return createItem(Material.SPYGLASS, 1, PATH + "empty.search-hint");
        }
        return createItem(Material.BARRIER, 1, PATH + "empty." + tab.key());
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        switch (tab) {
            case RECOMMENDED -> Sounds.MENU_OPEN_RECOMMENDATIONS.play(event.getPlayer());
            case OWN -> Sounds.MENU_OPEN_OWN_WORLDS_BROWSER.play(event.getPlayer());
            default -> Sounds.MENU_OPEN_WORLDS_BROWSER.play(event.getPlayer());
        }
    }
}
