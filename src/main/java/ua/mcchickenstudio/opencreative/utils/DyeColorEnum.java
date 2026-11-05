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

package ua.mcchickenstudio.opencreative.utils;

import org.bukkit.DyeColor;
import org.bukkit.Material;

public enum DyeColorEnum {

    WHITE(Material.WHITE_DYE, DyeColor.WHITE),
    ORANGE(Material.ORANGE_DYE, DyeColor.ORANGE),
    MAGENTA(Material.MAGENTA_DYE, DyeColor.MAGENTA),
    LIGHT_BLUE(Material.LIGHT_BLUE_DYE, DyeColor.LIGHT_BLUE),
    YELLOW(Material.YELLOW_DYE, DyeColor.YELLOW),
    LIME(Material.LIME_DYE, DyeColor.LIME),
    PINK(Material.PINK_DYE, DyeColor.PINK),
    GRAY(Material.GRAY_DYE, DyeColor.GRAY),
    LIGHT_GRAY(Material.LIGHT_GRAY_DYE, DyeColor.LIGHT_GRAY),
    CYAN(Material.CYAN_DYE, DyeColor.CYAN),
    PURPLE(Material.PURPLE_DYE, DyeColor.PURPLE),
    BLUE(Material.BLUE_DYE, DyeColor.BLUE),
    BROWN(Material.BROWN_DYE, DyeColor.BROWN),
    GREEN(Material.GREEN_DYE, DyeColor.GREEN),
    RED(Material.RED_DYE, DyeColor.RED),
    BLACK(Material.BLACK_DYE, DyeColor.BLACK);

    private DyeColorEnum(Material material, DyeColor dyeColor) {
        this.material = material;
        this.dyeColor = dyeColor;
    }

    private final Material material;
    private final DyeColor dyeColor;

    public Material getMaterial() {
        return material;
    }

    public DyeColor getColor() {
        return dyeColor;
    }

    public static DyeColor byMaterial(Material material) {
        for (DyeColorEnum colorEnum : DyeColorEnum.values()) {
            if (colorEnum.getMaterial() == material) {
                return colorEnum.getColor();
            }
        }
        return null;
    }
}
