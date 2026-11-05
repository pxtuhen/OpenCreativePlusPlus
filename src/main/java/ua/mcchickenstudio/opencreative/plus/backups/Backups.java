/*
 * OpenCreative++, fork of OpenCreative+ (C) 2022-2026, McChicken Studio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.plus.backups;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Backups of players' worlds (config.yml → backups): plugins/OpenCreativePP/backups/&lt;world id&gt;/&lt;date&gt;.zip.
 * <p>
 * Archive contains "build/" (world folder with settings, code, variables) and "dev/" (coding world).
 * Restore unloads the world, replaces both folders and lets the world load again on next join.
 */
public final class Backups {

    private static final PlusLog log = PlusLog.module("Бэкапы");
    private static final Set<Integer> busy = Collections.synchronizedSet(new HashSet<>());
    private static BukkitTask task;

    private Backups() {
    }

    private static @NotNull FileConfiguration config() {
        return OpenCreative.getPlugin().getConfig();
    }

    public static boolean isEnabled() {
        return config().getBoolean("backups.enabled", true);
    }

    public static @NotNull File folder(@NotNull Planet planet) {
        return new File(new File(OpenCreative.getPlugin().getDataFolder(), "backups"), String.valueOf(planet.getId()));
    }

    public static boolean isBusy(@NotNull Planet planet) {
        return busy.contains(planet.getId());
    }

    /**
     * Newest first.
     */
    public static @NotNull List<File> list(@NotNull Planet planet) {
        File[] files = folder(planet).listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return new ArrayList<>();
        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparingLong(File::lastModified).reversed());
        return list;
    }

    // ------------------------------------------------------------------ auto backups

    public static void start() {
        stop();
        if (!isEnabled()) return;
        long hours = Math.max(1, config().getLong("backups.interval-hours", 6));
        task = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), Backups::autoBackup, hours * 72000L, hours * 72000L);
    }

    public static void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    /**
     * Backs up worlds, that were active after their last backup, one every 5 seconds.
     */
    private static void autoBackup() {
        List<Planet> queue = new ArrayList<>();
        for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
            List<File> backups = list(planet);
            long last = backups.isEmpty() ? 0 : backups.getFirst().lastModified();
            if (planet.getLastActivityTime() > last || planet.isLoaded()) queue.add(planet);
        }
        for (int i = 0; i < queue.size(); i++) {
            Planet planet = queue.get(i);
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> create(planet, "auto", null), i * 100L);
        }
        if (!queue.isEmpty()) log.debug("Автобэкап: в очереди миров " + queue.size());
    }

    // ------------------------------------------------------------------ create

    /**
     * Creates a backup. Must be called on the main thread.
     *
     * @param done called on the main thread with the archive, or null if it failed.
     */
    public static void create(@NotNull Planet planet, @NotNull String reason, @Nullable Consumer<File> done) {
        if (!busy.add(planet.getId())) {
            if (done != null) done.accept(null);
            return;
        }
        try {
            // Save everything, that OpenCreative+ keeps in memory until unload.
            if (planet.isLoaded() && planet.getTerritory().getWorld() != null) planet.getTerritory().getWorld().save();
            if (planet.getDevPlanet().isLoaded() && planet.getDevPlanet().getWorld() != null) planet.getDevPlanet().getWorld().save();
            if (planet.getVariables().isLoaded()) planet.getVariables().save();
            planet.getConfiguration().save();
        } catch (Exception error) {
            log.error("Мир " + planet.getId() + ": не удалось сохранить перед бэкапом", error);
        }
        File build = FileUtils.getPlanetFolder(planet);
        File dev = FileUtils.getDevPlanetFolder(planet.getDevPlanet());
        // Config is saved asynchronously by OpenCreative+, give it a moment.
        Bukkit.getScheduler().runTaskLaterAsynchronously(OpenCreative.getPlugin(), () -> {
            File archive = null;
            try {
                File target = folder(planet);
                if (!target.exists() && !target.mkdirs()) throw new IOException("не создать папку " + target);
                String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
                        + (reason.equals("auto") ? "" : "_" + reason.replaceAll("[^a-zA-Z0-9-]", "")) + ".zip";
                archive = new File(target, name);
                long maxBytes = config().getLong("backups.max-size-mb", 200) * 1024 * 1024;
                try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))) {
                    addFolder(zip, build.toPath(), "build/");
                    if (dev.exists()) addFolder(zip, dev.toPath(), "dev/");
                }
                if (maxBytes > 0 && archive.length() > maxBytes) {
                    Files.deleteIfExists(archive.toPath());
                    throw new IOException("архив больше " + config().getLong("backups.max-size-mb", 200) + " МБ (backups.max-size-mb)");
                }
                prune(planet);
                log.debug("Мир " + planet.getId() + ": бэкап " + archive.getName() + " (" + (archive.length() / 1024) + " КБ)");
            } catch (Exception error) {
                log.error("Мир " + planet.getId() + ": бэкап не создан", error);
                archive = null;
            }
            File result = archive;
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                busy.remove(planet.getId());
                if (done != null) done.accept(result);
            });
        }, 10L);
    }

    private static void addFolder(@NotNull ZipOutputStream zip, @NotNull Path root, @NotNull String prefix) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String relative = root.relativize(path).toString().replace(File.separatorChar, '/');
                if (relative.endsWith("session.lock")) continue;
                zip.putNextEntry(new ZipEntry(prefix + relative));
                try {
                    Files.copy(path, zip);
                } catch (IOException locked) {
                    // file in use (rare on Windows): skip, world is saved anyway
                }
                zip.closeEntry();
            }
        }
    }

    private static void prune(@NotNull Planet planet) {
        int keep = Math.max(1, config().getInt("backups.keep", 5));
        List<File> backups = list(planet);
        for (int i = keep; i < backups.size(); i++) {
            try {
                Files.deleteIfExists(backups.get(i).toPath());
            } catch (IOException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ restore

    /**
     * Restores the world from archive: makes a "before-restore" backup, unloads both worlds,
     * replaces folders. Must be called on the main thread.
     */
    public static @NotNull CompletableFuture<Boolean> restore(@NotNull Planet planet, @NotNull File archive) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (!archive.isFile() || isBusy(planet)) {
            future.complete(false);
            return future;
        }
        create(planet, "before-restore", saved -> {
            if (!busy.add(planet.getId())) {
                future.complete(false);
                return;
            }
            planet.getTerritory().setIgnoreUnloading(true);
            for (Player player : new ArrayList<>(planet.getPlayers())) PlayerUtils.teleportToLobby(player);
            if (planet.getDevPlanet().getWorld() != null) {
                for (Player player : new ArrayList<>(planet.getDevPlanet().getWorld().getPlayers())) PlayerUtils.teleportToLobby(player);
            }
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> unloadAndReplace(planet, archive, future, 0), 10L);
        });
        return future;
    }

    private static void unloadAndReplace(@NotNull Planet planet, @NotNull File archive, @NotNull CompletableFuture<Boolean> future, int attempt) {
        World build = Bukkit.getWorld(planet.getWorldName());
        World dev = Bukkit.getWorld(planet.getDevPlanet().getWorldName());
        if (build != null || dev != null) {
            if (attempt == 0) {
                try {
                    planet.getTerritory().clearData();
                } catch (Exception ignored) {
                }
            }
            if (attempt >= 15) {
                busy.remove(planet.getId());
                planet.getTerritory().setIgnoreUnloading(false);
                log.warn("Мир " + planet.getId() + " не выгрузился за 15 секунд, откат отменён.");
                future.complete(false);
                return;
            }
            if (build != null) OpenCreative.getWorldManager().unloadWorld(build, false, planet);
            if (dev != null) OpenCreative.getWorldManager().unloadWorld(dev, false, planet);
            Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> unloadAndReplace(planet, archive, future, attempt + 1), 20L);
            return;
        }
        planet.getConfiguration().discard();
        File buildFolder = FileUtils.getPlanetFolder(planet);
        File devFolder = FileUtils.getDevPlanetFolder(planet.getDevPlanet());
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            boolean ok;
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(buildFolder);
                org.apache.commons.io.FileUtils.deleteDirectory(devFolder);
                unzip(archive, buildFolder, devFolder);
                FileUtils.deleteUnnecessaryWorldFiles(buildFolder);
                if (devFolder.exists()) FileUtils.deleteUnnecessaryWorldFiles(devFolder);
                ok = true;
            } catch (Exception error) {
                log.error("Мир " + planet.getId() + ": ошибка отката из " + archive.getName(), error);
                ok = false;
            }
            boolean result = ok;
            Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
                try {
                    planet.getInformation().loadInformation();
                } catch (Exception ignored) {
                }
                planet.getTerritory().setIgnoreUnloading(false);
                busy.remove(planet.getId());
                log.info("Мир " + planet.getId() + " восстановлен из " + archive.getName() + (result ? "" : " с ошибками"));
                future.complete(result);
            });
        });
    }

    private static void unzip(@NotNull File archive, @NotNull File buildFolder, @NotNull File devFolder) throws IOException {
        Path buildRoot = buildFolder.toPath().toAbsolutePath().normalize();
        Path devRoot = devFolder.toPath().toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                Path root;
                if (name.startsWith("build/")) {
                    root = buildRoot;
                    name = name.substring(6);
                } else if (name.startsWith("dev/")) {
                    root = devRoot;
                    name = name.substring(4);
                } else continue;
                Path target = root.resolve(name).normalize();
                if (!target.startsWith(root)) continue; // zip slip protection
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
