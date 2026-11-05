/*
 * OpenCreative++, fork of OpenCreative+ (C) 2022-2026, McChicken Studio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.plus.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.utils.PlayerConfirmation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * One-line chat input for OpenCreative++ menus (price, search...). Lives 60 seconds,
 * "отмена"/"cancel" cancels. Handled at the very beginning of OpenCreative+ ChatListener.
 */
public final class ChatPrompt {

    private record Prompt(Consumer<String> callback, long created) {
    }

    private static final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    private ChatPrompt() {
    }

    public static void ask(@NotNull Player player, @NotNull String message, @NotNull Consumer<String> callback) {
        PlayerConfirmation.clearConfirmations(player); // one input at a time
        prompts.put(player.getUniqueId(), new Prompt(callback, System.currentTimeMillis()));
        player.closeInventory();
        player.sendMessage(message);
    }

    /**
     * @return true if the message was an answer to a prompt (event is cancelled then).
     */
    public static boolean handle(@NotNull AsyncChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return false;
        if (System.currentTimeMillis() - prompt.created() > PlayerConfirmation.LIFETIME_MILLIS) return false;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        if (text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("cancel") || text.equals("-")) {
            player.sendMessage(getLocaleMessage("plus.input-cancelled"));
            return true;
        }
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> prompt.callback().accept(text));
        return true;
    }
}
