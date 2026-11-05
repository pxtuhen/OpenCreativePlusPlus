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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.repeatactions;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.*;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooManyRepeatsException;
import ua.mcchickenstudio.opencreative.planets.PlanetRunnable;

import java.util.List;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendPlanetCodeCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendPlanetCodeErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * <h1>RepeatAction</h1>
 * This class represents a repeat action, that
 * will execute actions for many times while
 * condition is met.
 */
public abstract class RepeatAction extends MultiAction {

    private int calls = 0;
    private boolean mustStop = false;
    private ActionsHandler internalHandler;
    private long callsResetTime = 0;

    public RepeatAction(Executor executor, Target target, int x, Arguments args, List<Action> actions) {
        super(executor, target, x, args, actions);
    }

    @Override
    protected void execute(Entity entity) {
        this.entity = entity;
        if (mustStop || !checkCanContinue()) {
            getHandler().executeNextAction();
            mustStop = false;
            return;
        }
        internalHandler = new ActionsHandler(this);
        internalHandler.executeActions(getActions());
    }

    public void repeat() {
        try {
            increaseCalls();
        } catch (TooManyRepeatsException exception) {
            if (getPlanet().getLimits().isTooManyCodingErrors()) {
                getPlanet().getTerritory().getScript().getExecutors().stopCode("errors limit");
                sendPlanetCodeCriticalErrorMessage(getPlanet(), getExecutor(), getLocaleMessage("coding-error.errors-limit", false).replace("%limit%", String.valueOf(getPlanet().getLimits().getCodingErrorsLimit())));
                return;
            }
            getPlanet().getVariables().garbageCollector(getHandler().getMainActionHandler());
            sendPlanetCodeErrorMessage(getExecutor(), this, getLocaleMessage("coding-error.toomanyrepeatsexception"), exception);
            return;
        }
        if (internalHandler == null) return;
        if (internalHandler.getWaitDelay() > 0) {
            getPlanet().getTerritory().scheduleRunnable(new PlanetRunnable(getPlanet()) {
                @Override
                public void execute() {
                    RepeatAction.this.execute(entity);
                }
            }, internalHandler.getWaitDelay());
        } else {
            execute(entity);
        }
    }

    /**
     * Increases call by 1 and checks limits.
     */
    public void increaseCalls() {
        long now = System.currentTimeMillis();
        if (now - callsResetTime > 1000) {
            calls = 0;
            callsResetTime = now;
        }
        calls++;
        if (calls > getPlanet().getLimits().getRepeatsAmountLimit()) {
            throw new TooManyRepeatsException();
        }
    }

    /**
     * Sets whether repeat action should stop repeating itself.
     *
     * @param mustStop true - stop repeater, false - continue.
     */
    public void setMustStop(boolean mustStop) {
        this.mustStop = mustStop;
    }

    /**
     * Checks whether repeat action can continue repeating and
     * executing the same actions.
     *
     * @return true - can continue and execute actions, false - stop repeater.
     */
    public abstract boolean checkCanContinue();

    /**
     * Returns a copied repeat action.
     *
     * @return copy of repeat action.
     */
    public @NotNull RepeatAction copy() {
        try {
            return getClass().getConstructor(Executor.class, Target.class, int.class,
                    Arguments.class, List.class).newInstance(getExecutor(), getTarget(), getX(),
                    getArguments().copy(), getActions());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to copy repeat action: " + getClass().getName(), exception);
        }
    }

    @Override
    public @NotNull ActionCategory getActionCategory() {
        return ActionCategory.REPEAT_ACTION;
    }
}
