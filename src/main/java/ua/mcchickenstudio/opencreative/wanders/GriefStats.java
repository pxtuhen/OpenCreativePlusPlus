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

package ua.mcchickenstudio.opencreative.wanders;

import org.jetbrains.annotations.NotNull;

/**
 * <h1>GriefStats</h1>
 * This class represents statistics of
 * destructive changes in a world.
 */
public final class GriefStats {

    private int destroyedBlocksAmount;
    private int lavaPlacementsAmount;
    private int tntPlacementsAmount;
    private int destroyedHangingsAmount;
    private int destroyedCodingBlocksAmount;
    private int creepersSummonsAmount;
    private int withersSummonsAmount;
    private int dragonsSummonsAmount;

    /**
     * Adds amount of destroyed blocks.
     *
     * @param amount amount of destroyed blocks.
     */
    public void addDestroyedBlocksAmount(int amount) {
        this.destroyedBlocksAmount += amount;
    }

    /**
     * Adds amount of destroyed coding blocks.
     *
     * @param amount amount of destroyed coding blocks.
     */
    public void addDestroyedCodingBlocksAmount(int amount) {
        this.destroyedCodingBlocksAmount += amount;
    }

    /**
     * Adds amount of destroyed item frames or paintings.
     *
     * @param amount amount of destroyed hangings.
     */
    public void addDestroyedHangingsAmount(int amount) {
        this.destroyedHangingsAmount += amount;
    }

    /**
     * Adds amount of lava placements.
     *
     * @param amount amount of placed lava.
     */
    public void addLavaPlacementsAmount(int amount) {
        this.lavaPlacementsAmount += amount;
    }

    /**
     * Adds amount of tnt placements.
     *
     * @param amount amount of placed tnt.
     */
    public void addTntPlacementsAmount(int amount) {
        this.tntPlacementsAmount += amount;
    }

    /**
     * Adds amount of creeper summons.
     *
     * @param amount amount of summoned creepers.
     */
    public void addCreeperSummonsAmount(int amount) {
        this.creepersSummonsAmount += amount;
    }

    /**
     * Adds amount of summoned withers.
     *
     * @param amount amount of summoned withers.
     */
    public void addWithersSummonsAmount(int amount) {
        this.withersSummonsAmount += amount;
    }

    /**
     * Adds amount of summoned ender dragons.
     *
     * @param amount amount of summoned ender dragons.
     */
    public void addDragonsSummonsAmount(int amount) {
        this.dragonsSummonsAmount += amount;
    }

    public int getDestroyedBlocksAmount() {
        return destroyedBlocksAmount;
    }

    public int getLavaPlacementsAmount() {
        return lavaPlacementsAmount;
    }

    public int getTntPlacementsAmount() {
        return tntPlacementsAmount;
    }

    public int getDestroyedCodingBlocksAmount() {
        return destroyedCodingBlocksAmount;
    }

    public int getDestroyedHangingsAmount() {
        return destroyedHangingsAmount;
    }

    public boolean isSuspicious() {
        return lavaPlacementsAmount >= 10 || destroyedHangingsAmount >= 10 || tntPlacementsAmount >= 3
                || destroyedBlocksAmount >= 20 || destroyedCodingBlocksAmount >= 2 || creepersSummonsAmount >= 3
                || withersSummonsAmount >= 1 || dragonsSummonsAmount >= 1;
    }

    public @NotNull String getAsString() {
        StringBuilder builder = new StringBuilder();
        if (lavaPlacementsAmount >= 1) builder.append("placed lava x").append(lavaPlacementsAmount).append(", ");
        if (tntPlacementsAmount >= 1) builder.append("placed tnt x").append(tntPlacementsAmount).append(", ");
        if (destroyedBlocksAmount >= 1) builder.append("destroyed blocks x").append(destroyedBlocksAmount).append(", ");
        if (destroyedCodingBlocksAmount >= 1) builder.append("destroyed coding blocks x").append(destroyedCodingBlocksAmount).append(", ");
        if (creepersSummonsAmount >= 1) builder.append("summoned creepers x").append(creepersSummonsAmount).append(", ");
        if (withersSummonsAmount >= 1) builder.append("summoned withers x").append(withersSummonsAmount).append(", ");
        if (dragonsSummonsAmount >= 1) builder.append("summoned ender dragons x").append(dragonsSummonsAmount).append(", ");
        if (destroyedHangingsAmount >= 1)
            builder.append("removed hangings x").append(destroyedHangingsAmount).append(", ");
        if (!builder.isEmpty()) {
            builder.delete(builder.length() - 2, builder.length());
            return builder.toString();
        }
        return "no destructive actions";
    }

    /**
     * Clears stats.
     */
    public void clear() {
        destroyedBlocksAmount = 0;
        lavaPlacementsAmount = 0;
        tntPlacementsAmount = 0;
        destroyedHangingsAmount = 0;
        creepersSummonsAmount = 0;
        withersSummonsAmount = 0;
        dragonsSummonsAmount = 0;
    }

}
