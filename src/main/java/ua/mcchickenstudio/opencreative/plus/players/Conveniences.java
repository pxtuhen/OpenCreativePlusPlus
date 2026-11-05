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

package ua.mcchickenstudio.opencreative.plus.players;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.planet.PlanetConnectPlayerEvent;
import ua.mcchickenstudio.opencreative.events.planet.PlanetCreationEvent;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.rating.Ratings;

import java.util.ArrayList;
import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toComponent;

/**
 * Small conveniences: world info in action bar on join, recent worlds, Discord message about new worlds.
 */
public final class Conveniences implements Listener {

    public static final String RECENT = "recent-worlds";
    private static final int RECENT_SIZE = 5;

    @EventHandler
    public void onConnect(PlanetConnectPlayerEvent event) {
        Player player = event.getPlayer();
        Planet planet = event.getPlanet();
        if (player == null) return;
        // recent worlds, newest first
        List<Integer> recent = new ArrayList<>(PlayerData.getIntList(player.getUniqueId(), RECENT));
        recent.remove(Integer.valueOf(planet.getId()));
        recent.addFirst(planet.getId());
        while (recent.size() > RECENT_SIZE) recent.removeLast();
        PlayerData.setIntList(player.getUniqueId(), RECENT, recent);
        if (!OpenCreative.getPlugin().getConfig().getBoolean("lobby.world-info-actionbar", true)) return;
        // a bit later, so OpenCreative+ hints and titles don't overwrite it
        Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
            if (!player.isOnline() || !planet.equals(OpenCreative.getPlanetsManager().getPlanetByPlayer(player))) return;
            String key = !planet.isOwner(player) && Ratings.vote(planet, player.getUniqueId()) == 0 ? "plus.world-info-rate" : "plus.world-info";
            player.sendActionBar(toComponent(getLocaleMessage(key, false)
                    .replace("%name%", planet.getInformation().getDisplayName())
                    .replace("%owner%", planet.getOwnerName())
                    .replace("%rating%", Ratings.format(planet))
                    .replace("%id%", planet.getInformation().getCustomID())));
        }, 30L);
    }

    @EventHandler
    public void onCreation(PlanetCreationEvent event) {
        Planet planet = event.getPlanet();
        Discord.send("new-world", getLocaleMessage("plus.discord.new-world", false)
                .replace("%player%", event.getPlayer().getName())
                .replace("%world%", org.bukkit.ChatColor.stripColor(planet.getInformation().getDisplayName()))
                .replace("%id%", String.valueOf(planet.getId())));
    }

    public static @NotNull List<Planet> recent(@NotNull java.util.UUID player) {
        List<Planet> planets = new ArrayList<>();
        for (int id : PlayerData.getIntList(player, RECENT)) {
            Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(id));
            if (planet != null) planets.add(planet);
        }
        return planets;
    }
}
