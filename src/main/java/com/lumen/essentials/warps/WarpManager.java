package com.lumen.essentials.warps;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Persistent named-warp store backed by {@code warps.yml}. Warps are simple named
 * destinations (e.g. {@code spawn}, {@code duels}) set by operators.
 */
public final class WarpManager {

    private final LumenEssentials plugin;
    private final Map<String, Location> warps = new LinkedHashMap<>();

    public WarpManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void load() {
        warps.clear();
        FileConfiguration cfg = plugin.configManager().warps();
        ConfigurationSection root = cfg.getConfigurationSection("warps");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            World world = Bukkit.getWorld(s.getString("world", "world"));
            if (world == null) {
                continue; // world not loaded yet; skip silently
            }
            warps.put(name.toLowerCase(Locale.ROOT), new Location(world,
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                    (float) s.getDouble("yaw"), (float) s.getDouble("pitch")));
        }
    }

    public void save() {
        FileConfiguration cfg = plugin.configManager().warps();
        cfg.set("warps", null);
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            Location loc = entry.getValue();
            String base = "warps." + entry.getKey();
            cfg.set(base + ".world", loc.getWorld() == null ? "world" : loc.getWorld().getName());
            cfg.set(base + ".x", loc.getX());
            cfg.set(base + ".y", loc.getY());
            cfg.set(base + ".z", loc.getZ());
            cfg.set(base + ".yaw", loc.getYaw());
            cfg.set(base + ".pitch", loc.getPitch());
        }
        plugin.configManager().saveWarps();
    }

    public void set(String name, Location location) {
        warps.put(name.toLowerCase(Locale.ROOT), location);
        save();
    }

    public boolean delete(String name) {
        boolean removed = warps.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Location get(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return warps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public List<String> names() {
        return new ArrayList<>(warps.keySet());
    }
}
