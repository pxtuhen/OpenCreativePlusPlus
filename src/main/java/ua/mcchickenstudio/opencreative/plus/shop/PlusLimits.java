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

package ua.mcchickenstudio.opencreative.plus.shop;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.players.PlayerData;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits of OpenCreative++ = group limits (groups.yml) + bought upgrades.
 * Contest worlds don't count as player's worlds.
 */
public final class PlusLimits {

    public static final String BONUS_FLOORS = "bonus.coding-floors";
    public static final String BONUS_SIZE = "bonus.world-size";
    public static final String WORLD_SLOTS = "world-slots";

    private static final Set<Integer> contestWorlds = ConcurrentHashMap.newKeySet();

    private PlusLimits() {
    }

    public static void markContestWorld(int planetId, boolean contest) {
        if (contest) contestWorlds.add(planetId);
        else contestWorlds.remove(planetId);
    }

    public static boolean isContestWorld(@NotNull Planet planet) {
        return contestWorlds.contains(planet.getId());
    }

    /**
     * Player's own worlds, without contest worlds.
     */
    public static int worldsUsed(@NotNull Player player) {
        int count = 0;
        for (Planet planet : OpenCreative.getPlanetsManager().getPlanetsByOwner(player)) {
            if (!isContestWorld(planet)) count++;
        }
        return count;
    }

    public static int boughtWorldSlots(@NotNull Player player) {
        return PlayerData.getInt(player.getUniqueId(), WORLD_SLOTS);
    }

    public static int worldsLimit(@NotNull Player player) {
        return OpenCreative.getSettings().getGroups().getGroup(player).getWorldsLimit() + boughtWorldSlots(player);
    }

    public static int bonusFloors(@NotNull Planet planet) {
        return planet.getConfiguration().getConfig().getInt(BONUS_FLOORS, 0);
    }

    public static int bonusSize(@NotNull Planet planet) {
        return planet.getConfiguration().getConfig().getInt(BONUS_SIZE, 0);
    }
}
