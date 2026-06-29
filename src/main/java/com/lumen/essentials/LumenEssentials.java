package com.lumen.essentials;

import com.lumen.essentials.alerts.AlertManager;
import com.lumen.essentials.antixray.AntiXrayManager;
import com.lumen.essentials.api.LumenAPI;
import com.lumen.essentials.checks.CheckManager;
import com.lumen.essentials.combat.CombatListener2;
import com.lumen.essentials.combat.CombatTagManager;
import com.lumen.essentials.command.CommandManager;
import com.lumen.essentials.command.FeatureCommandHandler;
import com.lumen.essentials.command.EconomyCommandHandler;
import com.lumen.essentials.config.ConfigManager;
import com.lumen.essentials.economy.AfkZoneManager;
import com.lumen.essentials.economy.CrateManager;
import com.lumen.essentials.economy.EconomyManager;
import com.lumen.essentials.economy.MerchantManager;
import com.lumen.essentials.economy.PlaytimeRewardManager;
import com.lumen.essentials.flags.FlagManager;
import com.lumen.essentials.gui.MenuListener;
import com.lumen.essentials.investigation.InvestigationManager;
import com.lumen.essentials.listener.CombatListener;
import com.lumen.essentials.listener.ConnectionListener;
import com.lumen.essentials.listener.MovementListener;
import com.lumen.essentials.listener.SilkSpawnerListener;
import com.lumen.essentials.listener.WorldListener;
import com.lumen.essentials.player.PlayerDataManager;
import com.lumen.essentials.punishments.PunishmentManager;
import com.lumen.essentials.rtp.RtpManager;
import com.lumen.essentials.settings.SettingsListener;
import com.lumen.essentials.settings.SettingsManager;
import com.lumen.essentials.stats.StatsManager;
import com.lumen.essentials.storage.StorageManager;
import com.lumen.essentials.teleport.TeleportManager;
import com.lumen.essentials.version.VersionManager;
import com.lumen.essentials.violations.ViolationManager;
import com.lumen.essentials.warps.WarpManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point and composition root for Lumen Essentials. Constructs and owns every
 * manager, wires the command and event listeners, and exposes typed accessors used
 * throughout the plugin. All wiring lives here so the dependency graph is explicit
 * and there are no static singletons to reason about.
 */
public final class LumenEssentials extends JavaPlugin {

    private ConfigManager configManager;
    private VersionManager versionManager;
    private StorageManager storageManager;
    private PlayerDataManager playerDataManager;
    private AlertManager alertManager;
    private PunishmentManager punishmentManager;
    private ViolationManager violationManager;
    private CheckManager checkManager;
    private AntiXrayManager antiXrayManager;
    private InvestigationManager investigationManager;
    private StatsManager statsManager;
    private FlagManager flagManager;
    private WarpManager warpManager;
    private RtpManager rtpManager;
    private CombatTagManager combatTagManager;
    private TeleportManager teleportManager;
    private SettingsManager settingsManager;
    private EconomyManager economyManager;
    private CrateManager crateManager;
    private MerchantManager merchantManager;
    private AfkZoneManager afkZoneManager;
    private PlaytimeRewardManager playtimeRewardManager;
    private CommandManager commandManager;
    private LumenAPI api;

    @Override
    public void onEnable() {
        // 1. Configuration first; everything else reads from it.
        this.configManager = new ConfigManager(this);
        configManager.loadAll();

        // 2. Version detection selects the correct adapter.
        this.versionManager = new VersionManager(getLogger());
        versionManager.detect();

        // 3. Core managers.
        this.storageManager = new StorageManager(this);
        storageManager.start();

        this.playerDataManager = new PlayerDataManager();
        this.alertManager = new AlertManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.violationManager = new ViolationManager(this);

        this.checkManager = new CheckManager(this);
        checkManager.registerAll();

        this.antiXrayManager = new AntiXrayManager(this);
        antiXrayManager.reload();

        this.investigationManager = new InvestigationManager(this);
        investigationManager.start();

        // SMP feature managers.
        this.statsManager = new StatsManager();
        this.flagManager = new FlagManager(this);
        flagManager.load();
        this.warpManager = new WarpManager(this);
        warpManager.load();
        this.rtpManager = new RtpManager(this);
        this.combatTagManager = new CombatTagManager(this);
        combatTagManager.start();
        this.teleportManager = new TeleportManager(this);
        this.settingsManager = new SettingsManager(this);
        settingsManager.start();
        // Load settings for anyone already online (e.g. on /reload).
        for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
            settingsManager.load(online);
        }

        // Economy: coins, keys, crates, merchant, AFK zones, playtime rewards.
        this.economyManager = new EconomyManager(this);
        economyManager.start();
        this.crateManager = new CrateManager(this);
        crateManager.reload();
        this.merchantManager = new MerchantManager(this);
        merchantManager.reload();
        this.afkZoneManager = new AfkZoneManager(this);
        afkZoneManager.start();
        this.playtimeRewardManager = new PlaytimeRewardManager(this);
        playtimeRewardManager.start();

        this.api = new LumenAPI(this);

        // 4. Listeners.
        registerListeners();

        // 5. Commands.
        this.commandManager = new CommandManager(this);
        PluginCommand command = getCommand("luac");
        if (command != null) {
            command.setExecutor(commandManager);
            command.setTabCompleter(commandManager);
        } else {
            getLogger().severe("Command 'luac' missing from plugin.yml; commands disabled.");
        }
        registerFeatureCommands();
        registerEconomyCommands();

        getLogger().info("Lumen Essentials enabled (" + checkManager.all().size()
                + " checks, adapter " + versionManager.adapter().name() + ").");
    }

    @Override
    public void onDisable() {
        if (playtimeRewardManager != null) {
            playtimeRewardManager.shutdown();
        }
        if (afkZoneManager != null) {
            afkZoneManager.shutdown();
        }
        if (economyManager != null) {
            economyManager.shutdown();
        }
        if (settingsManager != null) {
            settingsManager.shutdown();
        }
        if (combatTagManager != null) {
            combatTagManager.shutdown();
        }
        if (investigationManager != null) {
            investigationManager.shutdown();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.clear();
        }
        getLogger().info("Lumen Essentials disabled.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener2(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new SilkSpawnerListener(this), this);
        getServer().getPluginManager().registerEvents(new SettingsListener(this), this);
    }

    private void registerFeatureCommands() {
        FeatureCommandHandler handler = new FeatureCommandHandler(this);
        String[] commands = {"flag", "flaglist", "stats", "warp", "sswarp", "rtp",
                "rtpworld", "rtpamount", "tpa", "tpahere", "tpaccept", "tpauto", "tp", "settings"};
        bind(commands, handler, handler);
    }

    private void registerEconomyCommands() {
        EconomyCommandHandler handler = new EconomyCommandHandler(this);
        String[] commands = {"coins", "keys", "key", "crates", "crate", "merchant", "afkzone", "economy"};
        bind(commands, handler, handler);
    }

    private void bind(String[] names, CommandExecutor executor, TabCompleter completer) {
        for (String name : names) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(executor);
                command.setTabCompleter(completer);
            } else {
                getLogger().warning("Command '" + name + "' missing from plugin.yml.");
            }
        }
    }

    /** Reloads configs and re-applies them to checks and submodules (hot reload). */
    public void reloadConfiguration() {
        configManager.reloadAll();
        checkManager.reload();
        antiXrayManager.reload();
        if (flagManager != null) {
            flagManager.load();
        }
        if (warpManager != null) {
            warpManager.load();
        }
        if (crateManager != null) {
            crateManager.reload();
        }
        if (merchantManager != null) {
            merchantManager.reload();
        }
        if (afkZoneManager != null) {
            afkZoneManager.load();
        }
    }

    // --- Accessors ---------------------------------------------------------

    public ConfigManager configManager() {
        return configManager;
    }

    public VersionManager versionManager() {
        return versionManager;
    }

    public StorageManager storageManager() {
        return storageManager;
    }

    public PlayerDataManager playerDataManager() {
        return playerDataManager;
    }

    public AlertManager alertManager() {
        return alertManager;
    }

    public PunishmentManager punishmentManager() {
        return punishmentManager;
    }

    public ViolationManager violationManager() {
        return violationManager;
    }

    public CheckManager checkManager() {
        return checkManager;
    }

    public AntiXrayManager antiXrayManager() {
        return antiXrayManager;
    }

    public InvestigationManager investigationManager() {
        return investigationManager;
    }

    public StatsManager statsManager() {
        return statsManager;
    }

    public FlagManager flagManager() {
        return flagManager;
    }

    public WarpManager warpManager() {
        return warpManager;
    }

    public RtpManager rtpManager() {
        return rtpManager;
    }

    public CombatTagManager combatTagManager() {
        return combatTagManager;
    }

    public TeleportManager teleportManager() {
        return teleportManager;
    }

    public SettingsManager settingsManager() {
        return settingsManager;
    }

    public EconomyManager economyManager() {
        return economyManager;
    }

    public CrateManager crateManager() {
        return crateManager;
    }

    public MerchantManager merchantManager() {
        return merchantManager;
    }

    public AfkZoneManager afkZoneManager() {
        return afkZoneManager;
    }

    public PlaytimeRewardManager playtimeRewardManager() {
        return playtimeRewardManager;
    }

    /** The public developer API. */
    public LumenAPI api() {
        return api;
    }
}
