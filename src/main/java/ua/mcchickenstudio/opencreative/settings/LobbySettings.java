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

package ua.mcchickenstudio.opencreative.settings;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.utils.hooks.HookUtils;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.*;

/**
 * <h1>LobbySettings</h1>
 * This class represents settings of lobby world,
 * where players will be teleported on server join,
 * or by using /spawn command.
 */
public final class LobbySettings {

    private WorldApply clearInventory = WorldApply.ALL;
    private WorldApply resetGameMode = WorldApply.ALL;
    private WorldApply cancelOutOfBordersTeleport = WorldApply.ALL;

    private boolean disallowPlacingBlocks = true;
    private boolean disallowDestroyingBlocks = true;
    private boolean disallowSpawningMobs = true;
    private boolean disallowDamagingMobs = true;
    private boolean disallowWorldEdit = true;
    private boolean disallowChangingBlocks = true;
    private boolean disallowEditingArmorStands = true;
    private boolean disableExplosions = true;
    private boolean resetViewDistance = true;
    private boolean resetResourcePack = true;
    private boolean resetSkin = true;
    private boolean teleportOnJoin = true;

    /**
     * Loads settings of lobby from configuration.
     */
    public void load() {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        ConfigurationSection section = config.getConfigurationSection("lobby");

        if (section == null) {
            sendCriticalErrorMessage("Can't load lobby settings, section `lobby` in config.yml is empty.");
            return;
        }

        clearInventory = WorldApply.getWorldApply(section.getString("clear-inventory", "all"));
        resetGameMode = WorldApply.getWorldApply(section.getString("reset-gamemode", "all"));
        cancelOutOfBordersTeleport = WorldApply.getWorldApply(section.getString("cancel-out-of-borders-teleport", "planets"));

        disableExplosions = section.getBoolean("disable-explosions", true);
        disallowWorldEdit = section.getBoolean("disallow-world-edit", true);
        disallowDamagingMobs = section.getBoolean("disallow-damaging-mobs", true);
        disallowSpawningMobs = section.getBoolean("disallow-spawning-mobs", true);
        disallowPlacingBlocks = section.getBoolean("disallow-placing-blocks", true);
        disallowDestroyingBlocks = section.getBoolean("disallow-destroying-blocks", true);
        disallowChangingBlocks = section.getBoolean("disallow-changing-blocks", true);
        disallowEditingArmorStands = section.getBoolean("disallow-editing-armor-stands", true);

        resetViewDistance = section.getBoolean("reset-view-distance", true);
        resetResourcePack = section.getBoolean("reset-resource-pack", true);
        resetSkin = section.getBoolean("reset-skin", true);
        teleportOnJoin = section.getBoolean("teleport-on-join", true);
        if (HookUtils.isPluginEnabled("ItemsAdder")) {
            resetResourcePack = false;
        }
    }

    /**
     * Checks whether player's inventory should be
     * cleared after teleporting between worlds.
     *
     * @param world, where player currently is in.
     * @return true - will be cleared, false - not.
     */
    public boolean shouldClearInventory(@NotNull World world) {
        return clearInventory.isCompatibleWorld(world);
    }

    /**
     * Checks whether player's inventory should be
     * cleared after teleporting between worlds.
     *
     * @param world, where player currently is in.
     * @return true - will be reset, false - not.
     */
    public boolean shouldResetGameMode(@NotNull World world) {
        return resetGameMode.isCompatibleWorld(world);
    }

    /**
     * Checks whether teleport event should be canceled,
     * because it's out of borders.
     *
     * @param world, where player currently is in.
     * @return true - will be canceled, false - not.
     */
    public boolean shouldCancelOutOfBordersTeleport(@NotNull World world) {
        return cancelOutOfBordersTeleport.isCompatibleWorld(world);
    }

    /**
     * Checks whether explosions in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable explosions, false - allow them.
     */
    public boolean areExplosionsDisabled() {
        return disableExplosions;
    }

    /**
     * Checks whether damaging mobs in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable damaging mobs, false - allow it.
     */
    public boolean isDamagingMobsDisallowed() {
        return disallowDamagingMobs;
    }

    /**
     * Checks whether destroying blocks in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable destroying blocks, false - allow it.
     */
    public boolean isDestroyingBlocksDisallowed() {
        return disallowDestroyingBlocks;
    }

    /**
     * Checks whether placing blocks in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable placing blocks, false - allow it.
     */
    public boolean isPlacingBlocksDisallowed() {
        return disallowPlacingBlocks;
    }

    /**
     * Checks whether changing doors, trapdoors, fences
     * and flower pots in lobby world should be cancelled
     * without bypass permission.
     *
     * @return true - disable placing blocks, false - allow it.
     */
    public boolean isChangingBlocksDisallowed() {
        return disallowChangingBlocks;
    }

    /**
     * Checks whether editing armor stands in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable placing blocks, false - allow it.
     */
    public boolean isEditingArmorStandsDisallowed() {
        return disallowEditingArmorStands;
    }

    /**
     * Checks whether spawning mobs in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable spawning mobs, false - allow it.
     */
    public boolean isSpawningMobsDisallowed() {
        return disallowSpawningMobs;
    }

    /**
     * Checks whether using WorldEdit in lobby world
     * should be cancelled without bypass permission.
     *
     * @return true - disable spawning mobs, false - allow it.
     */
    public boolean isWorldEditDisallowed() {
        return disallowWorldEdit;
    }

    /**
     * Checks whether view and simulation distances
     * will be reset on entering lobby or changing worlds.
     *
     * @return true - reset view and simulation distances,
     * false - don't reset, but disable "Set View Distance"
     * and "Set Simulation Distance" player actions.
     */
    public boolean shouldResetViewDistance() {
        return resetViewDistance;
    }

    /**
     * Checks whether resource pack will be reset
     * on entering lobby or changing worlds.
     *
     * @return true - reset resource pack,
     * false - don't reset, but disable
     * "Set Resource Pack" player action.
     */
    public boolean shouldResetResourcePack() {
        return resetResourcePack;
    }


    /**
     * Checks whether player's skin will be reset
     * on entering lobby or changing worlds.
     *
     * @return true - reset skin and cape,
     * false - don't reset, but disable
     * "Set Skin", "Set Cape" player actions.
     */
    public boolean shouldResetSkin() {
        return resetSkin;
    }

    /**
     * Checks whether player will be teleported to lobby
     * when he connects to server.
     *
     * @return true - teleport to lobby on join, false - not.
     */
    public boolean shouldTeleportOnJoin() {
        return teleportOnJoin;
    }

    public enum WorldApply {

        ALL,
        CREATIVE,
        LOBBY,
        PLANETS,
        NONE;

        public boolean isCompatibleWorld(@NotNull World world) {
            return switch (this) {
                case NONE -> false;
                case CREATIVE -> isOpenCreativeWorld(world);
                case LOBBY -> isLobbyWorld(world);
                case PLANETS -> isPlanet(world);
                default -> true;
            };
        }

        public static @Nullable WorldApply getWorldApply(@NotNull String text) {
            return switch (text.toLowerCase()) {
                case "true", "all" -> ALL;
                case "opencreative", "creative" -> CREATIVE;
                case "planets", "worlds", "planet", "world" -> PLANETS;
                case "lobby", "spawn" -> LOBBY;
                case "false", "none" -> NONE;
                default -> null;
            };
        }

    }
}
