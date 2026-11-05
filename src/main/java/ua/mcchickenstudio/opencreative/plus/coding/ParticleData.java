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

package ua.mcchickenstudio.opencreative.plus.coding;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default data for particles, that require it. Since 1.21.9 particles like EFFECT, FLASH or
 * DRAGON_BREATH need data, and coding actions spawned them without it, so nothing appeared.
 */
public final class ParticleData {

    private ParticleData() {
    }

    public static @Nullable Object defaultData(@NotNull Particle particle, @NotNull Location location) {
        Class<?> type = particle.getDataType();
        if (type == Void.class) return null;
        if (type == Color.class) return Color.WHITE;
        if (type == Float.class) return 1.0f;
        if (type == Integer.class) return 0;
        if (type == Particle.DustOptions.class) return new Particle.DustOptions(Color.RED, 1.0f);
        if (type == Particle.DustTransition.class) return new Particle.DustTransition(Color.RED, Color.WHITE, 1.0f);
        if (type == Particle.Spell.class) return new Particle.Spell(Color.WHITE, 1.0f);
        if (type == Particle.Trail.class) return new Particle.Trail(location, Color.WHITE, 20);
        if (type == BlockData.class) return Material.STONE.createBlockData();
        if (type == ItemStack.class) return new ItemStack(Material.STONE);
        return null;
    }

    /**
     * Spawns particle for player with default data, if particle needs it.
     */
    public static void spawn(@NotNull Player player, @NotNull Particle particle, @NotNull Location location,
                             int count, double offsetX, double offsetY, double offsetZ) {
        Object data = defaultData(particle, location);
        if (data == null && particle.getDataType() != Void.class) return; // unknown data type (e.g. vibration)
        player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, data);
    }
}
