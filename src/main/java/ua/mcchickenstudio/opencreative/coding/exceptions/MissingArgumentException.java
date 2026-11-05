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

import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.variables.ValueType;

/**
 * <h1>MissingArgumentException</h1>
 * This class represents an exception, that occurs
 * when action or condition failed to get required argument.
 */
public final class MissingArgumentException extends RuntimeException {

    private final String argument;
    private final ValueType valueType;

    public MissingArgumentException(@NotNull String argument) {
        this(argument, ValueType.ANY);
    }

    public MissingArgumentException(@NotNull String argument, @NotNull ValueType valueType) {
        super("Missing argument " + argument + ".");
        this.argument = argument;
        this.valueType = valueType;
    }

    public @NotNull ValueType getValueType() {
        return valueType;
    }

    public @NotNull String getArgument() {
        return argument;
    }

}
