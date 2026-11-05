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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.modules.Module;
import ua.mcchickenstudio.opencreative.commands.experiments.Experiment;
import ua.mcchickenstudio.opencreative.commands.experiments.Experiments;
import ua.mcchickenstudio.opencreative.indev.messages.PlaceholderReplacer;
import ua.mcchickenstudio.opencreative.menus.CreativeMenu;
import ua.mcchickenstudio.opencreative.menus.world.WorldModerationMenu;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.filters.Filter;
import ua.mcchickenstudio.opencreative.settings.filters.FilterResult;
import ua.mcchickenstudio.opencreative.settings.groups.Group;
import ua.mcchickenstudio.opencreative.settings.groups.LimitType;
import ua.mcchickenstudio.opencreative.settings.items.Items;
import ua.mcchickenstudio.opencreative.settings.items.ItemsGroup;
import ua.mcchickenstudio.opencreative.utils.CooldownUtils;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.utils.MessageUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerUtils;
import ua.mcchickenstudio.opencreative.utils.world.WorldUtils;
import ua.mcchickenstudio.opencreative.utils.world.generators.EmptyGenerator;
import ua.mcchickenstudio.opencreative.utils.world.generators.FlatGenerator;
import ua.mcchickenstudio.opencreative.wanders.Wander;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ua.mcchickenstudio.opencreative.utils.CooldownUtils.checkAndSetCooldownWithMessage;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.*;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.*;

/**
 * <h1>CreativeCommand</h1>
 * This command has special tools only for server admins.
 * Can change plugin's behaviour, change settings or
 * manipulate with worlds.
 * <p>
 * Available: For server admins with specific permissions.
 */
public class CreativeCommand extends CommandHandler {

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getCopyrightMessage());
            if (sender instanceof Player player) {
                Sounds.OPENCREATIVE.play(player);
                new CreativeMenu().open(player);
            }
            return;
        }
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
            if (!checkAndSetCooldownWithMessage(player, CooldownUtils.CooldownType.GENERIC_COMMAND)) return;
        } else {
            player = null;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("opencreative.reload")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.reloading"));
                if (player != null) {
                    Sounds.RELOADING.play(player);
                }
                if (OpenCreative.getDownloadManager().isWorking()) {
                    OpenCreative.getDownloadManager().shutdown();
                }
                OpenCreative.getSettings().load(true);
                loadLocales();
                OpenCreative.getDownloadManager().start();
                sender.sendMessage(getLocaleMessage("creative.reloaded"));
                if (player != null) {
                    Sounds.RELOADED.play(player);
                }
            }
            case "updatelocale" -> {
                if (!sender.hasPermission("opencreative.updatelocale")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (player != null) {
                    Sounds.RELOADING.play(player);
                }
                int added = MessageUtils.addMissingMessageLines();
                if (added == -1) {
                    sender.sendMessage(getLocaleMessage("creative.locale.cant-update"));
                } else if (added == 0) {
                    sender.sendMessage(getLocaleMessage("creative.locale.not-updated"));
                } else {
                    sender.sendMessage(getLocaleMessage("creative.locale.updated").replace("%amount%", String.valueOf(added)));
                }
                if (player != null) {
                    Sounds.RELOADED.play(player);
                }
            }
            case "resetlocale" -> {
                if (!sender.hasPermission("opencreative.resetlocale")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.locale.resetting"));
                if (player != null) {
                    Sounds.RELOADING.play(player);
                }
                FileUtils.resetLocales();
                sender.sendMessage(getLocaleMessage("creative.locale.reset"));
                if (player != null) {
                    Sounds.RELOADED.play(player);
                }
            }
            case "info" -> {
                if (!sender.hasPermission("opencreative.info")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1]);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(args[1]);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return;
                }
                long now = System.currentTimeMillis();
                sender.sendMessage(getLocaleMessage("world.info").replace("%name%", planet.getInformation().getDisplayName()).replace("%id%", String.valueOf(planet.getId())).replace("%creation-time%", getElapsedTime(now, planet.getCreationTime())).replace("%activity-time%", getElapsedTime(now, planet.getLastActivityTime())).replace("%online%", String.valueOf(planet.getOnline())).replace("%builders%", planet.getWorldPlayers().getBuilders()).replace("%coders%", planet.getWorldPlayers().getDevelopers()).replace("%owner%", planet.getOwnerName()).replace("%sharing%", planet.getSharing().getName()).replace("%mode%", planet.getMode().getName()).replace("%description%", planet.getInformation().getDescription()));
            }
            case "groups" -> handleGroupsCommand(sender, args);
            case "register" -> {
                if (!sender.hasPermission("opencreative.world.register")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (Exception ignored) {
                    sender.sendMessage(getLocaleMessage("world.not-numeric-id"));
                    return;
                }
                File planetFolder = new File(FileUtils.getPlanetsStorageFolder(), "planet" + id);
                if (!planetFolder.exists() || !planetFolder.isDirectory()) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", planetFolder.getPath()));
                    return;
                }
                if (OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1]) != null) {
                    sender.sendMessage(getLocaleMessage("world.already-registered").replace("%id%", args[1]));
                    return;
                }
                Planet newPlanet = new Planet(id);
                OpenCreative.getPlanetsManager().registerPlanet(newPlanet);
                sender.sendMessage(getLocaleMessage("world.registered").replace("%id%", args[1]));
            }
            case "unregister" -> {
                if (!sender.hasPermission("opencreative.world.unregister")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", "/join " + id));
                    return;
                }
                OpenCreative.getPlanetsManager().unregisterPlanet(planet);
                sender.sendMessage(getLocaleMessage("world.unregistered").replace("%id%", args[1]));
            }
            case "updateicons" -> {
                if (!sender.hasPermission("opencreative.update-icons")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                    planet.getInformation().updateIconAsync();
                }
                for (Module module : OpenCreative.getModuleManager().getModules()) {
                    module.getInformation().updateIconAsync();
                }
                sender.sendMessage(getLocaleMessage("creative.updated-icons"));
            }
            case "updateworld" -> {
                if (!sender.hasPermission("opencreative.world.update")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", id).replace("%path%", "/join " + id));
                    return;
                }
                planet.loadInfo();
                planet.getInformation().loadInformation();
                planet.getInformation().updateIconAsync();
                sender.sendMessage(getLocaleMessage("world.updated-info").replace("%id%", args[1]));
            }
            case "setowner" -> {
                if (!sender.hasPermission("opencreative.world.set-owner")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", "/join " + id));
                    return;
                }
                String ownerNameOrUUID = args[2];
                OfflinePlayer newOwner;
                UUID uuid = getUUIDFromText(ownerNameOrUUID);
                if (uuid != null) {
                    newOwner = Bukkit.getOfflinePlayer(uuid);
                } else {
                    newOwner = Bukkit.getOfflinePlayer(ownerNameOrUUID);
                }
                if (planet.isOwner(newOwner.getUniqueId())) {
                    sender.sendMessage(getPlayerLocaleMessage("world.already-owner", newOwner).replace("%id%", id).replace("%uuid%", newOwner.getUniqueId().toString()));
                    Sounds.PLAYER_FAIL.play(sender);
                } else {
                    planet.setOwner(newOwner.getName());
                    planet.loadInfo();
                    planet.getInformation().loadInformation();
                    sender.sendMessage(getPlayerLocaleMessage("world.set-owner", newOwner).replace("%id%", id).replace("%uuid%", newOwner.getUniqueId().toString()));
                }
            }
            case "clearplayer" -> {
                if (!sender.hasPermission("opencreative.clear-player")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String name = args[1];
                Player foundPlayer = Bukkit.getPlayer(name);
                if (foundPlayer == null) {
                    sender.sendMessage(getLocaleMessage("not-found-player"));
                    return;
                }
                Wander wander = OpenCreative.getWander(foundPlayer);
                wander.setConnectingToPlanet(false);
            }
            case "setsize" -> {
                if (!sender.hasPermission("opencreative.world.set-size")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", "/join " + id));
                    return;
                }
                String sizeString = args[2];
                int size;
                try {
                    size = Integer.parseInt(sizeString);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(getLocaleMessage("world.bad-size").replace("%size%", sizeString).replace("%id%", id));
                    Sounds.PLAYER_FAIL.play(sender);
                    return;
                }
                if (size < 0) {
                    sender.sendMessage(getLocaleMessage("world.bad-size").replace("%size%", sizeString).replace("%id%", id));
                    Sounds.PLAYER_FAIL.play(sender);
                    return;
                }
                if (size > 9999999) {
                    size = 9999999;
                }
                if (size == 0) {
                    planet.getTerritory().resetWorldSize();
                    sender.sendMessage(getLocaleMessage("world.reset-size").replace("%size%", sizeString).replace("%id%", id));
                    return;
                }
                if (planet.getTerritory().getWorldSize() == size) {
                    sender.sendMessage(getLocaleMessage("world.same-size").replace("%size%", sizeString).replace("%id%", id));
                    Sounds.PLAYER_FAIL.play(sender);
                    return;
                }
                planet.getTerritory().setWorldSize(size, true);
                sender.sendMessage(getLocaleMessage("world.set-size").replace("%size%", sizeString).replace("%id%", id));
            }
            case "recommend" -> {
                if (!sender.hasPermission("opencreative.world.recommend")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", "/join " + id));
                    return;
                }
                if (OpenCreative.getSettings().addRecommendedWorld(planet.getId())) {
                    sender.sendMessage(getLocaleMessage("world.recommended").replace("%id%", id));
                } else {
                    sender.sendMessage(getLocaleMessage("world.already-recommended").replace("%id%", id));
                    Sounds.PLAYER_FAIL.play(sender);
                }
            }
            case "ignoremessage" -> {
                if (!sender.hasPermission("opencreative.resetlocale.add-ignored")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String path = args[1];
                if (!MessageUtils.messageExists(path)) {
                    sender.sendMessage(getLocaleMessage("creative.locale.unknown-message").replace("%path%", path));
                    return;
                }
                if (OpenCreative.getSettings().addMessageIgnoringReset(path)) {
                    sender.sendMessage(getLocaleMessage("creative.locale.ignored-message").replace("%path%", path));
                } else {
                    sender.sendMessage(getLocaleMessage("creative.locale.already-ignored-message").replace("%path%", path));
                    Sounds.PLAYER_FAIL.play(sender);
                }
            }
            case "unignoremessage" -> {
                if (!sender.hasPermission("opencreative.resetlocale.remove-ignored")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String path = args[1];
                if (OpenCreative.getSettings().removeMessageIgnoringReset(path)) {
                    sender.sendMessage(getLocaleMessage("creative.locale.unignored-message").replace("%path%", path));
                } else {
                    if (!MessageUtils.messageExists(path)) {
                        sender.sendMessage(getLocaleMessage("creative.locale.unknown-message").replace("%path%", path));
                    } else {
                        sender.sendMessage(getLocaleMessage("creative.locale.already-unignored-message").replace("%path%", path));
                    }
                    Sounds.PLAYER_FAIL.play(sender);
                }
            }
            case "setmessage" -> {
                if (!sender.hasPermission("opencreative.set-message")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String path = args[1];
                String newContent = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                MessageUtils.setMessage(path, newContent);
                printMessage(sender, path);
            }
            case "unrecommend" -> {
                if (!sender.hasPermission("opencreative.world.unrecommend")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String id = args[1];
                Planet planet = OpenCreative.getPlanetsManager().getPlanetById(id);
                if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(id);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("world.not-found").replace("%id%", args[1]).replace("%path%", "/join " + id));
                    return;
                }
                if (OpenCreative.getSettings().removeRecommendedWorld(planet.getId())) {
                    sender.sendMessage(getLocaleMessage("world.unrecommended").replace("%id%", id));
                } else {
                    sender.sendMessage(getLocaleMessage("world.already-unrecommended").replace("%id%", id));
                    Sounds.PLAYER_FAIL.play(sender);
                }
            }
            case "delete" -> {
                if (!sender.hasPermission("opencreative.delete")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1]);
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return;
                }
                if (OpenCreative.getPlanetsManager().deletePlanet(planet)) {
                    Sounds.WORLD_DELETION.play(sender);
                    OpenCreative.getPlugin().getLogger().info("Planet " + planet.getId() + " is being deleted by " + sender.getName());
                    sender.sendMessage(getLocaleMessage("deleting-world.message"));
                }
            }
            case "checkeveryone" -> {
                if (player == null) return;
                if (!sender.hasPermission("opencreative.moderation.check-everyone")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (!player.getMetadata("oc_checkeveryone").isEmpty()) {
                    player.removeMetadata("oc_checkeveryone", OpenCreative.getPlugin());
                    return;
                }
                List<Player> playersToCheck = Bukkit.getOnlinePlayers().stream().filter(p ->
                        !p.equals(player)).sorted(Comparator.comparingLong(Player::getFirstPlayed))
                        .map(p -> (Player) p).toList();
                if (playersToCheck.isEmpty()) {
                    sender.sendMessage(getLocaleMessage("creative.check-every-player.no-players"));
                    return;
                }

                int timeForCheck = 5;
                if (args.length >= 2) {
                    try {
                        timeForCheck = Math.clamp(Integer.parseInt(args[1]), 2, 10);
                    } catch (NumberFormatException ignored) {}
                }

                sender.sendMessage(getLocaleMessage("creative.check-every-player.started").replace("%players%", String.valueOf(playersToCheck.size())).replace("%time%", String.valueOf(timeForCheck)));

                player.setGameMode(GameMode.SPECTATOR);
                player.setFlying(true);
                Iterator<Player> iterator = playersToCheck.reversed().iterator();

                final int checkTime = timeForCheck;
                class State {
                    Player current;
                    int seconds = checkTime-2;
                }
                State state = new State();
                player.setMetadata("oc_checkeveryone", new FixedMetadataValue(OpenCreative.getPlugin(), 1));
                boolean previousVanishState = isVanished(player);
                setVanished(player, true);
                player.getScheduler().runAtFixedRate(OpenCreative.getPlugin(), task -> {
                    if (!player.isConnected()) {
                        task.cancel();
                        return;
                    }
                    if (player.getMetadata("oc_checkeveryone").isEmpty()) {
                        teleportToLobby(player);
                        sender.sendMessage(getLocaleMessage("creative.check-every-player.ended")
                                .replace("%players%", String.valueOf(playersToCheck.size())));
                        setVanished(player, previousVanishState);
                        task.cancel();
                        return;
                    }
                    if (state.current != null && state.current.isConnected() && state.seconds < checkTime) {
                        player.teleportAsync(state.current.getLocation()).thenRun(() -> {
                            player.setSpectatorTarget(state.current);
                            Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(state.current);
                            if (planet == null) {
                                player.sendActionBar(getPlayerLocaleComponent("creative.check-every-player.process.lobby.actionbar", state.current)
                                        .replaceText(new PlaceholderReplacer("firstjoin", getElapsedTime(System.currentTimeMillis(), state.current.getFirstPlayed())).get()));
                            } else {
                                player.sendActionBar(parsePlanetLines(planet, getPlayerLocaleComponent("creative.check-every-player.process."
                                        + (planet.isOwner(state.current) ? "owner" : "world") + ".actionbar", state.current)
                                        .replaceText(new PlaceholderReplacer("firstjoin", getElapsedTime(System.currentTimeMillis(), state.current.getFirstPlayed())).get())));
                            }
                        });
                    }
                    state.seconds++;
                    if (state.seconds <= checkTime) {
                        return;
                    }
                    while (iterator.hasNext()) {
                        Player target = iterator.next();
                        if (!target.isConnected()) {
                            sender.sendMessage(getPlayerLocaleMessage("creative.check-every-player.process.offline", target).replace("%firstjoin%", getElapsedTime(System.currentTimeMillis(), target.getFirstPlayed())));
                            continue;
                        }
                        state.current = target;
                        state.seconds = 0;

                        player.setGameMode(GameMode.SPECTATOR);
                        player.setFlying(true);

                        player.teleportAsync(target.getLocation()).thenRun(() -> {
                            player.setGameMode(GameMode.SPECTATOR);
                            player.setFlying(true);
                            player.setSpectatorTarget(target);
                        });

                        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(target);

                        if (planet == null) {
                            sender.sendMessage(getPlayerLocaleMessage("creative.check-every-player.process.lobby.message", target)
                                    .replace("%firstjoin%", getElapsedTime(System.currentTimeMillis(), target.getFirstPlayed())));
                        } else {
                            sender.sendMessage(parsePlanetLines(planet, getPlayerLocaleMessage("creative.check-every-player.process."
                                    + (planet.isOwner(target) ? "owner" : "world") + ".message", target)
                                    .replace("%firstjoin%", getElapsedTime(System.currentTimeMillis(), target.getFirstPlayed()))));
                        }

                        return;
                    }
                    player.removeMetadata("oc_checkeveryone", OpenCreative.getPlugin());
                    setVanished(player, previousVanishState);
                    teleportToLobby(player);
                    sender.sendMessage(getLocaleMessage("creative.check-every-player.ended")
                            .replace("%players%", String.valueOf(playersToCheck.size())));
                    task.cancel();

                }, null, 5L, 20L);
            }
            case "greplogs" -> {
                if (args.length <= 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                Bukkit.getAsyncScheduler().runNow(OpenCreative.getPlugin(), (task) -> {
                    int limit = 20;
                    try {
                        limit = Math.clamp(Integer.parseInt(args[1]), 1, 300);
                    } catch (Exception ignored) {}
                    File latestLog = new File(new File(Bukkit.getServer().getWorldContainer(), "logs"), "latest.log");
                    if (!latestLog.exists()) {
                        sender.sendMessage("Latest log is empty.");
                        return;
                    }
                    String match = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).toLowerCase(Locale.ROOT);
                    Deque<Component> matches = new ArrayDeque<>(limit);
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    Pattern dateTimePattern = Pattern.compile("^\\[(\\d{2}:\\d{2}:\\d{2})]");
                    int found = 0;
                    boolean isPlayer = sender instanceof Player;
                    try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder().setFile(latestLog).get()) {
                        String line;
                        while ((line = reader.readLine()) != null && found < limit) {
                            if (line.length() > 12 && line.startsWith("[Folia", 11)) continue;
                            if (!line.toLowerCase(Locale.ROOT).contains(match)) continue;
                            Matcher matcher = dateTimePattern.matcher(line);
                            if (!matcher.find()) {
                                continue;
                            }
                            LocalTime logTime = LocalTime.parse(matcher.group(1), dateTimeFormatter);
                            LocalTime now = LocalTime.now();
                            NamedTextColor color = NamedTextColor.RED;
                            long age = Duration.between(logTime, now).getSeconds();
                            if (age < 0) {
                                age += 24 * 60 * 60;
                            }
                            if (age <= 60) {
                                color = NamedTextColor.GREEN;
                            } else if (age <= 180) {
                                color = NamedTextColor.YELLOW;
                            } else if (age <= 300) {
                                color = NamedTextColor.GOLD;
                            }
                            String foundLine = line
                                    .replace("[Server thread/INFO]: ", "")
                                    .replace("issued server command:", "used command:")
                                    .replace("User Authenticator ", "");
                            matches.add(Component.text(" Found: ", NamedTextColor.GRAY)
                                    .append(Component.text(isPlayer ? substring(foundLine, 247) : foundLine, color)
                                            .clickEvent(ClickEvent.suggestCommand(substring(foundLine, 247)))));
                            found++;
                        }
                    } catch (Exception error) {
                        sender.sendMessage("Failed to read latest.log: " + error.getMessage());
                        return;
                    }
                    if (matches.isEmpty()) {
                        sender.sendMessage("No matches found for: " + match);
                        return;
                    }
                    sender.sendMessage(" ");
                    sender.sendMessage(" ");
                    sender.sendMessage("- - - - - ");
                    for (Component matched : matches.reversed()) {
                        sender.sendMessage(matched);
                    }
                    sender.sendMessage("- Found " + found + " lines containing: " + match + " -");
                });
            }
            case "moderate", "moderation" -> {
                if (player == null) return;
                if (!sender.hasPermission("opencreative.moderation.menus")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                Planet planet;
                if (args.length == 1) {
                    planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
                    if (planet == null) {
                        sender.sendMessage(getLocaleMessage("too-few-args"));
                        return;
                    }
                } else {
                    planet = OpenCreative.getPlanetsManager().getPlanetById(args[1]);
                    if (planet == null) planet = OpenCreative.getPlanetsManager().getPlanetByCustomID(args[1]);
                }
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return;
                }
                new WorldModerationMenu(planet).open(player);
            }
            case "load" -> {
                if (!sender.hasPermission("opencreative.world.load")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1].replace("dev", ""));
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return;
                }
                if (!planet.isLoaded()) {
                    planet.getTerritory().load().whenComplete((result, error) -> sender.sendMessage(getLocaleMessage("world.loaded").replace("%id%", args[1])));
                } else if (args[1].contains("dev") && !planet.getDevPlanet().isLoaded()) {
                    planet.getDevPlanet().load();
                    sender.sendMessage(getLocaleMessage("world.loaded").replace("%id%", args[1]));
                } else {
                    sender.sendMessage(getLocaleMessage("world.already-loaded").replace("%id%", args[1]));
                }
            }
            case "dev" -> {
                if (!sender.hasPermission("opencreative.world.dev.visit")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1].replace("dev", ""));
                if (planet == null) {
                    sender.sendMessage(getLocaleMessage("no-planet-found"));
                    return;
                }
                if (planet.getDevPlanet().isLoaded()) {
                    sender.sendMessage(getLocaleMessage("world.already-loaded").replace("%id%", args[1]));
                    return;
                }
                if (player == null) { // OpenCreative++: NPE when run from console
                    sender.sendMessage(getLocaleMessage("only-players"));
                    return;
                }
                if (!planet.isLoaded()) {
                    planet.getTerritory().load();
                }
                planet.getDevPlanet().connectPlayer(player);
                sender.sendMessage(getLocaleMessage("world.loaded").replace("%id%", args[1]));
            }
            case "creative-chat", "chat" -> {
                if (!sender.hasPermission("opencreative.creative-chat")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if ("disable".equalsIgnoreCase(args[1])) {
                    OpenCreative.getSettings().setCreativeChatEnabled(false);
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(getLocaleMessage("creative.creative-chat.disabled").replace("%player%", sender.getName()));
                    }
                } else if ("enable".equalsIgnoreCase(args[1])) {
                    OpenCreative.getSettings().setCreativeChatEnabled(true);
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(getLocaleMessage("creative.creative-chat.enabled").replace("%player%", sender.getName()));
                    }
                }
                if ("clear".equalsIgnoreCase(args[1])) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n  \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n ");
                        onlinePlayer.sendMessage(getLocaleMessage("creative.creative-chat.cleared").replace("%player%", sender.getName()));
                    }
                }
            }
            case "debug" -> {
                if (!sender.hasPermission("opencreative.debug")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if ("disable".equalsIgnoreCase(args[1]) || "off".equalsIgnoreCase(args[1])) {
                    OpenCreative.getSettings().setDebug(false);
                    sender.sendMessage(getLocaleMessage("creative.debug.disabled").replace("%player%", sender.getName()));
                } else if ("enable".equalsIgnoreCase(args[1]) || "on".equalsIgnoreCase(args[1])) {
                    OpenCreative.getSettings().setDebug(true);
                    sender.sendMessage(getLocaleMessage("creative.debug.enabled").replace("%player%", sender.getName()));
                }
            }
            case "all", "created" -> {
                if (!sender.hasPermission("opencreative.list.all")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                int amount = OpenCreative.getPlanetsManager().getPlanets().size();
                amount += OpenCreative.getPlanetsManager().getCorruptedPlanets().size();
                sender.sendMessage(getLocaleMessage("creative.all-worlds-amount").replace("%amount%", String.valueOf(amount)));
            }
            case "spy" -> {
                if (!sender.hasPermission("opencreative.spy")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (player == null) {
                    sender.sendMessage(getLocaleMessage("only-players"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if ("disable".equalsIgnoreCase(args[1]) || "off".equalsIgnoreCase(args[1])) {
                    if (PlayerUtils.disableSpying(player)) {
                        sender.sendMessage(getLocaleMessage("creative.spy.disabled"));
                    } else {
                        sender.sendMessage(getLocaleMessage("creative.spy.already-disabled"));
                    }
                } else if ("enable".equalsIgnoreCase(args[1]) || "on".equalsIgnoreCase(args[1])) {
                    if (PlayerUtils.enableSpying(player)) {
                        sender.sendMessage(getLocaleMessage("creative.spy.enabled"));
                    } else {
                        sender.sendMessage(getLocaleMessage("creative.spy.already-enabled"));
                    }
                }
            }
            case "locale", "lang", "language" -> {
                if (!sender.hasPermission("opencreative.locale")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if (localizationFileExists(args[1])) {
                    OpenCreative.getPlugin().getConfig().set("messages.locale", args[1]);
                    OpenCreative.getPlugin().saveConfig();
                    loadLocalizationFile();
                    sender.sendMessage(getLocaleMessage("creative.locale.changed"));
                } else {
                    sender.sendMessage(getLocaleMessage("creative.locale.not-found"));
                }
            }
            case "sounds", "soundtheme", "soundstheme" -> {
                if (!sender.hasPermission("opencreative.sounds.theme")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if (OpenCreative.getSettings().setSoundsTheme(args[1])) {
                    sender.sendMessage(getLocaleMessage("creative.sounds.set").replace("%theme%", args[1]));
                    if (player != null) {
                        Sounds.LOBBY.play(player);
                    }
                } else {
                    sender.sendMessage(getLocaleMessage("creative.sounds.not-found").replace("%theme%", args[1]));
                }
            }
            case "sound", "playsound" -> {
                if (!sender.hasPermission("opencreative.sounds.play")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                try {
                    Sounds sound = Sounds.valueOf(args[1].toUpperCase());
                    sound.play(sender);
                } catch (Exception ignored) {
                }
            }
            case "firework", "fireworks" -> {
                if (!sender.hasPermission("opencreative.fireworks")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length == 1) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                int times = 3;
                try {
                    times = Math.clamp(Integer.parseInt(args[1]), 1, 10);
                } catch (Exception ignored) {
                }
                int seconds = 5;
                if (args.length >= 3) {
                    try {
                        seconds = Math.clamp(Integer.parseInt(args[2]), 1, 10);
                    } catch (Exception ignored) {
                    }
                }
                sender.sendMessage(getLocaleMessage("creative.fireworks").replace("%amount%", String.valueOf(times)).replace("%cooldown%", String.valueOf(seconds)));
                WorldUtils.summonFireworks(times, seconds * 20);
            }
            case "setspawn" -> {
                if (!sender.hasPermission("opencreative.set-spawn")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length == 1) {
                    if (player == null) {
                        sender.sendMessage(getLocaleMessage("too-few-args"));
                        return;
                    }
                    if (isPlanet(player.getWorld())) {
                        sender.sendMessage(getLocaleMessage("only-in-lobby"));
                        return;
                    }
                    Location location = roundLocation(player.getLocation());
                    OpenCreative.getPlugin().getConfig().set("lobby.spawn", fromLocationToMap(location));
                    if (!player.getWorld().equals(getLobbyWorld())) {
                        OpenCreative.getPlugin().getConfig().set("lobby.world", player.getWorld().getName());
                    }
                    sender.sendMessage(getLocaleMessage("creative.set-spawn").replace("%x%", String.valueOf(location.getX())).replace("%y%", String.valueOf(location.getY())).replace("%z%", String.valueOf(location.getZ())).replace("%yaw%", String.valueOf(location.getYaw())).replace("%pitch%", String.valueOf(location.getPitch())));
                    OpenCreative.getPlugin().saveConfig();
                    OpenCreative.getPlugin().reloadConfig();
                    return;
                } else if (args.length < 4) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                // /oc setspawn x y z
                Location location = getLobbyLocation();
                double x = fromTextToCoordinate(args[1], location.getX());
                double y = fromTextToCoordinate(args[2], location.getY());
                double z = fromTextToCoordinate(args[3], location.getZ());
                float yaw = location.getYaw();
                float pitch = location.getPitch();
                if (args.length >= 6) {
                    try {
                        yaw = (float) fromTextToCoordinate(args[4], location.getYaw());
                    } catch (NumberFormatException ignored) {
                    }
                    try {
                        pitch = (float) fromTextToCoordinate(args[5], location.getPitch());
                    } catch (NumberFormatException ignored) {
                    }
                }
                location.set(x, y, z);
                location.setYaw(yaw);
                location.setPitch(pitch);
                roundLocation(location);
                sender.sendMessage(getLocaleMessage("creative.set-spawn").replace("%x%", String.valueOf(location.getX())).replace("%y%", String.valueOf(location.getY())).replace("%z%", String.valueOf(location.getZ())).replace("%yaw%", String.valueOf(location.getYaw())).replace("%pitch%", String.valueOf(location.getPitch())));
                OpenCreative.getPlugin().getConfig().set("lobby.spawn", fromLocationToMap(location));
                OpenCreative.getPlugin().saveConfig();
                OpenCreative.getPlugin().reloadConfig();
            }
            case "items" -> {
                if (player == null) {
                    sender.sendMessage(getLocaleMessage("only-players"));
                    return;
                }
                if (!sender.hasPermission("opencreative.items.get-kit") && !sender.hasPermission("opencreative.items.set") && !sender.hasPermission("opencreative.items.get-slot")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                // /oc items set lobby 1
                // /oc items get lobby
                // /oc items reset lobby
                //      0     1   2    3
                String groupId = args[2];
                ItemsGroup group = ItemsGroup.getById(groupId.toUpperCase().replace("-", "_"));
                if (group == null) {
                    sender.sendMessage(getLocaleMessage("creative.items.wrong-kit").replace("%kit%", groupId));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                if (args[1].equalsIgnoreCase("get")) {
                    if (args.length == 3) {
                        // /oc items get lobby
                        if (!sender.hasPermission("opencreative.items.get-kit")) {
                            sender.sendMessage(getLocaleMessage("no-perms"));
                            return;
                        }
                        sender.sendMessage(getLocaleMessage("creative.items.received-kit").replace("%kit%", groupId));
                        player.getInventory().clear();
                        group.setItems(player);
                    } else {
                        // /oc items get lobby slot
                        if (!sender.hasPermission("opencreative.items.get-slot")) {
                            sender.sendMessage(getLocaleMessage("no-perms"));
                            return;
                        }
                        int slot = 1;
                        try {
                            slot = Math.clamp(Integer.parseInt(args[3]), 1, 36);
                        } catch (Exception ignored) {
                        }
                        if (group.giveItem(player, slot)) {
                            sender.sendMessage(getLocaleMessage("creative.items.received-from-kit").replace("%kit%", groupId).replace("%slot%", String.valueOf(slot)));
                        } else {
                            sender.sendMessage(getLocaleMessage("creative.items.empty-slot").replace("%kit%", groupId).replace("%slot%", String.valueOf(slot)));
                            Sounds.PLAYER_FAIL.play(player);
                        }
                    }
                }
                if (args[1].equalsIgnoreCase("reset")) {
                    if (!sender.hasPermission("opencreative.items.reset")) {
                        sender.sendMessage(getLocaleMessage("no-perms"));
                        return;
                    }
                    OpenCreative.getSettings().resetItemsGroup(group);
                    sender.sendMessage(getLocaleMessage("creative.items.reset-kit").replace("%kit%", groupId));
                } else if (args[1].equalsIgnoreCase("set")) {
                    if (!sender.hasPermission("opencreative.items.set")) {
                        sender.sendMessage(getLocaleMessage("no-perms"));
                        return;
                    }
                    if (args.length == 3) {
                        sender.sendMessage(getLocaleMessage("too-few-args"));
                        return;
                    }
                    int slot = 1;
                    try {
                        slot = Math.clamp(Integer.parseInt(args[3]), 1, 36);
                    } catch (Exception ignored) {
                    }
                    if (args.length == 4) {
                        sender.sendMessage(getLocaleMessage("creative.items.changed").replace("%kit%", groupId).replace("%slot%", String.valueOf(slot)));
                        OpenCreative.getSettings().setCustomItem(group, slot, player.getInventory().getItemInMainHand());
                    } else {
                        Items item = Items.getById(args[4]);
                        if (item == null) {
                            sender.sendMessage(getLocaleMessage("creative.items.wrong-preset").replace("%preset%", args[4]));
                            Sounds.PLAYER_FAIL.play(player);
                            return;
                        }
                        sender.sendMessage(getLocaleMessage("creative.items.changed").replace("%kit%", groupId).replace("%slot%", String.valueOf(slot)));
                        OpenCreative.getSettings().setCustomItem(group, slot, item);
                    }
                }
            }
            case "item" -> {
                if (player == null) {
                    sender.sendMessage(getLocaleMessage("only-players"));
                    return;
                }
                if (!sender.hasPermission("opencreative.items.get")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length == 1) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String itemId = args[1].toUpperCase().replace("-", "_");
                Items item = Items.getById(itemId);
                if (item == null) {
                    sender.sendMessage(getLocaleMessage("creative.items.wrong-preset").replace("%preset%", args[1]));
                    Sounds.PLAYER_FAIL.play(player);
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.items.given").replace("%item%", itemId.toLowerCase()));
                player.getInventory().addItem(item.get(player));
            }
            case "kick-all" -> {
                if (!sender.hasPermission("opencreative.kick-all")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String nickname = args[2];
                if ("starts".equalsIgnoreCase(args[1])) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(nickname.toLowerCase())) {
                            if (!onlinePlayer.equals(player)) {
                                onlinePlayer.kick();
                            }
                        }
                    }
                } else if ("ends".equalsIgnoreCase(args[1])) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().endsWith(nickname.toLowerCase())) {
                            if (!onlinePlayer.equals(player)) {
                                onlinePlayer.kick();
                            }
                        }
                    }
                } else if ("contains".equalsIgnoreCase(args[1])) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().contains(nickname.toLowerCase())) {
                            if (!onlinePlayer.equals(player)) {
                                onlinePlayer.kick();
                            }
                        }
                    }
                } else if ("ignore".equalsIgnoreCase(args[1])) {
                    List<String> nicknames = new ArrayList<>(List.of(args)).subList(1, args.length);
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        boolean ignore = false;
                        for (String nick : nicknames) {
                            if (nick.equalsIgnoreCase(onlinePlayer.getName()) || onlinePlayer.getName().equalsIgnoreCase(sender.getName())) {
                                ignore = true;
                            }
                        }
                        if (!ignore) {
                            onlinePlayer.kick();
                        }
                    }
                }
            }
            case "maintenance" -> handleMaintenanceCommand(sender, Arrays.copyOfRange(args, 0, args.length));
            case "unload" -> handleUnloadCommand(sender, args);
            case "update", "updates", "checkupdate" -> handleUpdateCommand(sender);
            case "list" -> {
                if (args.length == 1) {
                    if (!sender.hasPermission("opencreative.list.loaded")) {
                        sender.sendMessage(getLocaleMessage("no-perms"));
                        return;
                    }
                    List<World> worlds = Bukkit.getWorlds().stream().filter(WorldUtils::isPlanet).toList();
                    if (worlds.isEmpty()) {
                        sender.sendMessage(getLocaleMessage("creative.loaded-worlds.no-worlds"));
                        return;
                    }
                    int devCount = 0;
                    Set<Planet> corruptedPlanets = OpenCreative.getPlanetsManager().getCorruptedPlanets();
                    for (World world : worlds) {
                        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(world);
                        if (planet == null) {
                            for (Planet corrupted : corruptedPlanets) {
                                if (corrupted.getWorldName().equals(world.getName())) {
                                    planet = corrupted;
                                    break;
                                }
                            }
                            if (planet == null) continue;
                        }
                        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
                        long now = System.currentTimeMillis();
                        if (isDevPlanet(world)) {
                            devCount++;
                            sender.sendMessage(getLocaleMessage("creative.loaded-worlds.world-dev").replace("%id%", String.valueOf(planet.getId())));
                        } else {
                            sender.sendMessage(getLocaleMessage("creative.loaded-worlds.world").replace("%id%", String.valueOf(planet.getId())).replace("%name%", substring(serializer.serialize(planet.getInformation().displayName()), 25)).replace("%online%", String.valueOf(planet.getOnline())).replace("%creation-time%", getElapsedTime(now, planet.getCreationTime())));
                        }
                    }
                    sender.sendMessage(getLocaleMessage("creative.loaded-worlds.list").replace("%amount%", String.valueOf(worlds.size())).replace("%build%", String.valueOf(worlds.size() - devCount)).replace("%dev%", String.valueOf(devCount)));
                } else {
                    if (!sender.hasPermission("opencreative.list.players")) {
                        sender.sendMessage(getLocaleMessage("no-perms"));
                        return;
                    }
                    String nickname = args[1];
                    Set<Planet> planets = OpenCreative.getPlanetsManager().getPlanetsByOwner(nickname);
                    if (planets.isEmpty()) {
                        sender.sendMessage(getLocaleMessage("creative.player-worlds.no-worlds").replace("%player%", nickname));
                        return;
                    }
                    for (Planet planet : planets) {
                        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
                        long now = System.currentTimeMillis();
                        sender.sendMessage(getLocaleMessage("creative.player-worlds.world").replace("%id%", String.valueOf(planet.getId())).replace("%name%", substring(serializer.serialize(planet.getInformation().displayName()), 25)).replace("%online%", String.valueOf(planet.getOnline())).replace("%creation-time%", getElapsedTime(now, planet.getCreationTime())));
                    }
                    sender.sendMessage(getLocaleMessage("creative.player-worlds.list").replace("%player%", nickname).replace("%amount%", String.valueOf(planets.size())));
                }
            }
            case "deprecated" -> handleDeprecatedCommand(sender, args);
            case "corrupted" -> handleCorruptedCommand(sender, args);
            case "bc", "broadcast" -> {
                if (!sender.hasPermission("opencreative.broadcast")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length <= 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String type = args[1];
                if (type.equalsIgnoreCase("title")) {
                    // /oc broadcast title 20 100 20 Title\nSubtitle
                    if (args.length <= 5) {
                        sender.sendMessage(getLocaleMessage("too-few-args"));
                        return;
                    }
                    String message = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                    String[] split = message.split("\\\\n");
                    int fadeIn = 20;
                    try {
                        fadeIn = Math.clamp(Integer.parseInt(args[2]), 0, 1000);
                    } catch (Exception ignored) {}
                    int stay = 100;
                    try {
                        stay = Math.clamp(Integer.parseInt(args[3]), 0, 1000);
                    } catch (Exception ignored) {}
                    int fadeOut = 20;
                    try {
                        fadeOut = Math.clamp(Integer.parseInt(args[4]), 0, 1000);
                    } catch (Exception ignored) {}
                    String title = split[0];
                    String subtitle = "";
                    if (split.length >= 2) {
                        subtitle = split[1];
                    }
                    Component titleComponent = toComponent(title);
                    Component subtitleComponent = toComponent(subtitle);
                    Bukkit.getConsoleSender().sendMessage(Component.text("[" + OpenCreative.getPlugin().getLogger().getName()
                            + "] [BROADCAST" + (player != null ? ": " + player.getName() : "") + "] [TITLE] ")
                            .append(titleComponent).appendSpace().append(subtitleComponent));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.showTitle(Title.title(titleComponent, subtitleComponent, fadeIn, stay, fadeOut));
                    }
                    return;
                }
                // /oc broadcast actionbar Text
                // /oc broadcast text Text
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (type.equalsIgnoreCase("text")) {
                    message = message.replace("\\n", "\n");
                }
                Component component = toComponent(message);
                if (type.equalsIgnoreCase("actionbar")) {
                    Bukkit.getConsoleSender().sendMessage(Component.text("[" + OpenCreative.getPlugin().getLogger().getName()
                            + "] [BROADCAST" + (player != null ? ": " + player.getName() : "") + "] [ACTIONBAR] ").append(component));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendActionBar(component);
                    }
                } else if (type.equalsIgnoreCase("text")) {
                    Bukkit.getConsoleSender().sendMessage(Component.text("[" + OpenCreative.getPlugin().getLogger().getName()
                            + "] [BROADCAST" + (player != null ? ": " + player.getName() : "") + "] [CHAT] ").append(component));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(component);
                    }
                }
            }
            case "print" -> {
                if (!sender.hasPermission("opencreative.print")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                printMessage(sender, args[1]);
            }
            case "checkfilters" -> {
                if (!sender.hasPermission("opencreative.filters.check")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                for (Filter.Context context : Filter.Context.values()) {
                    FilterResult result = Filter.getInstance().checkContent(text, context);
                    if (result.rule() == null) {
                        sender.sendMessage(context.name().toLowerCase() + ": no violation");
                        continue;
                    }
                    sender.sendMessage(context.name().toLowerCase() + ": " + result.rule().getId() + " -> " + result.filteredMessage());
                }
            }
            case "minimsg" -> {
                if (!sender.hasPermission("opencreative.print.minimessage")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                sender.sendMessage(MiniMessage.miniMessage().deserialize(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
            }
            case "minimsg2" -> {
                if (!sender.hasPermission("opencreative.print.minimessage")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if (player == null) {
                    sender.sendMessage(getLocaleMessage("only-players"));
                    return;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                sender.sendMessage(toComponent(message).clickEvent(ClickEvent.suggestCommand(fromLegacyToMiniMessage(message))));
                sender.sendActionBar(Component.text("Original: " + message));

            }
            case "stability" -> {
                if (!sender.hasPermission("opencreative.stability")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.stability.actionbar").replace("%memory%", OpenCreative.getStability().getMemoryState().getLocalized()).replace("%storage%", OpenCreative.getStability().getStorageState().getLocalized()).replace("%tps%", OpenCreative.getStability().getTicksState().getLocalized()).replace("%database%", OpenCreative.getStability().getDatabaseState().getLocalized()));
            }
            case "experiments" -> handleExperimentsCommand(sender, args);
            case "uuid", "getuuid" -> {
                if (!sender.hasPermission("opencreative.getuuid")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                String text = sender.getName();
                if (args.length >= 2) {
                    text = args[1];
                }
                String uuid = Bukkit.getOfflinePlayer(text).getUniqueId().toString();
                sender.sendMessage(Component.text(text + "'s UUID: ").append(Component.text(uuid).color(NamedTextColor.GREEN)).clickEvent(ClickEvent.suggestCommand(uuid)));
            }
            case "template" -> handleTemplateCommand(sender, args);
            default -> {
                sender.sendMessage(getUnknownArgumentMessage(label, args));
            }
        }
    }

    public Component getCopyrightMessage() {
        return toComponent(OpenCreative.getPlugin().getConfig().getString("messages.version", "\n§7 Open§fCreative§b+ §7%version%§f: §f%codename% \n §cMcChicken Studio 2017-2026\n ").replace("%version%", OpenCreative.getVersion()).replace("%codename%", OpenCreative.getCodename()));
    }

    private BukkitRunnable maintenanceRunnable;
    private BukkitRunnable restartRunnable;

    public void handleMaintenanceCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("opencreative.maintenance")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(getLocaleMessage("too-few-args"));
            return;
        }
        if ("start".equalsIgnoreCase(args[1])) {
            if (OpenCreative.getSettings().isMaintenance()) {
                sender.sendMessage(getLocaleMessage("creative.maintenance.already-started"));
                return;
            }
            int seconds = 5;
            if (args.length > 2) {
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (Exception ignored) {
                }
            }
            OpenCreative.getPlugin().getLogger().info("Maintenance mode will be enabled after " + seconds + " seconds by " + sender.getName());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                Sounds.MAINTENANCE_NOTIFY.play(onlinePlayer);
                onlinePlayer.sendMessage(getLocaleMessage("creative.maintenance.starting-notification").replace("%time%", String.valueOf(seconds)));
            }
            int time = seconds;
            if (maintenanceRunnable != null) {
                maintenanceRunnable.cancel();
            }
            maintenanceRunnable = new BukkitRunnable() {
                int seconds = time;

                @Override
                public void run() {
                    if (seconds >= 1) {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.sendActionBar(getLocaleMessage("creative.maintenance.starting-in").replace("%time%", String.valueOf(seconds)));
                        }
                        if (seconds <= 3) {
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                Sounds.MAINTENANCE_COUNT.play(onlinePlayer);
                                onlinePlayer.sendMessage(getLocaleMessage("creative.maintenance.starting-in").replace("%time%", String.valueOf(seconds)));
                            }
                        }
                        seconds--;
                    } else {
                        maintenanceRunnable = null;
                        OpenCreative.getSettings().setMaintenance(true);
                        cancel();
                    }
                }
            };
            maintenanceRunnable.runTaskTimer(OpenCreative.getPlugin(), 0L, 20L);
        } else if ("end".equalsIgnoreCase(args[1])) {
            if (maintenanceRunnable != null) {
                maintenanceRunnable.cancel();
                maintenanceRunnable = null;
            } else if (!OpenCreative.getSettings().isMaintenance()) {
                sender.sendMessage(getLocaleMessage("creative.maintenance.already-ended"));
                return;
            }
            OpenCreative.getSettings().setMaintenance(false);
        } else if ("restart".equalsIgnoreCase(args[1])) {
            if (!OpenCreative.getSettings().isMaintenance()) {
                sender.sendMessage(getLocaleMessage("creative.maintenance.already-ended"));
                return;
            }
            if (restartRunnable != null) {
                restartRunnable.cancel();
            }
            if (args.length > 2) {
                if ("end".equalsIgnoreCase(args[2])) {
                    OpenCreative.getPlugin().getLogger().info("Maintenance mode will be ended after shutdown.");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(getLocaleMessage("creative.maintenance.restarting-end"));
                    }
                    restartRunnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            OpenCreative.getSettings().setMaintenance(false);
                            Bukkit.shutdown();
                        }
                    };
                    restartRunnable.runTaskLater(OpenCreative.getPlugin(), 100L);
                    return;
                }
            }
            OpenCreative.getPlugin().getLogger().info("Maintenance mode will be continued after shutdown.");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(getLocaleMessage("creative.maintenance.restarting"));
            }
            restartRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.shutdown();
                }
            };
            restartRunnable.runTaskLater(OpenCreative.getPlugin(), 100L);
        }
    }

    public void handleTemplateCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("opencreative.template")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        if (!(sender instanceof Player player)) return;
        if (args.length == 1) return;
        File template = new File(OpenCreative.getPlugin().getDataPath() + File.separator + "templates" + File.separator + args[1]);
        if (!template.exists()) {
            sender.sendMessage("Template doesn't exist.");
            return;
        }
        if (!OpenCreative.getStability().isFine()) {
            player.sendMessage(getLocaleMessage("creative.stability.cannot"));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }
        File templateDev = new File(OpenCreative.getPlugin().getDataPath() + File.separator + "templates" + File.separator + args[1] + "dev");
        int id = WorldUtils.generateWorldID();
        File world = new File(Bukkit.getWorldContainer().getPath() + File.separator + "planets" + File.separator + "planet" + id + File.separator);
        File worldDev = new File(Bukkit.getWorldContainer().getPath() + File.separator + "planets" + File.separator + "planet" + id + File.separator + "dev");
        FileUtils.copyFilesToDirectory(template, world);
        if (templateDev.exists()) {
            FileUtils.copyFilesToDirectory(templateDev, worldDev);
        }
        OpenCreative.getPlanetsManager().createPlanet(player, id, new FlatGenerator());
    }

    public void handleGroupsCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("opencreative.groups.edit") && !sender.hasPermission("opencreative.groups.remove") && !sender.hasPermission("opencreative.groups.info")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        if (args.length <= 2) {
            sender.sendMessage(getLocaleMessage("too-few-args"));
            return;
        }
        // /oc groups edit
        String groupName = args[2];
        switch (args[1].toLowerCase()) {
            // /oc groups edit Name limit type 1
            case "edit" -> {
                if (!sender.hasPermission("opencreative.groups.edit")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (args.length <= 5) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                if (args[3].equalsIgnoreCase("limit")) {
                    LimitType type = LimitType.getByPath(args[4]);
                    if (type == null) {
                        sender.sendMessage(getLocaleMessage("creative.groups.wrong-limit").replace("%limit%", args[4]));
                        return;
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(args[5]);
                    } catch (Exception ignored) {
                    }
                    if (OpenCreative.getSettings().getGroups().setLimit(groupName, type, value)) {
                        sender.sendMessage(getLocaleMessage("creative.groups.set-limit").replace("%value%", String.valueOf(value)).replace("%type%", type.getPath()).replace("%group%", groupName));
                    } else {
                        sender.sendMessage(getLocaleMessage("creative.groups.not-found").replace("%group%", groupName));
                    }
                } else if (args[3].equalsIgnoreCase("modifier")) {
                    LimitType type = LimitType.getByPath(args[4]);
                    if (type == null) {
                        sender.sendMessage(getLocaleMessage("creative.groups.wrong-limit").replace("%limit%", args[4]));
                        return;
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(args[5]);
                    } catch (Exception ignored) {
                    }
                    if (OpenCreative.getSettings().getGroups().setLimitModifier(groupName, type, value)) {
                        sender.sendMessage(getLocaleMessage("creative.groups.set-modifier").replace("%value%", String.valueOf(value)).replace("%type%", type.getPath()).replace("%group%", groupName));
                    } else {
                        sender.sendMessage(getLocaleMessage("creative.groups.not-found").replace("%group%", groupName));
                    }
                }

            }
            // /oc groups remove name
            case "remove", "delete" -> {
                if (!sender.hasPermission("opencreative.groups.remove")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                if (OpenCreative.getSettings().getGroups().deleteGroup(groupName)) {
                    sender.sendMessage(getLocaleMessage("creative.groups.removed").replace("%group%", groupName));
                } else {
                    sender.sendMessage(getLocaleMessage("creative.groups.not-found").replace("%group%", groupName));
                }
            }
            // /oc groups info name
            case "info" -> {
                if (!sender.hasPermission("opencreative.groups.info")) {
                    sender.sendMessage(getLocaleMessage("no-perms"));
                    return;
                }
                Group group = OpenCreative.getSettings().getGroups().getGroupOrNull(groupName);
                if (group == null) {
                    sender.sendMessage(getLocaleMessage("creative.groups.not-found").replace("%group%", groupName));
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.groups.info.beginning").replace("%group%", groupName));
                sender.sendMessage(getLocaleMessage("creative.groups.info.cooldowns").replace("%generic%", String.valueOf(group.getGenericCommandCooldown())).replace("%chat%", String.valueOf(group.getChatCooldown())).replace("%creative-chat%", String.valueOf(group.getCreativeChatCooldown())).replace("%advertisement%", String.valueOf(group.getAdvertisementCooldown())).replace("%duplication%", String.valueOf(group.getBlocksDuplicationCooldown())).replace("%modules%", String.valueOf(group.getModuleManipulationCooldown())));
                sender.sendMessage(getLocaleMessage("creative.groups.info.limits.list"));
                for (LimitType type : LimitType.values()) {
                    sender.sendMessage(getLocaleMessage("creative.groups.info.limits.limit").replace("%type%", type.getPath()).replace("%limit%", String.valueOf(group.getLimit(type).limit())).replace("%modifier%", String.valueOf(group.getLimit(type).modifier())));
                }
                sender.sendMessage(getLocaleMessage("creative.groups.info.ending").replace("%worlds%", String.valueOf(group.getWorldsLimit())).replace("%modules%", String.valueOf(group.getModulesLimit())).replace("%size%", String.valueOf(group.getWorldSize())).replace("%like-reward%", String.valueOf(group.getLikeReward())).replace("%prompter%", String.valueOf(group.canUsePrompter())).replace("%group%", groupName).replace("%advertisement-cost%", String.valueOf(group.getAdvertisementPrice())));
            }
        }
    }

    public void handleUpdateCommand(@NotNull CommandSender sender) {
        if (!sender.hasPermission("opencreative.updates.check")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        OpenCreative.getUpdater().checkUpdates().thenAccept(version -> {
            if (version.isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.updates.up-to-date").replace("%version%", OpenCreative.getPlugin().getPluginMeta().getVersion()));
            } else {
                sender.sendMessage(getLocaleMessage("creative.updates.available").replace("%new%", version).replace("%old%", OpenCreative.getPlugin().getPluginMeta().getVersion()));
            }
        }).exceptionally(e -> {
            sender.sendMessage(getLocaleMessage("creative.updates.cant-check"));
            return null;
        });
    }

    public void handleUnloadCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("opencreative.world.unload")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(getLocaleMessage("too-few-args"));
            return;
        }
        if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) {
            if (!sender.hasPermission("opencreative.world.unload.all")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                if (planet.isLoaded()) {
                    planet.getTerritory().unload();
                } else if (planet.getDevPlanet().isLoaded()) {
                    planet.getDevPlanet().unload();
                }
            }
            for (Planet planet : OpenCreative.getPlanetsManager().getCorruptedPlanets()) {
                if (planet.isLoaded()) {
                    planet.getTerritory().unload();
                } else if (planet.getDevPlanet().isLoaded()) {
                    planet.getDevPlanet().unload();
                }
            }
            OpenCreative.getPlugin().getLogger().info("All worlds were unloaded by " + sender.getName());
            return;
        }
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorldName("./planets/planet" + args[1]);
        if (planet == null) {
            sender.sendMessage(getLocaleMessage("no-planet-found"));
            return;
        }
        if (planet.isLoaded()) {
            planet.getTerritory().unload();
            sender.sendMessage(getLocaleMessage("world.unloaded").replace("%id%", args[1]));
        } else if (args[1].contains("dev") && planet.getDevPlanet().isLoaded()) {
            planet.getDevPlanet().unload();
            sender.sendMessage(getLocaleMessage("world.unloaded").replace("%id%", args[1]));
        } else {
            sender.sendMessage(getLocaleMessage("world.already-unloaded").replace("%id%", args[1]));
        }
    }

    public void handleExperimentsCommand(@NotNull CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(getLocaleMessage("too-few-args"));
            return;
        }
        if (List.of("on", "enable").contains(args[1])) {
            if (!sender.hasPermission("opencreative.experiments.enable")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            if (Experiments.getInstance().getExperiments().isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.experiments.list.empty"));
                return;
            }
            if (args.length == 2) {
                sender.sendMessage(getLocaleMessage("too-few-args"));
                return;
            }
            String experimentName = args[2].toLowerCase().replace("-", "_");
            Experiment experiment = Experiments.getInstance().getExperiment(experimentName);
            if (experiment == null) {
                sender.sendMessage(getLocaleMessage("creative.experiments.not-found").replace("%id%", args[2]));
                return;
            }
            if (Experiments.getInstance().setEnabled(experiment, true)) {
                sender.sendMessage(getLocaleMessage("creative.experiments.enabled").replace("%id%", experimentName).replace("%name%", experiment.getName()).replace("%description%", experiment.getDescription()));
            } else {
                sender.sendMessage(getLocaleMessage("creative.experiments.already-enabled").replace("%id%", experimentName).replace("%name%", experiment.getName()).replace("%description%", experiment.getDescription()));
            }
        } else if (List.of("off", "disable").contains(args[1])) {
            if (!sender.hasPermission("opencreative.experiments.disable")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            if (Experiments.getInstance().getExperiments().isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.experiments.list.empty"));
                return;
            }
            if (args.length == 2) {
                sender.sendMessage(getLocaleMessage("too-few-args"));
                return;
            }
            String experimentName = args[2].toLowerCase().replace("-", "_");
            Experiment experiment = Experiments.getInstance().getExperiment(experimentName);
            if (experiment == null) {
                sender.sendMessage(getLocaleMessage("creative.experiments.not-found").replace("%id%", args[2]));
                return;
            }
            if (Experiments.getInstance().setEnabled(experiment, false)) {
                sender.sendMessage(getLocaleMessage("creative.experiments.disabled").replace("%id%", experimentName).replace("%name%", experiment.getName()).replace("%description%", experiment.getDescription()));
            } else {
                sender.sendMessage(getLocaleMessage("creative.experiments.already-disabled").replace("%id%", experimentName).replace("%name%", experiment.getName()).replace("%description%", experiment.getDescription()));
            }
        } else if ("list".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission("opencreative.experiments.list")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            List<Experiment> experiments = Experiments.getInstance().getExperiments();
            if (experiments.isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.experiments.list.empty"));
                return;
            }
            sender.sendMessage(getLocaleMessage("creative.experiments.list.amount").replace("%amount%", String.valueOf(experiments.size())));
            for (Experiment experiment : experiments) {
                sender.sendMessage(getLocaleMessage("creative.experiments.list.element").replace("%id%", experiment.getId()).replace("%name%", experiment.getName()).replace("%description%", experiment.getDescription()).replace("%status%", getLocaleMessage("creative.experiments.list.status." + (experiment.isEnabled() ? "enabled" : "disabled"))));
            }
        } else {
            if (!sender.hasPermission("opencreative.experiments.use")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            if (Experiments.getInstance().getExperiments().isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.experiments.list.empty"));
                return;
            }
            String experimentName = args[1].toLowerCase().replace("-", "_");
            Experiment experiment = Experiments.getInstance().getExperiment(experimentName);
            if (experiment == null || !experiment.isEnabled()) {
                sender.sendMessage(getLocaleMessage("creative.experiments.not-found").replace("%id%", args[1]));
                return;
            }
            if (!sender.hasPermission("opencreative.experiments." + experimentName.replace("_", "-"))) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            experiment.handleCommand(sender, Arrays.copyOfRange(args, 2, args.length));
        }
    }

    public void handleCorruptedCommand(@NotNull CommandSender sender, String[] args) {
        if (args.length < 3) {
            if (!sender.hasPermission("opencreative.list.corrupted")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
            List<Planet> corruptedPlanets = new ArrayList<>(OpenCreative.getPlanetsManager().getCorruptedPlanets());
            sender.sendMessage(getLocaleMessage("creative.corrupted-worlds.list").replace("%amount%", String.valueOf(corruptedPlanets.size())));
            Component worldMessage = getLocaleComponent("creative.corrupted-worlds.world");
            Comparator<Planet> sortById = Comparator.comparingInt(Planet::getId);
            corruptedPlanets.sort(sortById);
            for (Planet planet : corruptedPlanets) {
                sender.sendMessage(new PlaceholderReplacer("id", String.valueOf(planet.getId())).apply(worldMessage).clickEvent(ClickEvent.runCommand("/oc corrupted " + planet.getId() + " join")));
            }
            return;
        }
        if (!sender.hasPermission("opencreative.corrupted.recovery")) {
            sender.sendMessage(getLocaleMessage("no-perms"));
            return;
        }
        int id = -1;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
        }
        if (id < 0) return;
        Planet foundPlanet = null;
        for (Planet planet : OpenCreative.getPlanetsManager().getCorruptedPlanets()) {
            if (planet.getId() == id) {
                foundPlanet = planet;
                break;
            }
        }
        if (foundPlanet == null) {
            sender.sendMessage(getLocaleMessage("no-planet-found"));
            return;
        }
        String action = args[2];
        switch (action.toLowerCase()) {
            case "teleport", "tp", "join", "load" -> {
                if (foundPlanet.getConfiguration().getConfig().getString("generator", "").isEmpty()) {
                    foundPlanet.getTerritory().setGenerator(new EmptyGenerator());
                }
                Planet finalFoundPlanet = foundPlanet;
                foundPlanet.getTerritory().load().whenComplete((result, error) -> {
                    if (sender instanceof Player player) {
                        finalFoundPlanet.connectPlayer(player, true);
                    }
                });

            }
            case "unload" -> foundPlanet.getTerritory().unload();
            case "owner", "setowner" -> {
                if (args.length < 4) {
                    sender.sendMessage(getLocaleMessage("too-few-args"));
                    return;
                }
                sender.sendMessage(getLocaleMessage("creative.corrupted-worlds.set-owner").replace("%player%", args[3]));
                if (foundPlanet.getCreationTime() == 0) {
                    foundPlanet.getConfiguration().set("creation-time", System.currentTimeMillis());
                }
                if (foundPlanet.getLastActivityTime() == 0) {
                    foundPlanet.getConfiguration().set("last-activity-time", System.currentTimeMillis());
                }
                foundPlanet.setOwner(args[3]);
                OpenCreative.getPlanetsManager().unregisterPlanet(foundPlanet);
                Planet planet = new Planet(foundPlanet.getId());
                OpenCreative.getPlanetsManager().registerPlanet(planet);
            }
        }
    }

    public void handleDeprecatedCommand(@NotNull CommandSender sender, String[] args) {
        boolean delete = false;
        int months = 1;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("delete") || arg.equalsIgnoreCase("remove") || arg.equalsIgnoreCase("clean")) {
                delete = true;
            } else {
                try {
                    months = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (months < 1) months = 1;

        if (delete) {
            if (!sender.hasPermission("opencreative.delete.deprecated")
                    && !sender.hasPermission("opencreative.delete")
                    && !sender.hasPermission("opencreative.admin")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
        } else {
            if (!sender.hasPermission("opencreative.list.deprecated")
                    && !sender.hasPermission("opencreative.admin")) {
                sender.sendMessage(getLocaleMessage("no-perms"));
                return;
            }
        }

        long currentTime = System.currentTimeMillis();
        int finalMonths = months;
        boolean finalDelete = delete;
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            List<Planet> deprecatedWorlds = new ArrayList<>();
            long totalSize = 0L;
            for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                long monthsInMillis = 2592000000L * finalMonths;
                if (currentTime - planet.getCreationTime() > monthsInMillis && !OpenCreative.getPlanetsManager().getRecommendedPlanets().contains(planet)) {
                    OfflinePlayer planetOwner = Bukkit.getOfflinePlayer(planet.getOwner());
                    if (!planetOwner.isOnline()) {
                        long lastSeen = Math.max(planetOwner.getLastSeen(), planetOwner.getLastLogin());
                        if (lastSeen == 0 || currentTime - lastSeen > monthsInMillis) {
                            deprecatedWorlds.add(planet);
                            long folderSize = getFolderSize(getPlanetFolder(planet));
                            long devWorldSize = getFolderSize(getDevPlanetFolder(planet.getDevPlanet()));
                            totalSize += folderSize + devWorldSize;
                        }
                    }
                }
            }
            double totalSizeMB = totalSize / (1024.0 * 1024.0);
            if (deprecatedWorlds.isEmpty()) {
                sender.sendMessage(getLocaleMessage("creative.deprecated-worlds.none-found")
                        .replace("%months%", String.valueOf(finalMonths)));
                return;
            }

            if (finalDelete) {
                sender.sendMessage(getLocaleMessage("creative.deprecated-worlds.deleting")
                        .replace("%amount%", String.valueOf(deprecatedWorlds.size())));
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    int deletedCount = 0;
                    for (Planet planet : deprecatedWorlds) {
                        if (OpenCreative.getPlanetsManager().deletePlanet(planet)) {
                            deletedCount++;
                        }
                    }
                    Sounds.WORLD_DELETION.play(sender);
                    OpenCreative.getPlugin().getLogger().info(sender.getName() + " deleted " + deletedCount
                            + " deprecated worlds (" + String.format(Locale.US, "%.2f", totalSizeMB) + " MB freed).");
                    sender.sendMessage(getLocaleMessage("creative.deprecated-worlds.deleted")
                            .replace("%amount%", String.valueOf(deletedCount))
                            .replace("%size%", String.format(Locale.US, "%.2f", totalSizeMB))
                            .replace("%months%", String.valueOf(finalMonths)));
                });
            } else {
                Component worldMessage = getLocaleComponent("creative.deprecated-worlds.world");
                for (Planet planet : deprecatedWorlds) {
                    sender.sendMessage(worldMessage.replaceText(new PlaceholderReplacer(
                            "id", planet.getId(), "owner", planet.getOwnerName(),
                                    "created", getElapsedTime(currentTime, planet.getCreationTime()),
                                    "seen", getElapsedTime(currentTime, Bukkit.getOfflinePlayer(planet.getOwner()).getLastSeen())).get())
                            .clickEvent(ClickEvent.runCommand("/oc delete " + planet.getId()))
                            .hoverEvent(HoverEvent.showText(getLocaleComponent("creative.deprecated-worlds.hover"))));
                }
                sender.sendMessage(getLocaleMessage("creative.deprecated-worlds.list")
                        .replace("%amount%", String.valueOf(deprecatedWorlds.size()))
                        .replace("%size%", String.format(Locale.US, "%.2f", totalSizeMB))
                        .replace("%months%", String.valueOf(finalMonths)));

                if (sender.hasPermission("opencreative.delete.deprecated")
                        || sender.hasPermission("opencreative.delete")
                        || sender.hasPermission("opencreative.admin")) {
                    Component deleteAllMessage = getLocaleComponent("creative.deprecated-worlds.delete-all")
                            .clickEvent(ClickEvent.runCommand("/oc deprecated delete " + finalMonths))
                            .hoverEvent(HoverEvent.showText(getLocaleComponent("creative.deprecated-worlds.delete-all-hover")
                                    .replaceText(builder -> builder.matchLiteral("%amount%").replacement(String.valueOf(deprecatedWorlds.size())))));
                    sender.sendMessage(deleteAllMessage);
                }
            }
        });
    }

    @Override
    public List<String> onTab(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> tabCompleter = new ArrayList<>();
        if (!sender.hasPermission("opencreative.admin")) return null;
        if (args.length == 1) {
            tabCompleter.add("setspawn");
            tabCompleter.add("update");
            tabCompleter.add("moderation");
            tabCompleter.add("checkeveryone");
            tabCompleter.add("reload");
            tabCompleter.add("locale");
            tabCompleter.add("debug");
            tabCompleter.add("maintenance");
            tabCompleter.add("stability");
            tabCompleter.add("load");
            tabCompleter.add("unload");
            tabCompleter.add("resetlocale");
            tabCompleter.add("updatelocale");
            tabCompleter.add("chat");
            tabCompleter.add("creative-chat");
            tabCompleter.add("kick-all");
            tabCompleter.add("list");
            tabCompleter.add("deprecated");
            tabCompleter.add("corrupted");
            tabCompleter.add("sound");
            tabCompleter.add("sounds");
            tabCompleter.add("item");
            tabCompleter.add("items");
            tabCompleter.add("register");
            tabCompleter.add("unregister");
            tabCompleter.add("updateworld");
            tabCompleter.add("delete");
            tabCompleter.add("recommend");
            tabCompleter.add("unrecommend");
            tabCompleter.add("setowner");
            tabCompleter.add("setsize");
            tabCompleter.add("ignoremessage");
            tabCompleter.add("unignoremessage");
            tabCompleter.add("spy");
            tabCompleter.add("experiments");
            tabCompleter.add("updateicons");
            tabCompleter.add("fireworks");
            tabCompleter.add("groups");
            tabCompleter.add("setmessage");
            tabCompleter.add("dev");
            tabCompleter.add("all");
            tabCompleter.add("created");
            tabCompleter.add("fireworks");
            tabCompleter.add("print");
            tabCompleter.add("uuid");
            tabCompleter.add("getuuid");
            tabCompleter.add("template");
            tabCompleter.add("broadcast");
        } else if (args.length == 2) {
            if ("maintenance".equalsIgnoreCase(args[0])) {
                tabCompleter.add("start");
                tabCompleter.add("restart");
                tabCompleter.add("end");
            } else if ("kick-all".equalsIgnoreCase(args[0])) {
                tabCompleter.add("starts");
                tabCompleter.add("ends");
                tabCompleter.add("contains");
                tabCompleter.add("ignore");
            } else if ("creative-chat".equalsIgnoreCase(args[0]) || "chat".equalsIgnoreCase(args[0])) {
                tabCompleter.add("enable");
                tabCompleter.add("disable");
                tabCompleter.add("clear");
            } else if ("items".equalsIgnoreCase(args[0])) {
                tabCompleter.add("get");
                tabCompleter.add("set");
                tabCompleter.add("reset");
            } else if ("debug".equalsIgnoreCase(args[0]) || "spy".equalsIgnoreCase(args[0])) {
                tabCompleter.add("enable");
                tabCompleter.add("disable");
            } else if ("broadcast".equalsIgnoreCase(args[0]) || "bc".equalsIgnoreCase(args[0])) {
                tabCompleter.add("text");
                tabCompleter.add("title");
                tabCompleter.add("actionbar");
            } else if ("groups".equalsIgnoreCase(args[0])) {
                tabCompleter.add("remove");
                tabCompleter.add("edit");
                tabCompleter.add("info");
            } else if (List.of("load", "unload", "moderate", "moderation", "updateworld", "unregister", "delete", "setowner", "setsize", "dev").contains(args[0].toLowerCase())) {
                tabCompleter.addAll(OpenCreative.getPlanetsManager().getPlanets().stream().map(planet -> String.valueOf(planet.getId())).limit(10).toList());
            } else if ("recommend".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(OpenCreative.getPlanetsManager().getPlanets().stream().filter(planet -> !OpenCreative.getPlanetsManager().getRecommendedPlanets().contains(planet)).map(planet -> String.valueOf(planet.getId())).limit(10).toList());
            } else if ("unrecommend".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(OpenCreative.getPlanetsManager().getRecommendedPlanets().stream().map(planet -> String.valueOf(planet.getId())).toList());
            } else if ("corrupted".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(OpenCreative.getPlanetsManager().getCorruptedPlanets().stream().map(planet -> String.valueOf(planet.getId())).toList());
            } else if ("deprecated".equalsIgnoreCase(args[0])) {
                tabCompleter.add("delete");
                tabCompleter.add("1");
                tabCompleter.add("2");
                tabCompleter.add("3");
                tabCompleter.add("6");
                tabCompleter.add("12");
            } else if ("locale".equalsIgnoreCase(args[0])) {
                tabCompleter.add("en");
                tabCompleter.add("ru");
                tabCompleter.add("ua");
            } else if ("sounds".equalsIgnoreCase(args[0])) {
                ConfigurationSection config = OpenCreative.getPlugin().getConfig().getConfigurationSection("sounds");
                if (config == null) return null;
                tabCompleter.addAll(config.getKeys(false));
                tabCompleter.remove("theme");
            } else if ("sound".equalsIgnoreCase(args[0]) || "playsound".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(Arrays.stream(Sounds.values()).map(s -> s.name().toLowerCase()).filter(s -> s.startsWith(args[1].toLowerCase())).toList());
            } else if ("experiments".equalsIgnoreCase(args[0])) {
                List<Experiment> experiments = Experiments.getInstance().getExperiments();
                if (experiments.isEmpty()) return List.of();
                tabCompleter.add("on");
                tabCompleter.add("off");
                tabCompleter.add("list");
                for (Experiment experiment : experiments) {
                    if (!experiment.isEnabled()) continue;
                    tabCompleter.add(experiment.getId());
                }
            } else if (List.of("ignoremessage", "unignoremessage").contains(args[0].toLowerCase())) {
                if (args[1].isEmpty()) {
                    tabCompleter.add("creative.");
                    tabCompleter.add("lobby.");
                    tabCompleter.add("world.");
                    tabCompleter.add("settings.");
                    tabCompleter.add("creating-world.");
                    tabCompleter.add("creative-chat.");
                    tabCompleter.add("advertisement.");
                    tabCompleter.add("commands.");
                    tabCompleter.add("creative.");
                    tabCompleter.add("modules.");
                    tabCompleter.add("environment.");
                    tabCompleter.add("items.");
                }
            } else if ("item".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(Arrays.stream(Items.values()).map(i -> i.name().toLowerCase()).toList());
            } else if ("list".equals(args[0])) {
                return null;
            }
        } else if (args.length == 3) {
            if ("start".equalsIgnoreCase(args[1])) {
                tabCompleter.add("120");
                tabCompleter.add("60");
                tabCompleter.add("30");
                tabCompleter.add("15");
            } else if ("setowner".equalsIgnoreCase(args[0])) {
                return null;
            }
            if ("restart".equalsIgnoreCase(args[1])) {
                tabCompleter.add("end");
            } else if ("setsize".equalsIgnoreCase(args[0])) {
                tabCompleter.add("0");
                tabCompleter.add("25");
                tabCompleter.add("50");
                tabCompleter.add("100");
            } else if ("corrupted".equalsIgnoreCase(args[0])) {
                tabCompleter.add("owner");
                tabCompleter.add("join");
                tabCompleter.add("unload");
            } else if ("deprecated".equalsIgnoreCase(args[0])) {
                if ("delete".equalsIgnoreCase(args[1]) || "clean".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1])) {
                    tabCompleter.add("1");
                    tabCompleter.add("2");
                    tabCompleter.add("3");
                    tabCompleter.add("6");
                    tabCompleter.add("12");
                } else {
                    tabCompleter.add("delete");
                }
            } else if ("experiments".equalsIgnoreCase(args[0])) {
                if (List.of("on", "enable").contains(args[1].toLowerCase())) {
                    for (Experiment experiment : Experiments.getInstance().getExperiments()) {
                        if (experiment.isEnabled()) continue;
                        tabCompleter.add(experiment.getId());
                    }
                } else if (List.of("disable", "off").contains(args[1].toLowerCase())) {
                    for (Experiment experiment : Experiments.getInstance().getExperiments()) {
                        if (!experiment.isEnabled()) continue;
                        tabCompleter.add(experiment.getId());
                    }
                } else {
                    String experimentName = args[1].toLowerCase().replace("-", "_");
                    Experiment experiment = Experiments.getInstance().getExperiment(experimentName);
                    if (experiment == null || !experiment.isEnabled()) {
                        return null;
                    }
                    return experiment.tabCommand(sender, Arrays.copyOfRange(args, 3, args.length));
                }
            } else if ("items".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(Arrays.stream(ItemsGroup.values()).map(g -> g.name().toLowerCase()).toList());
            } else if ("groups".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(OpenCreative.getSettings().getGroups().getNames());
            } else if (args[1].equalsIgnoreCase("title") && ("broadcast".equalsIgnoreCase(args[0]) || "bc".equalsIgnoreCase(args[0]))) {
                tabCompleter.add("20");
            }
        } else if (args.length == 4) {
            if ("items".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"));
            } else if ("groups".equalsIgnoreCase(args[0]) && "edit".equalsIgnoreCase(args[1])) {
                tabCompleter.add("limit");
                tabCompleter.add("modifier");
            } else if ("corrupted".equalsIgnoreCase(args[0])) {
                return null;
            } else if (args[1].equalsIgnoreCase("title") && ("broadcast".equalsIgnoreCase(args[0]) || "bc".equalsIgnoreCase(args[0]))) {
                tabCompleter.add("100");
            }
        } else if (args.length == 5) {
            if ("items".equalsIgnoreCase(args[0])) {
                if (args[1].equalsIgnoreCase("set")) {
                    tabCompleter.addAll(Arrays.stream(Items.values()).map(i -> i.name().toLowerCase()).toList());
                }
            } else if ("groups".equalsIgnoreCase(args[0])) {
                tabCompleter.addAll(Arrays.stream(LimitType.values()).map(LimitType::getPath).toList());
            } else if (args[1].equalsIgnoreCase("title") && ("broadcast".equalsIgnoreCase(args[0]) || "bc".equalsIgnoreCase(args[0]))) {
                tabCompleter.add("20");
            }
        } else if (args.length == 6) {
            if ("groups".equalsIgnoreCase(args[0]) && "edit".equalsIgnoreCase(args[1])) {
                tabCompleter.addAll(List.of("100", "200", "300", "50", "0"));
            } else if (args[1].equalsIgnoreCase("title") && ("broadcast".equalsIgnoreCase(args[0]) || "bc".equalsIgnoreCase(args[0]))) {
                tabCompleter.add("Title\\nSubtitle");
            }
        }
        return tabCompleter;
    }

    private void printMessage(@NotNull CommandSender sender, @NotNull String path) {
        if (MessageUtils.getLocalization().isList(path)) {
            List<String> list = MessageUtils.getLocalization().getStringList(path);
            for (String message : list) {
                sender.sendMessage(toComponent(message).clickEvent(ClickEvent.suggestCommand(message)));
            }
        } else if (MessageUtils.getLocalization().isConfigurationSection(path)) {
            ConfigurationSection section = MessageUtils.getLocalization().getConfigurationSection(path);
            if (section == null) {
                sender.sendMessage("Section: " + path);
                return;
            }
            Set<String> insideKeys = section.getKeys(true);
            sender.sendMessage("Section: " + path + " (" + insideKeys.size() + " inside keys)");
            if (!insideKeys.isEmpty()) {
                sender.sendMessage(substring(String.join("\n", insideKeys), 100));
            }
        } else {
            sender.sendMessage(getLocaleComponent(path).clickEvent(ClickEvent.suggestCommand(MessageUtils.getLocalization().getString(path, "").replace("\n", "\\n"))));
        }
    }
}
