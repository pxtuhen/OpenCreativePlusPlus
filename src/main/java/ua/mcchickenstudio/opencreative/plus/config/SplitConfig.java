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

package ua.mcchickenstudio.opencreative.plus.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Plugin configuration spread over several files.
 * <p>
 * The rest of the plugin works with one in-memory configuration (as in OpenCreative+),
 * while on disk each top-level section lives in its own file. Saving writes every
 * value back into the file that owns it, changing only values, so comments stay.
 */
public final class SplitConfig {

    public static final String MAIN = "config.yml";

    /**
     * File name → top-level keys stored in it. Everything else is stored in config.yml.
     */
    private static final Map<String, List<String>> FILES = new LinkedHashMap<>();

    static {
        FILES.put("groups.yml", List.of("groups"));
        FILES.put("worlds.yml", List.of("recommended-worlds", "deprecated-worlds", "generators"));
        FILES.put("items.yml", List.of("items"));
        FILES.put("sounds.yml", List.of("sounds"));
        FILES.put("chat.yml", List.of("chat", "filter"));
        FILES.put("datapacks.yml", List.of("datapacks"));
        FILES.put("shop.yml", List.of("shop", "marketplace"));
    }

    private final JavaPlugin plugin;
    private final PlusLog log;
    private YamlConfiguration composed = new YamlConfiguration();

    public SplitConfig(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = PlusLog.module("Конфиг");
    }

    public @NotNull FileConfiguration get() {
        return composed;
    }

    public static @NotNull Set<String> fileNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add(MAIN);
        names.addAll(FILES.keySet());
        return names;
    }

    /**
     * Name of file, that stores specified top-level key.
     */
    public static @NotNull String ownerOf(@NotNull String topKey) {
        for (Map.Entry<String, List<String>> entry : FILES.entrySet()) {
            if (entry.getValue().contains(topKey)) return entry.getKey();
        }
        return MAIN;
    }

    private static boolean owns(@NotNull String file, @NotNull String topKey) {
        return ownerOf(topKey).equals(file);
    }

    /**
     * Copies missing default files into plugin folder. If config.yml is an old single-file
     * OpenCreative+ config, its sections are moved into the new files (values kept, comments
     * taken from default files) and removed from config.yml.
     */
    public void saveDefaults() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            log.error("Не удалось создать папку плагина: " + plugin.getDataFolder().getPath());
        }
        File mainFile = new File(plugin.getDataFolder(), MAIN);
        YamlConfiguration main = mainFile.exists() ? read(MAIN) : null;
        boolean mainChanged = false;
        for (String name : FILES.keySet()) {
            if (new File(plugin.getDataFolder(), name).exists()) continue;
            if (plugin.getResource(name) != null) plugin.saveResource(name, false);
            if (main == null) continue;
            YamlConfiguration file = readQuietly(name);
            List<String> moved = new ArrayList<>();
            for (String key : FILES.get(name)) {
                if (!main.contains(key, true)) continue;
                sync(main, file, key);
                main.set(key, null);
                moved.add(key);
            }
            if (!moved.isEmpty()) {
                write(name, file);
                mainChanged = true;
                log.info("Перенёс " + String.join(", ", moved) + " из config.yml в " + name);
            }
        }
        if (main == null) {
            if (plugin.getResource(MAIN) != null) plugin.saveResource(MAIN, false);
        } else if (mainChanged) {
            write(MAIN, main);
        }
    }

    /**
     * Reads all files and adds missing default values.
     */
    public void load() {
        saveDefaults();
        YamlConfiguration main = read(MAIN);
        YamlConfiguration result = new YamlConfiguration();
        for (String name : FILES.keySet()) {
            YamlConfiguration file = read(name);
            fillMissing(name, file);
            for (String key : FILES.get(name)) {
                if (file.contains(key, true)) copy(file, result, key);
            }
        }
        fillMissing(MAIN, main);
        replaceOldDefaults(main);
        for (String key : main.getKeys(false)) {
            if (owns(MAIN, key)) copy(main, result, key);
        }
        composed = result;
    }

    /**
     * Writes in-memory configuration back into its files.
     */
    public void save() {
        for (String name : fileNames()) {
            YamlConfiguration disk = readQuietly(name);
            String before = disk.saveToString();
            for (String key : new ArrayList<>(disk.getKeys(false))) {
                if (!owns(name, key) || !composed.contains(key, true)) disk.set(key, null);
            }
            for (String key : composed.getKeys(false)) {
                if (owns(name, key)) sync(composed, disk, key);
            }
            if (!disk.saveToString().equals(before)) write(name, disk);
        }
    }

    private @NotNull YamlConfiguration read(@NotNull String name) {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration config = new YamlConfiguration();
        if (!file.exists()) return config;
        try {
            config.load(file);
        } catch (InvalidConfigurationException error) {
            String broken = name.replace(".yml", "") + "-сломан-"
                    + new SimpleDateFormat("dd-MM-yyyy--HH-mm").format(new Date()) + ".yml";
            log.error(String.join("\n",
                    "Ошибка в " + name + ", файл не читается:",
                    "  " + shortYamlError(error),
                    "Файл переименован в " + broken + ", вместо него создан файл по умолчанию.",
                    "Частые причины: табы вместо пробелов, лишний/недостающий пробел, незакрытые кавычки."));
            if (!file.renameTo(new File(plugin.getDataFolder(), broken))) {
                log.error("Не удалось переименовать " + name + " в " + broken);
            }
            if (plugin.getResource(name) != null) plugin.saveResource(name, true);
            return readQuietly(name);
        } catch (Exception error) {
            log.error("Не удалось прочитать " + name + ": " + error.getMessage());
        }
        return config;
    }

    private @NotNull YamlConfiguration readQuietly(@NotNull String name) {
        YamlConfiguration config = new YamlConfiguration();
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) return config;
        try {
            config.load(file);
        } catch (Exception ignored) {
        }
        return config;
    }

    private void write(@NotNull String name, @NotNull YamlConfiguration config) {
        try {
            config.save(new File(plugin.getDataFolder(), name));
        } catch (Exception error) {
            log.error("Не удалось сохранить " + name + ": " + error.getMessage());
        }
    }

    /**
     * Adds keys, that exist in default file, but are missing in server's file.
     */
    private void fillMissing(@NotNull String name, @NotNull YamlConfiguration config) {
        InputStream input = plugin.getResource(name);
        if (input == null) return;
        YamlConfiguration defaults = new YamlConfiguration();
        try {
            defaults.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (Exception error) {
            log.error("Встроенный " + name + " повреждён: " + error.getMessage());
            return;
        }
        List<String> added = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (key.equals("version")) continue;
            String top = key.split("\\.")[0];
            if (!owns(name, top)) continue;
            if (isCustomSection(config, key)) continue;
            if (!config.contains(key, true)) {
                config.set(key, defaults.get(key));
                config.setComments(key, defaults.getComments(key));
                added.add(key);
            }
        }
        if (added.isEmpty()) return;
        if (name.equals(MAIN) && defaults.contains("version")) {
            config.set("version", defaults.get("version"));
        }
        write(name, config);
        log.info("Добавлено новых параметров в " + name + ": " + added.size()
                + (added.size() <= 10 ? " (" + String.join(", ", added) + ")" : ""));
    }

    /**
     * Values, that were defaults of Creative+ (old name of OpenCreative++). If server owner didn't change them,
     * they are replaced with new defaults; own values stay.
     */
    private void replaceOldDefaults(@NotNull YamlConfiguration main) {
        InputStream input = plugin.getResource(MAIN);
        if (input == null) return;
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        Map<String, String> oldDefaults = Map.of(
                "messages.branding", "<white>Creative<color:#74D3FF>+",
                "lobby.sidebar.title", "&b&lCreative&d&l+",
                "messages.version", "&fCreative<color:#74D3FF>+ &3%version%");
        boolean changed = false;
        for (Map.Entry<String, String> entry : oldDefaults.entrySet()) {
            String current = main.getString(entry.getKey());
            if (current != null && current.contains(entry.getValue()) && defaults.contains(entry.getKey())) {
                main.set(entry.getKey(), defaults.get(entry.getKey()));
                changed = true;
            }
        }
        if (changed) {
            write(MAIN, main);
            log.info("Название обновлено на OpenCreative++ в config.yml (только значения по умолчанию).");
        }
    }

    /**
     * Sections, that server owners fill with own entries. If such section exists,
     * default example entries are not added back (same rules as OpenCreative+).
     */
    private static boolean isCustomSection(@NotNull ConfigurationSection config, @NotNull String key) {
        if (key.startsWith("groups.premium") && config.contains("groups", true)) return true;
        if (key.startsWith("sounds.christmas") && config.contains("sounds", true)) return true;
        if (key.startsWith("commands.onLobby") && config.contains("commands", true)) return true;
        if (key.startsWith("filter.rules.swearing") && config.contains("filter.rules", true)) return true;
        if (key.startsWith("datapacks.library.") && config.contains("datapacks.library", true)) return true;
        if (key.startsWith("items.") && config.contains("items")) {
            String[] parts = key.split("\\.");
            return parts.length >= 2 && config.contains("items." + parts[1], true);
        }
        return false;
    }

    private static void copy(@NotNull ConfigurationSection from, @NotNull ConfigurationSection to, @NotNull String path) {
        if (from.isConfigurationSection(path)) {
            ConfigurationSection section = from.getConfigurationSection(path);
            to.createSection(path);
            if (section == null) return;
            for (String key : section.getKeys(false)) {
                copy(from, to, path + "." + key);
            }
        } else {
            to.set(path, from.get(path));
        }
    }

    /**
     * Makes {@code to} equal to {@code from} at path, changing only differing values.
     */
    private static void sync(@NotNull ConfigurationSection from, @NotNull ConfigurationSection to, @NotNull String path) {
        if (from.isConfigurationSection(path)) {
            ConfigurationSection source = from.getConfigurationSection(path);
            if (!to.isConfigurationSection(path)) to.createSection(path);
            ConfigurationSection target = to.getConfigurationSection(path);
            if (source == null || target == null) return;
            for (String key : new ArrayList<>(target.getKeys(false))) {
                if (!source.contains(key, true)) target.set(key, null);
            }
            for (String key : source.getKeys(false)) {
                sync(from, to, path + "." + key);
            }
        } else if (!Objects.equals(from.get(path), to.get(path))) {
            to.set(path, from.get(path));
        }
    }

    private static @NotNull String shortYamlError(@NotNull Exception error) {
        String message = String.valueOf(error.getMessage());
        // SnakeYAML message: "while scanning ...\n in 'reader', line 98, column 5:\n    <line>\n    ^\n..."
        String[] lines = message.split("\n");
        StringJoiner joiner = new StringJoiner(" | ");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.equals("^")) continue;
            joiner.add(trimmed.replace("in 'reader', ", ""));
            if (joiner.length() > 300) break;
        }
        return joiner.toString();
    }
}
