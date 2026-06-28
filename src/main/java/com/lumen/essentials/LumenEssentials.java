package com.lumen.essentials;

import com.lumen.essentials.alerts.AlertManager;
import com.lumen.essentials.antixray.AntiXrayManager;
import com.lumen.essentials.api.LumenAPI;
import com.lumen.essentials.checks.CheckManager;
import com.lumen.essentials.command.CommandManager;
import com.lumen.essentials.config.ConfigManager;
import com.lumen.essentials.investigation.InvestigationManager;
import com.lumen.essentials.listener.CombatListener;
import com.lumen.essentials.listener.ConnectionListener;
import com.lumen.essentials.listener.MovementListener;
import com.lumen.essentials.listener.WorldListener;
import com.lumen.essentials.player.PlayerDataManager;
import com.lumen.essentials.punishments.PunishmentManager;
import com.lumen.essentials.storage.StorageManager;
import com.lumen.essentials.version.VersionManager;
import com.lumen.essentials.violations.ViolationManager;
import org.bukkit.command.PluginCommand;
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

        this.api = new LumenAPI(this);

        // 4. Listeners.
        registerListeners();

        // 5. Command.
        this.commandManager = new CommandManager(this);
        PluginCommand command = getCommand("luac");
        if (command != null) {
            command.setExecutor(commandManager);
            command.setTabCompleter(commandManager);
        } else {
            getLogger().severe("Command 'luac' missing from plugin.yml; commands disabled.");
        }

        getLogger().info("Lumen Essentials enabled (" + checkManager.all().size()
                + " checks, adapter " + versionManager.adapter().name() + ").");
    }

    @Override
    public void onDisable() {
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
    }

    /** Reloads configs and re-applies them to checks and submodules (hot reload). */
    public void reloadConfiguration() {
        configManager.reloadAll();
        checkManager.reload();
        antiXrayManager.reload();
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

    /** The public developer API. */
    public LumenAPI api() {
        return api;
    }
}
