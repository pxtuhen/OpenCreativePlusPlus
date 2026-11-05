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

package ua.mcchickenstudio.opencreative.plus.datapacks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * /datapack — datapacks of the current world.
 * <pre>
 * /datapack          — menu (world owner, Premium)
 * /datapack [id]     — switch datapack in current world
 * /datapack list     — library and checks       (opencreative.datapacks.list)
 * /datapack reload   — re-read datapacks.yml    (opencreative.datapacks.reload)
 * </pre>
 */
public final class DatapackCommand implements TabExecutor {

    private static final String PATH = "plus.datapacks.";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("opencreative.datapacks.reload")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return true;
            }
            OpenCreative.getPlugin().reloadConfig();
            Datapacks.load();
            sender.sendMessage(getLocaleMessage(PATH + "reloaded")
                    .replace("%valid%", String.valueOf(Datapacks.getLibrary().stream().filter(Datapacks.Entry::valid).count()))
                    .replace("%all%", String.valueOf(Datapacks.getLibrary().size())));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("opencreative.datapacks.list")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return true;
            }
            sender.sendMessage(getLocaleMessage(PATH + "list-header").replace("%all%", String.valueOf(Datapacks.getLibrary().size())));
            for (Datapacks.Entry entry : Datapacks.getLibrary()) {
                sender.sendMessage(getLocaleMessage(PATH + (entry.valid() ? "list-ok" : "list-bad"), false)
                        .replace("%id%", entry.id())
                        .replace("%problem%", entry.valid() ? "" : entry.problem()));
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLocaleMessage(PATH + "usage"));
            return true;
        }
        if (args.length == 0) {
            if (!Datapacks.isEnabled()) {
                sendResult(player, Datapacks.ToggleResult.SYSTEM_DISABLED, "");
                return true;
            }
            Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet == null) {
                sendResult(player, Datapacks.ToggleResult.NOT_IN_WORLD, "");
                return true;
            }
            boolean admin = player.hasPermission("opencreativepp.admin");
            if (!planet.isOwner(player) && !admin) {
                sendResult(player, Datapacks.ToggleResult.NOT_OWNER, "");
                return true;
            }
            if (!admin && !player.hasPermission(Datapacks.permission())) {
                sendResult(player, Datapacks.ToggleResult.NO_PERMISSION, "");
                return true;
            }
            new DatapackMenu(player, planet).open(player);
            return true;
        }
        sendResult(player, Datapacks.toggle(player, args[0]), args[0]);
        return true;
    }

    static void sendResult(@NotNull Player player, @NotNull Datapacks.ToggleResult result, @NotNull String id) {
        String key = switch (result) {
            case ENABLED -> "enabled";
            case DISABLED -> "disabled";
            case SYSTEM_DISABLED -> "system-disabled";
            case NOT_IN_WORLD -> "not-in-world";
            case NOT_OWNER -> "not-owner";
            case NO_PERMISSION -> "no-permission";
            case UNKNOWN -> "unknown";
            case INVALID -> "invalid";
            case NO_PACK_PERMISSION -> "no-pack-permission";
            case LIMIT -> "limit";
        };
        Datapacks.Entry entry = id.isEmpty() ? null : Datapacks.get(id);
        player.sendMessage(getLocaleMessage(PATH + key)
                .replace("%id%", id)
                .replace("%limit%", String.valueOf(Datapacks.maxPerWorld()))
                .replace("%problem%", entry == null || entry.valid() ? "" : entry.problem()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        List<String> options = new ArrayList<>();
        for (Datapacks.Entry entry : Datapacks.getLibrary()) options.add(entry.id());
        for (Map.Entry<String, String> admin : Map.of("list", "opencreative.datapacks.list", "reload", "opencreative.datapacks.reload").entrySet()) {
            if (sender.hasPermission(admin.getValue())) options.add(admin.getKey());
        }
        return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
    }
}
