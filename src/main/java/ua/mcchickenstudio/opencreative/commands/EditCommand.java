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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.utils.CooldownUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.CooldownUtils.checkAndSetCooldownWithMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;

/**
 * <h1>EditCommand</h1>
 * This command is responsible for editing items data,
 * like display name and lore.
 * <p>
 * Available: For world builders or developers.
 */
public class EditCommand extends CommandHandler {

    private static final int TEXT_LIMIT = 100;
    private static final int LINES_LIMIT = 20;

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            OpenCreative.getPlugin().getLogger().info(getLocaleMessage("only-in-world"));
            return;
        }

        if (!checkPermissions(player)) return;

        if (args.length == 0) {
            sender.sendMessage(getLocaleMessage("commands.edit.help"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        ItemMeta meta = item.getItemMeta();
        if (item.getType().isAir() || meta == null) {
            sender.sendMessage(getLocaleMessage("commands.edit.item"));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "name":
                handleSetName(player, item, args);
                break;
            case "lore", "setlore":
                handleSetLore(player, item, args);
                break;
            case "addlore":
                handleAddLore(player, item, args);
                break;
            case "removelore", "deletelore", "dellore", "remlore":
                handleRemoveLore(player, item, args);
                break;
            case "clear":
                handleClear(player, item);
                break;
            case "glowing", "glow":
                handleGlowing(player, item);
                break;
            case "unglowing", "unglow":
                handleUnglowing(player, item);
                break;
            case "enchant":
                handleEnchant(player, item, args);
                break;
            case "clearenchants":
                handleClearEnchantments(player, item);
                break;
            case "unenchant":
                handleRemoveEnchant(player, item, args);
                break;
            default:
                sender.sendMessage(getUnknownArgumentMessage(label, args));
                break;
        }

    }

    private boolean checkPermissions(Player player) {
        if (!checkAndSetCooldownWithMessage(player, CooldownUtils.CooldownType.GENERIC_COMMAND)) return false;

        if (player.hasPermission("opencreative.edit.bypass")) return true;

        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        if (planet == null) {
            player.sendMessage(getLocaleMessage("only-in-world"));
            return false;
        }
        if (!(planet.isOwner(player) || planet.getWorldPlayers().canDevelop(player) || planet.getWorldPlayers().canBuild(player))) {
            player.sendMessage(getLocaleMessage("not-owner"));
            return false;
        }

        return true;
    }

    private String joinArgs(String[] args, int fromIndex) {
        return String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length));
    }

    private void handleSetName(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        if (args.length == 1) {
            Component displayName = meta.displayName();
            if (displayName == null) {
                player.sendMessage(getLocaleMessage("commands.edit.item"));
                return;
            }
            player.sendMessage(displayName.clickEvent(
                    ClickEvent.suggestCommand(LegacyComponentSerializer.legacyAmpersand().serialize(displayName))));
        } else {
            String message = joinArgs(args, 1);
            Component newName = fromInputToComponent(message);
            if (getComponentLength(newName) > TEXT_LIMIT) {
                player.sendMessage(toComponent(getLocaleMessage("commands.edit.text-limit")
                        .replace("%limit%", String.valueOf(TEXT_LIMIT))));
                Sounds.PLAYER_FAIL.play(player);
                return;
            }
            if (!newName.hasDecoration(TextDecoration.ITALIC)) {
                newName = newName.decoration(TextDecoration.ITALIC, false);
            }
            meta.displayName(newName);
            item.setItemMeta(meta);
            player.sendMessage(getComponentWithPlaceholders("commands.edit.renamed",
                    player, "name", newName)
                    .clickEvent(ClickEvent.suggestCommand(message)));
            Sounds.EDIT_ITEM_RENAMED.play(player);
        }
    }

    private int getComponentLength(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component).length();
    }

    private void handleSetLore(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        if (args.length == 1) {
            player.sendMessage(getLocaleMessage("commands.edit.help"));
            return;
        }
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[1]);
            if (lineNumber < 1) {
                lineNumber = 1;
            } else if (lineNumber > LINES_LIMIT) {
                player.sendMessage(toComponent(getLocaleMessage("commands.edit.lines-limit")
                        .replace("%limit%", String.valueOf(LINES_LIMIT))));
                Sounds.PLAYER_FAIL.play(player);
                return;
            }
        } catch (NumberFormatException ignored) {
            getComponentWithPlaceholders("commands.edit.not-number", player,
                    "argument", args[1]);
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        String message = joinArgs(args, 2);
        Component newLoreLine = fromInputToComponent(message);
        if (getComponentLength(newLoreLine) > TEXT_LIMIT) {
            player.sendMessage(toComponent(getLocaleMessage("commands.edit.text-limit")
                    .replace("%limit%", String.valueOf(TEXT_LIMIT))));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        if (!newLoreLine.hasDecoration(TextDecoration.ITALIC)) {
            newLoreLine = newLoreLine.decoration(TextDecoration.ITALIC, false);
        }
        List<Component> newLore = meta.lore();
        if (newLore == null) newLore = new ArrayList<>();
        if (newLore.size() < lineNumber) {
            while (newLore.size() < lineNumber) {
                newLore.add(Component.text(" "));
            }
        }
        if (args.length == 2) {
            player.sendMessage(newLore.get(lineNumber - 1)
                    .clickEvent(ClickEvent.suggestCommand(
                            LegacyComponentSerializer.legacyAmpersand().serialize(newLore.get(lineNumber - 1)))));
            return;
        }
        newLore.set(lineNumber - 1, newLoreLine);
        meta.lore(newLore);
        item.setItemMeta(meta);
        player.sendMessage(getComponentWithPlaceholders("commands.edit.set-lore",
                player, "number", lineNumber, "lore", newLoreLine)
                .clickEvent(ClickEvent.suggestCommand(message)));
        Sounds.EDIT_ITEM_LORE.play(player);
    }

    private void handleAddLore(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        String message = joinArgs(args, 1);
        Component newLoreLine = fromInputToComponent(message);
        if (getComponentLength(newLoreLine) > TEXT_LIMIT) {
            player.sendMessage(toComponent(getLocaleMessage("commands.edit.text-limit").replace("%limit%", String.valueOf(TEXT_LIMIT))));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        List<Component> newLore = meta.lore();
        if (newLore == null) newLore = new ArrayList<>();
        if (newLore.size() > LINES_LIMIT) {
            player.sendMessage(toComponent(getLocaleMessage("commands.edit.lines-limit")
                    .replace("%limit%", String.valueOf(LINES_LIMIT))));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        if (!newLoreLine.hasDecoration(TextDecoration.ITALIC)) {
            newLoreLine = newLoreLine.decoration(TextDecoration.ITALIC, false);
        }
        newLore.add(newLoreLine);
        meta.lore(newLore);
        item.setItemMeta(meta);
        player.sendMessage(getComponentWithPlaceholders("commands.edit.set-lore",
                player, "number", newLore.size(), "lore", message));
        Sounds.EDIT_ITEM_LORE.play(player);
    }

    private void handleRemoveLore(Player player, ItemStack item, String[] args) {
        if (args.length == 1) {
            player.sendMessage(getLocaleMessage("commands.edit.help"));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[1]);
            if (lineNumber < 1) {
                lineNumber = 1;
            } else if (lineNumber > LINES_LIMIT) {
                player.sendMessage(toComponent(getLocaleMessage("commands.edit.lines-limit")
                        .replace("%limit%", String.valueOf(LINES_LIMIT))));
                Sounds.PLAYER_FAIL.play(player);
                return;
            }
        } catch (NumberFormatException ignored) {
            getComponentWithPlaceholders("commands.edit.not-number", player,
                    "argument", args[1]);
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        List<Component> newLore = meta.lore();
        if (newLore == null) newLore = new ArrayList<>();
        if (newLore.size() >= lineNumber) {
            newLore.remove(lineNumber - 1);
        }
        meta.lore(newLore);
        item.setItemMeta(meta);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.removed-lore")
                .replace("%number%", String.valueOf(lineNumber))));
        Sounds.EDIT_ITEM_LORE.play(player);

    }

    private void handleEnchant(Player player, ItemStack item, String[] args) {
        if (args.length <= 2) {
            player.sendMessage(getLocaleMessage("commands.edit.help"));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
            int limit = OpenCreative.getSettings().getItemFixerSettings().getMaxEnchantLevel();
            if (level > limit) {
                player.sendMessage(getLocaleMessage("commands.edit.too-big-level")
                        .replace("%limit%", String.valueOf(limit)));
                Sounds.PLAYER_FAIL.play(player);
                return;
            }
            if (level <= 0) {
                level = 1;
            }
        } catch (Exception error) {
            player.sendMessage(getLocaleMessage("commands.edit.not-number")
                    .replace("%argument%", args[2]));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        String enchantmentString = args[1].toLowerCase();
        Enchantment enchantment;
        try {
            enchantment = Enchantment.getByName(enchantmentString.toUpperCase());
        } catch (Exception error) {
            player.sendMessage(getLocaleMessage("commands.edit.not-enchantment")
                    .replace("%enchantment%", enchantmentString));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        if (enchantment == null) {
            player.sendMessage(getLocaleMessage("commands.edit.not-enchantment")
                    .replace("%enchantment%", args[1]));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.enchanted")
                .replace("%enchantment%", enchantmentString)
                .replace("%level%", String.valueOf(level))));
        Sounds.EDIT_ITEM_ENCHANTED.play(player);
    }

    private void handleRemoveEnchant(Player player, ItemStack item, String[] args) {
        if (args.length <= 1) {
            player.sendMessage(getLocaleMessage("commands.edit.help"));
            return;
        }
        String enchantmentString = args[1].toLowerCase();
        Enchantment enchantment;
        try {
            enchantment = Enchantment.getByName(enchantmentString.toUpperCase());
        } catch (Exception error) {
            player.sendMessage(getLocaleMessage("commands.edit.not-enchantment")
                    .replace("%enchantment%", enchantmentString));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        if (enchantment == null) {
            player.sendMessage(getLocaleMessage("commands.edit.not-enchantment")
                    .replace("%enchantment%", args[1]));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(enchantment);
        item.setItemMeta(meta);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.removed-enchant")
                .replace("%enchantment%", enchantmentString)));
        Sounds.EDIT_ITEM_UNENCHANTED.play(player);
    }

    private void handleGlowing(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        Sounds.EDIT_ITEM_GLOW.play(player);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.glowing")));
    }

    private void handleUnglowing(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);
        Sounds.EDIT_ITEM_UNGLOW.play(player);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.no-glowing")));
    }

    private void handleClearEnchantments(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.removeEnchantments();
        item.setItemMeta(meta);
        Sounds.EDIT_ITEM_UNENCHANTED.play(player);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.unenchanted")));
    }

    private void handleClear(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(null);
        meta.lore(null);
        item.setItemMeta(meta);
        Sounds.EDIT_ITEM_UNENCHANTED.play(player);
        player.sendMessage(toComponent(getLocaleMessage("commands.edit.cleared")));
    }

    @Override
    public @Nullable List<String> onTab(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> tabCompleter = new ArrayList<>();
        if (args.length <= 1) {
            tabCompleter.add("name");
            tabCompleter.add("lore");
            tabCompleter.add("addlore");
            tabCompleter.add("removelore");
            tabCompleter.add("glow");
            tabCompleter.add("unglow");
            tabCompleter.add("enchant");
            tabCompleter.add("unenchant");
            tabCompleter.add("clear");
            tabCompleter.add("clearenchants");
        } else if (args.length == 2) {
            if (args[0].equals("enchant") || args[0].equals("unenchant")) {
                tabCompleter.addAll(Arrays.stream(Enchantment.values()).map(e
                        -> e.getKey().asMinimalString()).toList());
            }
        }  else if (args.length == 3) {
            if (args[0].equals("enchant")) {
                int limit = OpenCreative.getSettings().getItemFixerSettings().getMaxEnchantLevel();
                for (int i = 1; i <= limit; i++) {
                    if (i > 50) break;
                    tabCompleter.add(String.valueOf(i));
                }
            }
        }
        return tabCompleter;
    }
}
