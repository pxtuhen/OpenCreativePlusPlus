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

package ua.mcchickenstudio.opencreative.plus.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;

import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * /balance [nick], /pay nick amount, /baltop, /eco give|take|set|log nick amount.
 */
public final class EconomyCommands implements TabExecutor {

    private static final String PATH = "plus.economy.";
    private final Map<UUID, Long> payCooldowns = new HashMap<>();

    private static @NotNull String msg(@NotNull String key) {
        return getLocaleMessage(PATH + key).replace("%currency%", PlusEconomy.currencyName());
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer target(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private static long parse(@NotNull String text) {
        try {
            return Long.parseLong(text.replace("_", ""));
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PlusEconomy economy = PlusEconomy.get();
        if (economy == null) {
            sender.sendMessage(msg("disabled"));
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "balance" -> balance(sender, args, economy);
            case "pay" -> pay(sender, args, economy);
            case "baltop" -> baltop(sender, economy);
            case "eco" -> eco(sender, args, economy);
            default -> {
            }
        }
        return true;
    }

    private void balance(@NotNull CommandSender sender, @NotNull String[] args, @NotNull PlusEconomy economy) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getLocaleMessage("only-players"));
                return;
            }
            sender.sendMessage(msg("balance").replace("%balance%", PlusEconomy.format(economy.balance(player.getUniqueId()))));
            return;
        }
        OfflinePlayer target = target(args[0]);
        if (target == null) {
            sender.sendMessage(msg("unknown-player").replace("%player%", args[0]));
            return;
        }
        sender.sendMessage(msg("balance-other").replace("%player%", Objects.requireNonNullElse(target.getName(), args[0]))
                .replace("%balance%", PlusEconomy.format(economy.balance(target.getUniqueId()))));
    }

    private void pay(@NotNull CommandSender sender, @NotNull String[] args, @NotNull PlusEconomy economy) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLocaleMessage("only-players"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("pay-usage"));
            return;
        }
        long amount = parse(args[1]);
        long min = OpenCreative.getPlugin().getConfig().getLong("economy.pay.min", 1);
        if (amount < Math.max(1, min)) {
            sender.sendMessage(msg("pay-min").replace("%amount%", PlusEconomy.format(Math.max(1, min))));
            return;
        }
        OfflinePlayer target = target(args[0]);
        if (target == null) {
            sender.sendMessage(msg("unknown-player").replace("%player%", args[0]));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(msg("pay-self"));
            return;
        }
        long cooldown = OpenCreative.getPlugin().getConfig().getLong("economy.pay.cooldown-seconds", 5) * 1000L;
        long last = payCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < cooldown) {
            sender.sendMessage(msg("pay-cooldown"));
            return;
        }
        if (!economy.transfer(player.getUniqueId(), target.getUniqueId(), amount, "перевод " + player.getName() + " → " + target.getName())) {
            sender.sendMessage(msg("not-enough").replace("%balance%", PlusEconomy.format(economy.balance(player.getUniqueId()))));
            return;
        }
        payCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        sender.sendMessage(msg("pay-sent").replace("%amount%", PlusEconomy.format(amount)).replace("%player%", Objects.requireNonNullElse(target.getName(), args[0])));
        if (target.getPlayer() != null) {
            target.getPlayer().sendMessage(msg("pay-received").replace("%amount%", PlusEconomy.format(amount)).replace("%player%", player.getName()));
        }
    }

    private void baltop(@NotNull CommandSender sender, @NotNull PlusEconomy economy) {
        sender.sendMessage(msg("top-header"));
        int place = 1;
        for (Map.Entry<UUID, Long> entry : economy.top(10)) {
            String name = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(entry.getKey()).getName(), "?");
            sender.sendMessage(msg("top-line").replace("%place%", String.valueOf(place++))
                    .replace("%player%", name).replace("%balance%", PlusEconomy.format(entry.getValue())));
        }
    }

    private void eco(@NotNull CommandSender sender, @NotNull String[] args, @NotNull PlusEconomy economy) {
        if (!sender.hasPermission("opencreativepp.economy.admin")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("log")) {
            for (String line : economy.history(20)) sender.sendMessage("§7" + line);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("eco-usage"));
            return;
        }
        OfflinePlayer target = target(args[1]);
        long amount = parse(args[2]);
        if (target == null || amount < 0) {
            sender.sendMessage(msg("eco-usage"));
            return;
        }
        String reason = "админ " + sender.getName();
        switch (args[0].toLowerCase()) {
            case "give" -> economy.add(target.getUniqueId(), amount, reason);
            case "take" -> {
                long balance = economy.balance(target.getUniqueId());
                economy.take(target.getUniqueId(), Math.min(balance, amount), reason);
            }
            case "set" -> economy.set(target.getUniqueId(), amount, reason);
            default -> {
                sender.sendMessage(msg("eco-usage"));
                return;
            }
        }
        sender.sendMessage(msg("balance-other").replace("%player%", Objects.requireNonNullElse(target.getName(), args[1]))
                .replace("%balance%", PlusEconomy.format(economy.balance(target.getUniqueId()))));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase();
        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        List<String> options = switch (name) {
            case "balance", "pay" -> args.length == 1 ? players : List.of();
            case "eco" -> args.length == 1 ? List.of("give", "take", "set", "log") : args.length == 2 ? players : List.of();
            default -> List.of();
        };
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(last)).toList();
    }
}
