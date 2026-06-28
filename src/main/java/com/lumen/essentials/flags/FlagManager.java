package com.lumen.essentials.flags;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores manual staff flags and persists them to {@code flags.yml}. Flags are keyed
 * by player so re-flagging updates the reason rather than duplicating the entry.
 */
public final class FlagManager {

    private final LumenEssentials plugin;
    private final Map<UUID, Flag> flags = new LinkedHashMap<>();

    public FlagManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void load() {
        flags.clear();
        FileConfiguration cfg = plugin.configManager().flags();
        ConfigurationSection root = cfg.getConfigurationSection("flags");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                flags.put(uuid, new Flag(uuid,
                        s.getString("name", "?"),
                        s.getString("flagger", "?"),
                        s.getString("reason", "No reason"),
                        s.getLong("time", System.currentTimeMillis()),
                        s.getString("world", "world"),
                        s.getDouble("x"), s.getDouble("y"), s.getDouble("z")));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID keys.
            }
        }
    }

    public void save() {
        FileConfiguration cfg = plugin.configManager().flags();
        cfg.set("flags", null); // clear then rewrite
        for (Flag flag : flags.values()) {
            String base = "flags." + flag.target();
            cfg.set(base + ".name", flag.targetName());
            cfg.set(base + ".flagger", flag.flagger());
            cfg.set(base + ".reason", flag.reason());
            cfg.set(base + ".time", flag.timestamp());
            cfg.set(base + ".world", flag.world());
            cfg.set(base + ".x", flag.x());
            cfg.set(base + ".y", flag.y());
            cfg.set(base + ".z", flag.z());
        }
        plugin.configManager().saveFlags();
    }

    /** Adds or updates a flag for a player, persisting immediately. */
    public Flag add(Player target, String flagger, String reason) {
        Location loc = target.getLocation();
        Flag flag = new Flag(target.getUniqueId(), target.getName(), flagger, reason,
                System.currentTimeMillis(),
                loc.getWorld() == null ? "world" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ());
        flags.put(target.getUniqueId(), flag);
        save();
        return flag;
    }

    public boolean remove(UUID uuid) {
        boolean removed = flags.remove(uuid) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean isFlagged(UUID uuid) {
        return flags.containsKey(uuid);
    }

    public List<Flag> all() {
        return new ArrayList<>(flags.values());
    }

    public int size() {
        return flags.size();
    }
}
