/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.managers.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of Vault economy,
 * the most used on servers.
 */
public final class VaultEconomy implements Economy {

    private net.milkbowl.vault.economy.Economy vaultEconomy;

    @Override
    public boolean depositMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return vaultEconomy.depositPlayer(offlinePlayer, money.doubleValue()).transactionSuccess();
    }

    @Override
    public boolean withdrawMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return vaultEconomy.withdrawPlayer(offlinePlayer, money.doubleValue()).transactionSuccess();
    }

    @Override
    public @NotNull Number getBalance(OfflinePlayer offlinePlayer) {
        return vaultEconomy.getBalance(offlinePlayer);
    }

    @Override
    public void start() {
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null) {
            vaultEconomy = rsp.getProvider();
        }
    }

    @Override
    public boolean isWorking() {
        return vaultEconomy != null && vaultEconomy.isEnabled();
    }

    @Override
    public void shutdown() {
        vaultEconomy = null;
    }

    @Override
    public @NotNull String getName() {
        return "Vault Economy";
    }
}
