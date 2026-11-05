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

package ua.mcchickenstudio.opencreative.plus.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.economy.Economy;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Built-in currency of OpenCreative++ — works without Vault and EssentialsX.
 * <p>
 * Implements economy manager of OpenCreative+, so like rewards, "Request purchase" coding action
 * and advertisement price work with it. Balances: plugins/OpenCreativePP/balances.yml.
 */
public final class PlusEconomy implements Economy {

    private static final PlusLog log = PlusLog.module("Экономика");
    private static final int LOG_SIZE = 500;

    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<String, String> data = new HashMap<>(); // misc data: daily bonus dates, rewarded visits...
    private final Deque<String> history = new ArrayDeque<>();
    private boolean dirty = false;
    private boolean working = false;
    private BukkitTask saver;

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "balances.yml");
    }

    /**
     * Instance of built-in economy, or null if server uses Vault/disabled economy.
     */
    public static PlusEconomy get() {
        return OpenCreative.getEconomy() instanceof PlusEconomy economy ? economy : null;
    }

    public static @NotNull String currencyName() {
        return OpenCreative.getPlugin().getConfig().getString("economy.currency.name", "монет");
    }

    public static @NotNull String symbol() {
        return OpenCreative.getPlugin().getConfig().getString("economy.currency.symbol", "⛁");
    }

    /**
     * "1 250 ⛁"
     */
    public static @NotNull String format(long amount) {
        String number = String.format(Locale.ROOT, "%,d", amount).replace(',', ' ');
        return number + " " + symbol();
    }

    @Override
    public void start() {
        if (working) return; // OpenCreative+ starts all managers once more after settings registered them
        balances.clear();
        data.clear();
        history.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file());
        ConfigurationSection section = config.getConfigurationSection("balances");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    balances.put(UUID.fromString(key), section.getLong(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection dataSection = config.getConfigurationSection("data");
        if (dataSection != null) {
            for (String key : dataSection.getKeys(false)) {
                data.put(key, dataSection.getString(key, ""));
            }
        }
        history.addAll(config.getStringList("history"));
        working = true;
        saver = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), () -> {
            if (dirty) save();
        }, 600L, 600L);
        log.debug("Загружено балансов: " + balances.size());
    }

    @Override
    public void shutdown() {
        if (saver != null) saver.cancel();
        save();
        working = false;
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of("Балансы OpenCreative++. Файл ведёт плагин; менять — командой /eco."));
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            config.set("data." + entry.getKey(), entry.getValue());
        }
        config.set("history", new ArrayList<>(history));
        File target = file();
        File temporary = new File(target.getPath() + ".tmp");
        try {
            config.save(temporary);
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomic) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (Exception error) {
            log.error("Не удалось сохранить balances.yml", error);
        }
    }

    public synchronized long balance(@NotNull UUID player) {
        Long balance = balances.get(player);
        if (balance != null) return balance;
        long start = OpenCreative.getPlugin().getConfig().getLong("economy.currency.starting-balance", 100);
        balances.put(player, start);
        dirty = true;
        return start;
    }

    public synchronized void set(@NotNull UUID player, long amount, @NotNull String reason) {
        long before = balance(player);
        balances.put(player, Math.max(0, amount));
        record(player, Math.max(0, amount) - before, reason);
    }

    public synchronized boolean add(@NotNull UUID player, long amount, @NotNull String reason) {
        if (amount <= 0) return false;
        balances.put(player, balance(player) + amount);
        record(player, amount, reason);
        return true;
    }

    public synchronized boolean take(@NotNull UUID player, long amount, @NotNull String reason) {
        if (amount <= 0) return amount == 0;
        long balance = balance(player);
        if (balance < amount) return false;
        balances.put(player, balance - amount);
        record(player, -amount, reason);
        return true;
    }

    /**
     * Moves money from one player to another; nothing happens if sender has not enough.
     */
    public synchronized boolean transfer(@NotNull UUID from, @NotNull UUID to, long amount, @NotNull String reason) {
        if (amount <= 0 || from.equals(to)) return false;
        if (!take(from, amount, reason)) return false;
        add(to, amount, reason);
        return true;
    }

    private void record(@NotNull UUID player, long change, @NotNull String reason) {
        dirty = true;
        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(player).getName()).orElse(player.toString());
        history.addLast(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()) + " | " + name + " | "
                + (change >= 0 ? "+" : "") + change + " | " + reason);
        while (history.size() > LOG_SIZE) history.removeFirst();
    }

    public synchronized @NotNull List<Map.Entry<UUID, Long>> top(int amount) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(balances.entrySet());
        list.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        return list.subList(0, Math.min(amount, list.size()));
    }

    public synchronized @NotNull List<String> history(int amount) {
        List<String> list = new ArrayList<>(history);
        return list.subList(Math.max(0, list.size() - amount), list.size());
    }

    public synchronized @NotNull String data(@NotNull String key) {
        return data.getOrDefault(key, "");
    }

    public synchronized void data(@NotNull String key, @NotNull String value) {
        data.put(key, value);
        dirty = true;
    }

    // --- Economy of OpenCreative+ ---

    @Override
    public boolean depositMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return add(offlinePlayer.getUniqueId(), money.longValue(), "плагин");
    }

    @Override
    public boolean withdrawMoney(@NotNull OfflinePlayer offlinePlayer, @NotNull Number money) {
        return take(offlinePlayer.getUniqueId(), money.longValue(), "плагин");
    }

    @Override
    public @NotNull Number getBalance(OfflinePlayer offlinePlayer) {
        return offlinePlayer == null ? 0 : balance(offlinePlayer.getUniqueId());
    }

    @Override
    public boolean isWorking() {
        return working;
    }

    @Override
    public @NotNull String getName() {
        return "OpenCreative++ Economy";
    }
}
