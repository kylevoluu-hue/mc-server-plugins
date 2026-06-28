package com.lumen.essentials.punishments;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.violations.Violation;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Executes configurable punishment actions when a check crosses its punishment
 * threshold. Actions are expressed as console commands in {@code punishments.yml}
 * with placeholder substitution, which keeps the action set fully data-driven
 * (kick, tempban, ban, custom commands) without hardcoding any policy.
 *
 * <p>Defaults ship conservative: a single staff alert command, to avoid false bans.
 */
public final class PunishmentManager {

    private final LumenEssentials plugin;

    public PunishmentManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves and dispatches the configured commands for the given violation's
     * check. Commands are always run on the main thread (Bukkit requirement).
     */
    public void punish(Violation violation) {
        List<String> commands = resolveCommands(violation.checkName());
        if (commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            String command = applyPlaceholders(raw, violation);
            if (command.isEmpty()) {
                continue;
            }
            // Always dispatch on the main thread; we may be called from a listener.
            if (Bukkit.isPrimaryThread()) {
                dispatch(command);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> dispatch(command));
            }
        }
    }

    private void dispatch(String command) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Throwable ex) {
            plugin.getLogger().warning("Failed to execute punishment command '" + command + "': " + ex.getMessage());
        }
    }

    private List<String> resolveCommands(String checkName) {
        ConfigurationSection actions = plugin.configManager().punishments()
                .getConfigurationSection("actions");
        if (actions != null && actions.isList(checkName)) {
            return actions.getStringList(checkName);
        }
        return plugin.configManager().punishments().getStringList("default");
    }

    private String applyPlaceholders(String raw, Violation v) {
        return raw
                .replace("{player}", v.player())
                .replace("{check}", v.checkName())
                .replace("{vl}", String.format("%.1f", v.violationLevel()))
                .replace("{ping}", String.valueOf(v.ping()))
                .replace("{tps}", String.format("%.1f", v.tps()))
                .replace("{world}", v.world())
                .replace("{x}", String.format("%.0f", v.x()))
                .replace("{y}", String.format("%.0f", v.y()))
                .replace("{z}", String.format("%.0f", v.z()))
                .trim();
    }
}
