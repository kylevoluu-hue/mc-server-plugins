package com.lumen.essentials.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Central access point for all of the plugin's configuration files. Each logical
 * domain (general, checks, messages, punishments, investigation) lives in its own
 * file so server owners can reason about them independently, and all support hot
 * reload via {@link #reloadAll()}.
 */
public final class ConfigManager {

    private final ConfigFile config;
    private final ConfigFile checks;
    private final ConfigFile messages;
    private final ConfigFile punishments;
    private final ConfigFile investigation;
    private final ConfigFile features;
    // Runtime data stores (written to by the plugin, not shipped defaults).
    private final ConfigFile economy;
    private final ConfigFile warps;
    private final ConfigFile flags;
    private final ConfigFile settings;
    private final ConfigFile economyData;
    private final ConfigFile duelsData;

    public ConfigManager(Plugin plugin) {
        this.config = new ConfigFile(plugin, "config.yml");
        this.checks = new ConfigFile(plugin, "checks.yml");
        this.messages = new ConfigFile(plugin, "messages.yml");
        this.punishments = new ConfigFile(plugin, "punishments.yml");
        this.investigation = new ConfigFile(plugin, "investigation.yml");
        this.features = new ConfigFile(plugin, "features.yml");
        this.economy = new ConfigFile(plugin, "economy.yml");
        this.warps = new ConfigFile(plugin, "warps.yml", true);
        this.flags = new ConfigFile(plugin, "flags.yml", true);
        this.settings = new ConfigFile(plugin, "settings.yml", true);
        this.economyData = new ConfigFile(plugin, "economy-data.yml", true);
        this.duelsData = new ConfigFile(plugin, "duels-data.yml", true);
    }

    public void loadAll() {
        config.load();
        checks.load();
        messages.load();
        punishments.load();
        investigation.load();
        features.load();
        economy.load();
        warps.load();
        flags.load();
        settings.load();
        economyData.load();
        duelsData.load();
    }

    public void reloadAll() {
        config.reload();
        checks.reload();
        messages.reload();
        punishments.reload();
        investigation.reload();
        features.reload();
        economy.reload();
        warps.reload();
        flags.reload();
        settings.reload();
        economyData.reload();
        duelsData.reload();
    }

    public FileConfiguration config() {
        return config.get();
    }

    public FileConfiguration checks() {
        return checks.get();
    }

    public FileConfiguration messages() {
        return messages.get();
    }

    public FileConfiguration punishments() {
        return punishments.get();
    }

    public FileConfiguration investigation() {
        return investigation.get();
    }

    public FileConfiguration features() {
        return features.get();
    }

    public FileConfiguration warps() {
        return warps.get();
    }

    public void saveWarps() {
        warps.save();
    }

    public FileConfiguration flags() {
        return flags.get();
    }

    public void saveFlags() {
        flags.save();
    }

    public FileConfiguration settings() {
        return settings.get();
    }

    public void saveSettings() {
        settings.save();
    }

    public FileConfiguration economy() {
        return economy.get();
    }

    public FileConfiguration economyData() {
        return economyData.get();
    }

    public void saveEconomyData() {
        economyData.save();
    }

    public FileConfiguration duelsData() {
        return duelsData.get();
    }

    public void saveDuelsData() {
        duelsData.save();
    }

    /** Convenience accessor for a message string with a fallback. */
    public String message(String path, String fallback) {
        return messages().getString(path, fallback);
    }
}
