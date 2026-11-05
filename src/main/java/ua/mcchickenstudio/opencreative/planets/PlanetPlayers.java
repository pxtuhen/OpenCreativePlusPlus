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

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.events.player.world.QuitEvent;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.*;
import java.util.stream.Collectors;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.*;

/**
 * <h1>PlanetPlayers</h1>
 * This class represents a planet players with
 * data and statuses, like building or development
 * permissions.
 */
public class PlanetPlayers {

    private final Planet planet;

    private final Set<PlanetPlayer> planetPlayers = new HashSet<>();

    private final Set<UUID> buildersTrusted = new HashSet<>();
    private final Set<UUID> buildersNotTrusted = new HashSet<>();

    private final Set<UUID> developersTrusted = new HashSet<>();
    private final Set<UUID> developersNotTrusted = new HashSet<>();
    private final Set<UUID> developersGuests = new HashSet<>();

    private final Set<UUID> bannedPlayers = new HashSet<>();
    private final Set<UUID> whitelistedPlayers = new HashSet<>();

    public PlanetPlayers(Planet planet) {
        this.planet = planet;
    }

    /**
     * Registers player to planet.
     *
     * @param player online player to register.
     */
    public void registerPlayer(@NotNull Player player) {
        planetPlayers.add(new PlanetPlayer(planet, player));
    }

    public void unregisterPlayer(Player player) {
        planetPlayers.removeIf(planetPlayer -> planetPlayer.getPlayer().equals(player));
        planet.getDevPlanet().getLastLocations().remove(player.getUniqueId());
        planet.getDevPlanet().clearMarkedExecutors(player);
        planet.getDevPlanet().clearSavedInventory(player);
        planet.getLimits().clearPlayerLimits(player);
        planet.getTerritory().getRecipes().clearForPlayer(player);
        planet.getTerritory().removeConversation(player.getUniqueId());
    }

    public @Nullable PlanetPlayer getPlanetPlayer(@NotNull Player player) {
        for (PlanetPlayer planetPlayer : planetPlayers) {
            if (planetPlayer.getPlayer().equals(player)) {
                return planetPlayer;
            }
        }
        return null;
    }

    public void clear() {
        buildersTrusted.clear();
        developersTrusted.clear();
        buildersNotTrusted.clear();
        developersNotTrusted.clear();
        developersGuests.clear();
        bannedPlayers.clear();
        whitelistedPlayers.clear();
    }

    public void loadPlayers() {
        clear();
        FileConfiguration config = planet.getConfiguration().getConfig();

        buildersTrusted.addAll(loadUUIDList(config, "players.builders.trusted"));
        developersTrusted.addAll(loadUUIDList(config, "players.developers.trusted"));

        buildersNotTrusted.addAll(loadUUIDList(config, "players.builders.not-trusted"));
        developersNotTrusted.addAll(loadUUIDList(config, "players.developers.not-trusted"));

        developersGuests.addAll(loadUUIDList(config, "players.developers.guests"));
        bannedPlayers.addAll(loadUUIDList(config, "players.blacklist"));
        whitelistedPlayers.addAll(loadUUIDList(config, "players.whitelist"));
    }

    private @NotNull Set<UUID> loadUUIDList(@NotNull FileConfiguration config, @NotNull String path) {
        Set<UUID> uuids = new HashSet<>();
        for (@NotNull String uuidString : config.getStringList(path)) {
            try {
                uuids.add(UUID.fromString(uuidString));
            } catch (Exception ignored) {
            }
        }
        return uuids;
    }

    public @NotNull Set<UUID> getAllBuilders() {
        Set<UUID> builders = new HashSet<>(getBuildersTrusted());
        builders.addAll(getBuildersNotTrusted());
        return builders;
    }

    public @NotNull Set<UUID> getAllDevelopers() {
        Set<UUID> developers = new HashSet<>(getDevelopersTrusted());
        developers.addAll(getDevelopersNotTrusted());
        developers.addAll(getDevelopersGuests());
        return developers;
    }

    public boolean isTrustedDeveloper(Player player) {
        if (planet.isOwner(player)) {
            return true;
        }
        if (player.hasPermission("opencreative.world.dev.others")) {
            return true;
        }
        for (UUID uuid : getDevelopersTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isNotTrustedDeveloper(Player player) {
        for (UUID uuid : getDevelopersNotTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isNotTrustedBuilder(Player player) {
        for (UUID uuid : getBuildersNotTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isTrustedBuilder(Player player) {
        if (planet.isOwner(player)) {
            return true;
        }
        if (player.hasPermission("opencreative.world.build.others")) {
            return true;
        }
        for (UUID uuid : getBuildersTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeveloperGuest(Player player) {
        for (UUID uuid : getDevelopersGuests()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean canDevelop(Player player) {
        if (planet.isOwner(player)) {
            return true;
        }
        if (player.hasPermission("opencreative.world.dev.others")) {
            return true;
        }
        for (UUID uuid : getDevelopersTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        Player owner = Bukkit.getPlayer(planet.getOwner());
        if (owner == null) {
            return false;
        }
        if (!planet.equals(OpenCreative.getPlanetsManager().getPlanetByPlayer(owner))) {
            return false;
        }
        for (UUID uuid : getDevelopersNotTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean canBuild(Player player) {
        if (planet.isOwner(player)) {
            return true;
        }
        if (player.hasPermission("opencreative.world.build.others")) {
            return true;
        }
        for (UUID uuid : getBuildersTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        Player owner = Bukkit.getPlayer(planet.getOwner());
        if (owner == null) {
            return false;
        }
        if (!planet.equals(OpenCreative.getPlanetsManager().getPlanetByPlayer(owner))) {
            return false;
        }
        for (UUID uuid : getBuildersNotTrusted()) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public void removeBuilder(@NotNull String nickname) {
        removeBuilder(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void removeBuilder(@NotNull UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    player.setGameMode(GameMode.ADVENTURE);
                    clearWorldModePermissions(player);
                    if (!canDevelop(player)) {
                        giveVisitorPermissions(player);
                    }
                }
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        buildersNotTrusted.removeIf(builder -> builder.equals(uuid));
        buildersTrusted.removeIf(builder -> builder.equals(uuid));
        savePlayersToConfig("players.builders.not-trusted", buildersNotTrusted);
        savePlayersToConfig("players.builders.trusted", buildersTrusted);
    }

    public void removeDeveloper(@NotNull String nickname) {
        removeDeveloper(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }


    public void removeDeveloper(@NotNull UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
                if (isEntityInDevPlanet(player)) {
                    clearPlayer(player);
                    player.teleport(planet.getTerritory().getSpawnLocation());
                }
                clearWorldModePermissions(player);
                if (!canBuild(player)) {
                    giveVisitorPermissions(player);
                }
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        developersNotTrusted.removeIf(developer -> developer.equals(uuid));
        developersTrusted.removeIf(developer -> developer.equals(uuid));
        savePlayersToConfig("players.developers.not-trusted", developersNotTrusted);
        savePlayersToConfig("players.developers.trusted", developersTrusted);
    }

    public void addDeveloperGuest(@NotNull String nickname) {
        addDeveloperGuest(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void addDeveloperGuest(@NotNull UUID uuid) {
        if (getAllDevelopers().size() > planet.getLimits().getDevelopersLimit()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                player.sendMessage(getLocaleMessage("world.players.developers.player-guest").replace("%player%", player.getName()));
                Sounds.WORLD_NOW_DEVELOPER_GUEST.play(player);
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        developersGuests.add(uuid);
        developersNotTrusted.removeIf(developer -> developer.equals(uuid));
        developersTrusted.removeIf(developer -> developer.equals(uuid));
        savePlayersToConfig("players.developers.guests", developersGuests);
        savePlayersToConfig("players.developers.not-trusted", developersNotTrusted);
        savePlayersToConfig("players.developers.trusted", developersTrusted);
    }

    public void addDeveloper(@NotNull String nickname, boolean trusted) {
        addDeveloper(Bukkit.getOfflinePlayer(nickname).getUniqueId(), trusted);
    }

    public void addDeveloper(@NotNull UUID uuid, boolean trusted) {
        if (getAllDevelopers().size() > planet.getLimits().getDevelopersLimit()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                if (!trusted) {
                    player.sendMessage(getLocaleMessage("world.players.developers.player").replace("%player%", player.getName()));
                    Sounds.WORLD_NOW_DEVELOPER.play(player);
                    if (OpenCreative.getPlanetsManager().getDevPlanet(player) != null) {
                        player.setGameMode(GameMode.CREATIVE);
                        clearWorldModePermissions(player);
                        giveDevPermissions(player);
                    }
                }
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        if (trusted) {
            developersNotTrusted.removeIf(developer -> developer.equals(uuid));
            developersTrusted.add(uuid);
        } else {
            developersTrusted.removeIf(developer -> developer.equals(uuid));
            developersNotTrusted.add(uuid);
        }
        developersGuests.removeIf(developer -> developer.equals(uuid));
        savePlayersToConfig("players.developers.guests", developersGuests);
        savePlayersToConfig("players.developers.not-trusted", developersNotTrusted);
        savePlayersToConfig("players.developers.trusted", developersTrusted);
    }

    public void addBuilder(@NotNull String nickname, boolean trusted) {
        addBuilder(Bukkit.getOfflinePlayer(nickname).getUniqueId(), trusted);
    }

    public void addBuilder(@NotNull UUID uuid, boolean trusted) {
        if (getAllBuilders().size() > planet.getLimits().getBuildersLimit()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                if (!trusted) {
                    player.sendMessage(getLocaleMessage("world.players.builders.player").replace("%player%", player.getName()));
                    Sounds.WORLD_NOW_BUILDER.play(player);
                    if (OpenCreative.getPlanetsManager().getDevPlanet(player) == null) {
                        player.setGameMode(GameMode.CREATIVE);
                        clearWorldModePermissions(player);
                        giveBuildPermissions(player);
                    }
                }
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        if (trusted) {
            buildersNotTrusted.removeIf(builder -> builder.equals(uuid));
            buildersTrusted.add(uuid);
        } else {
            buildersTrusted.removeIf(builder -> builder.equals(uuid));
            buildersNotTrusted.add(uuid);
        }
        savePlayersToConfig("players.builders.not-trusted", buildersNotTrusted);
        savePlayersToConfig("players.builders.trusted", buildersTrusted);
        if (!planet.isLoaded()) clear();
    }

    public void unbanPlayer(@NotNull String nickname) {
        unbanPlayer(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void unbanPlayer(@NotNull UUID uuid) {
        if (!planet.isLoaded()) loadPlayers();
        this.bannedPlayers.remove(uuid);
        savePlayersToConfig("players.blacklist", bannedPlayers);
        if (!planet.isLoaded()) clear();
    }

    public void removeFromWhitelist(@NotNull String nickname) {
        removeFromWhitelist(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void removeFromWhitelist(@NotNull UUID uuid) {
        if (!planet.isLoaded()) loadPlayers();
        this.whitelistedPlayers.removeIf(whitelisted -> whitelisted.equals(uuid));
        savePlayersToConfig("players.whitelist", whitelistedPlayers);
        if (!planet.isLoaded()) clear();
    }

    public void banPlayer(@NotNull String nickname) {
        banPlayer(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void banPlayer(@NotNull UUID uuid) {
        if (planet.isOwner(uuid)) return;
        if (getBannedPlayers().size() > planet.getLimits().getBlacklistedLimit()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !player.hasPermission("opencreative.world.ban.bypass")) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                teleportToLobby(player);
                player.sendMessage(getLocaleMessage("world.players.black-list.player").replace("%player%", player.getName()));
                Sounds.WORLD_BANNED.play(player);
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        bannedPlayers.add(uuid);
        savePlayersToConfig("players.blacklist", bannedPlayers);
        if (!planet.isLoaded()) clear();
    }

    public void whitelistPlayer(@NotNull String nickname) {
        whitelistPlayer(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public void whitelistPlayer(@NotNull UUID uuid) {
        if (planet.isOwner(uuid)) return;
        if (getWhitelistedPlayers().size() > planet.getLimits().getWhitelistedLimit()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
            if (planet.equals(playerPlanet)) {
                player.sendMessage(getLocaleMessage("world.players.white-list.player").replace("%player%", player.getName()));
                Sounds.WORLD_WHITELIST_ADDED.play(player);
            }
        }
        if (!planet.isLoaded()) loadPlayers();
        whitelistedPlayers.add(uuid);
        savePlayersToConfig("players.whitelist", whitelistedPlayers);
        if (!planet.isLoaded()) clear();
    }

    public void kickPlayer(@NotNull Player player) {
        Planet playerPlanet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        if (planet.equals(playerPlanet) && !player.hasPermission("opencreative.world.kick.bypass")) {
            new QuitEvent(player).callEvent();
            removePassengers(player);
            teleportToLobby(player);
            player.sendMessage(getLocaleMessage("world.players.kick.player")
                    .replace("%player%", player.getName()));
            Sounds.WORLD_KICKED.play(player);
        }
    }

    public @NotNull Set<UUID> getAllPlayersFromConfig() {
        Set<UUID> allPlayers = new HashSet<>();
        planet.getPlayers().forEach(player -> allPlayers.add(player.getUniqueId()));
        allPlayers.addAll(getBuildersTrusted());
        allPlayers.addAll(getBuildersNotTrusted());
        allPlayers.addAll(getDevelopersTrusted());
        allPlayers.addAll(getDevelopersNotTrusted());
        allPlayers.addAll(getDevelopersGuests());
        allPlayers.addAll(getBannedPlayers());
        allPlayers.addAll(getWhitelistedPlayers());
        allPlayers.remove(planet.getOwner());
        return allPlayers;
    }

    public @NotNull Set<UUID> getBuildersTrusted() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.builders.trusted");
        }
        return new HashSet<>(buildersTrusted);
    }

    public @NotNull Set<UUID> getBuildersNotTrusted() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.builders.not-trusted");
        }
        return new HashSet<>(buildersNotTrusted);
    }

    public @NotNull Set<UUID> getDevelopersGuests() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.developers.guests");
        }
        return new HashSet<>(developersGuests);
    }

    public @NotNull Set<UUID> getDevelopersTrusted() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.developers.trusted");
        }
        return new HashSet<>(developersTrusted);
    }

    public @NotNull Set<UUID> getDevelopersNotTrusted() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.developers.not-trusted");
        }
        return new HashSet<>(developersNotTrusted);
    }

    public String getBuilders() {
        return planet.getWorldPlayers().getAllBuilders().stream()
                .map(uuid -> {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    return name != null ? name : "Unknown (" + uuid + ")";
                })
                .collect(Collectors.joining(", "));
    }

    public String getDevelopers() {
        return planet.getWorldPlayers().getAllDevelopers().stream()
                .map(uuid -> {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    return name != null ? name : "Unknown (" + uuid + ")";
                })
                .collect(Collectors.joining(", "));
    }

    public @NotNull Set<UUID> getBannedPlayers() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.blacklist");
        }
        return bannedPlayers;
    }

    public boolean addUnique(@NotNull UUID uuid) {
        List<String> uniques = planet.getConfiguration().getConfig().getStringList("players.unique");
        String uuidString = uuid.toString();
        for (@NotNull String likedUUID : uniques) {
            if (likedUUID.equals(uuidString)) {
                return false;
            }
        }
        uniques.add(uuid.toString());
        planet.getConfiguration().set("players.unique", uniques);
        return true;
    }

    public boolean addLike(@NotNull UUID uuid) {
        List<String> likes = planet.getConfiguration().getConfig().getStringList("players.liked");
        String uuidString = uuid.toString();
        for (@NotNull String likedUUID : likes) {
            if (likedUUID.equals(uuidString)) {
                return false;
            }
        }
        likes.add(uuid.toString());
        planet.getConfiguration().set("players.liked", likes);
        return true;
    }

    public boolean addDislike(@NotNull UUID uuid) {
        List<String> likes = planet.getConfiguration().getConfig().getStringList("players.disliked");
        String uuidString = uuid.toString();
        for (@NotNull String likedUUID : likes) {
            if (likedUUID.equals(uuidString)) {
                return false;
            }
        }
        likes.add(uuid.toString());
        planet.getConfiguration().set("players.disliked", likes);
        return true;
    }

    public @NotNull Set<UUID> getUniquePlayers() {
        return loadUUIDList(planet.getConfiguration().getConfig(), "players.unique");
    }

    public @NotNull Set<UUID> getLikedPlayers() {
        return loadUUIDList(planet.getConfiguration().getConfig(), "players.liked");
    }

    public @NotNull Set<UUID> getDislikedPlayers() {
        return loadUUIDList(planet.getConfiguration().getConfig(), "players.disliked");
    }

    public boolean hasLiked(@NotNull UUID uuid) {
        return getLikedPlayers().contains(uuid);
    }

    public boolean hasDisliked(@NotNull UUID uuid) {
        return getDislikedPlayers().contains(uuid);
    }

    public boolean hasRated(@NotNull UUID uuid) {
        return hasLiked(uuid) || hasDisliked(uuid);
    }

    public @NotNull Set<UUID> getWhitelistedPlayers() {
        if (!planet.isLoaded()) {
            return loadUUIDList(planet.getConfiguration().getConfig(), "players.whitelist");
        }
        return whitelistedPlayers;
    }

    public boolean isBanned(@NotNull String nickname) {
        return isBanned(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public boolean isBanned(@NotNull UUID uuid) {
        for (UUID banned : getBannedPlayers()) {
            if (banned.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWhitelisted(@NotNull UUID uuid) {
        for (UUID whitelisted : getWhitelistedPlayers()) {
            if (whitelisted.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWhitelisted(@NotNull String nickname) {
        return isWhitelisted(Bukkit.getOfflinePlayer(nickname).getUniqueId());
    }

    public @NotNull List<String> uuidsToStringList(@NotNull Set<UUID> uuids) {
        List<String> list = new ArrayList<>();
        for (UUID uuid : uuids) {
            list.add(uuid.toString());
        }
        return list;
    }

    public void savePlayersToConfig(@NotNull String path, @NotNull Set<UUID> uuids) {
        planet.getConfiguration().set(path, uuidsToStringList(uuids));
    }

    public void purgeData() {
        List<String> empty = new ArrayList<>();
        clear();
        planet.getConfiguration().set("players.unique", empty);
        planet.getConfiguration().set("players.liked", empty);
        planet.getConfiguration().set("players.disliked", empty);
        planet.getConfiguration().set("players.blacklist", empty);
        planet.getConfiguration().set("players.whitelist", empty);
        planet.getConfiguration().set("players.developers.trusted", empty);
        planet.getConfiguration().set("players.developers.not-trusted", empty);
        planet.getConfiguration().set("players.developers.guests", empty);
        planet.getConfiguration().set("players.builders.trusted", empty);
        planet.getConfiguration().set("players.builders.not-trusted", empty);
    }
}
