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

package ua.mcchickenstudio.opencreative.utils.hooks;

import ua.mcchickenstudio.opencreative.plus.PlusLog;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.blocks.BlocksManager;
import ua.mcchickenstudio.opencreative.managers.blocks.VanillaBlocksManager;
import ua.mcchickenstudio.opencreative.managers.blocks.WorldEditManager;
import ua.mcchickenstudio.opencreative.managers.disguises.DisabledDisguises;
import ua.mcchickenstudio.opencreative.managers.disguises.DisguiseManager;
import ua.mcchickenstudio.opencreative.managers.disguises.LibsDisguises;
import ua.mcchickenstudio.opencreative.managers.ignore.DisabledIgnore;
import ua.mcchickenstudio.opencreative.managers.ignore.EssentialsIgnore;
import ua.mcchickenstudio.opencreative.managers.ignore.IgnoreManager;
import ua.mcchickenstudio.opencreative.managers.packets.DisabledPacketManager;
import ua.mcchickenstudio.opencreative.managers.packets.PacketEventsManager;
import ua.mcchickenstudio.opencreative.managers.packets.PacketManager;
import ua.mcchickenstudio.opencreative.managers.packets.ProtocolLibManager;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;

public final class HookUtils {

    public static boolean isPlaceholderAPIEnabled = false;
    public static boolean isPacketEventsEnabled = false;
    public static boolean isProtocolLibEnabled = false;
    public static boolean isVaultEnabled = false;
    public static boolean isLibsDisguisesEnabled = false;
    public static boolean isWorldEditEnabled = false;
    public static boolean isEssentialsEnabled = false;

    /**
     * Load hooks into other plugins for working with them. For example: OpenCreative++ can hook into PlaceholderAPI.
     **/
    public static void loadHooks() {
        isPlaceholderAPIEnabled = isPluginEnabled("PlaceholderAPI");
        isProtocolLibEnabled = isPluginEnabled("ProtocolLib");
        isPacketEventsEnabled = isPluginEnabled("PacketEvents");
        isVaultEnabled = isPluginEnabled("Vault");
        isWorldEditEnabled = isPluginEnabled("WorldEdit");
        isLibsDisguisesEnabled = isPluginEnabled("LibsDisguises");
        isEssentialsEnabled = isPluginEnabled("Essentials");
        if (isPlaceholderAPIEnabled) PlusLog.module("Интеграции").info("PlaceholderAPI подключён: плейсхолдеры добавлены.");
        if (isLibsDisguisesEnabled) PlusLog.module("Интеграции").info("LibsDisguises подключён: действия маскировки добавлены.");
        if (isWorldEditEnabled) PlusLog.module("Интеграции").info("WorldEdit подключён: ограничение за границами мира.");
        if (isPlaceholderAPIEnabled) {
            PAPIUtils.registerPlaceholder();
        }
    }

    public static boolean isPluginEnabled(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    public static void clearEntitiesHook(World world) {
        try {
            if (OpenCreative.getDisguiseManager().isWorking()) {
                for (Entity entity : world.getEntities()) {
                    OpenCreative.getDisguiseManager().clearDisguises(entity);
                }
            }
        } catch (Exception error) {
            sendCriticalErrorMessage("Failed to clear disguises in world " + world.getName(), error);
        }
    }

    public static void clearPlayerHook(Player player) {
        try {
            if (OpenCreative.getDisguiseManager().isWorking()) {
                OpenCreative.getDisguiseManager().clearDisguises(player);
            }
        } catch (Exception error) {
            sendCriticalErrorMessage("Failed to clear disguise for player " + player.getName(), error);
        }
    }

    public static PacketManager getPacketManager() {
        if (isPacketEventsEnabled) {
            OpenCreative.getPlugin().getLogger().info("Successfully integrated to PacketEvents: Added blocks effects and spectator mode in tab support.");
            return new PacketEventsManager();
        } else if (isProtocolLibEnabled) {
            OpenCreative.getPlugin().getLogger().info("Successfully integrated to ProtocolLib: Added blocks effects and spectator mode in tab.");
            return new ProtocolLibManager();
        } else {
            PlusLog.module("Интеграции").debug("Нет PacketEvents/ProtocolLib: часть эффектов блоков и прозрачные ники в TAB недоступны.");
            return new DisabledPacketManager();
        }
    }

    public static BlocksManager getBlocks() {
        if (isWorldEditEnabled) {
            return new WorldEditManager();
        } else {
            return new VanillaBlocksManager();
        }
    }

    public static DisguiseManager getDisguises() {
        if (isLibsDisguisesEnabled) {
            return new LibsDisguises();
        } else {
            return new DisabledDisguises();
        }
    }

    public static IgnoreManager getIgnoreManager() {
        if (isEssentialsEnabled) {
            return new EssentialsIgnore();
        } else {
            return new DisabledIgnore();
        }
    }
}
