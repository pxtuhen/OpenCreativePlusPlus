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

package ua.mcchickenstudio.opencreative.settings.items;

import org.bukkit.Material;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>ItemsStack</h1>
 * This record represents a prepared existing item stack,
 * that doesn't depend on player or any setting.
 * @param item item stack.
 */
public record ItemsStack(@NotNull ItemStack item) implements ItemsLike {

    public static @NotNull ItemsStack light(int level) {
        ItemStack item = new ItemStack(Material.LIGHT);
        level = Math.clamp(level, 0, 15);
        if (item.getItemMeta() instanceof BlockDataMeta meta) {
            Light lightData = (Light) Material.LIGHT.createBlockData();
            lightData.setLevel(level);
            meta.setBlockData(lightData);
            item.setItemMeta(meta);
        }
        return new ItemsStack(item);
    }

    public static @NotNull ItemsStack material(@NotNull Material material) {
        return new ItemsStack(new ItemStack(material, 1));
    }

    @Override
    public ItemStack get(@NotNull Player player) {
        return item;
    }
}
