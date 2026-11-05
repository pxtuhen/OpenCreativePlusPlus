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

public final class DisabledIgnore implements IgnoreManager {

    @Override
    public boolean isIgnored(@NotNull Player ignorer, @NotNull Player ignored) {
        return false;
    }

    @Override
    public @NotNull String getName() {
        return "Disabled Ignore Manager";
    }
}
