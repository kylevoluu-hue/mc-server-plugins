package com.lumen.essentials.rtp;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Random-teleport around a configurable center point. Designed to stay cheap: each
 * request makes at most {@code max-attempts} bounded safe-location probes, validating
 * the surface block before teleporting. Per-player cooldowns prevent spamming the
 * search.
 */
public final class RtpManager {

    private final LumenEssentials plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RtpManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection cfg() {
        return plugin.configManager().features().getConfigurationSection("rtp");
    }

    public boolean isEnabled() {
        ConfigurationSection c = cfg();
        return c == null || c.getBoolean("enabled", true);
    }

    public void setWorld(String world) {
        plugin.configManager().features().set("rtp.world", world);
    }

    public void setMaxRadius(int radius) {
        plugin.configManager().features().set("rtp.max-radius", radius);
    }

    /**
     * Attempts a random teleport for the player. Returns true on success; messaging
     * is handled here.
     */
    public boolean teleport(Player player) {
        if (!isEnabled()) {
            MessageUtil.send(player, "&cRTP is currently disabled.");
            return false;
        }
        long remaining = cooldownRemaining(player.getUniqueId());
        if (remaining > 0 && !player.hasPermission("luac.admin")) {
            MessageUtil.send(player, "&cYou must wait &f" + remaining + "s &cbefore using /rtp again.");
            return false;
        }

        ConfigurationSection c = cfg();
        World world = plugin.getServer().getWorld(c.getString("world", "world"));
        if (world == null) {
            MessageUtil.send(player, "&cThe RTP world is not loaded.");
            return false;
        }

        int centerX = c.getInt("center-x", 0);
        int centerZ = c.getInt("center-z", 0);
        int maxRadius = c.getInt("max-radius", 5000);
        int minRadius = c.getInt("min-radius", 250);
        int attempts = Math.max(1, c.getInt("max-attempts", 30));

        Location safe = findSafe(world, centerX, centerZ, minRadius, maxRadius, attempts);
        if (safe == null) {
            MessageUtil.send(player, "&cCould not find a safe location, try again.");
            return false;
        }

        player.teleport(safe);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        MessageUtil.send(player, "&aTeleported to &f" + safe.getBlockX() + ", "
                + safe.getBlockY() + ", " + safe.getBlockZ() + "&a.");
        return true;
    }

    private Location findSafe(World world, int cx, int cz, int min, int max, int attempts) {
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * Math.max(1, max - min);
            int x = cx + (int) (Math.cos(angle) * dist);
            int z = cz + (int) (Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            if (isSafe(ground)) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private boolean isSafe(Block ground) {
        if (ground == null) {
            return false;
        }
        Material type = ground.getType();
        if (type == Material.LAVA || type == Material.WATER || !type.isSolid()) {
            return false;
        }
        String name = type.name();
        // Avoid fire/cactus/magma-style hazards.
        return !name.contains("FIRE") && !name.contains("MAGMA") && !name.contains("CACTUS");
    }

    private long cooldownRemaining(UUID uuid) {
        Long last = cooldowns.get(uuid);
        if (last == null) {
            return 0;
        }
        int cooldown = cfg().getInt("cooldown-seconds", 30);
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0, cooldown - elapsed);
    }
}
