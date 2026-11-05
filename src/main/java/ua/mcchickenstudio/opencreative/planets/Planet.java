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

package ua.mcchickenstudio.opencreative.planets;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.CodingBlockParser;
import ua.mcchickenstudio.opencreative.coding.blocks.events.player.world.JoinEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.events.player.world.QuitEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.events.world.other.GamePlayEvent;
import ua.mcchickenstudio.opencreative.coding.variables.WorldVariables;
import ua.mcchickenstudio.opencreative.commands.experiments.Experiments;
import ua.mcchickenstudio.opencreative.events.planet.PlanetConnectPlayerEvent;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.wanders.Wander;
import ua.mcchickenstudio.opencreative.listeners.player.ChangedWorld;
import ua.mcchickenstudio.opencreative.managers.stability.StabilityState;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.settings.groups.Group;
import ua.mcchickenstudio.opencreative.settings.items.ItemsGroup;
import ua.mcchickenstudio.opencreative.utils.MessageUtils;
import ua.mcchickenstudio.opencreative.utils.hooks.HookUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.*;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.*;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.checkBadContainersInWorld;

/**
 * <h1>Planet</h1>
 * This class represents a Planet, the individual place for players that
 * consists of two worlds: for building and for developing. It has owner,
 * world size, sharing, world mode, limits, flags, players data, variables
 * and states.
 *
 * <p>Planet files are stored in ./planets/planetID folder.</p>
 *
 * @author McChicken Studio
 * @version 6.0
 * @since 1.0
 */
public class Planet {

    private final int id;
    private final PlanetInfo info;
    private final DevPlanet devPlanet;
    private final PlanetConfig config;
    private final PlanetLimits limits;
    private final PlanetTerritory territory;
    private final PlanetPlayers worldPlayers;
    private final WorldVariables variables;
    private final PlanetExperiments experiments;
    private UUID ownerUUID;
    private String ownerName;
    private String ownerGroup;
    private long creationTime;
    private long lastActivityTime;

    private Mode mode;
    private Sharing sharing;

    private boolean debug;
    private boolean corrupted;
    private boolean changingOwner;

    /**
     * Loads a planet with world name.
     **/
    public Planet(int id) {

        this.id = id;
        config = new PlanetConfig(this);
        devPlanet = new DevPlanet(this);
        info = new PlanetInfo(this);

        loadInfo();

        worldPlayers = new PlanetPlayers(this);
        limits = new PlanetLimits(this);
        territory = new PlanetTerritory(this);

        variables = new WorldVariables(this);
        experiments = new PlanetExperiments(this);

        OpenCreative.getPlanetsManager().registerPlanet(this);

        info.updateIconAsync();
    }

    /**
     * Returns information of planet, that stores
     * display name, description, custom ID and icon.
     *
     * @return planet's info.
     */
    public @NotNull PlanetInfo getInformation() {
        return info;
    }

    /**
     * Returns players registry of planet, that stores
     * builders, developers, whitelisted and banned players.
     *
     * @return planet's registry of players.
     */
    public @NotNull PlanetPlayers getWorldPlayers() {
        return worldPlayers;
    }

    /**
     * Returns configuration, that stores settings of planet.
     *
     * @return planet's config.
     */
    public @NotNull PlanetConfig getConfiguration() {
        return config;
    }

    /**
     * Checks whether world's coding is in debug mode.
     *
     * @return true - in debug mode, false - not.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Sets debug mode for coding in world.
     *
     * @param debug true - enabled, false - disabled.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Returns holder of global and saved variables in planet.
     *
     * @return planet's variables.
     */
    public @NotNull WorldVariables getVariables() {
        return variables;
    }

    /**
     * Returns whether world is corrupted (doesn't have
     * owner information) or not.
     *
     * @return true - corrupted, false - not.
     */
    public boolean isCorrupted() {
        return corrupted;
    }

    /**
     * Returns holder of planet's limits and modifiers.
     *
     * @return planet's limits.
     */
    public @NotNull PlanetLimits getLimits() {
        return limits;
    }

    /**
     * Returns holder of planet's experiments.
     *
     * @return planet's experiments.
     */
    public @NotNull PlanetExperiments getExperiments() {
        return experiments;
    }

    /**
     * Return group of planet's owner.
     * <p>
     * Useful to get limits and modifiers.
     *
     * @return group of owner.
     */
    public @NotNull Group getGroup() {
        return OpenCreative.getSettings().getGroups().getGroup(ownerGroup);
    }

    /**
     * Returns sharing mode of planet.
     * <p>Public - all players can connect to the world.</p>
     * <p>Private - only world owner can connect to the world.</p>
     * <p>Closed - no one can connect to the world, deleting mode.</p>
     *
     * @return sharing mode.
     */
    public @NotNull Sharing getSharing() {
        return sharing;
    }

    /**
     * Sharing is ability for players to connect to the world.
     * <p>Public - all players can connect to the world.</p>
     * <p>Private - only world owner can connect to the world.</p>
     * <p>Closed - no one can connect to the world, deleting mode.</p>
     *
     * @param sharing sharing mode.
     */
    public void setSharing(@NotNull Sharing sharing) {
        if (this.sharing == sharing) return;
        this.sharing = sharing;
        config.set("sharing", sharing.name());
    }

    /**
     * Returns world's name on server in the format:
     * {@code ./planets/planetID}
     * <p>
     * Can be used for getting world from server.
     *
     * @return world's name on server.
     */
    public @NotNull String getWorldName() {
        return "./planets/planet" + id;
    }

    /**
     * Returns numeric ID of world.
     *
     * @return numeric ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns developer's planet of world, that has
     * coding platforms to create a code.
     *
     * @return developer's planet.
     */
    public @NotNull DevPlanet getDevPlanet() {
        return devPlanet;
    }

    /**
     * Checks whether world is in a state of changing owner.
     *
     * @return true - owner requested to transfer world, false - not.
     */
    public boolean isChangingOwner() {
        return changingOwner;
    }

    /**
     * Sets state of changing world's owner.
     *
     * @param changingOwner true - owner requested to transfer world, false - not.
     */
    public void setChangingOwner(boolean changingOwner) {
        this.changingOwner = changingOwner;
    }

    /**
     * Checks whether the player is owner of planet.
     *
     * @param player player to check.
     * @return true - is owner, false - not owner.
     */
    public boolean isOwner(@NotNull Player player) {
        return ownerUUID.equals(player.getUniqueId());
    }

    /**
     * Checks whether the player is owner of planet.
     * <p>
     * Ignores caps consideration.
     *
     * @param nickname name of player.
     * @return true - is owner, false - not owner.
     */
    public boolean isOwner(@NotNull String nickname) {
        return ownerUUID.equals(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    /**
     * Checks whether the player is owner of planet.
     *
     * @param uuid uuid to check.
     * @return true - is owner, false - not owner.
     */
    public boolean isOwner(@NotNull UUID uuid) {
        return ownerUUID.equals(uuid);
    }

    /**
     * Returns territory of planet, that contains planet's world,
     * environment, flags, boss bars, scoreboards, code script
     * and other temporary values.
     *
     * @return planet's territory.
     */
    public PlanetTerritory getTerritory() {
        return territory;
    }

    /**
     * Checks if loaded main world of planet.
     *
     * @return true - if loaded, false - unloaded.
     */
    public boolean isLoaded() {
        return getWorld() != null;
    }

    /**
     * Returns world if planet is loaded, null - planet is unloaded.
     *
     * @return world, or null.
     */
    public World getWorld() {
        if (territory == null) return null;
        UUID uuid = territory.getWorldUUID();
        if (uuid == null) return null;
        return Bukkit.getWorld(uuid);
    }

    /**
     * Returns current mode of planet.
     *
     * @return playing or build mode.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Changes planet's mode to Play or Build.
     * <p>In the Build mode players cannot get damaged, they only can look at builders which are creating a map.</p>
     * <p>In the Play mode code script will work, player damaging is enabled.</p>
     *
     * @param mode Mode to set.
     */
    public void setMode(@NotNull Mode mode) {
        setMode(mode, false);
    }

    /**
     * Returns total players count in world.
     * <p>
     * Includes all players from build world and developer's world.
     *
     * @return online of planet.
     */
    public int getOnline() {
        return this.getPlayers().size();
    }

    /**
     * Returns creation time of world.
     * <p>
     * If value is unknown (0), will return {@code 1670573410000L}
     * (publication of OpenCreative+).
     *
     * @return unix time, when world was created.
     */
    public long getCreationTime() {
        if (creationTime == 0) {
            return 1670573410000L;
        }
        return creationTime;
    }

    /**
     * Sets creation unix time of world.
     *
     * @param creationTime time, when world was created.
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
        config.set("sharing", String.valueOf(creationTime)); // string, because of older versions
        info.updateIconAsync();
    }

    /**
     * Returns last activity time of world.
     * <p>
     * If value is unknown (0), will return {@code 1670573410000L}
     * (publication of OpenCreative+).
     *
     * @return unix last time, when someone joined the world.
     */
    public long getLastActivityTime() {
        if (lastActivityTime == 0) {
            return 1670573410000L;
        }
        return lastActivityTime;
    }

    /**
     * Sets last activity unix time in world.
     *
     * @param activityTime last activity time.
     */
    public void setLastActivityTime(long activityTime) {
        this.lastActivityTime = activityTime;
        config.set("last-activity-time", activityTime); // string, because of older versions
    }

    /**
     * Returns list of all players in world.
     * <p>
     * Includes all players from build world and developer's world.
     *
     * @return list of all online players in world.
     */
    public List<Player> getPlayers() {
        List<Player> playerList = new ArrayList<>();
        if (!isLoaded()) return playerList;
        playerList.addAll(getWorld().getPlayers());
        if (getDevPlanet().isLoaded()) {
            playerList.addAll(getDevPlanet().getWorld().getPlayers());
        }
        return playerList;
    }

    /**
     * Returns audience of world, that contains
     * players in world and players from developer's world.
     *
     * @return audience of world.
     */
    public @NotNull Audience getAudience() {
        Audience audience = Audience.empty();
        if (isLoaded()) {
            audience = Audience.audience(getWorld());
            if (devPlanet.isLoaded()) {
                audience = Audience.audience(getWorld(), devPlanet.getWorld());
            }
        }
        return audience;
    }

    /**
     * Returns planet owner's UUID.
     *
     * @return owner's name.
     */
    public UUID getOwner() {
        return ownerUUID;
    }

    /**
     * Returns planet owner's nickname.
     *
     * @return owner's name.
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Sets new owner of world.
     *
     * @param owner owner to set.
     */
    public void setOwner(String owner) {
        this.ownerUUID = Bukkit.getOfflinePlayer(owner).getUniqueId();
        config.set("owner", owner);
        config.set("owner-uuid", Bukkit.getOfflinePlayer(owner).getUniqueId().toString());
        info.updateIconAsync();
    }

    /**
     * Returns owner's group of planet.
     *
     * @return owner's group.
     */
    public String getOwnerGroup() {
        return ownerGroup;
    }

    /**
     * Changes planet's mode to Play or Build.
     * <p>In the Build mode players cannot get damaged, they only can look at builders which are creating a map.</p>
     * <p>In the Play mode code script will work, player damaging is enabled.</p>
     *
     * @param mode         Mode to set.
     * @param ignoreEvents Don't call world start, player join or player quit events on mode change.
     */
    public void setMode(@NotNull Mode mode, boolean ignoreEvents) {
        if (this.mode == mode) return;
        config.set("mode", mode.name());
        if (!isLoaded()) {
            this.mode = mode;
            return;
        }
        if (mode == Mode.PLAYING && !OpenCreative.getSettings().getCodingSettings().isEnabled() && !ignoreEvents) {
            mode = Mode.BUILD;
        }
        if (ignoreEvents) this.mode = mode;
        try {
            territory.getSpawnLocation().getChunk().load(true);
            if (mode == Mode.BUILD) {
                for (Player player : getPlayers()) {
                    if (!isEntityInDevPlanet(player)) {
                        if (!ignoreEvents) new QuitEvent(player).callEvent();
                        player.showTitle(Title.title(
                                toComponent(getLocaleMessage("world.build-mode.title")), toComponent(getLocaleMessage("world.build-mode.subtitle")),
                                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(130))
                        ));
                        clearPlayer(player);
                        player.teleport(territory.getSpawnLocation());
                        Sounds.WORLD_MODE_BUILD.play(player);
                        territory.setWorldSize(territory.getWorldSize(), false);
                        territory.showBorders(player);
                        if (worldPlayers.canBuild(player)) {
                            if (isOwner(player)) {
                                ItemsGroup.BUILD_OWNER.setItems(player);
                            } else {
                                ItemsGroup.BUILD.setItems(player);
                            }
                            player.setGameMode(GameMode.CREATIVE);
                            giveBuildPermissions(player);
                            player.sendMessage(getLocaleMessage("world.build-mode.message.owner"));
                            if (!territory.isAutoSave()) {
                                player.sendMessage(getLocaleMessage("settings.autosave.warning"));
                            }
                        } else {
                            player.sendMessage(getLocaleMessage("world.build-mode.message.players"));
                        }
                    } else {
                        player.sendMessage(getLocaleMessage("world.build-mode.message.players"));
                    }
                }
                territory.stopBukkitRunnables();
                HookUtils.clearEntitiesHook(territory.getWorld());
            } else {
                this.mode = mode;
                territory.stopBukkitRunnables();
                HookUtils.clearEntitiesHook(territory.getWorld());
                for (Player player : getPlayers()) {
                    if (!isEntityInDevPlanet(player)) {
                        clearPlayer(player);
                        player.clearTitle();
                        player.teleport(territory.getSpawnLocation());
                        territory.showBorders(player);
                        if (worldPlayers.canDevelop(player)) {
                            player.sendMessage(getLocaleMessage("world.play-mode.message.owner"));
                            givePlayPermissions(player);
                        } else {
                            player.sendMessage(getLocaleMessage("world.play-mode.message.players"));
                        }
                        if (isOwner(player)) {
                            ItemsGroup.PLAY_OWNER.setItems(player);
                        }
                    } else {
                        player.sendMessage(getLocaleMessage("world.play-mode.message.owner"));
                    }
                }
                if (devPlanet.isLoaded() && devPlanet.isCodeChanged()) {
                    CompletableFuture<Boolean> parseFuture = new CodingBlockParser(devPlanet).parseCode(devPlanet);
                    parseFuture.thenAccept(success -> {
                        if (success && !ignoreEvents) {
                            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                                new GamePlayEvent(this).callEvent();
                                for (Player player : getPlayers()) {
                                    if (OpenCreative.getPlanetsManager().getDevPlanet(player) == null) {
                                        new JoinEvent(player).callEvent();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    territory.getScript().loadCode().thenAccept((success) -> {
                        if (success && !ignoreEvents) {
                            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                                new GamePlayEvent(this).callEvent();
                                for (Player player : getPlayers()) {
                                    if (OpenCreative.getPlanetsManager().getDevPlanet(player) == null) {
                                        new JoinEvent(player).callEvent();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        } catch (Exception error) {
            sendPlanetErrorMessage(this, "Failed to change mode to " + mode.name());
            ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").error("Мир " + id + ": не удалось сменить режим на " + mode.name(), error); // OpenCreative++: cause was hidden
        }
        this.mode = mode;
    }

    /**
     * Loads information of planet: owner, owner's group, mode,
     * sharing, corrupted state, creation time, last activity time.
     */
    public void loadInfo() {
        FileConfiguration config = updatePlanetConfig(this, getPlanetConfig(this));

        String ownerName = "Unknown owner";
        UUID ownerUUID = new UUID(0L, 0L);
        String ownerGroup = "default";
        Mode mode = Mode.BUILD;
        Sharing sharing = Sharing.PRIVATE;
        try {
            ownerUUID = UUID.fromString(config.getString("owner-uuid", ""));
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUUID);
            if (offlinePlayer.hasPlayedBefore()) {
                ownerName = offlinePlayer.getName();
                if (ownerName == null) {
                    ownerName = "Unknown owner";
                }
            } else {
                ownerName = config.getString("owner", "");
                if (ownerName.isEmpty()) {
                    corrupted = true;
                }
            }
        } catch (Exception ignored) {
            if (!config.contains("owner")) {
                corrupted = true;
            } else {
                ownerName = config.getString("owner", "Unknown owner");
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerName);
                if (offlinePlayer.hasPlayedBefore()) {
                    ownerUUID = offlinePlayer.getUniqueId();
                    setPlanetConfigParameter(this, "owner-uuid", ownerUUID.toString());
                } else {
                    corrupted = true;
                }
            }
        }

        if (config.getString("owner-group") != null) {
            ownerGroup = config.getString("owner-group");
        }
        if (config.getString("mode") != null && OpenCreative.getSettings().getCodingSettings().isEnabled()) {
            try {
                mode = Mode.valueOf(config.getString("mode"));
            } catch (Exception ignored) {
            }
        }
        if (config.getString("sharing") != null) {
            try {
                sharing = Sharing.valueOf(config.getString("sharing"));
            } catch (Exception ignored) {
            }
        }
        if (config.get("creation-time") != null) {
            try {
                creationTime = Long.parseLong(String.valueOf(config.get("creation-time")));
            } catch (Exception error) {
                creationTime = 1670573410000L;
            }
        }
        if (config.get("last-activity-time") != null) {
            try {
                lastActivityTime = Long.parseLong(String.valueOf(config.get("last-activity-time")));
            } catch (Exception error) {
                lastActivityTime = 1670573410000L;
            }
        }
        if (corrupted) {
            sendCriticalErrorMessage("Planet " + id + " lost it's config file, please check planet files in "
                    + (FileUtils.usingNewWorldsContainer() ? "./world/dimensions/minecraft/planets/planet" + id : getWorldName()));
        }
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.ownerGroup = ownerGroup;
        this.mode = mode;
        this.sharing = sharing;
    }


    /**
     * Returns byte value of flag.
     *
     * @param flag flag to get value.
     * @return value of flag.
     */
    public byte getFlagValue(@NotNull PlanetFlags.PlanetFlag flag) {
        return territory.getFlags().getFlagValue(flag);
    }

    /**
     * Sets byte value of flag.
     *
     * @param flag  flag to set value.
     * @param value new value.
     */
    public void setFlagValue(@NotNull PlanetFlags.PlanetFlag flag, byte value) {
        territory.getFlags().setFlag(flag, value);
    }

    /**
     * Connects player to the planet.
     * <p>
     * May not connect player to the planet if some conditions are not met.
     * <p>
     * For example: stability is not good, world is private, player is banned in world,
     * error while loading spawn location.
     *
     * @param player player to connect.
     */
    public void connectPlayer(@NotNull Player player) {
        connectPlayer(player, isVanished(player));
    }

    /**
     * Connects player to the planet.
     * <p>
     * May not connect player to the planet if some conditions are not met.
     * <p>
     * For example: stability is not good, world is private, player is banned in world,
     * error while loading spawn location.
     *
     * @param player     player to connect.
     * @param hidePlayer hide player's join message, and make him in spectator mode or not.
     */
    public void connectPlayer(@NotNull Player player, boolean hidePlayer) {
        // If stability is not good, not connecting
        if (OpenCreative.getStability().getState() != StabilityState.FINE && !isLoaded()) {
            player.sendMessage(getLocaleMessage("creative.stability.cannot"));
            Sounds.PLAYER_FAIL.play(player);
            return;
        }

        if (territory.isBusy()) {
            player.sendMessage(getLocaleMessage("world.connecting.unloading"));
            return;
        }
        Wander wander = OpenCreative.getWander(player);
        if (wander.isConnectingToPlanet()) {
            player.sendMessage(getLocaleMessage("world.connecting.busy"));
            return;
        }

        // If world is private/player is banned, not connecting
        if (!isOwner(player.getUniqueId())) {
            boolean hasPrivateBypass = player.hasPermission("opencreative.world.private.bypass");
            boolean hasBanBypass = player.hasPermission("opencreative.world.banned.bypass");
            Sharing sharing = getSharing();
            if (isLoaded()) {
                if (sharing != Sharing.PUBLIC && !hasPrivateBypass && !worldPlayers.isWhitelisted(player.getUniqueId())) {
                    player.sendMessage(MessageUtils.getPlayerLocaleMessage("private-planet", player));
                    return;
                }
                if (worldPlayers.isBanned(player.getUniqueId()) && !hasBanBypass) {
                    player.sendMessage(MessageUtils.getPlayerLocaleMessage("blacklisted-in-planet", player));
                    return;
                }
                handlePreTeleportationProcess(player, wander, hidePlayer);
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
                    boolean isWhitelisted = worldPlayers.isWhitelisted(player.getUniqueId());
                    boolean isBanned = worldPlayers.isBanned(player.getUniqueId());
                    if (sharing != Sharing.PUBLIC && !hasPrivateBypass && !isWhitelisted) {
                        player.sendMessage(MessageUtils.getPlayerLocaleMessage("private-planet", player));
                        return;
                    }
                    if (isBanned && !hasBanBypass) {
                        player.sendMessage(MessageUtils.getPlayerLocaleMessage("blacklisted-in-planet", player));
                        return;
                    }
                    Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                        handlePreTeleportationProcess(player, wander, hidePlayer);
                    });
                });
            }
        } else {
            handlePreTeleportationProcess(player, wander, hidePlayer);
        }
    }

    /**
     * Handles process of player pre-teleportation to planet.
     *
     * @param player  player to connect.
     * @param wander  player as wander.
     * @param hidePlayer whether player join message should be hidden.
     */
    private void handlePreTeleportationProcess(@NotNull Player player, @NotNull Wander wander, boolean hidePlayer) {
        // OpenCreative++: check "busy" before marking the player as connecting. OpenCreative+ returned after setting
        // the flag and a 30 s title, so the player was stuck "connecting" (every next join said busy) until relog.
        if (territory.isBusy()) {
            player.sendMessage(getLocaleMessage("world.connecting.unloading"));
            return;
        }
        new QuitEvent(player).callEvent();
        wander.setConnectingToPlanet(true);
        player.showTitle(Title.title(
                getLocaleComponent("world.connecting.title"), getLocaleComponent("world.connecting.subtitle"),
                Title.Times.times(Duration.ofMillis(710), Duration.ofSeconds(30), Duration.ofMillis(130))
        ));
        Sounds.WORLD_CONNECTION.play(player);

        if (territory.isBusy()) {
            wander.setConnectingToPlanet(false); // OpenCreative++: don't leave the player stuck "connecting"
            player.clearTitle();
            player.sendMessage(getLocaleMessage("world.connecting.unloading"));
            return;
        }

        boolean wasLoaded = isLoaded();
        if (!isLoaded()) {
            ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Loading planet " + id + " and teleporting " + player.getName());
            if (!OpenCreative.getSettings().getCodingSettings().isEnabled() && mode == Mode.PLAYING) mode = Mode.BUILD;
            territory.load().thenAccept(ignored -> {
                removePassengers(player);
                player.teleportAsync(territory.getSpawnLocation()).thenAccept(success -> {
                    handleTeleportationProcess(player, wasLoaded, hidePlayer, success);
                }).exceptionally(error -> {
                    player.clearTitle();
                    wander.setConnectingToPlanet(false);
                    String errorMessage = (error.getMessage() == null ? "Unknown error" : error.getMessage());
                    player.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                            player, "id", id, "wasloaded", wasLoaded,
                            "phase", "Teleportation", "loaded", isLoaded(), "error", errorMessage)
                            .clickEvent(ClickEvent.suggestCommand(errorMessage))
                            .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                    sendCriticalErrorMessage("Failed to connect the player to planet " + id
                            + " (was loaded: " + wasLoaded + ", loaded: " + isLoaded() + ", phase: Teleportation)", error);
                    Sounds.PLAYER_ERROR.play(player);
                    return null;
                });
            }).exceptionally(error -> {
                player.clearTitle();
                wander.setConnectingToPlanet(false);
                String errorMessage = (error.getMessage() == null ? "Unknown error" : error.getMessage());
                player.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                        player, "id", id, "wasloaded", wasLoaded,
                        "phase", "Loading World", "loaded", isLoaded(), "error", errorMessage)
                        .clickEvent(ClickEvent.suggestCommand(errorMessage))
                        .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                sendCriticalErrorMessage("Failed to connect the player to planet " + id
                        + " (was loaded: " + wasLoaded + ", loaded: " + isLoaded() + ", phase: Loading World)", error);
                Sounds.PLAYER_ERROR.play(player);
                return null;
            });
        } else {
            ua.mcchickenstudio.opencreative.plus.PlusLog.module("Миры").debug("Planet " + id + " is already loaded, teleporting " + player.getName());
            removePassengers(player);
            player.teleportAsync(territory.getSpawnLocation()).thenAccept(success -> {
                handleTeleportationProcess(player, wasLoaded, hidePlayer, success);
            }).exceptionally(error -> {
                player.clearTitle();
                wander.setConnectingToPlanet(false);
                String errorMessage = (error.getMessage() == null ? "Unknown error" : error.getMessage());
                player.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                        player, "id", id, "wasloaded", wasLoaded,
                        "phase", "Teleportation", "loaded", isLoaded(), "error", errorMessage)
                        .clickEvent(ClickEvent.suggestCommand(errorMessage))
                        .hoverEvent(HoverEvent.showText(Component.text(parseException(error, true)))));
                sendCriticalErrorMessage("Failed to connect the player to planet " + id
                        + " (was loaded: " + wasLoaded + ", loaded: " + isLoaded() + ", phase: Teleportation)", error);
                Sounds.PLAYER_ERROR.play(player);
                return null;
            });
        }
    }

    /**
     * Handles process of player teleportation to planet.
     *
     * @param player     player to connect.
     * @param wasLoaded  whether world was loaded before.
     * @param hidePlayer whether player join message should be hidden.
     * @param success    chunks are loaded successfully to teleport or not.
     */
    private void handleTeleportationProcess(@NotNull Player player, boolean wasLoaded, boolean hidePlayer, boolean success) {
        clearPlayer(player, false, OpenCreative.getSettings().getLobbySettings().shouldResetGameMode(player.getWorld()));
        if (success) {
            OpenCreative.getWander(player).setConnectingToPlanet(false);
            if (!hidePlayer && getFlagValue(PlanetFlags.PlanetFlag.JOIN_MESSAGES) == 1) {
                for (Player onlinePlayer : getPlayers()) {
                    onlinePlayer.sendMessage(MessageUtils.getPlayerLocaleMessage("world.joined", player));
                }
            }
            clearPlayer(player, false, OpenCreative.getSettings().getLobbySettings().shouldResetGameMode(player.getWorld()));
            Sounds.WORLD_CONNECTED.play(player);
            if (!wasLoaded) {
                Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                    checkBadContainersInWorld(getWorld(), 300_000L);
                });
            }
            mode.onPlayerConnect(player, this);
            PlanetPlayer planetPlayer = getWorldPlayers().getPlanetPlayer(player);
            player.clearTitle();
            territory.showBorders(player);
            if (!worldPlayers.getUniquePlayers().contains(player.getUniqueId())) {
                if (Experiments.isEnabled("wanders") && !isOwner(player)) {
                    Wander wander = OpenCreative.getWander(player);
                    wander.setVisits(wander.getVisits() + 1);
                }
                /*
                 * When player joins connects to the world for first time.
                 */
                worldPlayers.addUnique(player.getUniqueId());
                info.setUniques(info.getUniques() + 1);
                if (this.isOwner(player)) {
                    /*
                     * When world's owner connects to the world for first time
                     * (after world's creation).
                     */
                    player.showTitle(Title.title(
                            toComponent(MessageUtils.getPlayerLocaleMessage("creating-world.welcome-title", player)), toComponent(MessageUtils.getPlayerLocaleMessage("creating-world.welcome-subtitle", player)),
                            Title.Times.times(Duration.ofMillis(750), Duration.ofSeconds(9), Duration.ofSeconds(2))
                    ));
                    player.sendMessage(getLocaleMessage("creating-world.welcome"));
                    Sounds.WELCOME_TO_NEW_WORLD.play(player);
                    player.setGameMode(GameMode.CREATIVE);
                    ItemsGroup.BUILD_OWNER.setItems(player);
                }
            } else {
                if (isOwner(player) && getFlagValue(PlanetFlags.PlanetFlag.JOIN_MESSAGES) == 1) {
                    player.sendMessage(MessageUtils.getPlayerLocaleMessage("world.connecting.owner-help", player));
                }
            }
            if (this.isOwner(player)) {
                String newOwnerGroup = OpenCreative.getSettings().getGroups().getGroup(player).getName().toLowerCase();
                if (!newOwnerGroup.equals(ownerGroup)) {
                    // OpenCreative++: owner's new rank is saved and applied to the world border
                    // (OpenCreative+ kept the size from world creation forever).
                    ownerGroup = newOwnerGroup;
                    getConfiguration().set("owner-group", ownerGroup);
                    if (!getConfiguration().getConfig().contains("size")) {
                        territory.setWorldSize(getGroup().getWorldSize() + ua.mcchickenstudio.opencreative.plus.shop.PlusLimits.bonusSize(this), false);
                    }
                }
                if (mode == Mode.BUILD) {
                    ItemsGroup.BUILD_OWNER.setItems(player);
                } else if (mode == Mode.PLAYING) {
                    ItemsGroup.PLAY_OWNER.setItems(player);
                }
            } else if (worldPlayers.canBuild(player) && mode == Mode.BUILD) {
                ItemsGroup.BUILD.setItems(player);
            }
            if (!territory.isAutoSave() && worldPlayers.canBuild(player)) {
                player.sendMessage(getLocaleMessage("settings.autosave.warning"));
            }
            if (hidePlayer) {
                player.setGameMode(GameMode.SPECTATOR);
                ChangedWorld.addPlayerWithLocation(player);
                for (Player onlinePlayer : getPlayers()) {
                    onlinePlayer.hidePlayer(OpenCreative.getPlugin(), player);
                }
            }
            CompletableFuture<?> playerDataLoad = (planetPlayer != null) ? planetPlayer.load() : CompletableFuture.completedFuture(null);
            playerDataLoad.whenComplete((ignored, error) -> {
                if (!wasLoaded) {
                    variables.load().whenComplete((ignoredVariables, errorVariables) -> {
                        territory.getScript().loadCode().thenAccept(result -> {
                            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                                new GamePlayEvent(this).callEvent();
                                new JoinEvent(player).callEvent();
                            });
                        });
                    });

                } else if (!hidePlayer) {
                    Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                        new JoinEvent(player).callEvent();
                    });
                }
            });
            new PlanetConnectPlayerEvent(this, player).callEvent();
            info.updateIconAsync();
        } else {
            if (!player.isOnline() && isLoaded() && getPlayers().isEmpty()) {
                territory.unload();
                return;
            }
            if (isLoaded()) {
                sendPlayerErrorMessage(player,
                        "Failed to teleport to the world! " );
            } else {
                sendPlayerErrorMessage(player, "Failed to teleport to the world! World is unloaded.");
            }
            player.clearTitle();
            OpenCreative.getWander(player).setConnectingToPlanet(false);
            String errorMessage = "World is loaded, chunk is " + (territory.getSpawnLocation().getChunk().isLoaded() ? "loaded." : "unloaded.");
            player.sendMessage(getComponentWithPlaceholders("world.connecting.error",
                    player, "id", id, "wasloaded", wasLoaded,
                    "phase", "After Teleportation", "loaded", isLoaded(), "error", errorMessage)
                    .clickEvent(ClickEvent.suggestCommand(errorMessage)));
            sendCriticalErrorMessage("Failed to connect the player to planet " + id
                    + " (was loaded: " + wasLoaded + ", loaded: " + isLoaded() + ", phase: After Teleportation) " + errorMessage);
            Sounds.PLAYER_ERROR.play(player);
        }
    }

    public enum Mode {
        PLAYING() {
            public void onPlayerConnect(Player player, Planet planet) {
                if (planet.isOwner(player)) {
                    ItemsGroup.PLAY_OWNER.setItems(player);
                }
                player.setGameMode(GameMode.ADVENTURE);
            }
        }, BUILD() {
            public void onPlayerConnect(Player player, Planet planet) {
                // Removes build permissions for admins on world join (to prevent accidental griefs)
                if (planet.getWorldPlayers().canBuild(player) && (!player.hasPermission("opencreative.world.build.others") || planet.isOwner(player))) {
                    player.setGameMode(GameMode.CREATIVE);
                }
            }
        };

        public String getName() {
            return getLocaleMessage("world." + (this == PLAYING ? "play-mode" : "build-mode") + ".name", false);
        }

        public void onPlayerConnect(Player player, Planet planet) {
        }
    }

    public enum Sharing {
        PUBLIC, PRIVATE, CLOSED;

        public String getName() {
            return getLocaleMessage("world.sharing." + (this == PUBLIC ? "public" : "private"), false);
        }
    }

    public enum PlayersType {

        UNIQUE("players.unique"),
        LIKED("players.liked"),
        DISLIKED("players.disliked"),
        WHITELISTED("players.whitelist"),
        BLACKLISTED("players.blacklist"),
        BUILDERS_TRUSTED("players.builders.trusted"),
        BUILDERS_NOT_TRUSTED("players.builders.not-trusted"),
        DEVELOPERS_TRUSTED("players.developers.trusted"),
        DEVELOPERS_NOT_TRUSTED("players.developers.not-trusted"),
        DEVELOPERS_GUESTS("players.developers.guests");

        private final String path;

        PlayersType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}
