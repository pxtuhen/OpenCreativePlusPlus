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

package ua.mcchickenstudio.opencreative.managers.ignore;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>EssentialsIgnore</h1>
 * This class represents an ignore manager, that uses
 * Essentials plugin for checking whether player is
 * ignored by other player.
 */
public final class EssentialsIgnore implements IgnoreManager {

    @Override
    public boolean isIgnored(@NotNull Player ignorer, @NotNull Player ignored) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (plugin == null) return false;
        if (plugin instanceof Essentials essentials) {
            User user = essentials.getUser(ignorer.getUniqueId());
            User ignoredUser = essentials.getUser(ignored.getUniqueId());
            if (user == null || ignoredUser == null) {
                return false;
            }
            return user.isIgnoredPlayer(ignoredUser);
        }
        return false;
    }

    @Override
    public @NotNull String getName() {
        return "Essentials Ignore Manager";
    }
}
