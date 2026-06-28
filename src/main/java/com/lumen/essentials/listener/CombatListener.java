package com.lumen.essentials.listener;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.combat.CombatCheck;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Drives all combat checks when a player melee-attacks an entity. */
public final class CombatListener implements Listener {

    private final LumenEssentials plugin;

    public CombatListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        if (attacker.hasPermission("luac.bypass")) {
            return;
        }
        PlayerData data = plugin.playerDataManager().getOrCreate(attacker);

        for (CombatCheck check : plugin.checkManager().combat()) {
            if (check.isEnabled()) {
                try {
                    check.handleAttack(attacker, data, event.getEntity(), event);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Combat check " + check.name() + " errored: " + t);
                }
            }
        }
    }
}
