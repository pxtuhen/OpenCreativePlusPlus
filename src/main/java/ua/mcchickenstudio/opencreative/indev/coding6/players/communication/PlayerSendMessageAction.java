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

package ua.mcchickenstudio.opencreative.indev.coding6.players.communication;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooLongTextException;
import ua.mcchickenstudio.opencreative.coding.menus.MenusCategory;
import ua.mcchickenstudio.opencreative.indev.coding6.Choice;
import ua.mcchickenstudio.opencreative.indev.coding6.RequiredArguments;
import ua.mcchickenstudio.opencreative.indev.coding6.RequiresArguments;
import ua.mcchickenstudio.opencreative.indev.coding6.players.PlayerAction;

import java.util.List;

public class PlayerSendMessageAction extends PlayerAction implements RequiresArguments {

    public PlayerSendMessageAction() {
        super("send_message");
    }

    @Override
    public void executePlayer(@NotNull Player player) {
        String separator = arguments().getText("type", "new-line", this);
        List<Component> messages = arguments().getComponentList("messages", this);

        TextComponent.Builder builder = Component.text();
        Component separatorComponent = switch (separator) {
            case "new-line" -> Component.newline();
            case "join-spaces" -> Component.space();
            default -> Component.empty();
        };

        for (int i = 0; i < messages.size(); i++) {
            builder.append(messages.get(i));
            if (i != messages.size() - 1) {
                builder.append(separatorComponent);
            }
        }

        Component message = builder.build();
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (plainText.length() > 1024) {
            throw new TooLongTextException(1024);
        }

        player.sendMessage(message);
    }

    @Override
    public @NotNull RequiredArguments getRequiredArguments() {
        return RequiredArguments.builder()
                .texts("messages", 18)
                .parameter("type",
                        new Choice("new-line", Material.PAPER),
                        new Choice("join-spaces", Material.MAP),
                        new Choice("join", Material.FILLED_MAP))
                .build();
    }

    @Override
    public @NotNull String getExtensionId() {
        return "default";
    }

    @Override
    public @NotNull String getName() {
        return "Player Send Message";
    }

    @Override
    public @NotNull String getDescription() {
        return "Sends message to player";
    }

    @Override
    public @NotNull ItemStack getDisplayIcon() {
        return new ItemStack(Material.WRITABLE_BOOK);
    }

    @Override
    public @NotNull MenusCategory getCategory() {
        return MenusCategory.COMMUNICATION;
    }
}
