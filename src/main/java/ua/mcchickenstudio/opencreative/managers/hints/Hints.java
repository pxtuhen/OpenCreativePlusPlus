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

package ua.mcchickenstudio.opencreative.managers.hints;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionCategory;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.ExecutorCategory;
import ua.mcchickenstudio.opencreative.coding.variables.ValueType;
import ua.mcchickenstudio.opencreative.listeners.player.ChangedWorld;
import ua.mcchickenstudio.opencreative.managers.Startable;
import ua.mcchickenstudio.opencreative.utils.MessageUtils;

import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isEntityInDevPlanet;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isEntityInLobby;

public final class Hints implements HintManager, Startable {

    @Override
    public void checkForHints(@NotNull Player player) {
        if (!isEntityInDevPlanet(player)) {
            if (player.getInventory().getItemInMainHand().getType() != Material.PAPER) {
                return;
            }
            if (ChangedWorld.isPlayerWithLocation(player)) {
                // If player is in build world and they're setting location
                player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints.location.build", player));
            }
            return;
        }
        Block block = player.getTargetBlockExact(5);
        if (block != null && block.getBlockData() instanceof WallSign) {
            // Looking at sign of coding block
            Block farBlock = block.getRelative(BlockFace.NORTH);
            ExecutorCategory executor = ExecutorCategory.getByMaterial(farBlock.getType());
            if (executor != null) {
                String hint = switch (executor) {
                    case ExecutorCategory.CYCLE -> "cycle";
                    case ExecutorCategory.METHOD -> "method";
                    case ExecutorCategory.FUNCTION -> "function";
                    default -> "event";
                };
                player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints." + hint, player));
                return;
            }
            ActionCategory action = ActionCategory.getByMaterial(farBlock.getType());
            if (action != null) {
                String hint = switch (action) {
                    case ActionCategory.LAUNCH_FUNCTION_ACTION -> "launch-function";
                    case ActionCategory.LAUNCH_METHOD_ACTION -> "launch-method";
                    default -> {
                        if (action.isCondition()) yield "condition";
                        yield "action";
                    }
                };
                player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints." + hint, player));
            }
            return;
        }
        // Holding item
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            if (player.getInventory().getItemInOffHand().isEmpty()) {
                player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints.saved-inventory", player));
            }
            return;
        }
        String hint = switch (item.getType()) {
            case COMPARATOR -> "lines-controller";
            case FEATHER -> "fly-speed-changer";
            case ARROW -> "arrow-not";
            case REDSTONE_TORCH -> "debug-torch";
            case IRON_INGOT -> "values";
            case SPLASH_POTION, LINGERING_POTION -> "potion";
            default -> "";
        };
        if (!hint.isEmpty()) {
            player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints." + hint, player));
            return;
        }
        ValueType type = ValueType.getByMaterial(item.getType());
        if (type != null) {
            hint = switch (type) {
                case TEXT, NUMBER, EVENT_VALUE, VECTOR, POTION, PARTICLE, VARIABLE, BOOLEAN, COLOR ->
                        type.name().toLowerCase().replace("_", "-");
                case LOCATION -> "location.dev";
                default -> "";
            };
            if (!hint.isEmpty()) {
                player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints." + hint, player));
                return;
            }
        }
        ExecutorCategory event = ExecutorCategory.getByMaterial(item.getType());
        if (event != null) {
            player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints.holding-event", player));
            return;
        }
        ActionCategory action = ActionCategory.getByMaterial(item.getType());
        if (action != null) {
            player.sendActionBar(MessageUtils.getPlayerLocaleComponent("environment.hints.holding-action", player));
        }
    }

    @Override
    public void start() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isEntityInLobby(player)) {
                        continue;
                    }
                    checkForHints(player);
                }
            }
        };
        runnable.runTaskTimer(OpenCreative.getPlugin(), 40L, 40L);
    }

    @Override
    public @NotNull String getName() {
        return "Default Hints";
    }
}
