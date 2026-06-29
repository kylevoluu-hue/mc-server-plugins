package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Operator-defined AFK zones that pay out Coins over time. Zones are cuboids selected
 * with a position wand-free workflow ({@code /afkzone pos1|pos2|create}) and persisted
 * in {@code economy-data.yml}. A repeating task awards Coins to players standing in a
 * zone; an optional "actually AFK" check withholds pay from players who keep moving.
 */
public final class AfkZoneManager {

    /** A named cuboid that pays Coins. */
    public static final class AfkZone {
        public final String name;
        public final String world;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final long coins;

        AfkZone(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, long coins) {
            this.name = name;
            this.world = world;
            this.minX = Math.min(x1, x2);
            this.minY = Math.min(y1, y2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxY = Math.max(y1, y2);
            this.maxZ = Math.max(z1, z2);
            this.coins = coins;
        }

        boolean contains(Location loc) {
            if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) {
                return false;
            }
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    private final LumenEssentials plugin;
    private final Map<String, AfkZone> zones = new LinkedHashMap<>();
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private int taskId = -1;

    public AfkZoneManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        load();
        int interval = Math.max(1, plugin.configManager().economy()
                .getInt("afk-zone.interval-seconds", 60));
        this.taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::award, interval * 20L, interval * 20L).getTaskId();
    }

    public void shutdown() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void load() {
        zones.clear();
        ConfigurationSection root = plugin.configManager().economyData().getConfigurationSection("afk-zones");
        if (root == null) {
            return;
        }
        long defaultCoins = plugin.configManager().economy().getLong("afk-zone.coins-per-interval", 10);
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            zones.put(name.toLowerCase(Locale.ROOT), new AfkZone(name.toLowerCase(Locale.ROOT),
                    s.getString("world", "world"),
                    s.getInt("min.x"), s.getInt("min.y"), s.getInt("min.z"),
                    s.getInt("max.x"), s.getInt("max.y"), s.getInt("max.z"),
                    s.getLong("coins", defaultCoins)));
        }
    }

    private void save() {
        FileConfiguration data = plugin.configManager().economyData();
        data.set("afk-zones", null);
        for (AfkZone zone : zones.values()) {
            String base = "afk-zones." + zone.name;
            data.set(base + ".world", zone.world);
            data.set(base + ".min.x", zone.minX);
            data.set(base + ".min.y", zone.minY);
            data.set(base + ".min.z", zone.minZ);
            data.set(base + ".max.x", zone.maxX);
            data.set(base + ".max.y", zone.maxY);
            data.set(base + ".max.z", zone.maxZ);
            data.set(base + ".coins", zone.coins);
        }
        plugin.configManager().saveEconomyData();
    }

    public void setPos1(Player player) {
        pos1.put(player.getUniqueId(), player.getLocation());
        MessageUtil.send(player, "&aPosition 1 set to your location.");
    }

    public void setPos2(Player player) {
        pos2.put(player.getUniqueId(), player.getLocation());
        MessageUtil.send(player, "&aPosition 2 set to your location.");
    }

    public boolean create(Player player, String name) {
        Location a = pos1.get(player.getUniqueId());
        Location b = pos2.get(player.getUniqueId());
        if (a == null || b == null || a.getWorld() == null || a.getWorld() != b.getWorld()) {
            MessageUtil.send(player, "&cSet pos1 and pos2 in the same world first.");
            return false;
        }
        long defaultCoins = plugin.configManager().economy().getLong("afk-zone.coins-per-interval", 10);
        zones.put(name.toLowerCase(Locale.ROOT), new AfkZone(name.toLowerCase(Locale.ROOT),
                a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ(), defaultCoins));
        save();
        return true;
    }

    public boolean remove(String name) {
        boolean removed = zones.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean setReward(String name, long coins) {
        AfkZone zone = zones.get(name.toLowerCase(Locale.ROOT));
        if (zone == null) {
            return false;
        }
        zones.put(zone.name, new AfkZone(zone.name, zone.world,
                zone.minX, zone.minY, zone.minZ, zone.maxX, zone.maxY, zone.maxZ, coins));
        save();
        return true;
    }

    public List<String> zoneNames() {
        return new ArrayList<>(zones.keySet());
    }

    private void award() {
        if (!plugin.configManager().economy().getBoolean("afk-zone.enabled", true) || zones.isEmpty()) {
            return;
        }
        boolean requireAfk = plugin.configManager().economy().getBoolean("afk-zone.require-actually-afk", false);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location loc = player.getLocation();
            AfkZone zone = zoneAt(loc);
            if (zone == null) {
                lastLocation.put(player.getUniqueId(), loc);
                continue;
            }
            if (requireAfk && hasMoved(player.getUniqueId(), loc)) {
                lastLocation.put(player.getUniqueId(), loc);
                continue;
            }
            lastLocation.put(player.getUniqueId(), loc);
            plugin.economyManager().addCoins(player.getUniqueId(), zone.coins);
            MessageUtil.send(player, "&6+" + zone.coins + " Coins &7(AFK zone)");
        }
    }

    private AfkZone zoneAt(Location loc) {
        for (AfkZone zone : zones.values()) {
            if (zone.contains(loc)) {
                return zone;
            }
        }
        return null;
    }

    private boolean hasMoved(UUID uuid, Location now) {
        Location last = lastLocation.get(uuid);
        if (last == null || last.getWorld() != now.getWorld()) {
            return false;
        }
        return last.distanceSquared(now) > 1.0D;
    }
}
