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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.communication;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.PlayerAction;
import ua.mcchickenstudio.opencreative.coding.blocks.events.player.world.ChatEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooLongTextException;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.time.Duration;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getPlayerLocaleComponent;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isIgnored;

public final class SendMessageAction extends PlayerAction {

    public SendMessageAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void executePlayer(@NotNull Player player) {
        String separator = getArguments().getText("type", "new-line", this);
        List<Component> messages = getArguments().getComponentList("messages", this);
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
        if (!(getEvent() instanceof ChatEvent chatEvent)) {
            player.sendMessage(message);
            return;
        }
        Player author = chatEvent.getAuthor();
        if (plainText.contains(author.getName()) && author.isConnected()) {
            if (isIgnored(player, author)) {
                player.sendMessage(getPlayerLocaleComponent("ignored-game-message", player)
                        .replaceText(new PlaceholderReplacer("ignored", author.getName()).get())
                        .clickEvent(ClickEvent.callback(clicker -> {
                            Sounds.PLAYER_SHOW_IGNORED_MESSAGE.play(clicker);
                            clicker.sendMessage(message);
                        }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build())));
            } else {
                player.sendMessage(message);
            }
        } else {
            player.sendMessage(message);
        }
    }


    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.PLAYER_SEND_MESSAGE;
    }
}
