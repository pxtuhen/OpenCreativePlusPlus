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

package ua.mcchickenstudio.opencreative.plus.contest;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.events.planet.PlanetCreationEvent;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;
import ua.mcchickenstudio.opencreative.plus.players.Discord;
import ua.mcchickenstudio.opencreative.plus.rating.Ratings;
import ua.mcchickenstudio.opencreative.plus.shop.Payments;
import ua.mcchickenstudio.opencreative.plus.shop.PlusLimits;
import ua.mcchickenstudio.opencreative.utils.world.WorldUtils;
import ua.mcchickenstudio.opencreative.utils.world.generators.WorldGenerator;
import ua.mcchickenstudio.opencreative.utils.world.generators.WorldGenerators;

import java.io.File;
import java.time.Duration;
import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.MessageUtils.*;

/**
 * Build contests created by admins (config.yml → contests, data: plugins/OpenCreativePP/contests.yml).
 * <p>
 * One contest at a time: DRAFT → BUILDING → VOTING → FINISHED. Every participant gets a separate world
 * (doesn't count in world limit); after building ends blocks can't be changed. Players rate entries 1–5 stars.
 */
public final class Contests implements Listener {

    public enum Phase {NONE, DRAFT, BUILDING, VOTING, FINISHED}

    private static final PlusLog log = PlusLog.module("Конкурсы");
    private static YamlConfiguration data;
    private static BukkitTask ticker;
    private static final Map<UUID, Integer> pendingWorlds = new HashMap<>();

    private Contests() {
    }

    public static @NotNull Contests listener() {
        return new Contests();
    }

    private static @NotNull FileConfiguration settings() {
        return OpenCreative.getPlugin().getConfig();
    }

    private static @NotNull File file() {
        return new File(OpenCreative.getPlugin().getDataFolder(), "contests.yml");
    }

    private static synchronized @NotNull YamlConfiguration data() {
        if (data == null) data = YamlConfiguration.loadConfiguration(file());
        return data;
    }

    private static synchronized void save() {
        try {
            data().save(file());
        } catch (Exception error) {
            log.error("Не удалось сохранить contests.yml", error);
        }
    }

    // ------------------------------------------------------------------ state

    public static @NotNull Phase phase() {
        try {
            return Phase.valueOf(data().getString("current.phase", "NONE"));
        } catch (IllegalArgumentException error) {
            return Phase.NONE;
        }
    }

    public static boolean isActive() {
        return phase() == Phase.BUILDING || phase() == Phase.VOTING;
    }

    public static @NotNull String name() {
        return data().getString("current.name", "");
    }

    public static @NotNull String theme() {
        return data().getString("current.theme", "");
    }

    public static long phaseEnds() {
        return data().getLong("current.phase-ends", 0);
    }

    public static @NotNull String timeLeft() {
        long left = Math.max(0, phaseEnds() - System.currentTimeMillis());
        return convertTime(left);
    }

    /**
     * world id → owner uuid of current contest.
     */
    public static synchronized @NotNull Map<Integer, UUID> entries() {
        Map<Integer, UUID> entries = new LinkedHashMap<>();
        ConfigurationSection section = data().getConfigurationSection("current.participants");
        if (section == null) return entries;
        for (String uuid : section.getKeys(false)) {
            try {
                entries.put(section.getInt(uuid), UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return entries;
    }

    public static @NotNull List<String> results() {
        return data().getStringList("current.results");
    }

    public static @Nullable Integer worldOf(@NotNull UUID player) {
        int id = data().getInt("current.participants." + player, -1);
        return id < 0 ? null : id;
    }

    public static boolean isCurrentEntry(@NotNull Planet planet) {
        return entries().containsKey(planet.getId());
    }

    public static int vote(@NotNull Planet planet, @NotNull UUID player) {
        return data().getInt("current.votes." + planet.getId() + "." + player, 0);
    }

    public static synchronized @NotNull Map<UUID, Integer> votes(int worldId) {
        Map<UUID, Integer> votes = new HashMap<>();
        ConfigurationSection section = data().getConfigurationSection("current.votes." + worldId);
        if (section == null) return votes;
        for (String uuid : section.getKeys(false)) {
            try {
                votes.put(UUID.fromString(uuid), section.getInt(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return votes;
    }

    // ------------------------------------------------------------------ startup

    public static void start() {
        // all contest worlds ever created don't count in players' world limit
        for (int id : data().getIntegerList("all-worlds")) PlusLimits.markContestWorld(id, true);
        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), Contests::tick, 200L, 200L);
    }

    public static void stop() {
        if (ticker != null) ticker.cancel();
        ticker = null;
    }

    private static void tick() {
        if (!isActive() || System.currentTimeMillis() < phaseEnds()) return;
        if (phase() == Phase.BUILDING) startVoting();
        else finish();
    }

    // ------------------------------------------------------------------ admin

    public static synchronized void create(@NotNull String name) {
        data().set("current", null);
        data().set("current.phase", Phase.DRAFT.name());
        data().set("current.name", name);
        data().set("current.theme", name);
        data().set("current.generator", settings().getString("contests.generator", "flat"));
        data().set("current.world-size", settings().getInt("contests.world-size", 64));
        data().set("current.build-minutes", settings().getInt("contests.build-minutes", 1440));
        data().set("current.vote-minutes", settings().getInt("contests.vote-minutes", 360));
        data().set("current.rewards", settings().getLongList("contests.rewards"));
        save();
    }

    public static synchronized void set(@NotNull String key, @NotNull Object value) {
        data().set("current." + key, value);
        save();
    }

    public static synchronized @NotNull List<Long> rewards() {
        return data().getLongList("current.rewards");
    }

    public static synchronized @NotNull String draftInfo() {
        ConfigurationSection c = data().getConfigurationSection("current");
        if (c == null) return "-";
        return "name=" + c.getString("name") + ", theme=" + c.getString("theme") + ", generator=" + c.getString("generator")
                + ", size=" + c.getInt("world-size") + ", build=" + c.getInt("build-minutes") + "m, vote=" + c.getInt("vote-minutes")
                + "m, rewards=" + c.getLongList("rewards") + ", phase=" + phase() + ", entries=" + entries().size();
    }

    public static synchronized boolean startBuilding() {
        if (phase() != Phase.DRAFT) return false;
        data().set("current.phase", Phase.BUILDING.name());
        data().set("current.phase-ends", System.currentTimeMillis() + data().getInt("current.build-minutes", 1440) * 60_000L);
        save();
        announce("plus.contest.started", "contest-start");
        return true;
    }

    public static synchronized boolean startVoting() {
        if (phase() != Phase.BUILDING) return false;
        data().set("current.phase", Phase.VOTING.name());
        data().set("current.phase-ends", System.currentTimeMillis() + data().getInt("current.vote-minutes", 360) * 60_000L);
        save();
        announce("plus.contest.voting", "contest-voting");
        return true;
    }

    public static synchronized void cancel() {
        data().set("current", null);
        save();
    }

    /**
     * Counts results, gives rewards, announces winners.
     */
    public static synchronized void finish() {
        if (!isActive()) return;
        int minVotes = Math.max(1, settings().getInt("contests.min-votes", 1));
        List<Map.Entry<Integer, Double>> scores = new ArrayList<>();
        for (int worldId : entries().keySet()) {
            Map<UUID, Integer> votes = votes(worldId);
            if (votes.size() < minVotes) continue;
            scores.add(Map.entry(worldId, votes.values().stream().mapToInt(Integer::intValue).average().orElse(0)));
        }
        scores.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());
        List<Long> rewards = rewards();
        List<String> lines = new ArrayList<>();
        Map<Integer, UUID> entries = entries();
        for (int place = 0; place < Math.min(3, scores.size()); place++) {
            int worldId = scores.get(place).getKey();
            UUID owner = entries.get(worldId);
            String ownerName = Optional.ofNullable(Bukkit.getOfflinePlayer(owner).getName()).orElse("?");
            long reward = place < rewards.size() ? rewards.get(place) : 0;
            Payments.pay(owner, reward, "конкурс " + name() + ", место " + (place + 1));
            for (String command : settings().getStringList("contests.place-commands." + (place + 1))) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", ownerName));
            }
            lines.add(getLocaleMessage("plus.contest.result-line", false).replace("%place%", String.valueOf(place + 1))
                    .replace("%player%", ownerName).replace("%id%", String.valueOf(worldId))
                    .replace("%score%", String.format(Locale.ROOT, "%.2f", scores.get(place).getValue()))
                    .replace("%reward%", reward > 0 ? Payments.format(reward) : "-"));
            if (settings().getBoolean("contests.winners-to-recommended", true) && place == 0) {
                OpenCreative.getSettings().getRecommendedWorldsIDs().add(worldId);
                List<Integer> recommended = new ArrayList<>(settings().getIntegerList("recommended-worlds"));
                if (!recommended.contains(worldId)) {
                    recommended.add(worldId);
                    settings().set("recommended-worlds", recommended);
                    OpenCreative.getPlugin().saveConfig();
                }
            }
        }
        data().set("current.phase", Phase.FINISHED.name());
        data().set("current.results", lines);
        data().set("history." + System.currentTimeMillis(), name() + ": " + String.join(" | ", lines));
        save();
        String results = lines.isEmpty() ? getLocaleMessage("plus.contest.no-results", false) : String.join("\n", lines);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(getLocaleMessage("plus.contest.finished").replace("%name%", name()).replace("%results%", results));
        }
        Discord.send("contest", getLocaleMessage("plus.discord.contest-results", false).replace("%name%", name())
                .replace("%results%", String.join("; ", lines)));
        log.info("Конкурс «" + name() + "» завершён: " + String.join(" | ", lines));
    }

    private static void announce(@NotNull String key, @NotNull String discordKey) {
        String text = getLocaleMessage(key).replace("%name%", name()).replace("%theme%", theme()).replace("%time%", timeLeft());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(text);
            player.showTitle(Title.title(toComponent(getLocaleMessage(key + "-title", false).replace("%name%", name())),
                    toComponent("&f" + theme()), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))));
        }
        Discord.send("contest", getLocaleMessage("plus.discord." + discordKey, false).replace("%name%", name()).replace("%theme%", theme()));
    }

    // ------------------------------------------------------------------ players

    /**
     * Creates contest world for player (BUILDING phase only).
     */
    public static void join(@NotNull Player player) {
        if (phase() != Phase.BUILDING) {
            player.sendMessage(getLocaleMessage("plus.contest.not-building"));
            return;
        }
        Integer existing = worldOf(player.getUniqueId());
        if (existing != null) {
            Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(existing));
            if (planet != null) {
                player.closeInventory();
                planet.connectPlayer(player);
                return;
            }
        }
        WorldGenerator generator = WorldGenerators.getInstance().getById(data().getString("current.generator", "flat"));
        if (generator == null) generator = WorldGenerators.getInstance().getById("flat");
        if (generator == null) {
            player.sendMessage(getLocaleMessage("plus.contest.no-generator"));
            return;
        }
        int id = WorldUtils.generateWorldID();
        pendingWorlds.put(player.getUniqueId(), id);
        player.closeInventory();
        player.sendMessage(getLocaleMessage("plus.contest.creating-world"));
        OpenCreative.getPlanetsManager().createPlanet(player, id, generator);
    }

    @EventHandler
    public void onCreation(PlanetCreationEvent event) {
        Integer expected = pendingWorlds.get(event.getPlayer().getUniqueId());
        if (expected == null || expected != event.getPlanet().getId()) return;
        pendingWorlds.remove(event.getPlayer().getUniqueId());
        Planet planet = event.getPlanet();
        synchronized (Contests.class) {
            data().set("current.participants." + event.getPlayer().getUniqueId(), planet.getId());
            List<Integer> all = new ArrayList<>(data().getIntegerList("all-worlds"));
            all.add(planet.getId());
            data().set("all-worlds", all);
            save();
        }
        PlusLimits.markContestWorld(planet.getId(), true);
        planet.getConfiguration().set("contest.name", name());
        int size = data().getInt("current.world-size", 64);
        Bukkit.getScheduler().runTaskLater(OpenCreative.getPlugin(), () -> {
            planet.getInformation().setDisplayName("§6" + name() + " §7— §f" + event.getPlayer().getName());
            if (size > 0) planet.getTerritory().setWorldSize(size, true);
        }, 40L);
    }

    /**
     * Contest vote (called by /rate in a contest world during voting).
     */
    public static boolean rate(@NotNull Player player, @NotNull Planet planet, int value) {
        if (phase() != Phase.VOTING || !isCurrentEntry(planet)) return false;
        if (planet.isOwner(player)) {
            player.sendMessage(getLocaleMessage("plus.contest.own-entry"));
            return true;
        }
        synchronized (Contests.class) {
            data().set("current.votes." + planet.getId() + "." + player.getUniqueId(), Math.max(1, Math.min(5, value)));
            save();
        }
        int rated = (int) entries().keySet().stream().filter(id -> data().contains("current.votes." + id + "." + player.getUniqueId())).count();
        player.sendMessage(getLocaleMessage("plus.contest.voted").replace("%stars%", Ratings.stars(value))
                .replace("%rated%", String.valueOf(rated)).replace("%total%", String.valueOf(Math.max(0, entries().size() - (worldOf(player.getUniqueId()) == null ? 0 : 1)))));
        return true;
    }

    /**
     * Next entry, that player hasn't rated yet (not own).
     */
    public static @Nullable Planet nextUnrated(@NotNull Player player) {
        for (Map.Entry<Integer, UUID> entry : entries().entrySet()) {
            if (entry.getValue().equals(player.getUniqueId())) continue;
            if (data().contains("current.votes." + entry.getKey() + "." + player.getUniqueId())) continue;
            Planet planet = OpenCreative.getPlanetsManager().getPlanetById(String.valueOf(entry.getKey()));
            if (planet != null) return planet;
        }
        return null;
    }

    // ------------------------------------------------------------------ lock after building

    private static boolean locked(@NotNull Player player) {
        if (player.hasPermission("opencreativepp.contest.admin")) return false;
        Planet planet = OpenCreative.getPlanetsManager().getPlanetByPlayer(player);
        return planet != null && isCurrentEntry(planet) && phase() != Phase.BUILDING && phase() != Phase.DRAFT;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (locked(event.getPlayer())) event.setCancelled(true);
    }
}
