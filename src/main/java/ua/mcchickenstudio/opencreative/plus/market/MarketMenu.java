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

package ua.mcchickenstudio.opencreative.plus.market;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.modules.Module;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.ListBrowserMenu;
import ua.mcchickenstudio.opencreative.plus.chat.ChatPrompt;
import ua.mcchickenstudio.opencreative.plus.rating.StarPickerMenu;
import ua.mcchickenstudio.opencreative.plus.shop.Payments;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * /market — marketplace of code modules: All / Free / Paid / My purchases / My goods.
 */
public final class MarketMenu extends ListBrowserMenu<Module> {

    public enum Tab {ALL, FREE, PAID, PURCHASED, MINE}

    private enum Sort {POPULAR, RATING, NEW}

    private static final String PATH = "plus.market.";
    private static final int[] ELEMENTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] CHARMS = {45, 46, 47, 48, 49, 50, 51, 52, 53, 40};
    private static final int[] DECORATION = {36, 37, 38, 39, 41, 42, 43, 44};
    private static final int SLOT_SORT = 51, SLOT_SEARCH = 52, SLOT_BALANCE = 40;
    private static final Map<Integer, Tab> TAB_SLOTS = Map.of(46, Tab.ALL, 47, Tab.FREE, 48, Tab.PAID, 49, Tab.PURCHASED, 50, Tab.MINE);
    private static final Map<Tab, Material> TAB_ICONS = Map.of(Tab.ALL, Material.CHEST, Tab.FREE, Material.EMERALD,
            Tab.PAID, Material.GOLD_INGOT, Tab.PURCHASED, Material.ENDER_CHEST, Tab.MINE, Material.WRITABLE_BOOK);

    private final Tab tab;
    private final String query;
    private Sort sort = Sort.POPULAR;

    public MarketMenu(@NotNull Player player, @NotNull Tab tab, @NotNull String query) {
        super(player, getLocaleMessage(PATH + "title", false), ELEMENTS, CHARMS, DECORATION, 22, 45, 53);
        this.tab = tab;
        this.query = query;
    }

    @Override
    public List<Module> getElements() {
        UUID me = getPlayer().getUniqueId();
        List<Module> modules = new ArrayList<>();
        if (tab == Tab.MINE) {
            modules.addAll(OpenCreative.getModuleManager().getPlayerModules(me));
        } else {
            for (Market.Listing listing : Market.all()) {
                boolean show = switch (tab) {
                    case FREE -> listing.free();
                    case PAID -> !listing.free();
                    case PURCHASED -> listing.licenses().contains(me) || listing.users().contains(me);
                    default -> true;
                };
                Module module = Market.module(listing.moduleId());
                if (show && module != null) modules.add(module);
            }
        }
        if (!query.isEmpty()) {
            String needle = query.toLowerCase();
            modules.removeIf(module -> !ChatColor.stripColor(module.getInformation().getDisplayName()).toLowerCase().contains(needle));
        }
        Comparator<Module> comparator = switch (sort) {
            case RATING -> Comparator.comparingDouble(module -> Optional.ofNullable(Market.get(module.getId())).map(Market.Listing::average).orElse(0.0));
            case NEW -> Comparator.comparingLong(module -> Optional.ofNullable(Market.get(module.getId())).map(Market.Listing::listedAt).orElse(0L));
            default -> Comparator.comparingInt(module -> Optional.ofNullable(Market.get(module.getId())).map(Market.Listing::sales).orElse(0));
        };
        modules.sort(comparator.reversed());
        return modules;
    }

    @Override
    protected ItemStack getElementIcon(Module module) {
        ItemStack icon = module.getInformation().getIcon().clone();
        ItemMeta meta = icon.getItemMeta();
        Market.Listing listing = Market.get(module.getId());
        List<Component> lore = new ArrayList<>();
        lore.add(toComponent("&7" + module.getInformation().getDescription().replace("\\n", " ")));
        lore.add(Component.empty());
        lore.add(toComponent(getLocaleMessage(PATH + "author", false).replace("%player%", module.getOwnerName())));
        if (listing == null) {
            lore.add(toComponent(getLocaleMessage(PATH + "not-listed", false)));
        } else {
            lore.add(toComponent(listing.free() ? getLocaleMessage(PATH + "free", false)
                    : getLocaleMessage(PATH + "price", false).replace("%price%", Payments.format(listing.price()))));
            lore.add(toComponent(getLocaleMessage(PATH + "stats", false).replace("%sales%", String.valueOf(listing.sales()))
                    .replace("%rating%", listing.stars().isEmpty() ? "-" : String.format(Locale.ROOT, "%.1f (%d)", listing.average(), listing.stars().size()))));
        }
        lore.add(Component.empty());
        if (tab == Tab.MINE) {
            lore.add(toComponent(getLocaleMessage(PATH + (listing == null ? "hint-sell" : "hint-change-price"), false)));
            if (listing != null) lore.add(toComponent(getLocaleMessage(PATH + "hint-unlist", false)));
            lore.add(toComponent(getLocaleMessage(PATH + "hint-install", false)));
        } else if (listing != null && Market.hasAccess(getPlayer(), listing)) {
            lore.add(toComponent(getLocaleMessage(PATH + "hint-install-left", false)));
            if (!listing.seller().equals(getPlayer().getUniqueId())) lore.add(toComponent(getLocaleMessage(PATH + "hint-rate", false)));
        } else if (listing != null) {
            lore.add(toComponent(getLocaleMessage(PATH + "hint-buy", false).replace("%price%", Payments.format(listing.price()))));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(OpenCreative.getPlugin(), "market_module"),
                org.bukkit.persistence.PersistentDataType.INTEGER, module.getId());
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    protected void fillOtherItems() {
        for (Map.Entry<Integer, Tab> entry : TAB_SLOTS.entrySet()) {
            ItemStack item = createItem(TAB_ICONS.get(entry.getValue()), 1, PATH + "tabs." + entry.getValue().name().toLowerCase());
            if (entry.getValue() == tab) {
                ItemMeta meta = item.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
            setItem(entry.getKey(), item);
        }
        setItem(SLOT_SORT, replacePlaceholdersInItem(createItem(Material.HOPPER, 1, PATH + "items.sort"),
                new PlaceholderReplacer("sort", getLocaleMessage(PATH + "sort." + sort.name().toLowerCase(), false))));
        setItem(SLOT_SEARCH, replacePlaceholdersInItem(createItem(Material.SPYGLASS, 1, PATH + "items.search"),
                new PlaceholderReplacer("query", query.isEmpty() ? "-" : query)));
        setItem(SLOT_BALANCE, replacePlaceholdersInItem(createItem(Material.SUNFLOWER, 1, PATH + "items.balance"),
                new PlaceholderReplacer("balance", Payments.format(Payments.balance(getPlayer())))));
        if (elements.isEmpty()) setItem(22, getNoElementsButton());
    }

    @Override
    protected void onCharmsBarClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = getPlayer();
        Tab target = TAB_SLOTS.get(event.getRawSlot());
        if (target != null) {
            if (target != tab) new MarketMenu(player, target, query).open(player);
            return;
        }
        if (event.getRawSlot() == SLOT_SORT) {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            updateElements();
            setCurrentPage(1);
            fillElements(1);
            fillArrowsItems(1);
            fillOtherItems();
            Sounds.MENU_WORLDS_BROWSER_SORT.play(player);
        } else if (event.getRawSlot() == SLOT_SEARCH) {
            if (event.isRightClick()) {
                new MarketMenu(player, tab, "").open(player);
                return;
            }
            ChatPrompt.ask(player, getLocaleMessage(PATH + "search-prompt"), text -> new MarketMenu(player, tab, text).open(player));
        }
    }

    @Override
    protected void onElementClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;
        Integer id = item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(OpenCreative.getPlugin(), "market_module"),
                org.bukkit.persistence.PersistentDataType.INTEGER);
        if (id == null) return;
        Module module = Market.module(id);
        if (module == null) return;
        Player player = getPlayer();
        Market.Listing listing = Market.get(id);
        ClickType click = event.getClick();
        if (tab == Tab.MINE) {
            if (click == ClickType.SHIFT_RIGHT && listing != null) {
                Market.unlist(id);
                player.sendMessage(getLocaleMessage(PATH + "unlisted").replace("%name%", module.getInformation().getDisplayName()));
                new MarketMenu(player, tab, query).open(player);
            } else if (click.isRightClick()) {
                Market.install(player, module);
            } else {
                ChatPrompt.ask(player, getLocaleMessage(PATH + "price-prompt"), text -> {
                    long price;
                    try {
                        price = Long.parseLong(text.trim());
                    } catch (NumberFormatException error) {
                        player.sendMessage(getLocaleMessage(PATH + "bad-price"));
                        return;
                    }
                    Market.SellResult result = Market.list(player, module, price);
                    player.sendMessage(getLocaleMessage(PATH + "sell." + result.name().toLowerCase().replace('_', '-'))
                            .replace("%name%", module.getInformation().getDisplayName())
                            .replace("%price%", price == 0 ? getLocaleMessage(PATH + "free-word", false) : Payments.format(price))
                            .replace("%min%", String.valueOf(OpenCreative.getPlugin().getConfig().getLong("marketplace.min-price", 10)))
                            .replace("%max%", String.valueOf(OpenCreative.getPlugin().getConfig().getLong("marketplace.max-price", 100000))));
                });
            }
            return;
        }
        if (listing == null) return;
        if (click.isRightClick() && Market.hasAccess(player, listing) && !listing.seller().equals(player.getUniqueId())) {
            int current = listing.stars().getOrDefault(player.getUniqueId(), 0);
            new StarPickerMenu(getLocaleMessage(PATH + "rate-title", false), current, value -> Market.rate(player, id, value)).open(player);
            return;
        }
        if (Market.hasAccess(player, listing)) {
            // remember free modules taken, so they appear in "My purchases"
            if (listing.free()) Market.markTaken(player, id);
            Market.install(player, module);
        } else {
            Market.buy(player, listing);
        }
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
        return createItem(Material.BARRIER, 1, PATH + "empty." + tab.name().toLowerCase());
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLDS_BROWSER.play(event.getPlayer());
    }

    /**
     * /market [search text]
     */
    public static final class MarketCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getLocaleMessage("only-players"));
                return true;
            }
            if (!Market.isEnabled()) {
                player.sendMessage(getLocaleMessage(PATH + "disabled"));
                return true;
            }
            new MarketMenu(player, Tab.ALL, String.join(" ", args)).open(player);
            return true;
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return List.of();
        }
    }
}
