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

    public ConfigManager(Plugin plugin) {
        this.config = new ConfigFile(plugin, "config.yml");
        this.checks = new ConfigFile(plugin, "checks.yml");
        this.messages = new ConfigFile(plugin, "messages.yml");
        this.punishments = new ConfigFile(plugin, "punishments.yml");
        this.investigation = new ConfigFile(plugin, "investigation.yml");
    }

    public void loadAll() {
        config.load();
        checks.load();
        messages.load();
        punishments.load();
        investigation.load();
    }

    public void reloadAll() {
        config.reload();
        checks.reload();
        messages.reload();
        punishments.reload();
        investigation.reload();
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

    /** Convenience accessor for a message string with a fallback. */
    public String message(String path, String fallback) {
        return messages().getString(path, fallback);
    }
}
