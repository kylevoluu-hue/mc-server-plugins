package com.lumen.essentials.teleport;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central teleport orchestration shared by /tpa, /tpahere, /tpaccept, /tpauto, /tp,
 * /warp and /rtp. Provides per-player cooldowns and a warmup countdown shown on a
 * neon-yellow boss bar that cancels if the player moves or enters combat.
 */
public final class TeleportManager {

    private final LumenEssentials plugin;

    private final Map<UUID, TeleportRequest> requests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Warmup> warmups = new HashMap<>();
    private final java.util.Set<UUID> autoAccept = ConcurrentHashMap.newKeySet();

    /** Active warmup state for a single player. */
    private static final class Warmup {
        BossBar bar;
        int taskId;
        Location start;
        int secondsLeft;
        Location destination;
        Runnable onComplete;
    }

    public TeleportManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.configManager().features();
    }

    // --- Requests ----------------------------------------------------------

    public void sendRequest(Player requester, Player target, boolean here) {
        int expiry = cfg().getInt("teleport.request-expiry-seconds", 60);
        TeleportRequest request = new TeleportRequest(requester.getUniqueId(),
                requester.getName(), here, System.currentTimeMillis() + expiry * 1000L);
        requests.put(target.getUniqueId(), request);

        MessageUtil.send(requester, cfg().getString("teleport.message-request-sent",
                "&aTeleport request sent to &f{target}&a.").replace("{target}", target.getName()));
        MessageUtil.send(target, cfg().getString("teleport.message-request-received",
                "&e{player} &7wants to teleport. &a/tpaccept&7.").replace("{player}", requester.getName()));

        if (autoAccept.contains(target.getUniqueId())) {
            accept(target);
        }
    }

    public boolean accept(Player target) {
        TeleportRequest request = requests.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            MessageUtil.send(target, "&cYou have no pending teleport requests.");
            return false;
        }
        Player requester = plugin.getServer().getPlayer(request.requester());
        if (requester == null) {
            MessageUtil.send(target, "&cThat player is no longer online.");
            return false;
        }
        if (request.here()) {
            // Target teleports to the requester.
            beginWarmup(target, requester.getLocation(), null);
        } else {
            // Requester teleports to the target.
            beginWarmup(requester, target.getLocation(), null);
        }
        return true;
    }

    public boolean toggleAuto(Player player) {
        if (autoAccept.remove(player.getUniqueId())) {
            return false;
        }
        autoAccept.add(player.getUniqueId());
        return true;
    }

    // --- Warmup + cooldown -------------------------------------------------

    /**
     * Starts a warmup countdown then teleports the player. Honors the shared
     * cooldown; cancels on movement/combat. {@code onComplete} runs after a
     * successful teleport (may be null).
     */
    public void beginWarmup(Player player, Location destination, Runnable onComplete) {
        if (destination == null) {
            return;
        }
        long remaining = cooldownRemaining(player.getUniqueId());
        if (remaining > 0 && !player.hasPermission("luac.admin")) {
            MessageUtil.send(player, cfg().getString("teleport.message-on-cooldown",
                    "&cYou must wait &f{time}s&c.").replace("{time}", String.valueOf(remaining)));
            return;
        }
        cancelWarmup(player.getUniqueId(), null); // replace any existing warmup

        int warmupSeconds = cfg().getInt("teleport.warmup-seconds", 3);
        if (warmupSeconds <= 0) {
            complete(player, destination, onComplete);
            return;
        }

        Warmup warmup = new Warmup();
        warmup.start = player.getLocation();
        warmup.secondsLeft = warmupSeconds;
        warmup.destination = destination;
        warmup.onComplete = onComplete;
        warmup.bar = createBar(warmupSeconds);
        if (warmup.bar != null) {
            try {
                warmup.bar.addPlayer(player);
            } catch (Throwable ignored) {
                // ignore
            }
        }
        warmups.put(player.getUniqueId(), warmup);

        warmup.taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, () -> tickWarmup(player.getUniqueId()), 20L, 20L).getTaskId();
    }

    private void tickWarmup(UUID uuid) {
        Warmup warmup = warmups.get(uuid);
        if (warmup == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            cancelWarmup(uuid, null);
            return;
        }
        // Cancel on combat.
        if (plugin.combatTagManager().isTagged(uuid)) {
            cancelWarmup(uuid, cfg().getString("teleport.message-cancelled-combat",
                    "&cTeleport cancelled - you are in combat!"));
            return;
        }
        // Cancel on movement beyond the threshold.
        double maxMove = cfg().getDouble("teleport.cancel-on-move-distance", 0.5);
        if (movedTooFar(warmup.start, player.getLocation(), maxMove)) {
            cancelWarmup(uuid, cfg().getString("teleport.message-cancelled-move",
                    "&cTeleport cancelled - you moved!"));
            return;
        }

        warmup.secondsLeft--;
        if (warmup.secondsLeft <= 0) {
            Location dest = warmup.destination;
            Runnable onComplete = warmup.onComplete;
            cancelWarmup(uuid, null); // tears down bar + task
            complete(player, dest, onComplete);
            return;
        }
        updateBar(warmup);
    }

    private void complete(Player player, Location destination, Runnable onComplete) {
        player.teleport(destination);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        if (onComplete != null) {
            onComplete.run();
        }
    }

    public boolean isInWarmup(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    public void cancelWarmup(UUID uuid, String redMessage) {
        Warmup warmup = warmups.remove(uuid);
        if (warmup == null) {
            return;
        }
        if (warmup.taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(warmup.taskId);
        }
        if (warmup.bar != null) {
            try {
                warmup.bar.removeAll();
            } catch (Throwable ignored) {
                // ignore
            }
        }
        if (redMessage != null) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                MessageUtil.send(player, redMessage);
            }
        }
    }

    private boolean movedTooFar(Location start, Location now, double max) {
        if (start.getWorld() != now.getWorld()) {
            return true;
        }
        return start.distanceSquared(now) > max * max;
    }

    private BossBar createBar(int total) {
        try {
            String raw = cfg().getString("teleport.bar-color", "YELLOW");
            BarColor color;
            try {
                color = BarColor.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException ex) {
                color = BarColor.YELLOW;
            }
            String title = cfg().getString("teleport.bar-title", "&eTeleporting in &f{time}&e...")
                    .replace("{time}", total + "s");
            BossBar bar = plugin.getServer() == null ? null : org.bukkit.Bukkit
                    .createBossBar(MessageUtil.color(title), color, BarStyle.SOLID);
            if (bar != null) {
                bar.setProgress(1.0);
            }
            return bar;
        } catch (Throwable ignored) {
            return null; // legacy: no boss bar
        }
    }

    private void updateBar(Warmup warmup) {
        if (warmup.bar == null) {
            return;
        }
        try {
            int total = cfg().getInt("teleport.warmup-seconds", 3);
            String title = cfg().getString("teleport.bar-title", "&eTeleporting in &f{time}&e...")
                    .replace("{time}", warmup.secondsLeft + "s");
            warmup.bar.setTitle(MessageUtil.color(title));
            warmup.bar.setProgress(total <= 0 ? 0 : Math.max(0.0,
                    Math.min(1.0, (double) warmup.secondsLeft / total)));
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private long cooldownRemaining(UUID uuid) {
        Long last = cooldowns.get(uuid);
        if (last == null) {
            return 0;
        }
        int cooldown = cfg().getInt("teleport.cooldown-seconds", 5);
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0, cooldown - elapsed);
    }

    public void handleQuit(UUID uuid) {
        cancelWarmup(uuid, null);
        requests.remove(uuid);
    }
}
