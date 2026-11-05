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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.awt.*;
import java.util.List;
import java.util.Locale;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getPlayerLocaleComponent;

/**
 * <h1>ColorCommand</h1>
 * This command allows player to select a color
 * by clicking on color pallette.
 * <p>
 * Available: For every player.
 */
public class ColorCommand extends CommandHandler {

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLocaleMessage("only-players"));
            return;
        }
        if (args.length == 0) {
            player.sendMessage(getLocaleMessage("commands.color-picker.help"));
            return;
        }
        /*
         * /color rgb 1 2 3
         * /color hsv 1 2 3
         * /color hex #112233
         */
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "picker" -> {
                sendPalette(player, new HSV(0, 100, 100));
            }
            case "hsv" -> {
                if (args.length <= 3) {
                    player.sendMessage(getLocaleMessage("too-few-args"));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                try {
                    double h = Double.parseDouble(args[1]);
                    double s = Double.parseDouble(args[2]);
                    double v = Double.parseDouble(args[3]);
                    if (h < 0 || h > 360 || s < 0 || s > 100 || v < 0 || v > 100) {
                        player.sendMessage(getLocaleMessage("commands.color-picker.bad-hsv"));
                        Sounds.PLAYER_FAIL.play(player);
                        return;
                    }
                    sendPalette(player, new HSV(h, s, v));
                } catch (Exception error) {
                    player.sendMessage(getLocaleMessage("commands.color-picker.bad-hsv"));
                    Sounds.PLAYER_FAIL.play(player);
                }
            }
            case "rgb" -> {
                if (args.length <= 3) {
                    player.sendMessage(getLocaleMessage("too-few-args"));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                try {
                    int r = Integer.parseInt(args[1]);
                    int g = Integer.parseInt(args[2]);
                    int b = Integer.parseInt(args[3]);
                    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                        player.sendMessage(getLocaleMessage("commands.color-picker.bad-rgb"));
                        Sounds.PLAYER_FAIL.play(player);
                        return;
                    }
                    Color color = new Color(r, g, b);
                    sendPalette(player, fromRGBToHSV(color));
                } catch (Exception error) {
                    player.sendMessage(getLocaleMessage("commands.color-picker.bad-hsv"));
                    Sounds.PLAYER_FAIL.play(player);
                }

            }
            case "hex" -> {
                if (args.length == 1) {
                    player.sendMessage(getLocaleMessage("too-few-args"));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                String hex = args[1];
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                if (hex.startsWith("&x")) {
                    hex = hex.substring(2).replace("&", "");
                }
                if (hex.startsWith("&#")) {
                    hex = hex.substring(2);
                }
                if (!hex.matches("[0-9a-fA-F]{6}")) {
                    player.sendMessage(getLocaleMessage("commands.color-picker.bad-hex"));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                Color color = Color.decode("#" + hex);
                sendPalette(player, fromRGBToHSV(color));
            }
            default -> player.sendMessage(getLocaleMessage("commands.color-picker.help"));
        }

    }

    private void sendPalette(@NotNull Player player, @NotNull HSV hsv) {
        int rgb = fromHSVtoRGB(hsv);

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        String hex = String.format("#%02X%02X%02X", r, g, b);
        String symbol = getLocaleMessage("commands.color-picker.symbol");
        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.info", player));
        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.line-h", player)
                .replaceText(new PlaceholderReplacer("line", hueBar(hsv, symbol), "value", formatNumber(hsv.h)).get()));
        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.line-s", player)
                .replaceText(new PlaceholderReplacer("line", saturationBar(hsv, symbol), "value", formatNumber(hsv.s)).get()));
        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.line-v", player)
                .replaceText(new PlaceholderReplacer("line", valueBar(hsv, symbol), "value", formatNumber(hsv.v)).get()));

        Component copyHover = getPlayerLocaleComponent("commands.color-picker.copy-hover", player)
                .color(TextColor.color(HSVLike.fromRGB(r, g, b)));
        Component copyMiniMessage = getPlayerLocaleComponent("commands.color-picker.copy-minimessage", player)
                .clickEvent(ClickEvent.suggestCommand("<" + hex + ">"))
                .hoverEvent(HoverEvent.showText(copyHover));
        Component copyLegacy = getPlayerLocaleComponent("commands.color-picker.copy-legacy", player)
                .clickEvent(ClickEvent.suggestCommand(toLegacyHex(hex)))
                .hoverEvent(HoverEvent.showText(copyHover));
        Component copyHex = getPlayerLocaleComponent("commands.color-picker.copy-hex", player)
                .clickEvent(ClickEvent.suggestCommand(hex))
                .hoverEvent(HoverEvent.showText(copyHover));

        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.look", player)
                .replaceText(new PlaceholderReplacer("hex", hex).get())
                .color(TextColor.color(HSVLike.fromRGB(r, g, b))));
        player.sendMessage(getPlayerLocaleComponent("commands.color-picker.copy", player)
                .replaceText(new PlaceholderReplacer("minimessage", copyMiniMessage,
                        "legacy", copyLegacy, "hex", copyHex).get()));
    }

    private @NotNull Component hueBar(@NotNull HSV hsv, @NotNull String symbol) {
        Component result = Component.empty();
        for (int h = 0; h <= 360; h += 5) {
            HSV selected = new HSV(h, hsv.s, hsv.v);
            int rgb = fromHSVtoRGB(selected);
            Component block = Component.text(symbol, TextColor.color(rgb))
                    .clickEvent(ClickEvent.runCommand("/color hsv " + formatNumber(h) + " " + formatNumber(hsv.s) + " " + formatNumber(hsv.v)))
                    .hoverEvent(HoverEvent.showText(Component.text("H: " + h)));
            result = result.append(block);
        }

        return result;
    }

    private @NotNull Component saturationBar(@NotNull HSV hsv, @NotNull String symbol) {
        Component result = Component.empty();
        for (int i = 0; i <= 72; i++) {
            int s = Math.round(i * 100f / 72f);
            HSV selected = new HSV(hsv.h, s, hsv.v);
            int rgb = fromHSVtoRGB(selected);
            Component block = Component.text(symbol, TextColor.color(rgb))
                    .clickEvent(ClickEvent.runCommand("/color hsv " + formatNumber(hsv.h) + " " + s + " " + formatNumber(hsv.v)))
                    .hoverEvent(HoverEvent.showText(Component.text("S: " + s)));
            result = result.append(block);
        }
        return result;
    }

    private @NotNull Component valueBar(@NotNull HSV hsv, @NotNull String symbol) {
        Component result = Component.empty();
        for (int i = 0; i <= 72; i++) {
            int v = Math.round(i * 100f / 72f);
            HSV selected = new HSV(hsv.h, hsv.s, v);
            int rgb = fromHSVtoRGB(selected);
            Component block = Component.text(symbol, TextColor.color(rgb))
                    .clickEvent(ClickEvent.runCommand("/color hsv " + formatNumber(hsv.h) + " " + formatNumber(hsv.s) + " " + v))
                    .hoverEvent(HoverEvent.showText(Component.text("V: " + v)));
            result = result.append(block);
        }
        return result;
    }

    private static @NotNull String toLegacyHex(@NotNull String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        StringBuilder result = new StringBuilder("&x");
        for (char character : clean.toCharArray()) {
            result.append('&').append(character);
        }
        return result.toString();
    }

    private static int fromHSVtoRGB(@NotNull HSV hsv) {
        float h = (float) hsv.h / 360f;
        float s = (float) hsv.s / 100f;
        float v = (float) hsv.v / 100f;

        return Color.HSBtoRGB(h, s, v) & 0xFFFFFF;
    }

    private static @NotNull HSV fromRGBToHSV(@NotNull Color color) {
        float[] hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new HSV(hsv[0] * 360, hsv[1] * 100, hsv[2] * 100);
    }

    private static @NotNull String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return Integer.toString((int) value);
        }
        return String.format("%.1f", value);
    }

    private record HSV(double h, double s, double v) {}

    @Override
    public @NotNull List<String> onTab(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            return List.of("picker", "rgb", "hsv", "hex");
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "hsv" -> {
                if (args.length == 2) {
                    return List.of("0", "60", "120", "180", "260", "300", "360");
                }
                if (args.length == 3 || args.length == 4) {
                    return List.of("0", "20", "40", "60", "80", "100");
                }
            }
            case "rgb" -> {
                if (args.length <= 4) {
                    return List.of("0", "20", "67", "100", "140", "180", "220", "255");
                }
            }
            case "hex" -> {
                if (args.length == 2) {
                    return List.of("#FF0000", "#FFFFFF", "#63FF7D");
                }
            }
        }
        return noTabCompletion();
    }

}