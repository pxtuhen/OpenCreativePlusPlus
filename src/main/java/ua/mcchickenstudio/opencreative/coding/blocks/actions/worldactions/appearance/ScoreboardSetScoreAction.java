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

package ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.appearance;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.WorldAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooManyScoresException;

import java.util.Locale;

public final class ScoreboardSetScoreAction extends WorldAction {
    public ScoreboardSetScoreAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    protected void execute() {
        arguments.requireArguments(this, "name", "object");
        String name = getArguments().getText("name", "board", this);
        String object = getArguments().getText("object", "board", this);
        int score = getArguments().getInt("score", 0, this);
        Scoreboard scoreboard = getPlanet().getTerritory().getScoreboards().getScoreboard(name.toLowerCase(Locale.ROOT));
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective("score");
        if (objective != null) {
            if (scoreboard.getEntries().size() > 100) {
                throw new TooManyScoresException(name.toLowerCase(Locale.ROOT));
            }
            objective.getScore(object).setScore(score);
        }
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.WORLD_SCOREBOARD_SET_SCORE;
    }
}
