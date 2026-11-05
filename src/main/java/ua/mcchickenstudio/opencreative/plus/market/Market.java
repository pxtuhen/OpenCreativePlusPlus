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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.modules.Module;
import ua.mcchickenstudio.opencreative.events.module.ModuleInstallationEvent;
import ua.mcchickenstudio.opencreative.planets.DevPlanet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;
import ua.mcchickenstudio.opencreative.plus.shop.Payments;

import java.io.File;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * Marketplace of code modules (shop.yml → marketplace). Data: plugins/OpenCreativePP/market.yml.
 * <p>
 * A listing is a module of OpenCreative+ with a price (0 = free). Buying gives a license:
 * the buyer installs the module into any own world, any number of times. Author stays the owner.
 */
public final class Market implements Listener {

    private static final PlusLog log = PlusLog.module("Маркет");
    private static YamlConfiguration data;

    /**
     * One module on the market.
     */
    public record Listing(int moduleId, long price, UUID seller, long listedAt, int sales,
                          Set<UUID> licenses, Map<UUID, Integer> stars, Set<UUID> users) {
        public boolean free() {
            return price <= 0;
        }

        public double average() {
            return stars.isEmpty() ? 0 : stars.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        }
    }

    private static @NotNull FileConfiguration settings() {
        return OpenCreative.getPlugin().getConfig();
    }

    public static boolean isEnabled() {
        return settings().getBoolean("marketplace.enabled", true);
    }

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "market.yml");
    }

    private static synchronized @NotNull YamlConfiguration data() {
        if (data == null) data = YamlConfiguration.loadConfiguration(file());
        return data;
    }

    private static synchronized void save() {
        try {
            data().save(file());
        } catch (Exception error) {
            log.error("Не удалось сохранить market.yml", error);
        }
    }

    public static synchronized @Nullable Listing get(int moduleId) {
        ConfigurationSection section = data().getConfigurationSection("listings." + moduleId);
        if (section == null) return null;
        Set<UUID> licenses = new HashSet<>();
        for (String uuid : section.getStringList("licenses")) {
            try {
                licenses.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Set<UUID> users = new HashSet<>();
        for (String uuid : section.getStringList("users")) {
            try {
                users.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Map<UUID, Integer> stars = new HashMap<>();
        ConfigurationSection starsSection = section.getConfigurationSection("stars");
        if (starsSection != null) {
            for (String uuid : starsSection.getKeys(false)) {
                try {
                    stars.put(UUID.fromString(uuid), starsSection.getInt(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        UUID seller;
        try {
            seller = UUID.fromString(section.getString("seller", ""));
        } catch (IllegalArgumentException error) {
            return null;
        }
        return new Listing(moduleId, section.getLong("price"), seller, section.getLong("listed-at"),
                section.getInt("sales"), licenses, stars, users);
    }

    /**
     * Listings, whose modules still exist.
     */
    public static synchronized @NotNull List<Listing> all() {
        List<Listing> list = new ArrayList<>();
        ConfigurationSection section = data().getConfigurationSection("listings");
        if (section == null) return list;
        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                if (module(id) == null) continue;
                Listing listing = get(id);
                if (listing != null) list.add(listing);
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }

    public static @Nullable Module module(int id) {
        return OpenCreative.getModuleManager().getModuleById(String.valueOf(id));
    }

    public static boolean hasAccess(@NotNull Player player, @NotNull Listing listing) {
        return listing.free() || listing.seller().equals(player.getUniqueId()) || listing.licenses().contains(player.getUniqueId())
                || player.hasPermission("opencreativepp.admin");
    }

    // ------------------------------------------------------------------ selling

    public enum SellResult {OK, DISABLED, NOT_OWNER, BAD_PRICE, TOO_MANY}

    public static synchronized @NotNull SellResult list(@NotNull Player seller, @NotNull Module module, long price) {
        if (!isEnabled()) return SellResult.DISABLED;
        if (!module.isOwner(seller)) return SellResult.NOT_OWNER;
        long min = settings().getLong("marketplace.min-price", 10), max = settings().getLong("marketplace.max-price", 100000);
        if (price < 0 || (price > 0 && (price < min || price > max))) return SellResult.BAD_PRICE;
        String path = "listings." + module.getId();
        if (!data().contains(path)) {
            long mine = all().stream().filter(listing -> listing.seller().equals(seller.getUniqueId())).count();
            if (mine >= settings().getInt("marketplace.max-listings-per-player", 10)) return SellResult.TOO_MANY;
            data().set(path + ".listed-at", System.currentTimeMillis());
            data().set(path + ".sales", 0);
        }
        data().set(path + ".seller", seller.getUniqueId().toString());
        data().set(path + ".price", price);
        module.getInformation().setPublic(true);
        save();
        return SellResult.OK;
    }

    public static synchronized void unlist(int moduleId) {
        data().set("listings." + moduleId, null);
        save();
    }

    // ------------------------------------------------------------------ buying

    /**
     * Opens purchase confirmation; on success gives a license and pays the author (minus commission).
     */
    public static void buy(@NotNull Player buyer, @NotNull Listing listing) {
        Module module = module(listing.moduleId());
        if (module == null) return;
        if (hasAccess(buyer, listing)) {
            buyer.sendMessage(getLocaleMessage("plus.market.already-own"));
            return;
        }
        Payments.confirmPurchase(buyer, Material.CHEST, module.getInformation().getDisplayName(), listing.price(),
                "покупка модуля " + listing.moduleId(), () -> {
                    synchronized (Market.class) {
                        String path = "listings." + listing.moduleId();
                        List<String> licenses = new ArrayList<>(data().getStringList(path + ".licenses"));
                        licenses.add(buyer.getUniqueId().toString());
                        data().set(path + ".licenses", licenses);
                        data().set(path + ".sales", data().getInt(path + ".sales") + 1);
                        save();
                    }
                    double commission = Math.max(0, Math.min(1, settings().getDouble("marketplace.commission", 0.1)));
                    long income = Math.round(listing.price() * (1 - commission));
                    Payments.pay(listing.seller(), income, "продажа модуля " + listing.moduleId());
                    buyer.sendMessage(getLocaleMessage("plus.market.bought").replace("%name%", module.getInformation().getDisplayName())
                            .replace("%price%", Payments.format(listing.price())));
                    Player seller = Bukkit.getPlayer(listing.seller());
                    if (seller != null) {
                        seller.sendMessage(getLocaleMessage("plus.market.sold").replace("%name%", module.getInformation().getDisplayName())
                                .replace("%player%", buyer.getName()).replace("%income%", Payments.format(income)));
                    }
                    ua.mcchickenstudio.opencreative.plus.players.Discord.send("market",
                            getLocaleMessage("plus.discord.market-sale", false).replace("%name%", module.getInformation().getDisplayName())
                                    .replace("%player%", buyer.getName()).replace("%price%", Payments.format(listing.price())));
                });
    }

    /**
     * Installs module into dev world, where player is now.
     */
    public static void install(@NotNull Player player, @NotNull Module module) {
        DevPlanet devPlanet = OpenCreative.getPlanetsManager().getDevPlanet(player);
        if (devPlanet == null) {
            player.sendMessage(getLocaleMessage("plus.market.need-dev"));
            return;
        }
        player.closeInventory();
        module.place(devPlanet, player);
    }

    /**
     * Remembers, that player took a free module (for "My purchases" and rating).
     */
    public static synchronized void markTaken(@NotNull Player player, int moduleId) {
        String path = "listings." + moduleId + ".users";
        List<String> users = new ArrayList<>(data().getStringList(path));
        if (users.contains(player.getUniqueId().toString())) return;
        users.add(player.getUniqueId().toString());
        data().set(path, users);
        save();
    }

    public static synchronized void rate(@NotNull Player player, int moduleId, int value) {
        data().set("listings." + moduleId + ".stars." + player.getUniqueId(), Math.max(1, Math.min(5, value)));
        save();
        player.sendMessage(getLocaleMessage("plus.market.rated").replace("%stars%", ua.mcchickenstudio.opencreative.plus.rating.Ratings.stars(value)));
    }

    /**
     * Paid modules can't be installed without a license (also through /module and modules browser of OpenCreative+).
     */
    @EventHandler
    public void onInstall(ModuleInstallationEvent event) {
        Listing listing = get(event.getModule().getId());
        if (listing == null || hasAccess(event.getPlayer(), listing)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(getLocaleMessage("plus.market.need-license")
                .replace("%price%", Payments.format(listing.price())));
    }
}
