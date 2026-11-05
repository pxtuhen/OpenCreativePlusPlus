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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.fromLegacyToMiniMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.stripMiniMessageTags;

/**
 * Turns player's text with colour codes (&a, &#RRGGBB, &l...) into a component,
 * allowing only the styles, that player has permissions for and that are enabled in chat.yml.
 */
public final class ColorFormatter {

    /**
     * Where the text is written.
     */
    public enum Place {
        CHAT("chat.messages", "opencreativepp.chat"),
        SIGN("chat.signs", "opencreativepp.sign");

        private final String configPath;
        private final String permission;

        Place(String configPath, String permission) {
            this.configPath = configPath;
            this.permission = permission;
        }
    }

    /**
     * Allowed styles for one sender.
     */
    public record Styles(boolean colors, boolean hex, boolean bold, boolean italic,
                         boolean underlined, boolean strikethrough, boolean obfuscated) {
        public boolean any() {
            return colors || hex || bold || italic || underlined || strikethrough || obfuscated;
        }
    }

    private ColorFormatter() {
    }

    /**
     * @param legacyPermission permission part of OpenCreative+ ("chat", "creative-chat"),
     *                         so old permissions opencreative.chat.color etc. still work; null for signs.
     */
    public static @NotNull Styles styles(@NotNull CommandSender sender, @NotNull Place place, String legacyPermission) {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        String path = place.configPath + ".";
        String perm = place.permission + ".";
        String old = legacyPermission == null ? null : "opencreative." + legacyPermission + ".";
        boolean colors = config.getBoolean(path + "colors", true)
                && (sender.hasPermission(perm + "color") || has(sender, old, "color"));
        boolean hex = config.getBoolean(path + "hex", true)
                && (sender.hasPermission(perm + "hex") || has(sender, old, "color"));
        boolean formats = config.getBoolean(path + "formats", true);
        boolean format = sender.hasPermission(perm + "format");
        boolean bold = formats && (format || has(sender, old, "bold"));
        boolean italic = formats && (format || has(sender, old, "italic"));
        boolean underlined = formats && (format || has(sender, old, "underline"));
        boolean strikethrough = formats && (format || has(sender, old, "strikethrough"));
        boolean obfuscated = config.getBoolean(path + "obfuscated", true)
                && (sender.hasPermission(perm + "obfuscated") || has(sender, old, "obfuscated"));
        return new Styles(colors, hex, bold, italic, underlined, strikethrough, obfuscated);
    }

    private static boolean has(@NotNull CommandSender sender, String prefix, @NotNull String node) {
        return prefix != null && sender.hasPermission(prefix + node);
    }

    public static @NotNull Component format(@NotNull CommandSender sender, @NotNull Place place,
                                            String legacyPermission, @NotNull String input) {
        return format(styles(sender, place, legacyPermission), input);
    }

    public static @NotNull Component format(@NotNull Styles styles, @NotNull String input) {
        TagResolver.Builder resolver = TagResolver.builder();
        if (styles.colors()) {
            resolver.resolvers(StandardTags.color(), StandardTags.gradient(),
                    StandardTags.pride(), StandardTags.rainbow());
        } else if (styles.hex()) {
            resolver.resolvers(StandardTags.color());
        }
        if (styles.bold()) resolver.resolver(StandardTags.decorations(TextDecoration.BOLD));
        if (styles.italic()) resolver.resolver(StandardTags.decorations(TextDecoration.ITALIC));
        if (styles.underlined()) resolver.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
        if (styles.strikethrough()) resolver.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        if (styles.obfuscated()) resolver.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        String mini = fromLegacyToMiniMessage(input, styles.colors(), styles.hex(), styles.bold(),
                styles.italic(), styles.underlined(), styles.strikethrough(), styles.obfuscated());
        return stripMiniMessageTags(MiniMessage.builder().tags(resolver.build()).build().deserialize(mini));
    }
}
