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

package ua.mcchickenstudio.opencreative.managers.stability;

import org.jetbrains.annotations.NotNull;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

public enum StabilityState {

    /**
     * This state allows players to create, connect worlds and launch a code.
     */
    FINE("Fine"),
    /**
     * This state allows players to connect loaded worlds, but disallows to create, compile a code.
     */
    NOT_OKAY("Not Okay"),
    /**
     * This state disallows players everything: browsing, connecting, compiling.
     */
    NIGHTMARE("Nightmare");

    private final String name;

    StabilityState(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getLocalized() {
        return getLocaleMessage("creative.stability." + name().toLowerCase().replace("_", "-"), false);
    }

}
