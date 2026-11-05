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

package ua.mcchickenstudio.opencreative.coding.blocks.executors.other;

import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.blocks.events.WorldEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.ExecutorCategory;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCodingDebugExecutor;

/**
 * <h1>Cycle</h1>
 * This class represents cycle, that executes actions
 * after passing a time, like timer.
 */
public final class Cycle extends NameableExecutor {

    private int repeatTime;
    private boolean enabled = false;
    private BukkitRunnable runnable = null;

    public Cycle() {
        super("cycle", ExecutorCategory.CYCLE);
    }

    public void setRepeatTime(int repeatTime) {
        this.repeatTime = repeatTime;
    }

    @Override
    public void run(@NotNull WorldEvent event) {
        if (!enabled) {
            enabled = true;
            Executor executor = this;
            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    sendCodingDebugExecutor(executor);
                    executeActions(event);
                }
            };
            getPlanet().getTerritory().addBukkitRunnable(runnable);
            runnable.runTaskTimer(OpenCreative.getPlugin(), 0, repeatTime);
        }
    }

    public void stop() {
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public @NotNull String getName() {
        return "Cycle";
    }

    @Override
    public @NotNull String getExtensionId() {
        return "default";
    }

    @Override
    public @NotNull String getDescription() {
        return "Repeats executing actions with specified period";
    }

}
