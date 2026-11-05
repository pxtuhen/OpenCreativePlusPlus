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

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.managers.Manager;

/**
 * <h1>IgnoreManager</h1>
 * This interface represents a manager, that can check
 * whether player is ignored by other player.
 */
public interface IgnoreManager extends Manager {

    /**
     * Checks whether first player ignore seconds player.
     *
     * @param ignorer first player, that can ignore someone.
     * @param ignored second player, that can be ignored by someone.
     * @return true - second player is ignored by first, false - not ignored.
     */
    boolean isIgnored(@NotNull Player ignorer, @NotNull Player ignored);

}
