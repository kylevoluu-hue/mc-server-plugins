package com.lumen.essentials.combat;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Locale;

/**
 * Wires the combat-tag system to gameplay: tags both players on PvP damage, blocks
 * configured commands while tagged, and optionally punishes combat logging on quit.
 */
public final class CombatListener2 implements Listener {

    private final LumenEssentials plugin;

    public CombatListener2(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!plugin.combatTagManager().isEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            plugin.combatTagManager().tag((Player) event.getEntity());
            plugin.combatTagManager().tag((Player) event.getDamager());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.combatTagManager().isTagged(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("luac.bypass")) {
            return;
        }
        String first = stripSlash(event.getMessage());
        List<String> blocked = plugin.configManager().features().getStringList("combat.blocked-commands");
        for (String cmd : blocked) {
            if (first.equalsIgnoreCase(cmd)) {
                event.setCancelled(true);
                long remaining = plugin.combatTagManager().remainingSeconds(player.getUniqueId());
                MessageUtil.send(player, "&cYou cannot do that in combat! &f(" + remaining + "s left)");
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean tagged = plugin.combatTagManager().isTagged(player.getUniqueId());
        if (tagged) {
            // Kill combat-loggers: they die on logout and respawn at spawn next login.
            if (plugin.configManager().features().getBoolean("combat.kill-on-combat-log", true)) {
                try {
                    player.setHealth(0.0D);
                } catch (Throwable ignored) {
                    // Some forks restrict setHealth on quit; the punish commands still run.
                }
            }
            if (plugin.configManager().features().getBoolean("combat.punish-combat-log.enabled", false)) {
                for (String raw : plugin.configManager().features()
                        .getStringList("combat.punish-combat-log.commands")) {
                    String command = raw.replace("{player}", player.getName());
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getServer().dispatchCommand(
                                    plugin.getServer().getConsoleSender(), command));
                }
            }
        }
        plugin.combatTagManager().clear(player.getUniqueId());
    }

    private String stripSlash(String message) {
        String trimmed = message.startsWith("/") ? message.substring(1) : message;
        int space = trimmed.indexOf(' ');
        if (space != -1) {
            trimmed = trimmed.substring(0, space);
        }
        // Strip plugin namespace (e.g. "essentials:tp" -> "tp").
        int colon = trimmed.indexOf(':');
        if (colon != -1) {
            trimmed = trimmed.substring(colon + 1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
