/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.indev.coding6;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.menus.layouts.ArgumentSlot;
import ua.mcchickenstudio.opencreative.coding.menus.layouts.ParameterSlot;
import ua.mcchickenstudio.opencreative.coding.variables.ValueType;

import java.util.ArrayList;
import java.util.List;

public class RequiredArguments {

    private final List<ArgumentSlot> arguments;

    public RequiredArguments(@NotNull List<ArgumentSlot> slots) {
        this.arguments = slots;
    }

    public @NotNull List<ArgumentSlot> getArguments() {
        return arguments;
    }

    public static @NotNull RequiredArgumentsBuilder builder() {
        return new RequiredArgumentsBuilder();
    }

    public static class RequiredArgumentsBuilder {

        private final List<ArgumentSlot> slots = new ArrayList<>();

        public RequiredArgumentsBuilder text(@NotNull String id) {
            slots.add(new ArgumentSlot(id, ValueType.TEXT));
            return this;
        }

        public RequiredArgumentsBuilder parameter(@NotNull String id, @NotNull Choice... choices) {
            List<Object> values = new ArrayList<>();
            List<Material> icons = new ArrayList<>();
            for (Choice choice : choices) {
                values.add(choice.id());
                icons.add(choice.material());
            }
            slots.add(new ParameterSlot(id, values, icons));
            return this;
        }

        public RequiredArgumentsBuilder texts(@NotNull String id, int amount) {
            slots.add(new ArgumentSlot(id, ValueType.TEXT, (byte) amount));
            return this;
        }

        public @NotNull RequiredArguments build() {
            return new RequiredArguments(slots);
        }

    }

}
