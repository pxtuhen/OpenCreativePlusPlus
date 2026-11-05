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

package ua.mcchickenstudio.opencreative.coding.blocks.executors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.*;
import ua.mcchickenstudio.opencreative.coding.blocks.conditions.Condition;
import ua.mcchickenstudio.opencreative.coding.blocks.events.WorldEvent;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.entity.interaction.EntitySpawnedExecutor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.entity.state.PlayerAirChangedExecutor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.other.Cycle;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.other.Function;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.other.Method;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.other.NameableExecutor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.player.interaction.*;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.player.movement.PlayerJumpExecutor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.player.movement.PlayerWalkExecutor;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.world.blocks.WorldBlockFluidChangedExecutor;
import ua.mcchickenstudio.opencreative.coding.variables.ValueType;
import ua.mcchickenstudio.opencreative.planets.Planet;

import java.io.File;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.*;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;

/**
 * <h1>PlanetExecutors</h1>
 * This class represents Executors in every planet code script.
 *
 * @author McChicken Studio
 * @version 6.0
 * @since 5.0
 */
public class PlanetExecutors {

    protected final Planet planet;

    private final List<Cycle> cycles = new ArrayList<>();
    private final List<Function> functions = new ArrayList<>();
    private final List<Method> methods = new ArrayList<>();
    private final Map<Class<? extends WorldEvent>, List<EventAwaiter>> events = new HashMap<>();
    private int executorsAmount = 0;
    private int actionsAmount = 0;

    public PlanetExecutors(Planet planet) {
        this.planet = planet;
    }

    /**
     * Finds executor for world event and activates it, if found.
     *
     * @param event event to activate executor.
     */
    public static void activate(@NotNull WorldEvent event) {
        Planet planet = event.getPlanet();
        if (!OpenCreative.getSettings().getCodingSettings().isEnabled()) return;
        if (planet == null) return;
        if (planet.getMode() != Planet.Mode.PLAYING) return;
        PlanetExecutors executors = planet.getTerritory().getScript().getExecutors();
        List<EventAwaiter> list = executors.events.get(event.getClass());
        if (list == null) return;
        for (EventAwaiter awaiter : list) {
            activate((Executor) awaiter, event);
        }
    }

    /**
     * Calls executor that executes actions.
     *
     * @param executor executor to call.
     * @param event    event of executor.
     */
    public static void activate(@NotNull Executor executor, @NotNull WorldEvent event) {
        Planet planet = executor.getPlanet();
        if (planet.getMode() != Planet.Mode.PLAYING) return;
        if (canRunExecutor(planet, executor)) {
            executor.run(event);
        }
    }

    public static boolean canRunExecutor(@NotNull Planet planet, @NotNull Executor executor) {
        if (executor instanceof PlayerWalkExecutor || executor instanceof PlayerAirChangedExecutor
                || executor instanceof WorldBlockFluidChangedExecutor || executor instanceof PlayerDestroyBlockExecutor
                || executor instanceof PlayerPlaceBlockExecutor || executor instanceof EntitySpawnedExecutor
                || executor instanceof PlayerInteractExecutor || executor instanceof PlayerLeftClickExecutor
                || executor instanceof PlayerRightClickExecutor || executor instanceof PlayerJumpExecutor) {
            if (executor.getLastCalls() >= planet.getLimits().getCodeOperationsLimit()) {
                planet.getTerritory().getScript().getExecutors().stopCode("operations limit");
                sendPlanetCodeCriticalErrorMessage(planet, executor, getLocaleMessage("coding-error.operations-limit", false)
                        .replace("%limit%", String.valueOf(planet.getLimits().getCodeOperationsLimit())));
                return false;
            }
            executor.increaseCall();
            new BukkitRunnable() {
                @Override
                public void run() {
                    executor.decreaseCall();
                }
            }.runTaskLater(OpenCreative.getPlugin(), 35L);
            return true;
        }
        int limit = planet.getLimits().getCodeOperationsLimit();
        int depth = StackWalker.getInstance().walk(stream ->
                (int) stream.limit(limit + 1).count()
        );
        if (depth > limit) {
            planet.getTerritory().getScript().getExecutors().stopCode("operations limit");
            sendPlanetCodeCriticalErrorMessage(planet, executor, getLocaleMessage("coding-error.operations-limit", false)
                    .replace("%limit%", String.valueOf(limit)));
            return false;
        }
        return true;
    }

    /**
     * Clears temporary data: executors list, last executors calls.
     */
    public void clear() {
        events.clear();
        functions.clear();
        methods.clear();
        cycles.clear();
        executorsAmount = 0;
        actionsAmount = 0;
    }

    /**
     * Loads executors from script file.
     *
     * @param file script file.
     */
    public void load(File file) {
        executorsAmount = 0;
        actionsAmount = 0;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("code.blocks");
        if (section != null) {
            OpenCreative.getPlugin().getLogger().info("Loading code in planet " + planet.getId() + "...");
            long time = System.currentTimeMillis();
            List<Executor> executors = new ArrayList<>();
            Set<String> keys = section.getKeys(false);
            String path;
            for (String key : keys) {
                path = "code.blocks." + key;
                if (config.getString(path + ".type") != null) {
                    Executor executor = createExecutor(config, path);
                    if (executor != null) {
                        executors.add(executor);
                    }
                }
            }
            int actions = actionsAmount;
            clear();
            actionsAmount = actions;
            for (Executor executor : executors) {
                registerExecutor(executor);
            }
            sendCodingDebugLog(planet, getLocaleMessage("coding-debug.loaded-code", false)
                    .replace("%time%", String.valueOf(Math.floor((System.currentTimeMillis() - time) / 10.0) / 100.0)));
            OpenCreative.getPlugin().getLogger().info("Loaded code in planet " + planet.getId() + " in " + (System.currentTimeMillis() - time) + " ms with " + executors.size() + " executors!");
        } else {
            sendCodingDebugLog(planet, getLocaleMessage("coding-debug.loaded-code", false)
                    .replace("%time%", "0"));
            OpenCreative.getPlugin().getLogger().info("Planet " + planet.getId() + " has no code to load.");
        }
    }

    private void registerExecutor(@NotNull Executor executor) {
        if (executor instanceof EventAwaiter awaiter) {
            events.computeIfAbsent(awaiter.getEventClass(),
                    list -> new ArrayList<>())
                    .add(awaiter);
            executorsAmount++;
        }

        if (executor instanceof Cycle cycle) {
            cycles.add(cycle);
            executorsAmount++;
        }

        if (executor instanceof Function function) {
            functions.add(function);
            executorsAmount++;
        }

        if (executor instanceof Method method) {
            methods.add(method);
            executorsAmount++;
        }
    }

    public @NotNull List<Executor> getExecutorsList() {
        List<Executor> executors = new ArrayList<>();
        for (List<EventAwaiter> list : events.values()) {
            for (EventAwaiter awaiter : list) {
                if (awaiter instanceof Executor executor) {
                    executors.add(executor);
                }
            }
        }
        executors.addAll(cycles);
        executors.addAll(methods);
        executors.addAll(functions);
        return executors;
    }

    /**
     * Returns list of actions inside the condition or multi action.
     *
     * @param action condition or multi action.
     * @return list of inside actions.
     */
    public @NotNull List<Action> getInsideActionsList(@NotNull Action action) {
        List<Action> actions = new ArrayList<>();
        actions.add(action);
        if (action instanceof Condition condition) {
            for (Action inside : condition.getActions()) {
                actions.addAll(getInsideActionsList(inside));
            }
            for (Action inside : condition.getElseActions()) {
                actions.addAll(getInsideActionsList(inside));
            }
        } else if (action instanceof MultiAction multiAction) {
            for (Action inside : multiAction.getActions()) {
                actions.addAll(getInsideActionsList(inside));
            }
        }
        return actions;
    }

    /**
     * Returns list of registered cycle executors.
     *
     * @return list of cycles.
     */
    public @NotNull List<Cycle> getCyclesList() {
        return cycles;
    }

    /**
     * Returns list of registered function executors.
     *
     * @return list of functions.
     */
    public @NotNull List<Function> getFunctionsList() {
        return functions;
    }

    /**
     * Returns list of registered method executors.
     *
     * @return list of methods.
     */
    public @NotNull List<Method> getMethodsList() {
        return methods;
    }

    private int[] getCoords(YamlConfiguration config, String path) {
        int[] coords = new int[3];
        coords[0] = config.getInt(path + ".location.x");
        coords[1] = config.getInt(path + ".location.y");
        coords[2] = config.getInt(path + ".location.z");
        return coords;
    }

    /**
     * Creates an instance of executor.
     *
     * @param config script file.
     * @param path   path of executor.
     * @return instance of executor, or null.
     */
    private Executor createExecutor(@NotNull YamlConfiguration config, @NotNull String path) {
        try {
            int[] coords = getCoords(config, path);
            Executor executor = Executors.getInstance().getById(config.getString(path + ".type", "").toLowerCase());
            if (executor == null) return null;
            executor = executor.getClass().getDeclaredConstructor().newInstance();
            if (executor instanceof Cycle cycle) {
                String name = config.getString(path + ".name");
                int time = config.getInt(path + ".time");
                /*
                 * We will not create cycle without name,
                 * because it can't be called in code.
                 */
                if (time < 5 || time > 3600) {
                    time = 20;
                }
                if (name != null) {
                    cycle.setCallName(name);
                    cycle.setRepeatTime(time);
                    cycle.init(planet, coords[0], coords[1], coords[2]);
                }
            } else if (executor instanceof NameableExecutor nameable) {
                String name = config.getString(path + ".name");
                /*
                 * We will not create function or method without name,
                 * because it can't be called in code.
                 */
                if (name != null) {
                    nameable.setCallName(name);
                    nameable.init(planet, coords[0], coords[1], coords[2]);
                }
            } else {
                executor.init(planet, coords[0], coords[1], coords[2]);
            }
            List<Action> allActionsList = createActionList(executor, path + ".actions", config);
            if (!allActionsList.isEmpty()) {
                executor.setActions(allActionsList);
            }
            boolean debug = config.getBoolean(path + ".debug", false);
            if (debug) {
                executor.setDebug(true);
            }
            return executor;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Creates a list of actions for executor.
     *
     * @param executor executor that will store actions.
     * @param path     path of actions inside executor.
     * @param config   script file.
     * @return list of actions, or empty list.
     */
    private @NotNull List<Action> createActionList(@NotNull Executor executor,
                                                   @NotNull String path,
                                                   @NotNull YamlConfiguration config) {
        List<Action> actionList = new ArrayList<>();
        ConfigurationSection actions = config.getConfigurationSection(path);
        if (actions != null) {
            Set<String> actionsBlocks = actions.getKeys(false);
            for (String actionBlock : actionsBlocks) {
                String actionPath = path + "." + actionBlock;
                Action action = createAction(executor, actionPath, config);
                if (action != null) {
                    actionList.add(action);
                    actionsAmount++;
                }
            }
        }
        return actionList;
    }

    /**
     * Creates an instance of action from script.
     *
     * @param executor executor that stores action.
     * @param path     path of action inside executor.
     * @param config   script file.
     * @return instance of action, or null.
     */
    private @Nullable Action createAction(@NotNull Executor executor,
                                          @NotNull String path,
                                          @NotNull YamlConfiguration config) {

        String type = config.getString(path + ".type");
        if (type == null) return null;

        try {
            ActionType actionType = ActionType.valueOf(type);
            Arguments args = new Arguments(executor.getPlanet());
            Target target = Target.DEFAULT;
            String targetString = config.getString(path + ".target");
            if (targetString != null && !targetString.isEmpty()) {
                target = Target.valueOf(targetString);
            }
            ConfigurationSection section = config.getConfigurationSection(path + ".arguments");
            if (section != null) {
                args.load(section);
            }
            if (actionType == ActionType.LAUNCH_FUNCTION || actionType == ActionType.LAUNCH_METHOD) {
                if (config.getString(path + ".name") != null) {
                    args.setArgumentValue("name", ValueType.TEXT, config.getString(path + ".name", " "));
                }
            } else if (actionType == ActionType.SELECTION_SET || actionType == ActionType.SELECTION_ADD || actionType == ActionType.SELECTION_REMOVE) {
                if (config.getConfigurationSection(path + ".condition") != null) {
                    boolean isOpposed = config.getBoolean(path + ".condition.opposed", false);
                    ActionCategory conditionCategory = ActionCategory.valueOf(config.getString(path + ".condition.category"));
                    ActionType conditionType = ActionType.valueOf(config.getString(path + ".condition.type"));
                    return actionType.getActionClass().getConstructor(Executor.class, int.class, Arguments.class, ActionCategory.class, ActionType.class, boolean.class).newInstance(executor, config.getInt(path + ".location.x"), args, conditionCategory, conditionType, isOpposed);
                } else if (config.getString(path + ".target") != null) {
                    if (targetString != null && !targetString.isEmpty()) {
                        target = Target.valueOf(targetString);
                    }
                    return actionType.getActionClass().getConstructor(Executor.class, int.class, Arguments.class, Target.class).newInstance(executor, config.getInt(path + ".location.x"), args, target);
                }
                if (config.getString(path + ".condition.type") != null) {
                    args.setArgumentValue("name", ValueType.TEXT, config.getString(path + ".name", ""));
                }
            }
            if (actionType.getCategory().isMultiAction()) {
                if (actionType.getCategory().isCondition()) {
                    boolean isOpposed = config.getBoolean(path + ".opposed", false);
                    return actionType.getActionClass()
                            .getConstructor(Executor.class, Target.class, int.class, Arguments.class, List.class, List.class, boolean.class)
                            .newInstance(executor, target, config.getInt(path + ".location.x"),
                                    args, createActionList(executor, path + ".actions", config),
                                    createActionList(executor, path + ".else", config), isOpposed);
                } else if (actionType == ActionType.REPEAT_WHILE || actionType == ActionType.REPEAT_WHILE_NOT) {
                    if (config.getConfigurationSection(path + ".condition") != null) {
                        ActionType conditionType = ActionType.valueOf(config.getString(path + ".condition.type"));
                        return actionType.getActionClass().getConstructor(Executor.class, Target.class, int.class,
                                Arguments.class, List.class, ActionType.class).newInstance(executor,
                                target, config.getInt(path + ".location.x"),
                                args, createActionList(executor, path + ".actions", config), conditionType);
                    }
                } else {
                    return actionType.getActionClass().getConstructor(Executor.class, Target.class, int.class, Arguments.class, List.class)
                            .newInstance(executor, target, config.getInt(path + ".location.x"),
                                    args, createActionList(executor, path + ".actions", config));
                }
            }
            return actionType.getActionClass().getConstructor(Executor.class, Target.class, int.class, Arguments.class).newInstance(executor, target, config.getInt(path + ".location.x"), args);
        } catch (Exception error) {
            sendDebugError("Can't create an action", error);
            return null;
        }
    }

    /**
     * Returns amount of executors.
     *
     * @return amount of executors.
     */
    public int getExecutorsAmount() {
        return executorsAmount;
    }

    /**
     * Returns amount of actions and conditions.
     *
     * @return amount of actions and conditions.
     */
    public int getActionsAmount() {
        return actionsAmount;
    }

    /**
     * Stops code in planet by setting its mode to Build
     * and sends log in console. Doesn't call quit events.
     *
     * @param reason reason of stopping the code.
     */
    public void stopCode(@NotNull String reason) {
        if (planet.getMode() == Planet.Mode.BUILD) return;
        OpenCreative.getPlugin().getLogger().info("Planet code has been stopped in " + planet.getId() + " because of " + reason + ".");
        planet.setMode(Planet.Mode.BUILD, true);
    }

}
