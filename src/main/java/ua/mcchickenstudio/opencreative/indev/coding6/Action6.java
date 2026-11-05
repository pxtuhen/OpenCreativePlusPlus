/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.indev.coding6;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.ExtensionContent;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.CodingBlock;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Action;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionCategory;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionsHandler;
import ua.mcchickenstudio.opencreative.coding.blocks.events.WorldEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.util.List;
import java.util.Objects;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendPlanetCodeCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

public abstract class Action6 extends Action implements CodingBlock, ExtensionContent {

    private final String id;
    private final ActionCategory category;

    private Executor executor;
    private Arguments arguments;
    private int x;
    private int y;
    private int z;

    private WorldEvent event;
    private ActionsHandler handler;

    public Action6(@NotNull String id, @NotNull ActionCategory category) {
        super(null, null, 1, null);
        this.id = id;
        this.category = category;
    }

    public void init(@NotNull ActionContext context) {
        if (this.executor != null) {
            throw new IllegalStateException("Action is already initialized and associated with executor " + this.executor.getID());
        }
        this.executor = context.getExecutor();
        this.arguments = context.getArguments();
        this.x = context.getX();
        this.y = context.getY();
        this.z = context.getZ();
    }

    public void run(@NotNull ActionsHandler handler) {
        if (this.executor == null) {
            throw new IllegalStateException("Action is not initialized and not associated with any executor.");
        }
        if (isDisabled()) {
            return;
        }
        this.handler = handler;
        this.event = handler.getEvent();
        // TODO: sendCodingDebugAction(this);
        if (this instanceof NoTargetAction action) {
            if (cannotRun(1)) {
                return;
            }
            action.execute();
            return;
        }
        if (!(this instanceof TargetAction targetAction)) {
            return;
        }

        List<Entity> targets = targetAction.getTargets();
        if (cannotRun(targets.size())) {
            return;
        }
        for (Entity entity : targets) {
            if (entity == null) continue;
            if (entity.getWorld().equals(world()) || !OpenCreative.getSettings().getCodingSettings().shouldIgnoreActionsIfEntityNotInWorld()) {
                targetAction.execute(entity);
            }
        }
    }

    private boolean cannotRun(int count) {
        if (planet().getMode() != Planet.Mode.PLAYING) return true;
        if (planet().getLimits().isTooManyActionsAtOnce(count)) {
            planet().getTerritory().getScript().getExecutors().stopCode("actions limit");
            sendPlanetCodeCriticalErrorMessage(planet(), executor, getLocaleMessage("coding-error.actions-limit", false)
                    .replace("%limit%", String.valueOf(planet().getLimits().getCodingActionsCallsLimit())));
            return true;
        }
        return false;
    }

    /**
     * Returns coding block category of this executor.
     *
     * @return category of executor.
     */
    public final @NotNull ActionCategory getBlockCategory() {
        return category;
    }

    /**
     * Returns planet, that was associated with executor.
     *
     * @return planet.
     */
    public final @NotNull Planet planet() {
        return executor.getPlanet();
    }

    public final @NotNull World world() {
        return executor.getPlanet().getWorld();
    }

    /**
     * Returns last world event of executor.
     * <p>
     * When a new event happens, it will be replaced
     * with a new one.
     *
     * @return last world event.
     */
    public final WorldEvent getEvent() {
        return event;
    }

    public final ActionsHandler getHandler() {
        return handler;
    }

    public final boolean isDisabled() {
        return false; // TODO:
    }

    public final @NotNull String getID() {
        return id;
    }

    /**
     * Returns localized name of action.
     *
     * @return localized name.
     */
    public @NotNull String getLocaleName() {
        return getLocaleMessage("items.developer.actions." + id.replace("_", "-") + ".name", false);
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public final int getZ() {
        return z;
    }

    @Override
    public final int hashCode() {
        return (id.toLowerCase() + x + " " + y + " " + z).hashCode();
    }

    @Override
    public String toString() {
        return "Action | Planet: " + planet().getWorldName() + " Coords: " + x + " " + y + " " + z;
    }

    /*public @NotNull Executor getExecutor() {
        return executor;
    }*/

    public @NotNull Arguments arguments() {
        return arguments;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Action6 action)) return false;
        if (action.x != this.x) return false;
        if (action.y != this.y) return false;
        if (action.z != this.z) return false;
        return Objects.equals(action.id, this.id);
    }

    @Override
    public @NotNull ActionType getActionType() {
        return null;
    }

    @Override
    public @NotNull ActionCategory getActionCategory() {
        return null;
    }
}