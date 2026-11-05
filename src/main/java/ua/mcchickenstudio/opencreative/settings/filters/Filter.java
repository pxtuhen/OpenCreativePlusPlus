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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <h1>Filter</h1>
 * This class represents a filter, that checks messages
 * for violating some rules.
 */
public final class Filter {

    private final List<FilterRule> rules = new ArrayList<>();
    private final List<Pattern> whitelist = new ArrayList<>();
    private static Filter instance;

    /**
     * Returns instance of filter.
     *
     * @return instance of filter.
     */
    public static @NotNull Filter getInstance() {
        if (instance == null) {
            instance = new Filter();
        }
        return instance;
    }

    /**
     * Checks specified text in context and returns a check result.
     *
     * @param text text to check.
     * @param context context of check.
     * @return a filter result.
     */
    public @NotNull FilterResult checkContent(@NotNull String text,
                                              @NotNull Context context) {
        String editedText = text.toLowerCase(Locale.ROOT);
        for (FilterRule rule : rules) {
            if (context.isCompatibleRule(rule)) {
                FilterResult result = rule.filterText(editedText, context);
                if (result.rule() == null) continue;
                return result;
            }
        }
        return new FilterResult(text, text, context, null, List.of());
    }

    /**
     * Returns list of whitelist regex patterns.
     *
     * @return whitelist patterns.
     */
    public @NotNull List<Pattern> getWhitelistPatterns() {
        return whitelist;
    }

    /**
     * Adds pattern to whitelist.
     *
     * @param pattern pattern to add.
     */
    public void addWhitelist(@NotNull Pattern pattern) {
        whitelist.add(pattern);
    }

    /**
     * Registers filter rule.
     *
     * @param rule rule to add.
     */
    public void addRule(@NotNull FilterRule rule) {
        rules.add(rule);
    }

    /**
     * Clears all filter rules.
     */
    public void clearRules() {
        rules.clear();
        whitelist.clear();
    }

    /**
     * This enum represents a context of place where filter was called.
     */
    public enum Context {

        /**
         * Chat messages.
         */
        CHAT,

        /**
         * Sign edits.
         */
        SIGN,

        /**
         * Renaming item in anvil.
         */
        ANVIL,

        /**
         * Editing a book's content.
         */
        BOOK;

        /**
         * Checks whether rule is compatible with context.
         *
         * @param rule rule to check.
         * @return true - rule should be checked, false - not.
         */
        public boolean isCompatibleRule(@NotNull FilterRule rule) {
            return switch (this) {
                case CHAT -> rule.shouldCheckChat();
                case SIGN -> rule.shouldCheckSigns();
                case ANVIL -> rule.shouldCheckAnvils();
                case BOOK -> rule.shouldCheckBooks();
            };
        }

    }

}
