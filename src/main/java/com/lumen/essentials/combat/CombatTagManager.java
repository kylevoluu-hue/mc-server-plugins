package com.lumen.essentials.combat;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Combat-tag ("anti-combat-log") system. A player is tagged for a configurable
 * duration whenever they deal or take player damage; each new hit resets the timer.
 * A neon-blue boss bar shows the countdown, and tagged players are blocked from
 * teleporting. On servers without the boss-bar API (legacy) it degrades to silent
 * tagging.
 */
public final class CombatTagManager {

    private final LumenEssentials plugin;
    private final Map<UUID, Long> tagExpiry = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private int taskId = -1;

    public CombatTagManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.configManager().features().getBoolean("combat.enabled", true);
    }

    public void start() {
        // Update boss bars / expiry once per second.
        this.taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    public void shutdown() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (BossBar bar : bars.values()) {
            try {
                bar.removeAll();
            } catch (Throwable ignored) {
                // ignore
            }
        }
        bars.clear();
        tagExpiry.clear();
    }

    /** Tags a player (or resets their timer) for the configured duration. */
    public void tag(Player player) {
        if (!isEnabled() || player == null) {
            return;
        }
        int duration = plugin.configManager().features().getInt("combat.duration-seconds", 15);
        boolean wasTagged = isTagged(player.getUniqueId());
        tagExpiry.put(player.getUniqueId(), System.currentTimeMillis() + duration * 1000L);
        updateBar(player, duration);
        if (!wasTagged) {
            MessageUtil.send(player, "&cYou are now in combat!");
        }
    }

    public boolean isTagged(UUID uuid) {
        Long expiry = tagExpiry.get(uuid);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid) {
        Long expiry = tagExpiry.get(uuid);
        if (expiry == null) {
            return 0;
        }
        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000L);
    }

    /** Clears a player's tag and removes their boss bar (e.g. on death/quit). */
    public void clear(UUID uuid) {
        tagExpiry.remove(uuid);
        BossBar bar = bars.remove(uuid);
        if (bar != null) {
            try {
                bar.removeAll();
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    private void updateBar(Player player, int totalDuration) {
        BossBar bar = bars.get(player.getUniqueId());
        long remaining = remainingSeconds(player.getUniqueId());
        String title = plugin.configManager().features()
                .getString("combat.bar-title", "&bCombat : &f{time}")
                .replace("{time}", remaining + "s");
        try {
            if (bar == null) {
                BarColor color = parseColor();
                bar = Bukkit.createBossBar(MessageUtil.color(title), color, BarStyle.SOLID);
                bar.addPlayer(player);
                bars.put(player.getUniqueId(), bar);
            } else {
                bar.setTitle(MessageUtil.color(title));
            }
            double progress = totalDuration <= 0 ? 0 : Math.min(1.0, (double) remaining / totalDuration);
            bar.setProgress(Math.max(0.0, progress));
        } catch (Throwable ignored) {
            // Boss bar API unavailable (legacy server); tagging still works silently.
        }
    }

    private BarColor parseColor() {
        String raw = plugin.configManager().features().getString("combat.bar-color", "BLUE");
        try {
            return BarColor.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BarColor.BLUE;
        }
    }

    private void tick() {
        int duration = plugin.configManager().features().getInt("combat.duration-seconds", 15);
        Iterator<Map.Entry<UUID, Long>> it = tagExpiry.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (entry.getValue() <= System.currentTimeMillis()) {
                it.remove();
                BossBar bar = bars.remove(uuid);
                if (bar != null) {
                    try {
                        bar.removeAll();
                    } catch (Throwable ignored) {
                        // ignore
                    }
                }
                if (player != null) {
                    MessageUtil.send(player, "&aYou are no longer in combat.");
                }
            } else if (player != null) {
                updateBar(player, duration);
            }
        }
    }
}
