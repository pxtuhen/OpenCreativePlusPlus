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

package ua.mcchickenstudio.opencreative.plus.backups;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.menus.AbstractMenu;
import ua.mcchickenstudio.opencreative.menus.ConfirmationMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * Backups of a world: list, "create now", restore with confirmation.
 */
public final class BackupsMenu extends AbstractMenu {

    private static final String PATH = "plus.backups.";
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int SLOT_CREATE = 49;
    private static final Map<Integer, Long> manualCooldowns = new HashMap<>();

    private final Player player;
    private final Planet planet;
    private final Map<Integer, File> slots = new HashMap<>();

    public BackupsMenu(@NotNull Player player, @NotNull Planet planet) {
        super(6, getLocaleMessage(PATH + "title", false).replace("%id%", String.valueOf(planet.getId())));
        this.player = player;
        this.planet = planet;
    }

    @Override
    public void fillItems(Player player) {
        slots.clear();
        for (int slot = 0; slot < 54; slot++) setItem(slot, DECORATION_PANE_ITEM);
        List<File> backups = Backups.list(planet);
        SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        for (int i = 0; i < backups.size() && i < SLOTS.length; i++) {
            File file = backups.get(i);
            ItemStack item = new ItemStack(file.getName().contains("before-restore") ? Material.ORANGE_DYE
                    : file.getName().contains("manual") ? Material.LIME_DYE : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(toComponent("&f" + date.format(new Date(file.lastModified()))));
            String kind = file.getName().contains("before-restore") ? "kind-before-restore"
                    : file.getName().contains("manual") ? "kind-manual" : "kind-auto";
            meta.lore(List.of(
                    toComponent(getLocaleMessage(PATH + kind, false)),
                    toComponent("&7" + (file.length() / 1024) + " КБ"),
                    toComponent(""),
                    toComponent(getLocaleMessage(PATH + "click-restore", false))));
            item.setItemMeta(meta);
            setItem(SLOTS[i], item);
            slots.put(SLOTS[i], file);
        }
        if (backups.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(toComponent(getLocaleMessage(PATH + "empty", false)));
            empty.setItemMeta(meta);
            setItem(22, empty);
        }
        ItemStack create = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = create.getItemMeta();
        meta.displayName(toComponent(getLocaleMessage(PATH + "create", false)));
        meta.lore(List.of(toComponent(getLocaleMessage(PATH + "create-lore", false)
                .replace("%keep%", String.valueOf(OpenCreative.getPlugin().getConfig().getInt("backups.keep", 5))))));
        create.setItemMeta(meta);
        setItem(SLOT_CREATE, create);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!isPlayerClicked(event) || !isClickedInMenuSlots(event)) return;
        if (Backups.isBusy(planet)) {
            player.sendMessage(getLocaleMessage(PATH + "busy"));
            return;
        }
        if (event.getRawSlot() == SLOT_CREATE) {
            long cooldown = OpenCreative.getPlugin().getConfig().getLong("backups.manual-cooldown-minutes", 10) * 60_000L;
            long last = manualCooldowns.getOrDefault(planet.getId(), 0L);
            if (!player.hasPermission("opencreativepp.backup.admin") && System.currentTimeMillis() - last < cooldown) {
                player.sendMessage(getLocaleMessage(PATH + "cooldown")
                        .replace("%minutes%", String.valueOf((cooldown - (System.currentTimeMillis() - last)) / 60_000 + 1)));
                return;
            }
            manualCooldowns.put(planet.getId(), System.currentTimeMillis());
            player.closeInventory();
            player.sendMessage(getLocaleMessage(PATH + "creating"));
            Backups.create(planet, "manual", file -> player.sendMessage(getLocaleMessage(file == null ? PATH + "failed" : PATH + "created")));
            return;
        }
        File file = slots.get(event.getRawSlot());
        if (file == null) return;
        String when = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(file.lastModified()));
        ConfirmationMenu confirm = new ConfirmationMenu(
                getLocaleMessage(PATH + "confirm-title", false),
                Material.CLOCK,
                getLocaleMessage(PATH + "confirm-name", false).replace("%date%", when),
                List.of(getLocaleMessage(PATH + "confirm-lore", false)),
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(getLocaleMessage(PATH + "restoring"));
                        Backups.restore(planet, file).thenAccept(ok -> player.sendMessage(getLocaleMessage(ok ? PATH + "restored" : PATH + "failed")));
                    }
                });
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> confirm.open(player));
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {
        Sounds.MENU_OPEN_WORLD_SETTINGS.play(event.getPlayer());
    }

    /**
     * /backups — backups of current own world; /backups &lt;id&gt; — any world (opencreativepp.backup.admin).
     * Console: /backups &lt;id&gt; create, /backups &lt;id&gt; restore &lt;file&gt;.
     */
    public static final class BackupsCommand implements TabExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!Backups.isEnabled()) {
                sender.sendMessage(getLocaleMessage(PATH + "disabled"));
                return true;
            }
            Planet planet;
            if (args.length >= 1) {
                if (!sender.hasPermission("opencreativepp.backup.admin")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return true;
                }
                planet = OpenCreative.getPlanetsManager().getPlanetById(args[0]);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return true;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("restore")) {
                    File file = Backups.list(planet).stream().filter(backup -> backup.getName().equals(args[2])).findFirst().orElse(null);
                    if (file == null) {
                        sender.sendMessage(getLocaleMessage(PATH + "failed"));
                        return true;
                    }
                    sender.sendMessage(getLocaleMessage(PATH + "restoring"));
                    Backups.restore(planet, file).thenAccept(ok -> sender.sendMessage(getLocaleMessage(ok ? PATH + "restored" : PATH + "failed")));
                    return true;
                }
                if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
                    sender.sendMessage(getLocaleMessage(PATH + "creating"));
                    Backups.create(planet, "manual", file -> sender.sendMessage(file == null
                            ? getLocaleMessage(PATH + "failed") : getLocaleMessage(PATH + "created") + " " + file.getName()));
                    return true;
                }
            } else {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("/backups <id> create");
                    return true;
                }
                planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
                if (planet == null || !planet.isOwner(player)) {
                    sender.sendMessage(getLocaleMessage(PATH + "only-owner"));
                    return true;
                }
            }
            if (sender instanceof Player player) new BackupsMenu(player, planet).open(player);
            return true;
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 2) return List.of("create", "restore");
            if (args.length == 3 && args[1].equalsIgnoreCase("restore")) {
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(args[0]);
                if (planet != null) return Backups.list(planet).stream().map(File::getName).toList();
            }
            return List.of();
        }
    }
}
