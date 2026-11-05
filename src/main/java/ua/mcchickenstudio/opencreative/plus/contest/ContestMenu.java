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

package ua.mcchickenstudio.opencreative.plus.contest;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.rating.Ratings;
import ua.mcchickenstudio.opencreative.plus.rating.StarPickerMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * /contest — current build contest: join (building), entries and voting (voting), results (finished).
 */
public final class ContestMenu extends AbstractMenu {

    private static final String PATH = "plus.contest.";
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int SLOT_INFO = 4, SLOT_ACTION = 13;
    private final Player player;
    private final Map<Integer, Integer> slots = new HashMap<>();

    public ContestMenu(@NotNull Player player) {
        super(6, getLocaleMessage(PATH + "menu-title", false));
        this.player = player;
    }

    private static @NotNull ItemStack item(@NotNull Material material, @NotNull String name, @NotNull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) lines.add(toComponent(line));
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void fillItems(Player player) {
        slots.clear();
        for (int slot = 0; slot < 54; slot++) setItem(slot, DECORATION_PANE_ITEM);
        Contests.Phase phase = Contests.phase();
        List<String> info = new ArrayList<>();
        info.add(getLocaleMessage(PATH + "info-theme", false).replace("%theme%", Contests.theme()));
        info.add(getLocaleMessage(PATH + "phase." + phase.name().toLowerCase(), false));
        if (Contests.isActive()) info.add(getLocaleMessage(PATH + "info-time", false).replace("%time%", Contests.timeLeft()));
        info.add(getLocaleMessage(PATH + "info-entries", false).replace("%entries%", String.valueOf(Contests.entries().size())));
        if (phase == Contests.Phase.FINISHED) {
            info.add("");
            info.addAll(Contests.results());
        }
        setItem(SLOT_INFO, item(Material.GOLDEN_HELMET, "&6🏆 " + (Contests.name().isEmpty() ? "-" : Contests.name()), info));
        switch (phase) {
            case BUILDING -> setItem(SLOT_ACTION, Contests.worldOf(player.getUniqueId()) == null
                    ? item(Material.LIME_CONCRETE, getLocaleMessage(PATH + "join", false), List.of(getLocaleMessage(PATH + "join-lore", false)))
                    : item(Material.ENDER_PEARL, getLocaleMessage(PATH + "my-world", false), List.of()));
            case VOTING -> {
                setItem(SLOT_ACTION, item(Material.ENDER_EYE, getLocaleMessage(PATH + "next-entry", false),
                        List.of(getLocaleMessage(PATH + "next-entry-lore", false))));
                int index = 0;
                for (Map.Entry<Integer, UUID> entry : Contests.entries().entrySet()) {
                    if (index >= SLOTS.length) break;
                    Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(entry.getKey()));
                    if (planet == null) continue;
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getValue()));
                    int vote = Contests.vote(planet, player.getUniqueId());
                    meta.displayName(toComponent("&f" + Optional.ofNullable(Bukkit.getOfflinePlayer(entry.getValue()).getName()).orElse("?")));
                    meta.lore(List.of(
                            toComponent(vote > 0 ? getLocaleMessage(PATH + "your-vote", false).replace("%stars%", Ratings.stars(vote))
                                    : getLocaleMessage(PATH + "not-rated", false)),
                            toComponent(getLocaleMessage(PATH + "entry-click", false))));
                    head.setItemMeta(meta);
                    setItem(SLOTS[index], head);
                    slots.put(SLOTS[index], entry.getKey());
                    index++;
                }
            }
            default -> setItem(SLOT_ACTION, item(Material.BARRIER, getLocaleMessage(PATH + "nothing", false), List.of()));
        }
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        int slot = event.getRawSlot();
        if (slot == SLOT_ACTION) {
            if (Contests.phase() == Contests.Phase.BUILDING) {
                Contests.join(player);
            } else if (Contests.phase() == Contests.Phase.VOTING) {
                Planet next = Contests.nextUnrated(player);
                if (next == null) {
                    player.sendMessage(getLocaleMessage(PATH + "all-rated"));
                    return;
                }
                player.closeInventory();
                next.connectPlayer(player);
            }
            return;
        }
        Integer worldId = slots.get(slot);
        if (worldId == null) return;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(worldId));
        if (planet == null) return;
        if (event.isRightClick()) {
            new StarPickerMenu(getLocaleMessage(PATH + "rate-title", false), Contests.vote(planet, player.getUniqueId()),
                    value -> Contests.rate(player, planet, value)).open(player);
        } else {
            player.closeInventory();
            planet.connectPlayer(player);
        }
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_RECOMMENDATIONS.play(event.getPlayer());
    }

    /**
     * /contest for players; admin subcommands with opencreativepp.contest.admin.
     */
    public static final class ContestCommand implements TabExecutor {
        private static final List<String> ADMIN = List.of("create", "theme", "generator", "size", "time", "reward", "start", "vote", "finish", "cancel", "info");

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length > 0 && ADMIN.contains(args[0].toLowerCase())) {
                if (!sender.hasPermission("opencreativepp.contest.admin")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return true;
                }
                admin(sender, args);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getLocaleMessage(PATH + "admin-usage"));
                return true;
            }
            if (Contests.phase() == Contests.Phase.NONE || Contests.phase() == Contests.Phase.DRAFT) {
                player.sendMessage(getLocaleMessage(PATH + "no-contest"));
                return true;
            }
            new ContestMenu(player).open(player);
            return true;
        }

        private static void admin(@NotNull CommandSender sender, @NotNull String[] args) {
            String rest = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            boolean draft = Contests.phase() == Contests.Phase.DRAFT;
            switch (args[0].toLowerCase()) {
                case "create" -> {
                    if (Contests.isActive()) {
                        sender.sendMessage(getLocaleMessage(PATH + "admin-active"));
                        return;
                    }
                    if (rest.isEmpty()) {
                        sender.sendMessage(getLocaleMessage(PATH + "admin-usage"));
                        return;
                    }
                    Contests.create(rest);
                }
                case "theme" -> {
                    if (!draft || rest.isEmpty()) break;
                    Contests.set("theme", rest);
                }
                case "generator" -> {
                    if (!draft || rest.isEmpty()) break;
                    Contests.set("generator", rest);
                }
                case "size", "time", "reward" -> {
                    if (!draft) break;
                    try {
                        if (args[0].equalsIgnoreCase("size")) Contests.set("world-size", Integer.parseInt(args[1]));
                        else if (args[0].equalsIgnoreCase("time")) {
                            Contests.set("build-minutes", Integer.parseInt(args[1]));
                            Contests.set("vote-minutes", Integer.parseInt(args[2]));
                        } else {
                            List<Long> rewards = new ArrayList<>(Contests.rewards());
                            int place = Integer.parseInt(args[1]);
                            while (rewards.size() < place) rewards.add(0L);
                            rewards.set(place - 1, Long.parseLong(args[2]));
                            Contests.set("rewards", rewards);
                        }
                    } catch (Exception error) {
                        sender.sendMessage(getLocaleMessage(PATH + "admin-usage"));
                        return;
                    }
                }
                case "start" -> {
                    if (!Contests.startBuilding()) sender.sendMessage(getLocaleMessage(PATH + "admin-usage"));
                }
                case "vote" -> {
                    if (!Contests.startVoting()) sender.sendMessage(getLocaleMessage(PATH + "admin-usage"));
                }
                case "finish" -> Contests.finish();
                case "cancel" -> Contests.cancel();
                default -> {
                }
            }
            sender.sendMessage("§7" + Contests.draftInfo());
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1 && sender.hasPermission("opencreativepp.contest.admin")) {
                return ADMIN.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("generator")) {
                return ua.mcchickenstudio.opencreative.utils.world.generators.WorldGenerators.getInstance().getWorldGenerators()
                        .stream().map(generator -> generator.getID()).toList();
            }
            return List.of();
        }
    }
}
