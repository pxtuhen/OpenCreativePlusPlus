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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.economy.Economy;
import ua.mcchickenstudio.opencreative.menus.ConfirmationMenu;
import ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.List;
import java.util.UUID;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * Payments for shop and marketplace: built-in currency (with reasons in its log) or Vault.
 */
public final class Payments {

    private Payments() {
    }

    public static boolean isWorking() {
        Economy economy = OpenCreative.getEconomy();
        return economy != null && economy.isWorking();
    }

    public static long balance(@NotNull OfflinePlayer player) {
        return isWorking() ? OpenCreative.getEconomy().getBalance(player).longValue() : 0;
    }

    public static @NotNull String format(long amount) {
        return PlusEconomy.get() != null ? PlusEconomy.format(amount) : String.valueOf(amount);
    }

    public static boolean charge(@NotNull OfflinePlayer player, long amount, @NotNull String reason) {
        if (amount <= 0) return true;
        if (!isWorking()) return false;
        PlusEconomy economy = PlusEconomy.get();
        if (economy != null) return economy.take(player.getUniqueId(), amount, reason);
        if (balance(player) < amount) return false;
        return OpenCreative.getEconomy().withdrawMoney(player, amount);
    }

    public static void pay(@NotNull UUID player, long amount, @NotNull String reason) {
        if (amount <= 0 || !isWorking()) return;
        PlusEconomy economy = PlusEconomy.get();
        if (economy != null) economy.add(player, amount, reason);
        else OpenCreative.getEconomy().depositMoney(Bukkit.getOfflinePlayer(player), amount);
    }

    /**
     * "Buy X for Y?" dialog; charges on confirm and then runs {@code onPaid}.
     */
    public static void confirmPurchase(@NotNull Player player, @NotNull Material icon, @NotNull String what,
                                       long price, @NotNull String reason, @NotNull Runnable onPaid) {
        if (!isWorking() && price > 0) {
            player.sendMessage(getLocaleMessage("plus.shop.economy-off"));
            return;
        }
        if (balance(player) < price) {
            player.sendMessage(getLocaleMessage("plus.shop.not-enough").replace("%price%", format(price))
                    .replace("%balance%", format(balance(player))));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        ConfirmationMenu menu = new ConfirmationMenu(
                getLocaleMessage("plus.shop.confirm-title", false).replace("%name%", what),
                icon,
                getLocaleMessage("plus.shop.confirm-name", false).replace("%name%", what).replace("%price%", format(price)),
                List.of(getLocaleMessage("plus.shop.confirm-balance", false).replace("%balance%", format(balance(player)))),
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!charge(player, price, reason)) {
                            player.sendMessage(getLocaleMessage("plus.shop.not-enough").replace("%price%", format(price))
                                    .replace("%balance%", format(balance(player))));
                            Sounds.PLAYER_FAIL.play(player);
                            return;
                        }
                        onPaid.run();
                        Sounds.WORLD_PURCHASE.play(player);
                    }
                });
        // opened on the next tick: opening a menu inside a click of another menu is unsafe
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> menu.open(player));
    }
}
