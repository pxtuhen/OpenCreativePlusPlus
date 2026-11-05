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

package ua.mcchickenstudio.opencreative.managers.stability;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.Toggleable;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;
import static ua.mcchickenstudio.opencreative.utils.world.WorldUtils.isDevPlanet;

/**
 * <h1>Watchdog</h1>
 * This class represents a stability manager, called Watchdog, that
 * logs about TPS stability and memory usage.
 */
public final class Watchdog implements StabilityManager, Toggleable {

    private final Deque<Long> tickTimes = new ArrayDeque<>();
    private volatile long lastTickTime = System.currentTimeMillis();
    private final ScheduledExecutorService spectatorExecutor = Executors.newSingleThreadScheduledExecutor();

    private FileStore STORAGE_VOLUME;
    private BukkitTask runnable;
    private StabilityState pluginState = StabilityState.FINE;
    private StabilityState databaseState = StabilityState.FINE;
    private StabilityState storageState = StabilityState.FINE;
    private StabilityState memoryState = StabilityState.FINE;
    private StabilityState ticksState = StabilityState.FINE;

    private long lastWatchdogNotificationTime = 0L;
    private long lastTicksNotificationTime = 0L;
    private long lastMemoryNotificationTime = 0L;
    private long lastStorageNotificationTime = 0L;
    private long lastDatabaseNotificationTime = 0L;
    private long lastStableTPSTime = 0L;

    @Override
    public void start() {
        lastStableTPSTime = System.currentTimeMillis();
        try {
            STORAGE_VOLUME = Files.getFileStore(Paths.get("."));
        } catch (IOException ignored) {
            STORAGE_VOLUME = null;
        }
        if (runnable != null) {
            runnable.cancel();
        }
        Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
            spectatorExecutor.scheduleAtFixedRate(this::checkServerTicks, 1, 1, TimeUnit.SECONDS);
        }, 300L);
        runnable = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), () -> {
            lastTickTime = System.currentTimeMillis();
            long now = System.nanoTime();
            tickTimes.addLast(now);
            long cutoff = now - 5_000_000_000L; // 5 seconds
            while (!tickTimes.isEmpty() && tickTimes.peekFirst() < cutoff) {
                tickTimes.removeFirst();
            }
        }, 150L, 1L);
    }

    /**
     * Returns a string with stack trace of currently used
     * methods of plugin in the main server thread.
     *
     * @return string with plugin's stack trace.
     */
    private @NotNull String dumpStackTrace() {
        Thread thread = Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals("Server thread")).findFirst().orElse(null);
        if (thread == null) {
            return "No server thread found.";
        }
        boolean changed = false;
        StringBuilder builder = new StringBuilder("Stack trace:");
        for (StackTraceElement element : thread.getStackTrace()) {
            if (element.getClassName().startsWith("ua.mcchickenstudio.opencreative.")) {
                changed = true;
                builder.append("\n ")
                        .append(element.getClassName().replace("ua.mcchickenstudio.opencreative.", ""))
                        .append("#")
                        .append(element.getMethodName())
                        .append(":")
                        .append(element.getLineNumber());
            }
        }
        if (changed) {
            return builder.toString();
        }
        return "No stack trace found.";
    }

    /**
     * Returns a string with details of loaded planets:
     * online, uptime, creation time, mode, owner.
     *
     * @return string with loaded planets' info.
     */
    private @NotNull String dumpPlanets() {
        long now = System.currentTimeMillis();
        Set<Planet> loadedPlanets = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            if (isDevPlanet(world)) continue;
            Planet planet = OpenCreative.getPlanetsManager().getPlanetByWorld(world);
            if (planet == null) continue;
            if (!isDevPlanet(world)) {
                loadedPlanets.add(planet);
            }
        }
        if (loadedPlanets.isEmpty()) {
            return "No loaded planets.";
        }
        StringBuilder builder = new StringBuilder("Loaded planets (" + loadedPlanets.size() + "): ");
        for (Planet planet : loadedPlanets) {
            builder.append("\n ").append(planet.getId()).append(" - Players (").append(planet.getInformation().getAsyncOnline()).append(") - ").append(planet.getMode() == Planet.Mode.PLAYING ? "Play" : "Build").append(" - Uptime: ").append(convertTime(now - planet.getLastActivityTime())).append(" - Created: ").append(getElapsedTime(now, planet.getCreationTime())).append(" by ").append(planet.getOwnerName());
            if (planet.getMode() == Planet.Mode.PLAYING) {
                builder.append(" - ").append(planet.getVariables().getTotalVariablesAmount()).append(" variables, ").append(planet.getTerritory().getScript().getExecutors().getExecutorsAmount()).append(" events, ").append(planet.getTerritory().getScript().getExecutors().getActionsAmount()).append(" actions. ");
            }

        }
        return builder.toString();
    }

    /**
     * Returns a string with list of online players.
     *
     * @return loaded players.
     */
    private @NotNull String dumpPlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            return "No online players.";
        }
        StringBuilder builder = new StringBuilder("Online players (" + players.size() + "): ");
        StringJoiner joiner = new StringJoiner(", ");
        for (Player player : players) {
            joiner.add(player.getName());
        }
        builder.append(joiner);
        return builder.toString();
    }

    @Override
    public boolean isWorking() {
        return true;
    }

    @Override
    public void shutdown() {
        if (runnable != null) {
            runnable.cancel();
        }
        spectatorExecutor.shutdownNow();
    }

    @Override
    public @NotNull StabilityState getDatabaseState() {
        return databaseState;
    }

    @Override
    public @NotNull StabilityState getMemoryState() {
        return memoryState;
    }

    @Override
    public @NotNull StabilityState getStorageState() {
        return storageState;
    }

    @Override
    public @NotNull StabilityState getTicksState() {
        return ticksState;
    }

    @Override
    public @NotNull String getName() {
        return "Stability Watchdog";
    }

    /**
     * Returns available space on storage (in megabytes).
     *
     * @return available space on storage.
     */
    private long getAvailableSpace() {
        return (getTotalSpace() - getUsedSpace()) / 1000000;
    }

    /**
     * Returns used space on storage (in bytes).
     *
     * @return used space on storage, or 1 - if failed to get.
     */
    private long getUsedSpace() {
        if (STORAGE_VOLUME == null) {
            return 1;
        }
        try {
            long total = STORAGE_VOLUME.getTotalSpace();
            return total - STORAGE_VOLUME.getUsableSpace();
        } catch (IOException e) {
            return 1;
        }
    }

    /**
     * Returns total space of storage (in bytes).
     *
     * @return total space of storage, or 1 - if failed to get.
     */
    private long getTotalSpace() {
        if (STORAGE_VOLUME == null) {
            return 1;
        }
        try {
            return STORAGE_VOLUME.getTotalSpace();
        } catch (IOException e) {
            return 1;
        }
    }

    /**
     * Checks server's stability: TPS, memory, storage, and notifies about it into console.
     */
    private void checkServerTicks() {

        long now = System.currentTimeMillis();
        long passedTimeFromLastTick = now - lastTickTime;
        double tps = getTPS();

        long heapSize = Runtime.getRuntime().totalMemory();
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        long freeMemory = Runtime.getRuntime().freeMemory() / 1000000;

        if (heapSize > heapMaxSize) {
            if (now - lastMemoryNotificationTime > 5_000) {
                OpenCreative.getPlugin().getLogger().warning("[WATCHDOG] A lot of memory was used :(");
                memoryState = StabilityState.NOT_OKAY;
                lastMemoryNotificationTime = now;
            }
        } else {
            if (freeMemory >= 100) { // 100 MB
                memoryState = StabilityState.FINE;
            } else if (freeMemory >= 50) { // 50 MB
                memoryState = StabilityState.NOT_OKAY;
            } else {
                memoryState = StabilityState.NIGHTMARE;
            }
            if (memoryState != StabilityState.FINE && now - lastMemoryNotificationTime > 5_000) {
                OpenCreative.getPlugin().getLogger().warning("[WATCHDOG] Too few memory (" + freeMemory + " MB) left :(");
                lastMemoryNotificationTime = now;
            }
        }

        long availableSpace = getAvailableSpace();
        if (availableSpace >= 200) { // 200 MB
            storageState = StabilityState.FINE;
        } else if (availableSpace >= 100) { // 100 MB
            storageState = StabilityState.NOT_OKAY;
        } else {
            storageState = StabilityState.NIGHTMARE;
        }
        if (storageState != StabilityState.FINE && now - lastStorageNotificationTime > 5_000) {
            OpenCreative.getPlugin().getLogger().warning("[WATCHDOG] Too few space (" + availableSpace + " MB) left on storage :(");
            lastStorageNotificationTime = now;
        }

        if (OpenCreative.getPlanetsManager().isStableConnection()) {
            databaseState = StabilityState.FINE;
        } else if (OpenCreative.getPlanetsManager().isWorking()) {
            databaseState = StabilityState.NOT_OKAY;
        } else {
            databaseState = StabilityState.NIGHTMARE;
        }
        if (databaseState != StabilityState.FINE && now - lastDatabaseNotificationTime > 5_000) {
            OpenCreative.getPlugin().getLogger().warning("[WATCHDOG] Database is busy :(");
            lastDatabaseNotificationTime = now;
        }

        if (tps >= 17) {
            if (ticksState != StabilityState.FINE) {
                sendGreenLog("TPS is normal now :) " + "§8(passed: " + (now - lastStableTPSTime) / 1000 + " seconds)");
            }
            lastStableTPSTime = now;
            ticksState = StabilityState.FINE;
        } else if (tps >= 13) {
            ticksState = StabilityState.NOT_OKAY;
        } else {
            ticksState = StabilityState.NIGHTMARE;
        }
        if (ticksState != StabilityState.FINE && now - lastTicksNotificationTime > 6_000) {
            String planetsDump = dumpPlanets();
            String playersDump = dumpPlayers();
            String stacktrace = dumpStackTrace();
            OpenCreative.getPlugin().getLogger().warning("[WATCHDOG] Server is overloaded, TPS"
                    + (tps <= 1 ? " is too low. :(" : ": " + tps + "/20 :(\n \n" + planetsDump + "\n \n" + playersDump + "\n" + stacktrace));
            lastTicksNotificationTime = now;
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("%tps%", tps);
            placeholders.put("%planets%", planetsDump);
            placeholders.put("%players%", playersDump);
            placeholders.put("%freememory%", freeMemory);
            placeholders.put("%freestorage%", availableSpace);
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> OpenCreative.getSettings().getCommands().execute(null, "onWatchdogServerOverload", placeholders));
        }

        StabilityState oldState = pluginState;
        if (storageState == databaseState && databaseState == memoryState && memoryState == ticksState) {
            pluginState = storageState;
        } else {
            pluginState = StabilityState.NIGHTMARE;
        }

        if (now - lastWatchdogNotificationTime > 10_000 && passedTimeFromLastTick >= 8_000) {
            lastWatchdogNotificationTime = now;
            OpenCreative.getPlugin().getLogger().warning(String.join("\n", "-------- OPENCREATIVE+ WATCHDOG --------", "Server has not responded for " + passedTimeFromLastTick / 1000 + " seconds :(", "", "  TPS: " + ticksState.getName() + " (" + tps + "/20)", "  Memory: " + memoryState.getName() + " (" + freeMemory + " MB free)", "  Storage: " + storageState.getName() + " (" + availableSpace + " MB available)", "  Database: " + databaseState.getName(), "", dumpPlanets(), dumpPlayers(), dumpStackTrace(), "-------- --------  --------"));
        }

        if (pluginState == StabilityState.NIGHTMARE) {
            if (oldState != StabilityState.NIGHTMARE && OpenCreative.getSettings().getWatchdogSettings().shouldUnloadWorldsWhenUnstable()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(getLocaleMessage("creative.stability.unload"));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        Sounds.MAINTENANCE_START.play(onlinePlayer);
                        for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
                            if (planet.isLoaded()) {
                                planet.getTerritory().unload();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sends green-colored message into console.
     *
     * @param log message to send.
     */
    private void sendGreenLog(@NotNull String log) {
        Bukkit.getConsoleSender().sendMessage("§a§l[" + OpenCreative.getPlugin().getLogger().getName() + "] [WATCHDOG] " + log);
    }

    /**
     * Returns average TPS value in the latest 5 seconds.
     *
     * @return average TPS.
     */
    public double getTPS() {
        long now = System.nanoTime();
        long cutoff = now - 5_000_000_000L;
        while (!tickTimes.isEmpty() && tickTimes.peekFirst() < cutoff) {
            tickTimes.removeFirst();
        }
        int ticks = tickTimes.size();
        return ticks / 5.0;
    }

    @Override
    public @NotNull StabilityState getState() {
        if (!OpenCreative.getSettings().getWatchdogSettings().shouldLimitOperationsWhenUnstable()) {
            return StabilityState.FINE;
        }
        return pluginState;
    }
}
