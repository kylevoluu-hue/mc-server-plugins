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
    private final ConfigFile warps;
    private final ConfigFile flags;

    public ConfigManager(Plugin plugin) {
        this.config = new ConfigFile(plugin, "config.yml");
        this.checks = new ConfigFile(plugin, "checks.yml");
        this.messages = new ConfigFile(plugin, "messages.yml");
        this.punishments = new ConfigFile(plugin, "punishments.yml");
        this.investigation = new ConfigFile(plugin, "investigation.yml");
        this.features = new ConfigFile(plugin, "features.yml");
        this.warps = new ConfigFile(plugin, "warps.yml", true);
        this.flags = new ConfigFile(plugin, "flags.yml", true);
    }

    public void loadAll() {
        config.load();
        checks.load();
        messages.load();
        punishments.load();
        investigation.load();
        features.load();
        warps.load();
        flags.load();
    }

    public void reloadAll() {
        config.reload();
        checks.reload();
        messages.reload();
        punishments.reload();
        investigation.reload();
        features.reload();
        warps.reload();
        flags.reload();
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

    /** Convenience accessor for a message string with a fallback. */
    public String message(String path, String fallback) {
        return messages().getString(path, fallback);
    }
}
