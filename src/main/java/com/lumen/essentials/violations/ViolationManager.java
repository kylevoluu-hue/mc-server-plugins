package com.lumen.essentials.violations;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.api.event.PunishmentEvent;
import com.lumen.essentials.api.event.ViolationEvent;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.checks.CheckSettings;
import com.lumen.essentials.player.PlayerData;
import com.lumen.essentials.utilities.ServerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Central coordinator between checks and the rest of the system. Applies exemptions,
 * confidence scaling and decay, records the violation, fires the developer API
 * events, and routes alerts/punishments once thresholds are crossed.
 *
 * <p>Keeping this logic in one place is what lets individual checks remain tiny and
 * keeps anti-false-positive policy (confidence weighting, decay, thresholds)
 * uniform and auditable.
 */
public final class ViolationManager {

    private static final int RECENT_LIMIT = 100;

    private final LumenEssentials plugin;
    private final Deque<Violation> recent = new ArrayDeque<>();

    public ViolationManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a detection from {@code check} against {@code player}.
     *
     * @param amount raw violation weight; scaled by the check's confidence before use
     * @param debug  diagnostic string for verbose output and logs
     */
    public void flag(Check check, Player player, PlayerData data, double amount, String debug) {
        if (player == null || data == null) {
            return;
        }
        // Exemptions short-circuit everything (trusted players / staff testing).
        if (data.isExempt() || player.hasPermission("luac.bypass")) {
            return;
        }

        CheckSettings settings = check.settings();
        double scaledAmount = amount * Math.max(0.0D, settings.confidence());
        double vl = data.violations().add(check.name(), scaledAmount, settings.decay());

        int ping = ServerUtil.getPing(player);
        double tps = ServerUtil.getTps();
        Location loc = player.getLocation();

        Violation violation = Violation.builder()
                .player(player.getName())
                .checkName(check.name())
                .category(check.category())
                .violationLevel(vl)
                .confidence(settings.confidence())
                .ping(ping)
                .tps(tps)
                .location(loc.getX(), loc.getY(), loc.getZ(),
                        loc.getWorld() == null ? "unknown" : loc.getWorld().getName())
                .serverVersion(plugin.versionManager().versionString())
                .pluginVersion(plugin.getDescription().getVersion())
                .debugInfo(settings.debug() ? debug : "")
                .build();

        // Fire the API event; listeners may cancel to suppress side effects.
        ViolationEvent event = new ViolationEvent(player, violation);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        record(violation);
        plugin.storageManager().logViolation(violation);

        if (settings.debug()) {
            plugin.alertManager().debug(player.getName() + " [" + check.name() + "] vl="
                    + String.format("%.1f", vl) + " " + debug);
        }

        if (vl >= settings.alertThreshold()) {
            plugin.alertManager().alert(violation);
        }

        // Auto-flag once a player's violation level for a check crosses the threshold.
        double autoFlagThreshold = plugin.configManager().config()
                .getDouble("auto-flag.violation-threshold", 15.0D);
        if (autoFlagThreshold > 0 && vl >= autoFlagThreshold) {
            plugin.flagManager().autoFlag(player,
                    "Auto: " + check.name() + " (vl " + String.format("%.1f", vl) + ")");
        }

        if (vl >= settings.punishmentThreshold()) {
            PunishmentEvent punishEvent = new PunishmentEvent(player, violation);
            plugin.getServer().getPluginManager().callEvent(punishEvent);
            if (!punishEvent.isCancelled()) {
                plugin.punishmentManager().punish(violation);
                // Reset after punishing so the same threshold is not re-triggered every tick.
                data.violations().reset(check.name());
            }
        }
    }

    private void record(Violation violation) {
        synchronized (recent) {
            recent.addLast(violation);
            while (recent.size() > RECENT_LIMIT) {
                recent.pollFirst();
            }
        }
    }

    /** Snapshot of the most recent violations (newest last). */
    public Collection<Violation> recentViolations() {
        synchronized (recent) {
            return new ArrayDeque<>(recent);
        }
    }
}
