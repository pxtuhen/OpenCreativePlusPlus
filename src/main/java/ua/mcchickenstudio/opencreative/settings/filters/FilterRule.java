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

package ua.mcchickenstudio.opencreative.settings.filters;

import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.settings.Command;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleComponent;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.substring;

/**
 * <h1>FilterRule</h1>
 * This class represents a filter rule, that has identifier,
 * check settings, list of patterns and list of commands.
 */
public final class FilterRule {

    private final String id;
    private final boolean warnPlayer;
    private final boolean notifyStaff;

    private final boolean checkChat;
    private final boolean checkAnvils;
    private final boolean checkBooks;
    private final boolean checkSigns;

    private final FilterTextAction action;
    private final String replacement;
    private final List<Pattern> patterns;
    private final List<Command> commands;

    public FilterRule(@NotNull String id, boolean warnPlayer, boolean notifyStaff,
                      boolean checkChat, boolean checkAnvils,
                      boolean checkBooks, boolean checkSigns,
                      @NotNull FilterTextAction action, @NotNull String replacement,
                      @NotNull List<Pattern> patterns, @NotNull List<Command> commands) {
        this.warnPlayer = warnPlayer;
        this.notifyStaff = notifyStaff;
        this.action = action;
        this.replacement = replacement;
        this.patterns = patterns;
        this.commands = commands;
        this.checkChat = checkChat;
        this.checkBooks = checkBooks;
        this.checkAnvils = checkAnvils;
        this.checkSigns = checkSigns;
        this.id = id;
    }

    /**
     * Checks whether text matches and violates the rule.
     *
     * @param text text to check.
     * @return true - text violates the rule, false - text is fine.
     */
    public @Nullable String matches(@NotNull String text) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String found = matcher.group();
                boolean isWhitelisted = false;
                for (Pattern whitelisted : Filter.getInstance().getWhitelistPatterns()) {
                    if (whitelisted.matcher(found).find()) {
                        isWhitelisted = true;
                        break;
                    }
                }
                if (isWhitelisted) continue;
                return found;
            }
        }
        return null;
    }

    /**
     * Checks whether renaming item in anvil should be checked.
     *
     * @return true - should be checked, false - not.
     */
    public boolean shouldCheckAnvils() {
        return checkAnvils;
    }

    /**
     * Checks whether editing book's content should be checked.
     *
     * @return true - should be checked, false - not.
     */
    public boolean shouldCheckBooks() {
        return checkBooks;
    }

    /**
     * Checks whether chat messages should be checked.
     *
     * @return true - should be checked, false - not.
     */
    public boolean shouldCheckChat() {
        return checkChat;
    }

    /**
     * Checks whether editing sign's text should be checked.
     *
     * @return true - should be checked, false - not.
     */
    public boolean shouldCheckSigns() {
        return checkSigns;
    }

    /**
     * Filters text and returns a filter result.
     *
     * @param text text to check.
     * @param context context to check.
     * @return a filter result.
     */
    public @NotNull FilterResult filterText(@NotNull String text, @NotNull Filter.Context context) {
        List<String> matches = new ArrayList<>();
        String filteredText = switch (action) {
            case FULL -> {
                String match = matches(text);
                if (match != null) {
                    matches.add(match);
                    yield replacement.repeat(text.length()).substring(0, text.length());
                }
                yield null;
            }
            case SPACES -> {
                String match = matches(text);
                if (match != null) {
                    matches.add(match);
                    StringBuilder builder = new StringBuilder();
                    for (char character : text.toCharArray()) {
                        builder.append(character == ' ' ? character : replacement);
                    }
                    yield builder.toString();
                }
                yield null;
            }
            case COVER_WORDS -> {
                String result = text;
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(result);
                    StringBuilder builder = new StringBuilder();
                    while (matcher.find()) {
                        String word = matcher.group();
                        boolean isWhitelisted = false;
                        for (Pattern whitelisted : Filter.getInstance().getWhitelistPatterns()) {
                            if (whitelisted.matcher(word).find()) {
                                isWhitelisted = true;
                                break;
                            }
                        }
                        if (isWhitelisted) continue;
                        matches.add(word);
                        String covered = replacement.repeat(word.length());
                        matcher.appendReplacement(builder, covered);
                    }
                    matcher.appendTail(builder);
                    result = builder.toString();
                }
                if (matches.isEmpty()) yield null;
                yield result;
            }
            case REPLACE_WORDS -> {
                String result = text;
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(result);
                    while (matcher.find()) {
                        String word = matcher.group();
                        boolean isWhitelisted = false;
                        for (Pattern whitelisted : Filter.getInstance().getWhitelistPatterns()) {
                            if (whitelisted.matcher(word).find()) {
                                isWhitelisted = true;
                                break;
                            }
                        }
                        if (isWhitelisted) continue;
                        matches.add(word);
                        result = result.replace(word, replacement);
                    }
                }
                if (matches.isEmpty()) yield null;
                yield result;
            }
        };
        if (filteredText != null) {
            return new FilterResult(text, filteredText, context, this, matches);
        }
        return new FilterResult(text, text, context, null, matches);
    }

    /**
     * Executes actions when text violated the rule.
     *
     * @param player player to do action.
     * @param result a filter result.
     */
    public void onViolation(@NotNull Player player,
                            @NotNull FilterResult result) {
        if (result.rule() == null) return;
        if (notifyStaff) {
            String match = substring(result.matches().getFirst(), 50);
            OpenCreative.getPlugin().getLogger().info("[FILTER: " + player.getName() + "] Filtered "
                    + result.context().name().toLowerCase() + " by rule: " + result.rule().id + ", matched: " + match);
            for (Player moderator : Bukkit.getOnlinePlayers()) {
                if (moderator.hasPermission("opencreative.moderation.filter-notify") && !moderator.equals(player)) {
                    moderator.sendMessage(getLocaleComponent(
                            "filters." + result.context().name().toLowerCase() + "-violation.staff")
                            .replaceText(new PlaceholderReplacer("player", player.getName()).get())
                            .hoverEvent(HoverEvent.showText(new PlaceholderReplacer("rule", result.rule().id,
                                    "match", match, "online", player.getWorld().getPlayerCount())
                                    .apply(getLocaleComponent("filters.hover-text")))
                            ));
                }
            }
        }
        if (warnPlayer) {
            player.sendMessage(getLocaleComponent(
                    "filters." + result.context().name().toLowerCase() + "-violation.player"));
            Sounds.PLAYER_FILTER.play(player);
        }
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () ->
                executeCommands(player, result.originalMessage(), result.filteredMessage(),
                result.context().name().toLowerCase()));
    }

    /**
     * Executes violation commands on player.
     *
     * @param player player to replace placeholders.
     * @param content initial content.
     * @param result censored content or same.
     * @param context context that was checked.
     */
    public void executeCommands(@NotNull Player player,
                                @NotNull String content,
                                @NotNull String result,
                                @NotNull String context) {
        for (Command command : commands) {
            command.execute(player, Map.of("%player%", player.getName(),
                    "%content%", content, "%result%", result,
                    "%rule%", id, "%context%", context));
        }
    }

    public @NotNull String getId() {
        return id;
    }
}
