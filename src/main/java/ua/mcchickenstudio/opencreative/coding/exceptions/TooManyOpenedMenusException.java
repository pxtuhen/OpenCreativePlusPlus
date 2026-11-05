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

package ua.mcchickenstudio.opencreative.coding.exceptions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>TooManyOpenedMenusException</h1>
 * This class represents an exception, that happens when
 * code tries to open too many menus for player at short time.
 */
public final class TooManyOpenedMenusException extends PlayerException {

    public TooManyOpenedMenusException(@NotNull Player player) {
        super(player.getName(), player.getUniqueId(), "Too many actions with inventory was called for " + player + " in short time.");
    }

}
