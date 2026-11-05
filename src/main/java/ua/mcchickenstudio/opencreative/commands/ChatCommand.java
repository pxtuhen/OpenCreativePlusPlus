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

package ua.mcchickenstudio.opencreative.commands;

import ua.mcchickenstudio.opencreative.plus.chat.ColorFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.player.CreativeChatEvent;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.filters.Filter;
import ua.mcchickenstudio.opencreative.settings.filters.FilterResult;
import ua.mcchickenstudio.opencreative.utils.CooldownUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static ua.mcchickenstudio.opencreative.utils.CooldownUtils.checkAndSetCooldownWithMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isIgnored;

/**
 * <h1>ChatCommand</h1>
 * This command is used to chat with all players on server.
 * <p>
 * Available: For all players.
 */
public class ChatCommand extends CommandHandler {

    public static final Set<Player> creativeChatOff = new HashSet<>();

    private static @NotNull Component parseAdvertisementInMessage(@NotNull Component component) {
        Component result = parseAdvertisementCommand(component, "join");
        if (!component.equals(result)) {
            // If message already has /join invite then return it.
            return result;
        }
        return parseAdvertisementCommand(component, "ad");
    }

    private static @NotNull Component parseAdvertisementCommand(@NotNull Component component, @NotNull String commandLabel) {
        AtomicBoolean alreadyReplaced = new AtomicBoolean(false);
        return component.replaceText(TextReplacementConfig.builder()
                .match("(/" + commandLabel + ")\\s+(\\S+)")
                .once()
                .condition((match, matchCount, replaced) -> {
                    String id = match.group(2);
                    Planet planet = OpenCreative.getPlanetsManager().getPlanetByAnyID(id);
                    if (planet == null) {
                        return PatternReplacementResult.CONTINUE;
                    }
                    if (alreadyReplaced.get()) return PatternReplacementResult.STOP;
                    alreadyReplaced.set(true);
                    return PatternReplacementResult.REPLACE;
                })
                .replacement((match, builder) -> {
                    String command = match.group(1) + " " + match.group(2);
                    String id = match.group(2);
                    Planet planet = OpenCreative.getPlanetsManager().getPlanetByAnyID(id);
                    if (planet == null) {
                        return Component.text(command);
                    }
                    Component hover = parsePlanetLines(planet, getLocaleComponent("advertisement.hover"));
                    return Component.text(command)
                            .color(NamedTextColor.YELLOW)
                            .decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(hover))
                            .clickEvent(ClickEvent.suggestCommand(command));
                })
                .build());
    }

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (OpenCreative.getSettings().isMaintenance() && !sender.hasPermission("opencreative.maintenance.bypass")) {
            sender.sendMessage(getLocaleMessage("maintenance"));
            return;
        }
        if (OpenCreative.getStability().isVeryBad() && !sender.hasPermission("opencreative.stability.bypass")) {
            sender.sendMessage(getLocaleMessage("creative.stability.cannot"));
            return;
        }
        if (OpenCreative.getSettings().isCreativeChatDisabled() && !sender.hasPermission("opencreative.creative-chat.bypass")) {
            sender.sendMessage(getLocaleMessage("creative.creative-chat.off"));
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(getLocaleMessage("creative-chat.cc-usage"));
            return;
        }
        if (sender instanceof Player player) {
            if (!checkAndSetCooldownWithMessage(player, CooldownUtils.CooldownType.CREATIVE_CHAT)) return;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("off")
                || args[0].equalsIgnoreCase("on")
                || args[0].equalsIgnoreCase("enable")
                || args[0].equalsIgnoreCase("disable"))
                && sender instanceof Player player) {
            if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                creativeChatOff.add(player);
                sender.sendMessage(getLocaleMessage("creative-chat.turned-off"));
            } else if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                creativeChatOff.remove(player);
                sender.sendMessage(getLocaleMessage("creative-chat.turned-on"));
            }
            return;
        }
        if (sender instanceof Player player) {
            if (creativeChatOff.contains(player)) {
                sender.sendMessage(getLocaleMessage("creative-chat.on-usage"));
                return;
            }
        }
        OpenCreative.getPlugin().getLogger().info("[CREATIVE-CHAT] " + sender.getName()
                + ": " + String.join(" ", args));

        AtomicReference<String> text = new AtomicReference<>(String.join(" ", args));
        String prefix = OpenCreative.getPlugin().getConfig().getString("messages.cc-prefix", "&6 Chat &8| &7");
        if (!(sender instanceof Player player)) {
            // If sender is console
            Component formatted = toComponent(prefix + sender.getName() + text);

            CreativeChatEvent event = new CreativeChatEvent(sender, text.get(), formatted);
            event.callEvent();
            if (event.isCancelled()) return;

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!(creativeChatOff.contains(onlinePlayer))) {
                    onlinePlayer.sendMessage(formatted);
                }
            }
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            FilterResult result = Filter.getInstance().checkContent(text.get(), Filter.Context.CHAT);
            if (result.rule() != null) {
                text.set(result.filteredMessage());
                result.onViolation(player);
            }
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                String format = OpenCreative.getPlugin().getConfig().getString("messages.cc-chat", "&6%cc-prefix% &7%player%: %message%")
                        .replace("%player%", sender.getName())
                        .replace("%cc-prefix%", prefix);
                format = parsePAPI(player, format);

                Component formatted = new PlaceholderReplacer("message", formatPlayerChatInput(sender, "creative-chat", text.get()))
                        .apply(toComponent(format));
                if (formatted.clickEvent() == null)
                    formatted = formatted.clickEvent(ClickEvent.suggestCommand(text.get()));
                formatted = parseAdvertisementInMessage(formatted);

                CreativeChatEvent event = new CreativeChatEvent(sender, text.get(), formatted);
                event.callEvent();
                if (event.isCancelled()) return;

                formatted = event.getFormattedMessage();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!(creativeChatOff.contains(onlinePlayer))) {
                        if (isIgnored(onlinePlayer, player)) {
                            onlinePlayer.sendMessage(getIgnoredMessage(onlinePlayer, player, formatted));
                        } else {
                            onlinePlayer.sendMessage(formatted);
                        }
                    }
                }
            });
        });
    }

    @Override
    public @Nullable List<String> onTab(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }

    private @NotNull Component getIgnoredMessage(@NotNull Player ignorer, @NotNull Player ignored, @NotNull Component original) {
        return getPlayerLocaleComponent("ignored-creative-chat-message", ignorer)
                .replaceText(new PlaceholderReplacer("ignored", ignored.getName()).get())
                .clickEvent(ClickEvent.callback(clicker -> {
                    clicker.sendMessage(original);
                    Sounds.PLAYER_SHOW_IGNORED_MESSAGE.play(clicker);
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()));
    }

    public static @NotNull Component formatPlayerChatInput(@NotNull CommandSender sender, @NotNull String permission, @NotNull String input) {
        // OpenCreative++: colours by opencreativepp.chat.* (everyone by default) or old opencreative.<permission>.*,
        // switched in chat.yml -> chat.messages.
        return ColorFormatter.format(sender, ColorFormatter.Place.CHAT, permission, input);
    }
}
