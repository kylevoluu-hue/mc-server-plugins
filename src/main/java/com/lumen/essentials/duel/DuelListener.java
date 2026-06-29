package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bridges Bukkit events into the {@link DuelManager}: routes damage (to detect lethal
 * hits without a real death) and handles disconnects as forfeits.
 */
public final class DuelListener implements Listener {

    private final LumenEssentials plugin;

    public DuelListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        plugin.duelManager().handleDamage(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.duelManager().handleQuit(event.getPlayer().getUniqueId());
    }
}
