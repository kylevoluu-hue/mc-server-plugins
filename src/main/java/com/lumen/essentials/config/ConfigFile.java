package com.lumen.essentials.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Wraps a single YAML resource file, handling saving of the bundled default and
 * (re)loading from disk. Keeps {@link ConfigManager} small and uniform.
 */
public final class ConfigFile {

    private final Plugin plugin;
    private final String fileName;
    private final File file;
    private FileConfiguration configuration;

    public ConfigFile(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    /** Saves the bundled default if the file is missing, then loads it. */
    public void load() {
        if (!file.exists()) {
            // saveResource copies the packaged default without overwriting user edits.
            plugin.saveResource(fileName, false);
        }
        reload();
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (configuration == null) {
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save " + fileName, ex);
        }
    }

    public FileConfiguration get() {
        if (configuration == null) {
            reload();
        }
        return configuration;
    }

    public String name() {
        return fileName;
    }
}
