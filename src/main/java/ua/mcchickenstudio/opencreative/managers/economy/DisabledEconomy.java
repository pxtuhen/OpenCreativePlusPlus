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

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a disabled economy manager, that will be
 * used by default, if Vault will be not detected. It will not
 * do any money operation and will be disabled forever.
 */
public final class DisabledEconomy implements Economy {

    @Override
    public boolean depositMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return false;
    }

    @Override
    public boolean withdrawMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return false;
    }

    @Override
    public @NotNull Number getBalance(OfflinePlayer offlinePlayer) {
        return 0;
    }

    @Override
    public void start() {}

    @Override
    public void shutdown() {}

    @Override
    public boolean isWorking() {
        return false;
    }

    @Override
    public @NotNull String getName() {
        return "Disabled Economy";
    }
}
