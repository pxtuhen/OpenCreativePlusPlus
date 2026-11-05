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

package ua.mcchickenstudio.opencreative;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import ua.mcchickenstudio.opencreative.plus.PlusLog;
import ua.mcchickenstudio.opencreative.plus.config.SplitConfig;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ua.mcchickenstudio.opencreative.commands.*;
import ua.mcchickenstudio.opencreative.commands.minecraft.*;
import ua.mcchickenstudio.opencreative.commands.world.*;
import ua.mcchickenstudio.opencreative.commands.world.modes.*;
import ua.mcchickenstudio.opencreative.commands.world.reputation.*;
import ua.mcchickenstudio.opencreative.listeners.ServerListener;
import ua.mcchickenstudio.opencreative.managers.Managers;
import ua.mcchickenstudio.opencreative.managers.ignore.IgnoreManager;
import ua.mcchickenstudio.opencreative.managers.worlds.VanillaWorldManager;
import ua.mcchickenstudio.opencreative.managers.worlds.WorldManager;
import ua.mcchickenstudio.opencreative.wanders.OfflineWander;
import ua.mcchickenstudio.opencreative.wanders.Wander;
import ua.mcchickenstudio.opencreative.coding.prompters.*;
import ua.mcchickenstudio.opencreative.listeners.CreativeListener;
import ua.mcchickenstudio.opencreative.listeners.creative.PlanetListener;
import ua.mcchickenstudio.opencreative.listeners.entity.*;
import ua.mcchickenstudio.opencreative.listeners.player.*;
import ua.mcchickenstudio.opencreative.listeners.world.*;
import ua.mcchickenstudio.opencreative.managers.blocks.BlocksManager;
import ua.mcchickenstudio.opencreative.managers.disguises.DisguiseManager;
import ua.mcchickenstudio.opencreative.managers.downloader.*;
import ua.mcchickenstudio.opencreative.managers.economy.*;
import ua.mcchickenstudio.opencreative.managers.hints.*;
import ua.mcchickenstudio.opencreative.managers.modules.*;
import ua.mcchickenstudio.opencreative.managers.packets.PacketManager;
import ua.mcchickenstudio.opencreative.settings.Sounds;
import ua.mcchickenstudio.opencreative.utils.world.WorldUtils;
import ua.mcchickenstudio.opencreative.utils.world.platforms.*;
import ua.mcchickenstudio.opencreative.managers.stability.*;
import ua.mcchickenstudio.opencreative.managers.updater.*;
import ua.mcchickenstudio.opencreative.menus.Menus;
import ua.mcchickenstudio.opencreative.managers.space.*;
import ua.mcchickenstudio.opencreative.settings.Settings;
import ua.mcchickenstudio.opencreative.utils.FileUtils;
import ua.mcchickenstudio.opencreative.utils.PlayerUtils;
import ua.mcchickenstudio.opencreative.utils.hooks.HookUtils;
import ua.mcchickenstudio.opencreative.utils.hooks.Metrics;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.worldactions.world.phys.data.PhysicsManager;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.parseException;
import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.isEntityInLobby;
import static ua.mcchickenstudio.opencreative.utils.PlayerUtils.teleportToLobby;

/**
 * This class represents OpenCreative+ java plugin for PaperMC.
 * Only for loading, enabling and disabling plugin. Contains
 * general information about plugin's version and codename.
 *
 * @author McChicken Studio
 */
public final class OpenCreative extends JavaPlugin {

    private static OpenCreative plugin;
    private final Map<UUID, Wander> wanders = new HashMap<>();
    private final Managers managers = new Managers();

    private Settings settings;
    private DevPlatformer devPlatformer;
    private SplitConfig splitConfig;
    private boolean splitConfigLoaded = false;

    /*
     * OpenCreative++: configuration is stored in several files (config.yml, groups.yml, items.yml...),
     * but the plugin still sees one configuration, so all OpenCreative+ code works as before.
     */

    private @NotNull SplitConfig splitConfig() {
        if (splitConfig == null) splitConfig = new SplitConfig(this);
        return splitConfig;
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (!splitConfigLoaded) reloadConfig();
        return splitConfig().get();
    }

    @Override
    public void reloadConfig() {
        splitConfig().load();
        splitConfigLoaded = true;
        PlusLog.setDebug(splitConfig().get().getBoolean("debug", false));
    }

    @Override
    public void saveConfig() {
        splitConfig().save();
    }

    @Override
    public void saveDefaultConfig() {
        splitConfig().saveDefaults();
    }

    private static final String version = "6.0.0 Pre-release 9";
    private static final String codename = "Build. Code. Play.";

    private static final int planetConfigVersion = 1;

    /**
     * Plugin load operations.
     *
     * @see #onEnable
     */
    @Override
    public void onLoad() {
        migrateOldDataFolder();
        getLogger().info(String.join("\n",
                "", "",
                "This software was made by Ukrainians, suffering from never-ending air alerts, explosions, and deaths.",
                "We're AGAINST THE WAR. This software IS NOT DESIGNED for those who support killing and robbing another country.",
                "",
                "Let us have fun, like players who create their worlds...",
                "McChicken Studio 2017–2026",
                ""
        ));
    }

    /**
     * OpenCreative++: copies data of OpenCreative+ (plugins/OpenCreative) into plugins/OpenCreativePP
     * on first launch, so worlds settings, locales and config are kept. Old folder is not changed.
     */
    private void migrateOldDataFolder() {
        File newFolder = getDataFolder();
        if (newFolder.exists()) return;
        // OpenCreative++ was called Creative+ (plugins/CreativePlus) before; OpenCreative+ uses plugins/OpenCreative.
        File oldFolder = new File(newFolder.getParentFile(), "CreativePlus");
        if (!oldFolder.isDirectory()) oldFolder = new File(newFolder.getParentFile(), "OpenCreative");
        if (!oldFolder.isDirectory()) return;
        try {
            org.apache.commons.io.FileUtils.copyDirectory(oldFolder, newFolder);
            PlusLog.module("Конфиг").info("Найдена папка plugins/" + oldFolder.getName() + ", данные скопированы в plugins/" + newFolder.getName()
                    + ". Старая папка не изменена, её можно удалить после проверки.");
        } catch (Exception error) {
            PlusLog.module("Конфиг").error("Не удалось скопировать plugins/OpenCreative в plugins/" + newFolder.getName(), error);
        }
    }

    /**
     * Plugin startup operations.
     * <p>
     * Loads settings, registers commands and events,
     * starts managers and notifies players about startup.
     *
     * @see #onDisable
     **/
    @Override
    public void onEnable() {
        plugin = this;
        long startTime = System.currentTimeMillis();
        logStartup();

        loadCore();
        loadManagers();

        // OpenCreative++: last worlds, /profile, lobby side panel and TAB
        getServer().getPluginManager().registerEvents(new ua.mcchickenstudio.opencreative.plus.players.LastWorlds(), this);
        var profileCommand = getCommand("profile");
        if (profileCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.players.ProfileCommand();
            profileCommand.setExecutor(executor);
            profileCommand.setTabCompleter(executor);
        }
        ua.mcchickenstudio.opencreative.plus.players.LobbyDisplay.start();
        getServer().getPluginManager().registerEvents(new ua.mcchickenstudio.opencreative.plus.players.Conveniences(), this);
        ua.mcchickenstudio.opencreative.plus.backups.Backups.start();
        ua.mcchickenstudio.opencreative.plus.web.WebSite.start();
        var backupsCommand = getCommand("backups");
        if (backupsCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.backups.BackupsMenu.BackupsCommand();
            backupsCommand.setExecutor(executor);
            backupsCommand.setTabCompleter(executor);
        }
        getServer().getPluginManager().registerEvents(new ua.mcchickenstudio.opencreative.plus.market.Market(), this);
        getServer().getPluginManager().registerEvents(ua.mcchickenstudio.opencreative.plus.contest.Contests.listener(), this);
        ua.mcchickenstudio.opencreative.plus.contest.Contests.start();
        var contestCommand = getCommand("contest");
        if (contestCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.contest.ContestMenu.ContestCommand();
            contestCommand.setExecutor(executor);
            contestCommand.setTabCompleter(executor);
        }
        var marketCommand = getCommand("market");
        if (marketCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.market.MarketMenu.MarketCommand();
            marketCommand.setExecutor(executor);
            marketCommand.setTabCompleter(executor);
        }
        var upgradesCommand = getCommand("upgrades");
        if (upgradesCommand != null) upgradesCommand.setExecutor(new ua.mcchickenstudio.opencreative.plus.shop.UpgradesMenu.UpgradesCommand());
        var rateCommand = getCommand("rate");
        if (rateCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.rating.RateMenu.RateCommand();
            rateCommand.setExecutor(executor);
            rateCommand.setTabCompleter(executor);
        }
        // OpenCreative++: built-in currency commands and rewards
        var economyCommands = new ua.mcchickenstudio.opencreative.plus.economy.EconomyCommands();
        for (String name : List.of("balance", "pay", "baltop", "eco")) {
            var pluginCommand = getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(economyCommands);
                pluginCommand.setTabCompleter(economyCommands);
            }
        }
        getServer().getPluginManager().registerEvents(new ua.mcchickenstudio.opencreative.plus.economy.EconomyRewards(), this);
        ua.mcchickenstudio.opencreative.plus.economy.EconomyRewards.start();
        // OpenCreative++: datapacks in worlds (datapacks.yml) and /datapack command
        ua.mcchickenstudio.opencreative.plus.datapacks.Datapacks.load();
        // OpenCreative++: saved variables were written only on world unload, a crash lost them. Save loaded worlds every 5 minutes.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (ua.mcchickenstudio.opencreative.planets.Planet planet : getPlanetsManager().getPlanets()) {
                if (planet.isLoaded()) planet.getVariables().save();
            }
        }, 6000L, 6000L);
        var datapackCommand = getCommand("datapack");
        if (datapackCommand != null) {
            var executor = new ua.mcchickenstudio.opencreative.plus.datapacks.DatapackCommand();
            datapackCommand.setExecutor(executor);
            datapackCommand.setTabCompleter(executor);
        }
        finalizeStartup(startTime);
        new Metrics(this, 22001);
    }

    /**
     * Plugin shutdown operations.
     * <p>
     * Unloads worlds when plugin is being disabled.
     *
     * @see #onEnable
     */
    @Override
    public void onDisable() {
        PlusLog.module("").info("Выключение OpenCreative++...");
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                                " \n<white> Выключение <white>OpenCreative<color:#74D3FF>++ <gray>" + getVersion() + "<white>...\n "
                        ));
                if (WorldUtils.isPlanet(player.getWorld())) {
                    teleportToLobby(player);
                }
            }
            ua.mcchickenstudio.opencreative.plus.datapacks.Datapacks.stopTicker();
            ua.mcchickenstudio.opencreative.plus.players.LobbyDisplay.stop();
            ua.mcchickenstudio.opencreative.plus.economy.EconomyRewards.stop();
            ua.mcchickenstudio.opencreative.plus.backups.Backups.stop();
            ua.mcchickenstudio.opencreative.plus.web.WebSite.stop();
            ua.mcchickenstudio.opencreative.plus.contest.Contests.stop();
            unloadManagers();
        } catch (Exception error) {
            PlusLog.module("").error("Ошибка при выключении", error);
        }
        if (getConfig().getBoolean("logs.startup-banner", false)) {
            getLogger().info(String.join("\n",
                    "",
                    "Goodbye from OpenCreative+",
                    "",
                    " " + codename,
                    "  Made by McChicken Studio 2017–2026",
                    ""
            ));
        } else {
            PlusLog.module("").info("OpenCreative++ выключен.");
        }
    }


    /**
     * Get a plugin instance for operations with it.
     * <p>
     * Useful for accessing planets manager, or settings.
     *
     * @return plugin instance.
     **/
    public static @NotNull OpenCreative getPlugin() {
        return plugin;
    }

    /**
     * Notifies console and players about OpenCreative+ startup.
     */
    private void logStartup() {
        PlusLog.module("").info("Запуск OpenCreative++ " + getPluginMeta().getVersion() + " (на основе OpenCreative+ " + version + ")...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
            player.showTitle(Title.title(
                    MiniMessage.miniMessage().deserialize("<white>OpenCreative<color:#74D3FF>++ <gray>" + getVersion()),
                    Component.text("§f" + codename + "..."),
                    Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(5), Duration.ofSeconds(0))
            ));
        }
    }

    /**
     * Loads OpenCreative+ settings, localization files,
     * registers commands and events.
     */
    private void loadCore() {
        settings = new Settings();
        HookUtils.loadHooks();
        settings.load(false);
        registerCommands();
        registerEvents();
        //Ticker.runTicker();
        FileUtils.loadLocales();
        PlayerUtils.loadPermissions();
    }

    /**
     * Loads and assigns OpenCreative+ managers.
     */
    @SuppressWarnings("ConstantConditions")
    private void loadManagers() {
        managers.register(WorldManager.class, new VanillaWorldManager());
        managers.register(PlanetsManager.class, new Space());
        managers.register(ModuleManager.class, new Moduler());
        managers.start(PlanetsManager.class, ModuleManager.class);
        if (devPlatformer == null) devPlatformer = new HorizontalPlatformer();
        managers.registerIfAbsent(CodingPrompter.class, new DisabledCodingPrompter());
        managers.registerIfAbsent(StabilityManager.class, new DisabledWatchdog());
        managers.registerIfAbsent(DownloadManager.class, new DisabledDownloader());
        managers.registerIfAbsent(Economy.class, new DisabledEconomy());
        managers.register(PhysicsManager.class, new PhysicsManager());
        managers.register(Updater.class, new HangarUpdater());
        managers.register(HintManager.class, new Hints());
        managers.register(PacketManager.class, HookUtils.getPacketManager());
        managers.register(BlocksManager.class, HookUtils.getBlocks());
        managers.register(DisguiseManager.class, HookUtils.getDisguises());
        managers.register(IgnoreManager.class, HookUtils.getIgnoreManager());
        managers.start(CodingPrompter.class, StabilityManager.class, DownloadManager.class,
                Economy.class, Updater.class, BlocksManager.class, HintManager.class,
                DisguiseManager.class, PacketManager.class, PhysicsManager.class,
                WorldManager.class);
    }

    /**
     * Shutdowns all managers of OpenCreative+.
     */
    private void unloadManagers() {
        managers.shutdown(PlanetsManager.class, ModuleManager.class,
                DownloadManager.class, Economy.class, StabilityManager.class,
                BlocksManager.class, PacketManager.class, DisguiseManager.class,
                CodingPrompter.class, Updater.class, HintManager.class,
                WorldManager.class);
    }

    /**
     * Notifies console and players about successful
     * startup of OpenCreative+.
     *
     * @param startTime timestamp of the beginning of startup.
     */
    private void finalizeStartup(long startTime) {
        long loadedTime = System.currentTimeMillis() - startTime;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (WorldUtils.isPlanet(player.getWorld())) {
                teleportToLobby(player);
            } else if (isEntityInLobby(player)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                Sounds.LOBBY.play(player);
                player.clearTitle();
                player.sendMessage(
                        MiniMessage.miniMessage().deserialize("\n <white>OpenCreative<color:#74D3FF>++ <gray>" + getVersion() + " <white>запущен <green>:) \n ")
                );
            }
        }
        getServer().sendActionBar(
                MiniMessage.miniMessage().deserialize(
                        "<white>OpenCreative<color:#74D3FF>++ <gray>" + getVersion() + "<white> запущен за " + loadedTime + " мс."
                )
        );
        if (getConfig().getBoolean("logs.startup-banner", false)) {
            String phrase = getRandomPhrase();
            getLogger().info(String.join("\n",
                    "OpenCreative+ " + version + ": " + codename + " is loaded for " + loadedTime + " ms.",
                    "",
                    " Welcome to OpenCreative+ " + version + "!",
                    "",
                    "  Running on " + Bukkit.getMinecraftVersion() + " server",
                    "  Current time " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
                    "  " + (phrase.isEmpty() ? "" : phrase + "\n"),
                    "  " + codename,
                    "  Made by McChicken Studio 2017–2026",
                    ""
            ));
        }
        PlusLog.module("").info(startupSummary(loadedTime));
        if (settings.isFirstLaunch()) {
            PlusLog.module("").info("Первый запуск: конфиги созданы в plugins/" + getDataFolder().getName()
                    + ". Начни с config.yml и groups.yml. Оригинал: https://gitlab.com/eagles-creative/opencreative");
        }
    }

    /**
     * One line with the state of OpenCreative++ after startup.
     */
    private @NotNull String startupSummary(long loadedTime) {
        List<String> hooks = new ArrayList<>();
        for (String name : List.of("Vault", "PlaceholderAPI", "WorldEdit", "FastAsyncWorldEdit", "ProtocolLib", "PacketEvents", "LibsDisguises")) {
            if (Bukkit.getPluginManager().isPluginEnabled(name)) hooks.add(name);
        }
        return "OpenCreative++ " + getPluginMeta().getVersion() + " запущен за " + loadedTime + " мс"
                + " | Paper " + Bukkit.getMinecraftVersion()
                + " | групп: " + settings.getGroups().getNames().size()
                + " | язык: " + getConfig().getString("messages.locale", "ru")
                + " | интеграции: " + (hooks.isEmpty() ? "нет" : String.join(", ", hooks));
    }

    /**
     * Registers plugin's commands and their tab completer. If some
     * commands failed to register, it notifies, but doesn't disable
     * entire plugin.
     */
    private void registerCommands() {
        PlusLog.module("").debug("Регистрация команд...");
        int registeredCommands = 0;
        Map<String, Class<? extends CommandExecutor>> commands = new HashMap<>();
        commands.put("creative", CreativeCommand.class);
        commands.put("spawn", SpawnCommand.class);
        commands.put("menu", MenuCommand.class);
        commands.put("world", WorldCommand.class);
        commands.put("chat", ChatCommand.class);
        commands.put("join", JoinCommand.class);
        commands.put("ad", AdvertisementCommand.class);
        commands.put("play", PlayCommand.class);
        commands.put("build", BuildCommand.class);
        commands.put("dev", DevCommand.class);
        commands.put("environment", EnvironmentCommand.class);
        commands.put("like", LikeCommand.class);
        commands.put("dislike", DislikeCommand.class);
        commands.put("locate", LocateCommand.class);
        commands.put("gamemode", GamemodeCommand.class);
        commands.put("give", GiveCommand.class);
        commands.put("teleport", TeleportCommand.class);
        commands.put("edit", EditCommand.class);
        commands.put("playsound", PlaySoundCommand.class);
        commands.put("stopsound", StopSoundCommand.class);
        commands.put("time", TimeCommand.class);
        commands.put("weather", WeatherCommand.class);
        commands.put("value", ValueCommand.class);
        commands.put("module", ModuleCommand.class);
        commands.put("jointo", JoinToCommand.class);
        commands.put("ownworlds", OwnMenuCommand.class);
        commands.put("color", ColorCommand.class);
        for (String commandName : commands.keySet()) {
            PluginCommand command =  getCommand(commandName);
            if (command != null) {
                try {
                    command.setExecutor(commands.get(commandName).getDeclaredConstructor().newInstance());
                    registeredCommands++;
                } catch (Exception error) {
                    sendCriticalErrorMessage("Couldn't register command " + commandName, error);
                }
            } else {
                sendCriticalErrorMessage("Couldn't get command with name " + commandName + ", it is null. Maybe it doesn't exist in plugins.yml?");
            }
        }
        if (registeredCommands != commands.size()) PlusLog.module("").warn("Зарегистрировано команд: " + registeredCommands + " из " + commands.size());
    }

    /**
     * Registers plugin's event listeners. If some listener is failed
     * to register, then it notifies, but doesn't disable entire plugin.
     */
    private void registerEvents() {
        PlusLog.module("").debug("Регистрация обработчиков событий...");
        int registeredListeners = 0;
        Class<?>[] listeners = new Class[]{
                ChangedWorld.class, EntitySpawnListener.class, EntityDamageListener.class,
                JoinListener.class, QuitListener.class, RespawnListener.class,
                DeathListener.class, TeleportListener.class, MoveListener.class,
                ChatListener.class, ua.mcchickenstudio.opencreative.plus.chat.SignColorListener.class, InteractListener.class, DropItemListener.class,
                PlaceBlockListener.class, DestroyBlockListener.class, BucketListener.class,
                ClickListener.class, RedstoneListener.class, BlockChangeListener.class,
                Menus.class, GameModeListener.class, EntityStateListener.class,
                CreativeListener.class, PotionListener.class, PlanetListener.class,
                CraftListener.class, WorldListener.class, WitherCreationListener.class,
                ServerListener.class
        };
        for (Class<?> listenerClass : listeners) {
            try {
                getServer().getPluginManager().registerEvents(
                        (Listener) listenerClass.getDeclaredConstructor().newInstance(), this
                );
                registeredListeners++;
            } catch (Exception exception) {
                sendCriticalErrorMessage("Couldn't register event listener: " + listenerClass.getSimpleName(), exception);
            }
        }
        if (registeredListeners != listeners.length) PlusLog.module("").warn("Зарегистрировано обработчиков: " + registeredListeners + " из " + listeners.length);
    }

    /**
     * Returns OpenCreative+ settings.
     *
     * @return settings of plugin.
     */
    public static @NotNull Settings getSettings() {
        return getPlugin().settings;
    }

    /**
     * Returns managers registry.
     *
     * @return registry of managers.
     */
    public static @NotNull Managers getManagers() {
        return getPlugin().managers;
    }

    /**
     * Gets economy manager, that has money operations for players.
     *
     * @return economy manager.
     */
    public static Economy getEconomy() {
        return getPlugin().managers.get(Economy.class);
    }

    /**
     * Gets packet manager, that has packets modifiers methods.
     *
     * @return packet manager.
     */
    public static PacketManager getPacketManager() {
        return getPlugin().managers.get(PacketManager.class);
    }

    /**
     * Gets hint manager, that sends suggestions to players in action bar.
     *
     * @return hint manager.
     */
    @SuppressWarnings("unused")
    public static HintManager getHintManager() {
        return getPlugin().managers.get(HintManager.class);
    }

    /**
     * Gets physics manager, that handles physical objects..
     *
     * @return physics manager.
     */
    @SuppressWarnings("unused")
    public static PhysicsManager getPhysicsManager() {
        return getPlugin().managers.get(PhysicsManager.class);
    }

    /**
     * Gets blocks manager, that changes a lot
     * of blocks in world.
     *
     * @return blocks manager.
     */
    @SuppressWarnings("unused")
    public static BlocksManager getBlocksManager() {
        return getPlugin().managers.get(BlocksManager.class);
    }

    /**
     * Gets disguise manager, that disguises
     * entities as players, other entities, blocks.
     *
     * @return disguise manager.
     */
    @SuppressWarnings("unused")
    public static DisguiseManager getDisguiseManager() {
        return getPlugin().managers.get(DisguiseManager.class);
    }

    /**
     * Gets module manager, that creates
     * or deletes modules.
     *
     * @return modules manager.
     */
    public static ModuleManager getModuleManager() {
        return getPlugin().managers.get(ModuleManager.class);
    }

    /**
     * Sets custom coding platforms manager.
     *
     * @param platformsManager developer platforms manager.
     */
    @SuppressWarnings("unused")
    public static void setDevPlatformer(@NotNull DevPlatformer platformsManager) {
        if (!(platformsManager instanceof VerticalPlatformer || platformsManager instanceof HorizontalPlatformer)) {
            getPlugin().getLogger().info("Now using dev platforms manager: " + platformsManager.getName());
        }
        getPlugin().devPlatformer = platformsManager;
    }

    /**
     * Gets coding platforms manager, that
     * creates and manipulates with dev platforms
     * in developer worlds.
     *
     * @return coding platforms manager.
     */
    @SuppressWarnings("unused")
    public static DevPlatformer getDevPlatformer() {
        return getPlugin().devPlatformer;
    }

    /**
     * Gets world manager, that loads
     * and unloads worlds.
     *
     * @return world manager.
     */
    @SuppressWarnings("unused")
    public static WorldManager getWorldManager() {
        return getPlugin().managers.get(WorldManager.class);
    }

    /**
     * Gets download manager, that
     * uploads world acrhive and allows
     * players to download it.
     *
     * @return download manager.
     */
    @SuppressWarnings("unused")
    public static DownloadManager getDownloadManager() {
        return getPlugin().managers.get(DownloadManager.class);
    }

    /**
     * Gets coding prompt manager, that
     * generates code by players prompts.
     *
     * @return coding prompter.
     */
    @SuppressWarnings("unused")
    public static CodingPrompter getCodingPrompter() {
        return getPlugin().managers.get(CodingPrompter.class);
    }

    /**
     * Gets planets manager, that stores planets in base
     * and has methods to create, find and delete them.
     *
     * @return planets manager.
     */
    public static PlanetsManager getPlanetsManager() {
        return getPlugin().managers.get(PlanetsManager.class);
    }

    /**
     * Gets stability manager, that checks server's
     * performance and makes sure everything is fine.
     *
     * @return stability manager.
     */
    public static StabilityManager getStability() {
        return getPlugin().managers.get(StabilityManager.class);
    }

    /**
     * Gets version of OpenCreative+.
     *
     * @return version of plugin.
     */
    public static @NotNull String getVersion() {
        // OpenCreative++: version of the fork (plugin.yml), not of OpenCreative+ it is based on
        return getPlugin().getPluginMeta().getVersion();
    }

    /**
     * OpenCreative++: version of OpenCreative+, that this fork is based on.
     */
    public static @NotNull String getBaseVersion() {
        return version;
    }

    /**
     * Gets update manager, that has methods to
     * check available updates for plugin.
     */
    public static Updater getUpdater() {
        return getPlugin().managers.get(Updater.class);
    }

    /**
     * Gets codename of current OpenCreative+ version.
     *
     * @return codename of version.
     */
    public static @NotNull String getCodename() {
        return codename;
    }

    /**
     * Returns version of the current planet's config format.
     *
     * @return planet's config version number.
     */
    public static int getPlanetConfigVersion() {
        return planetConfigVersion;
    }

    /**
     * Returns a random phrase for startup message that depends
     * on today's date, or empty string - if it's boring date!
     *
     * @return random phrase, or empty "".
     */
    private static @NotNull String getRandomPhrase() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        switch (month) {
            case Calendar.APRIL -> {
                if (day == 17) return "Happy birthday, OpenCreative++ Wiki! :-)";
            }
            case Calendar.AUGUST -> {
                if (day == 24) return "Happy birthday, PryanikXXX! :-)";
                if (day == 28) return "Happy birthday, Eagle's OpenCreative++! :-)";
            }
            case Calendar.OCTOBER -> {
                if (day == 31) return "Spo-o-o-oky Halloween, server owners! O_o 🎃";
            }
            case Calendar.DECEMBER -> {
                if (day == 9) return "Happy birthday, OpenCreative+! :-)";
                if (day == 16) return "Happy " + (calendar.get(Calendar.YEAR)-2014) + "th birthday, OpenCreative++ mode! :-)";
                if (day == 25) return "Ho-ho-ho! Merry Christmas, server owners! :-) ❆";
                if (day == 31) return "Ho-ho-ho! Let's meet " + (calendar.get(Calendar.YEAR)+1) + " together! :-) ❆";
            }
            case Calendar.JANUARY -> {
                if (day <= 2) return "Ho-ho-ho! Happy New Year " + calendar.get(Calendar.YEAR) + ", server owners! :-) ❆";
            }
        }
        return "";
    }

    public @NotNull Wander registerWander(@NotNull Player player) {
        Wander wander = new Wander(player);
        wanders.put(player.getUniqueId(), wander);
        return wander;
    }

    public void unregisterWander(@NotNull Player player) {
        wanders.remove(player.getUniqueId());
    }

    /**
     * Returns online wander, that plays on server.
     *
     * @return wander - if online, otherwise - null
     */
    public static @Nullable Wander getWander(@NotNull UUID uuid) {
        return getPlugin().wanders.get(uuid);
    }

    /**
     * Returns online wander casted by player.
     *
     * @return wander of player.
     */
    public static @NotNull Wander getWander(@NotNull Player player) {
        Wander wander = getWander(player.getUniqueId());
        if (wander == null) {
            wander = getPlugin().registerWander(player);
        }
        return wander;
    }

    /**
     * Returns offline wander, that can be online or offline.
     *
     * @return offline wander.
     */
    public static @NotNull OfflineWander getOfflineWander(@NotNull UUID uuid) {
        return new OfflineWander(uuid);
    }

}
