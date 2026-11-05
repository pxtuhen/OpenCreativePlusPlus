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

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionCategory;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.events.player.fighting.KillerVictimEvent;
import ua.mcchickenstudio.opencreative.listeners.player.ChangedWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class TargetAction extends Action6 {

    private Target target;
    private Entity entity;

    public TargetAction(@NotNull String id, @NotNull ActionCategory category) {
        super(id, category);
    }

    @Override
    public void init(@NotNull ActionContext context) {
        super.init(context);
        this.target = context.getTarget();
    }

    public void execute(@Nullable Entity entity) {
        this.entity = entity;
        if (entity != null) {
            executeEntity(entity);
        }
    }

    public abstract void executeEntity(@NotNull Entity entity);

    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns enum of target.
     *
     * @return Enum of target.
     */
    public @NotNull Target getTarget() {
        return target;
    }

    /**
     * Returns list of entities that will execute this action.
     *
     * @return List of entities to execute action.
     */
    protected List<Entity> getTargets() {
        List<Entity> entities = new ArrayList<>();
        List<Entity> eventEntities = getHandler().getEvent().getSelection();
        switch (target) {
            case RANDOM_PLAYER -> {
                Player randomPlayer = null;
                List<Player> playerList = this.getExecutor().getPlanet().getWorld().getPlayers();
                if (!playerList.isEmpty()) {
                    Random r = new Random();
                    int i = r.nextInt(playerList.size());
                    randomPlayer = playerList.get(i);
                }
                if (randomPlayer != null) {
                    entities.add(randomPlayer);
                }
            }
            case ALL_PLAYERS -> {
                List<Player> playerList = this.getExecutor().getPlanet().getWorld().getPlayers();
                if (!playerList.isEmpty()) {
                    entities.addAll(playerList);
                }
            }
            case KILLER -> {
                Entity killer = getKiller();
                if (killer != null) {
                    entities.add(killer);
                }
            }
            case VICTIM -> {
                Entity victim = getVictim();
                if (victim != null) {
                    entities.add(victim);
                }
            }
            case SELECTED -> entities.addAll(getHandler().getSelectedTargets());
            case ALL_ENTITIES -> {
                int amount = 0;
                for (Entity entity : world().getEntities()) {
                    if (amount > planet().getLimits().getEntitiesLimit()) {
                        break;
                    }
                    if (!(entity instanceof Player)) {
                        entities.add(entity);
                        amount++;
                    }
                }
            }
            case RANDOM_TARGET -> {
                List<Entity> selectedTargets = new ArrayList<>(getHandler().getSelectedTargets());
                if (!selectedTargets.isEmpty()) {
                    entities.add(selectedTargets.get(new Random().nextInt(selectedTargets.size())));
                }
            }
            case LAST_SPAWNED -> {
                Entity spawned = getHandler().getMainActionHandler().getLastSpawnedEntity();
                if (spawned != null) {
                    entities.add(spawned);
                }
            }
            default -> entities.addAll(eventEntities);
        }
        entities.removeIf(entity -> entity instanceof Player player && ChangedWorld.isPlayerWithLocation(player));
        //entities.removeIf(entity -> !entity.getWorld().equals(getPlanet().getWorld()));
        int selectionLimit = planet().getLimits().getEntitiesLimit() + planet().getPlayers().size(); // adding players count if entities limit is set to 0
        if (entities.size() > selectionLimit) {
            entities = entities.subList(0, selectionLimit);
        }
        return entities;
    }

    private Entity getVictim() {
        if (getExecutor().getEvent() instanceof KillerVictimEvent victimEvent) {
            return victimEvent.getVictim();
        }
        return null;
    }

    /**
     * Returns entity killer, that involved in damage event.
     *
     * @return Killer, or null if there's no involved entity in damage event.
     */
    private Entity getKiller() {
        if (getExecutor().getEvent() instanceof KillerVictimEvent mobEvent) {
            return mobEvent.getKiller();
        }
        return null;
    }

}
