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
    private final boolean dataFile;
    private FileConfiguration configuration;

    public ConfigFile(Plugin plugin, String fileName) {
        this(plugin, fileName, false);
    }

    /**
     * @param dataFile when true this is a runtime data store (no bundled default);
     *                 it is created empty if missing instead of copied from the jar.
     */
    public ConfigFile(Plugin plugin, String fileName, boolean dataFile) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.dataFile = dataFile;
    }

    /** Saves the bundled default (or creates an empty data file), then loads it. */
    public void load() {
        if (!file.exists()) {
            if (dataFile) {
                createEmpty();
            } else {
                // saveResource copies the packaged default without overwriting user edits.
                plugin.saveResource(fileName, false);
            }
        }
        reload();
    }

    private void createEmpty() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
        } catch (java.io.IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not create data file " + fileName, ex);
        }
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
