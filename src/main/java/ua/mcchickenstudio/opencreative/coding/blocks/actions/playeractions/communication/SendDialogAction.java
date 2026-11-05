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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.communication;

import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.PlayerAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.List;
import java.util.UUID;

public final class SendDialogAction extends PlayerAction {
    public SendDialogAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void executePlayer(@NotNull Player player) {
        List<Component> text = getArguments().getComponentList("messages", this);
        if (text.isEmpty()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        getPlanet().getTerritory().removeConversation(uuid);
        int cooldown = Math.clamp(getArguments().getInt("cooldown", 20, this), 5, 400);
        World planetWorld = getWorld();
        Planet planet = getPlanet();
        BukkitRunnable task = new BukkitRunnable() {
            private int current = 0;

            @Override
            public void run() {
                if (!player.isOnline()
                        || !player.getWorld().equals(planetWorld)
                        || planet.getMode() != Planet.Mode.PLAYING
                        || current >= text.size()) {

                    planet.getTerritory().removeConversation(player.getUniqueId());
                    planet.getTerritory().removeBukkitRunnable(this);
                    cancel();
                    return;
                }
                player.sendMessage(text.get(current));
                current++;
            }
        };
        getPlanet().getTerritory().putConversation(player.getUniqueId(), task);
        planet.getTerritory().addBukkitRunnable(task);
        task.runTaskTimerAsynchronously(OpenCreative.getPlugin(), 0, cooldown);
    }


    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.PLAYER_SEND_DIALOG;
    }
}
