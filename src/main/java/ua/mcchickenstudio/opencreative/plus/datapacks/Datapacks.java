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

package ua.mcchickenstudio.opencreative.plus.datapacks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datapack.Datapack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Datapacks in players' worlds (datapacks.yml).
 * <p>
 * Minecraft loads datapacks for the whole server, so every valid datapack from the library
 * is installed into the main world's datapacks folder. Its functions are run only in worlds,
 * where world owner enabled it: load-function once when Play mode starts, tick-function every tick
 * while world is in Play mode. Functions run as Minecraft runs #tick functions
 * (permission level 2, no output), but positioned in the player's world.
 */
public final class Datapacks {

    private static final PlusLog log = PlusLog.module("Датапаки");
    private static final Pattern ID = Pattern.compile("^[a-z0-9_]+$");
    private static final Pattern FUNCTION = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final String PREFIX = "creativeplus_";

    /**
     * Datapack from library and result of its checks.
     */
    public record Entry(@NotNull String id, @NotNull String file, @NotNull String name, @NotNull List<String> description,
                        @NotNull String icon, @NotNull String loadFunction, @NotNull String tickFunction,
                        @NotNull String permission, @Nullable String problem) {
        public boolean valid() {
            return problem == null;
        }
    }

    private static final Map<String, Entry> library = new LinkedHashMap<>();
    /** Function ids (namespace:path) of every valid datapack, for the coding action. */
    private static final Map<String, Set<String>> functions = new HashMap<>();
    private static final Map<Integer, LinkedHashSet<String>> enabledInWorlds = new HashMap<>();
    private static final Set<Integer> started = new HashSet<>();
    private static final Map<String, Integer> tickErrors = new HashMap<>();
    private static BukkitTask ticker;

    private Datapacks() {
    }

    private static @NotNull FileConfiguration config() {
        return OpenCreative.getPlugin().getConfig();
    }

    public static boolean isEnabled() {
        return config().getBoolean("datapacks.enabled", true);
    }

    public static @NotNull String permission() {
        return config().getString("datapacks.permission", "opencreativepp.datapack");
    }

    public static int maxPerWorld() {
        return config().getInt("datapacks.max-per-world", 5);
    }

    public static @NotNull Collection<Entry> getLibrary() {
        return Collections.unmodifiableCollection(library.values());
    }

    public static @Nullable Entry get(@NotNull String id) {
        return library.get(id.toLowerCase());
    }

    private static @NotNull File folder() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "datapacks");
    }

    // ------------------------------------------------------------------ loading and checks

    /**
     * Reads datapacks.yml, checks every datapack, installs valid ones and starts the ticker.
     */
    public static void load() {
        library.clear();
        functions.clear();
        tickErrors.clear();
        loadWorldsFile();
        if (!folder().exists() && !folder().mkdirs()) {
            log.error("Не удалось создать папку " + folder().getPath());
        }
        if (!isEnabled()) {
            stopTicker();
            log.debug("Датапаки выключены (datapacks.enabled: false).");
            return;
        }
        ConfigurationSection section = config().getConfigurationSection("datapacks.library");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection pack = section.getConfigurationSection(id);
                if (pack == null) continue;
                Entry entry = check(id, pack);
                library.put(id.toLowerCase(), entry);
                if (entry.valid()) {
                    functions.put(entry.id(), listFunctions(Objects.requireNonNull(source(entry.file()))));
                    warnGlobalSelectors(entry.id(), Objects.requireNonNull(source(entry.file())));
                }
                if (!entry.valid()) log.warn("«" + id + "» не подключён: " + entry.problem());
            }
        }
        install();
        long valid = library.values().stream().filter(Entry::valid).count();
        if (!library.isEmpty()) {
            log.info("Датапаков в библиотеке: " + library.size() + ", подключено: " + valid);
        }
        startTicker();
    }

    private static @NotNull Entry check(@NotNull String id, @NotNull ConfigurationSection pack) {
        String file = pack.getString("file", id);
        String name = pack.getString("name", id);
        List<String> description = pack.getStringList("description");
        String icon = pack.getString("icon", "KNOWLEDGE_BOOK");
        String load = pack.getString("load-function", "");
        String tick = pack.getString("tick-function", "");
        String permission = pack.getString("permission", "");
        String problem = findProblem(id, file, load, tick);
        return new Entry(id.toLowerCase(), file, name, description, icon, load, tick, permission, problem);
    }

    /**
     * @return description of the first failed check, or null if datapack is fine.
     */
    private static @Nullable String findProblem(@NotNull String id, @NotNull String file, @NotNull String load, @NotNull String tick) {
        if (!ID.matcher(id).matches()) {
            return "ID «" + id + "» — допустимы только латиница в нижнем регистре, цифры и _";
        }
        File source = source(file);
        if (source == null) {
            return "нет файла или папки «" + file + "» в plugins/OpenCreativePP/datapacks/";
        }
        DatapackFiles files;
        try {
            files = DatapackFiles.open(source);
        } catch (Exception error) {
            return "не удалось прочитать " + source.getName() + ": " + error.getMessage();
        }
        try (files) {
            if (!files.exists("pack.mcmeta")) {
                String nested = files.nestedRoot();
                return nested != null
                        ? "pack.mcmeta лежит в папке «" + nested + "» внутри архива — он должен быть в корне"
                        : "это не датапак: нет файла pack.mcmeta";
            }
            if (!files.hasDirectory("data/")) {
                return "это не датапак: нет папки data/";
            }
            String mcmetaProblem = checkMcmeta(files.read("pack.mcmeta"));
            if (mcmetaProblem != null) return mcmetaProblem;
            for (String function : List.of(load, tick)) {
                if (function.isEmpty()) continue;
                String functionProblem = checkFunction(files, function);
                if (functionProblem != null) return functionProblem;
            }
        } catch (Exception error) {
            return "ошибка проверки: " + error.getMessage();
        }
        return null;
    }

    private static @Nullable String checkMcmeta(@Nullable String content) {
        if (content == null) return "pack.mcmeta пустой";
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(content);
            if (!element.isJsonObject()) return "pack.mcmeta — не JSON-объект";
            root = element.getAsJsonObject();
        } catch (Exception error) {
            return "pack.mcmeta — битый JSON: " + error.getMessage();
        }
        if (!root.has("pack") || !root.get("pack").isJsonObject()) {
            return "в pack.mcmeta нет раздела \"pack\"";
        }
        JsonObject pack = root.getAsJsonObject("pack");
        // 1.21.9+ datapacks may use min_format/max_format instead of pack_format.
        if (!pack.has("pack_format") && !pack.has("min_format")) {
            return "в pack.mcmeta нет pack.pack_format (или min_format)";
        }
        return null;
    }

    private static @Nullable String checkFunction(@NotNull DatapackFiles files, @NotNull String function) {
        if (!FUNCTION.matcher(function).matches()) {
            return "функция «" + function + "» записана неверно, нужно namespace:путь (например parkour:tick)";
        }
        String[] parts = function.split(":", 2);
        String path = "data/" + parts[0] + "/function/" + parts[1] + ".mcfunction";
        if (files.exists(path)) return null;
        if (files.exists("data/" + parts[0] + "/functions/" + parts[1] + ".mcfunction")) {
            return "функция «" + function + "» лежит в папке functions/ — с 1.21 папка называется function/";
        }
        return "нет функции «" + function + "» (ожидался файл " + path + ")";
    }

    private static @NotNull Set<String> listFunctions(@NotNull File source) {
        Set<String> found = new HashSet<>();
        java.util.function.Consumer<String> add = path -> {
            // data/<namespace>/function/<path>.mcfunction
            String[] parts = path.replace('\\', '/').split("/", 4);
            if (parts.length == 4 && parts[0].equals("data") && parts[2].equals("function") && parts[3].endsWith(".mcfunction")) {
                found.add(parts[1] + ":" + parts[3].substring(0, parts[3].length() - ".mcfunction".length()));
            }
        };
        try {
            if (source.isDirectory()) {
                java.nio.file.Path root = source.toPath();
                try (var stream = Files.walk(root)) {
                    stream.filter(Files::isRegularFile).forEach(path -> add.accept(root.relativize(path).toString()));
                }
            } else {
                try (ZipFile zip = new ZipFile(source)) {
                    zip.stream().map(ZipEntry::getName).forEach(add);
                }
            }
        } catch (Exception error) {
            log.warn("Не удалось прочитать функции из " + source.getName() + ": " + error.getMessage());
        }
        return found;
    }

    /**
     * @a / @e without distance= select players and entities of ALL worlds of the server, not only the world,
     * where the function runs. It's allowed (admin decides), but worth a warning.
     */
    private static void warnGlobalSelectors(@NotNull String id, @NotNull File source) {
        List<String> risky = new ArrayList<>();
        java.util.function.BiConsumer<String, String> check = (name, content) -> {
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) continue;
                if ((trimmed.contains("@a") || trimmed.contains("@e")) && !trimmed.contains("distance=")) {
                    risky.add(name);
                    return;
                }
            }
        };
        try {
            if (source.isDirectory()) {
                java.nio.file.Path root = source.toPath();
                try (var stream = Files.walk(root)) {
                    for (java.nio.file.Path path : stream.filter(p -> p.toString().endsWith(".mcfunction")).toList()) {
                        check.accept(root.relativize(path).toString(), Files.readString(path, StandardCharsets.UTF_8));
                    }
                }
            } else {
                try (ZipFile zip = new ZipFile(source)) {
                    for (ZipEntry entry : zip.stream().filter(e -> e.getName().endsWith(".mcfunction")).toList()) {
                        try (InputStream input = zip.getInputStream(entry)) {
                            check.accept(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!risky.isEmpty()) {
            log.warn("«" + id + "»: в " + risky.size() + " функциях селекторы @a/@e без distance= — они затронут игроков"
                    + " и существ всех миров сервера (например " + String.join(", ", risky.subList(0, Math.min(3, risky.size()))) + ")."
                    + " Внутри мира используй @a[distance=0..].");
        }
    }

    /**
     * Coding action "Run datapack function": only functions of datapacks enabled in this world.
     */
    public static void runFromCode(@NotNull Planet planet, @NotNull World world, @NotNull String function) {
        if (!isEnabled()) throw new IllegalStateException("датапаки отключены на сервере");
        for (String id : getEnabled(planet)) {
            Entry entry = library.get(id);
            if (entry != null && entry.valid() && functions.getOrDefault(id, Set.of()).contains(function)) {
                run(planet, world, entry, function);
                return;
            }
        }
        throw new IllegalArgumentException("функции " + function + " нет во включённых в мире датапаках (/datapack)");
    }

    private static @Nullable File source(@NotNull String file) {
        File exact = new File(folder(), file);
        if (exact.exists()) return exact;
        File zip = new File(folder(), file + ".zip");
        return zip.exists() ? zip : null;
    }

    /**
     * Copies valid datapacks into main world's datapacks folder and enables them.
     */
    private static void install() {
        if (Bukkit.getWorlds().isEmpty()) return;
        File target = new File(Bukkit.getWorlds().getFirst().getWorldFolder(), "datapacks");
        if (!target.exists() && !target.mkdirs()) {
            log.error("Не удалось создать папку " + target.getPath());
            return;
        }
        Set<String> wanted = new HashSet<>();
        for (Entry entry : library.values()) {
            if (!entry.valid()) continue;
            File source = Objects.requireNonNull(source(entry.file()));
            String installed = PREFIX + entry.id() + (source.isDirectory() ? "" : ".zip");
            wanted.add("file/" + installed);
            try {
                File destination = new File(target, installed);
                if (source.isDirectory()) {
                    org.apache.commons.io.FileUtils.deleteDirectory(destination);
                    org.apache.commons.io.FileUtils.copyDirectory(source, destination);
                } else {
                    org.apache.commons.io.FileUtils.copyFile(source, destination);
                }
            } catch (Exception error) {
                replace(entry, "не удалось скопировать в датапаки сервера: " + error.getMessage());
            }
        }
        var manager = Bukkit.getServer().getDatapackManager();
        manager.refreshPacks();
        for (Datapack pack : manager.getPacks()) {
            String name = pack.getName();
            if (name.startsWith("file/" + PREFIX) && !wanted.contains(name) && pack.isEnabled()) {
                pack.setEnabled(false);
                log.info("Отключён датапак, которого больше нет в библиотеке: " + name);
            }
        }
        for (Entry entry : new ArrayList<>(library.values())) {
            if (!entry.valid()) continue;
            File source = Objects.requireNonNull(source(entry.file()));
            String name = "file/" + PREFIX + entry.id() + (source.isDirectory() ? "" : ".zip");
            Datapack pack = manager.getPack(name);
            if (pack == null) {
                replace(entry, "Minecraft не увидел датапак после копирования (проверь pack_format для 1.21.11)");
                continue;
            }
            if (!pack.isEnabled()) pack.setEnabled(true);
        }
    }

    private static void replace(@NotNull Entry entry, @NotNull String problem) {
        library.put(entry.id(), new Entry(entry.id(), entry.file(), entry.name(), entry.description(), entry.icon(),
                entry.loadFunction(), entry.tickFunction(), entry.permission(), problem));
        log.warn("«" + entry.id() + "» не подключён: " + problem);
    }

    // ------------------------------------------------------------------ datapacks of worlds

    private static @NotNull File worldsFile() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "datapacks-worlds.yml");
    }

    private static void loadWorldsFile() {
        enabledInWorlds.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(worldsFile());
        for (String key : config.getKeys(false)) {
            try {
                enabledInWorlds.put(Integer.parseInt(key), new LinkedHashSet<>(config.getStringList(key)));
            } catch (NumberFormatException ignored) {
                log.warn("datapacks-worlds.yml: пропущен неверный ID мира " + key);
            }
        }
    }

    private static void saveWorldsFile() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of("Какие датапаки включены в каких мирах: ID мира -> список ID датапаков. Файл ведёт плагин."));
        for (var entry : enabledInWorlds.entrySet()) {
            if (!entry.getValue().isEmpty()) config.set(String.valueOf(entry.getKey()), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(worldsFile());
        } catch (Exception error) {
            log.error("Не удалось сохранить datapacks-worlds.yml", error);
        }
    }

    public static @NotNull Set<String> getEnabled(@NotNull Planet planet) {
        return Collections.unmodifiableSet(enabledInWorlds.getOrDefault(planet.getId(), new LinkedHashSet<>()));
    }

    public static boolean isEnabledIn(@NotNull Planet planet, @NotNull String id) {
        return enabledInWorlds.getOrDefault(planet.getId(), new LinkedHashSet<>()).contains(id.toLowerCase());
    }

    /**
     * Result of trying to switch datapack in a world.
     */
    public enum ToggleResult {ENABLED, DISABLED, SYSTEM_DISABLED, NOT_IN_WORLD, NOT_OWNER, NO_PERMISSION,
        UNKNOWN, INVALID, NO_PACK_PERMISSION, LIMIT}

    /**
     * Switches datapack in the world, where player is. All checks are here.
     */
    public static @NotNull ToggleResult toggle(@NotNull Player player, @NotNull String id) {
        if (!isEnabled()) return ToggleResult.SYSTEM_DISABLED;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        if (planet == null) return ToggleResult.NOT_IN_WORLD;
        boolean admin = player.hasPermission("opencreativepp.admin");
        if (!planet.isOwner(player) && !admin) return ToggleResult.NOT_OWNER;
        if (!admin && !player.hasPermission(permission())) return ToggleResult.NO_PERMISSION;
        Entry entry = get(id);
        LinkedHashSet<String> enabled = enabledInWorlds.computeIfAbsent(planet.getId(), key -> new LinkedHashSet<>());
        if (entry != null && enabled.contains(entry.id())) {
            enabled.remove(entry.id());
            started.remove(planet.getId());
            saveWorldsFile();
            return ToggleResult.DISABLED;
        }
        if (entry == null) return ToggleResult.UNKNOWN;
        if (!entry.valid()) return ToggleResult.INVALID;
        if (!admin && !entry.permission().isEmpty() && !player.hasPermission(entry.permission())) {
            return ToggleResult.NO_PACK_PERMISSION;
        }
        if (!admin && enabled.size() >= maxPerWorld()) return ToggleResult.LIMIT;
        enabled.add(entry.id());
        started.remove(planet.getId()); // run load-function again on next tick in Play mode
        saveWorldsFile();
        return ToggleResult.ENABLED;
    }

    // ------------------------------------------------------------------ running functions

    private static void startTicker() {
        stopTicker();
        ticker = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), Datapacks::tick, 1L, 1L);
    }

    public static void stopTicker() {
        if (ticker != null) ticker.cancel();
        ticker = null;
        started.clear();
    }

    private static void tick() {
        if (enabledInWorlds.isEmpty()) return;
        for (var worldEntry : enabledInWorlds.entrySet()) {
            if (worldEntry.getValue().isEmpty()) continue;
            Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(worldEntry.getKey()));
            World world = planet == null || !planet.isLoaded() ? null : planet.getTerritory().getWorld();
            if (world == null || planet.getMode() != Planet.Mode.PLAYING) {
                started.remove(worldEntry.getKey());
                continue;
            }
            boolean first = started.add(planet.getId());
            for (String id : new ArrayList<>(worldEntry.getValue())) {
                Entry entry = library.get(id);
                if (entry == null || !entry.valid()) continue;
                if (first && !entry.loadFunction().isEmpty()) run(planet, world, entry, entry.loadFunction());
                if (!entry.tickFunction().isEmpty()) run(planet, world, entry, entry.tickFunction());
            }
        }
    }

    /**
     * Runs datapack function inside the world (like "execute in [world] positioned [spawn] run function").
     */
    public static boolean run(@NotNull Planet planet, @NotNull World world, @NotNull Entry entry, @NotNull String function) {
        String key = planet.getId() + ":" + entry.id();
        try {
            MinecraftServer server = MinecraftServer.getServer();
            Identifier identifier = Identifier.tryParse(function);
            if (identifier == null) throw new IllegalArgumentException("неверное имя функции " + function);
            Optional<CommandFunction<CommandSourceStack>> found = server.getFunctions().get(identifier);
            if (found.isEmpty()) throw new IllegalStateException("функция " + function + " не загружена Minecraft");
            Location spawn = world.getSpawnLocation();
            CommandSourceStack source = server.getFunctions().getGameLoopSender()
                    .withLevel(((CraftWorld) world).getHandle())
                    .withPosition(new Vec3(spawn.getX(), spawn.getY(), spawn.getZ()));
            server.getFunctions().execute(found.get(), source);
            tickErrors.remove(key);
            return true;
        } catch (Exception error) {
            int errors = tickErrors.merge(key, 1, Integer::sum);
            int max = config().getInt("datapacks.max-tick-errors", 20);
            log.debug("Мир " + planet.getId() + ", «" + entry.id() + "»: " + error.getMessage());
            if (max > 0 && errors >= max) {
                tickErrors.remove(key);
                LinkedHashSet<String> enabled = enabledInWorlds.get(planet.getId());
                if (enabled != null) enabled.remove(entry.id());
                saveWorldsFile();
                log.warn("Мир " + planet.getId() + ": датапак «" + entry.id() + "» выключен после " + max
                        + " ошибок подряд (" + error.getMessage() + ")");
                for (Player player : world.getPlayers()) {
                    if (planet.isOwner(player)) {
                        player.sendMessage(ua.mcchickenstudio.opencreative.utils.MessageUtils
                                .getLocaleMessage("plus.datapacks.auto-disabled").replace("%id%", entry.id()));
                    }
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ reading folder or zip

    /**
     * Files of a datapack, that is a folder or a .zip archive.
     */
    private interface DatapackFiles extends AutoCloseable {
        boolean exists(@NotNull String path);

        boolean hasDirectory(@NotNull String prefix);

        @Nullable String read(@NotNull String path) throws Exception;

        /**
         * Name of the only folder in archive root, that contains pack.mcmeta (a common packing mistake).
         */
        @Nullable String nestedRoot();

        @Override
        void close();

        static @NotNull DatapackFiles open(@NotNull File source) throws Exception {
            if (source.isDirectory()) {
                return new DatapackFiles() {
                    public boolean exists(@NotNull String path) {
                        return new File(source, path).isFile();
                    }

                    public boolean hasDirectory(@NotNull String prefix) {
                        return new File(source, prefix).isDirectory();
                    }

                    public String read(@NotNull String path) throws Exception {
                        File file = new File(source, path);
                        return file.isFile() ? Files.readString(file.toPath(), StandardCharsets.UTF_8) : null;
                    }

                    public String nestedRoot() {
                        File[] children = source.listFiles(File::isDirectory);
                        if (children == null) return null;
                        for (File child : children) {
                            if (new File(child, "pack.mcmeta").isFile()) return child.getName();
                        }
                        return null;
                    }

                    public void close() {
                    }
                };
            }
            if (!source.getName().toLowerCase().endsWith(".zip")) {
                throw new IllegalArgumentException("файл должен быть папкой или .zip");
            }
            ZipFile zip = new ZipFile(source);
            return new DatapackFiles() {
                public boolean exists(@NotNull String path) {
                    ZipEntry entry = zip.getEntry(path);
                    return entry != null && !entry.isDirectory();
                }

                public boolean hasDirectory(@NotNull String prefix) {
                    return zip.stream().anyMatch(entry -> entry.getName().startsWith(prefix));
                }

                public String read(@NotNull String path) throws Exception {
                    ZipEntry entry = zip.getEntry(path);
                    if (entry == null) return null;
                    try (InputStream input = zip.getInputStream(entry)) {
                        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }

                public String nestedRoot() {
                    return zip.stream().map(ZipEntry::getName)
                            .filter(name -> name.endsWith("/pack.mcmeta") && name.indexOf('/') == name.lastIndexOf('/'))
                            .map(name -> name.substring(0, name.indexOf('/')))
                            .findFirst().orElse(null);
                }

                public void close() {
                    try {
                        zip.close();
                    } catch (Exception ignored) {
                    }
                }
            };
        }
    }
}
