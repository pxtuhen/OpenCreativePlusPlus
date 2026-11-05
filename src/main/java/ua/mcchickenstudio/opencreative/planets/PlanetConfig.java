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

package ua.mcchickenstudio.opencreative.planets;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.*;

/**
 * <h1>PlanetConfig</h1>
 * This class represents a config of planet.
 */
public class PlanetConfig {

    private final Planet planet;
    private FileConfiguration config;
    private boolean changed;

    public PlanetConfig(@NotNull Planet planet) {
        this.planet = planet;
    }

    public @NotNull CompletableFuture<Void> load() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            config = getPlanetConfig(planet);
            future.complete(null);
        });
        return future;
    }

    public void set(@NotNull String key, Object value) {
        if (value instanceof Set<?> set) {
            value = new ArrayList<>(set);
        }
        if (config == null) {
            FileConfiguration copy = getPlanetConfig(planet);
            copy.set(key, value);
            try {
                copy.save(getPlanetConfigFile(planet));
            } catch (Exception error) {
                sendCriticalErrorMessage("Can't save changes to planet's settings config to file.", error);
            }
            return;
        }
        config.set(key, value);
    }

    public @NotNull FileConfiguration getConfig() {
        if (config != null) {
            return config;
        }
        return getPlanetConfig(planet);
    }

    public void remove(@NotNull String key) {
        if (config == null) {
            FileConfiguration copy = getPlanetConfig(planet);
            copy.set(key, null);
            try {
                copy.save(getPlanetConfigFile(planet));
            } catch (Exception error) {
                sendCriticalErrorMessage("Can't save changes to planet's settings config to file.", error);
            }
            return;
        }
        config.set(key, null);
    }

    /**
     * OpenCreative++: forget cached settings without saving them (used after a backup was restored,
     * otherwise the old cached config would overwrite restored settings.yml).
     */
    public void discard() {
        config = null;
    }

    public void unload() {
        if (config != null) {
            save().whenComplete((result, error) -> {
                config = null;
            });
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public CompletableFuture<Void> save() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (config == null) {
            future.complete(null);
            return future;
        }
        if (OpenCreative.getPlugin().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () ->  {
                saveConfig(config);
                future.complete(null);
            });
        } else {
            saveConfig(config);
            future.complete(null);
        }
        return future;
    }

    private void saveConfig(FileConfiguration config) {
        File configFile = getPlanetConfigFile(planet);
        File tempFile = new File(getTempFolder(), "planet" + planet.getId() + "-settings.yml");
        try {
            if (!configFile.exists()) return; // Planet was deleted
            config.save(tempFile);
            moveFiles(tempFile, configFile);
        } catch (Exception error) {
            sendCriticalErrorMessage("Can't save planet's settings config to file.", error);
        } finally {
            tempFile.delete();
        }
    }

    private void moveFiles(@NotNull File source, @NotNull File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
