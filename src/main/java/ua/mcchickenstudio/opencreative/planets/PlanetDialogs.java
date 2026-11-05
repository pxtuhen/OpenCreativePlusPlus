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

package ua.mcchickenstudio.opencreative.planets;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendWarningMessage;

public class PlanetDialogs {

    private final Planet planet;
    private final Map<String, Object> dialogs = new HashMap<>();

    public PlanetDialogs(@NotNull Planet planet) {
        this.planet = planet;
    }

    public void addDialog(@NotNull String id, @NotNull Component title) {
        try {
            Class<?> builderInterface = Class.forName("io.papermc.paper.registry.data.dialog.DialogBase");
            Method builderMethod = builderInterface.getMethod("builder", Component.class);
            Object builderInstance = builderMethod.invoke(null, title);
            dialogs.put(id, builderInstance);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception error) {
            sendWarningMessage("Failed to create a dialog", error);
        }
    }

    public void showDialog(@NotNull Player player, @NotNull String id) {
        try {
            Object dialogBaseBuilder = dialogs.get(id);
            if (dialogBaseBuilder == null) return;
            Method buildMethod = dialogBaseBuilder.getClass().getMethod("build");
            Object dialogBase = buildMethod.invoke(dialogBaseBuilder);

            Class<?> dialogClass = Class.forName("io.papermc.paper.dialog.Dialog");
            Class<?> dialogLikeClass = Class.forName("net.kyori.adventure.dialog.DialogLike");
            Class<?> dialogBaseClass = Class.forName("io.papermc.paper.registry.data.dialog.DialogBase");
            Class<?> dialogTypeClass = Class.forName("io.papermc.paper.registry.data.dialog.type.DialogType");
            Object dialogType = dialogTypeClass.getMethod("notice").invoke(null);
            Consumer<Object> consumer = factory -> {
                try {
                    Object builder = factory.getClass()
                            .getMethod("empty")
                            .invoke(factory);

                    builder.getClass()
                            .getMethod("base", dialogBaseClass)
                            .invoke(builder, dialogBase);

                    builder.getClass()
                            .getMethod("type", dialogTypeClass)
                            .invoke(builder, dialogType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            Object dialog = dialogClass
                    .getMethod("create", Consumer.class)
                    .invoke(null, consumer);

            Audience.class
                    .getMethod("showDialog", dialogLikeClass)
                    .invoke(player, dialog);

        } catch (Exception error) {
            sendWarningMessage("Failed to show a dialog for player " + player.getName(), error);
        }
    }


}
