package com.lumen.essentials.alerts;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import com.lumen.essentials.violations.Violation;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes staff alerts and debug output to subscribed staff. Subscriptions are
 * toggled with {@code /luac alerts} / {@code /luac debug} and held in memory only.
 */
public final class AlertManager {

    private final LumenEssentials plugin;
    private final Set<UUID> alertSubscribers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> debugSubscribers = ConcurrentHashMap.newKeySet();

    public AlertManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public boolean toggleAlerts(Player player) {
        return toggle(alertSubscribers, player.getUniqueId());
    }

    public boolean toggleDebug(Player player) {
        return toggle(debugSubscribers, player.getUniqueId());
    }

    public boolean isReceivingAlerts(UUID uuid) {
        return alertSubscribers.contains(uuid);
    }

    public boolean isReceivingDebug(UUID uuid) {
        return debugSubscribers.contains(uuid);
    }

    private boolean toggle(Set<UUID> set, UUID uuid) {
        if (set.contains(uuid)) {
            set.remove(uuid);
            return false;
        }
        set.add(uuid);
        return true;
    }

    /** Broadcasts a formatted alert to all subscribed staff with the alerts permission. */
    public void alert(Violation violation) {
        String template = plugin.configManager().message("alert-format",
                "&8[&bLumen&8] &e{player} &7failed &c{check} &7(&fvl {vl}&7) "
                        + "&8| &7ping {ping}ms tps {tps}");
        String message = template
                .replace("{player}", violation.player())
                .replace("{check}", violation.checkName())
                .replace("{vl}", format(violation.violationLevel()))
                .replace("{ping}", String.valueOf(violation.ping()))
                .replace("{tps}", format(violation.tps()))
                .replace("{world}", violation.world())
                .replace("{x}", format(violation.x()))
                .replace("{y}", format(violation.y()))
                .replace("{z}", format(violation.z()));

        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("luac.alerts") && isReceivingAlerts(staff.getUniqueId())) {
                MessageUtil.send(staff, message);
            }
        }
    }

    /**
     * Broadcasts a non-violation informational alert (xray suspicion, investigation
     * events) to staff subscribed to alerts. Prefixed but otherwise free-form.
     */
    public void notifyStaff(String message) {
        String formatted = "&8[&bLumen&8] &7" + message;
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("luac.alerts") && isReceivingAlerts(staff.getUniqueId())) {
                MessageUtil.send(staff, formatted);
            }
        }
    }

    /** Sends a low-level debug line to staff that opted into debug output. */
    public void debug(String message) {
        String formatted = "&8[&bLumen&8/&dDebug&8] &7" + message;
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("luac.debug") && isReceivingDebug(staff.getUniqueId())) {
                MessageUtil.send(staff, formatted);
            }
        }
    }

    /** Cleans up subscriptions when a staff member disconnects. */
    public void handleQuit(UUID uuid) {
        alertSubscribers.remove(uuid);
        debugSubscribers.remove(uuid);
    }

    private String format(double value) {
        return String.format("%.1f", value);
    }
}
