package com.lumen.essentials.listener;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.movement.MovementCheck;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

/**
 * Drives all movement checks. Refreshes the shared movement snapshot once per
 * position change (ignoring pure look-only moves to save work), then dispatches to
 * each enabled {@link MovementCheck}. Velocity and knockback events are recorded so
 * checks can grant leniency around legitimate launches.
 */
public final class MovementListener implements Listener {

    private final LumenEssentials plugin;

    public MovementListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        // Skip look-only movement: cheap early-out for the most common event.
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("luac.bypass")) {
            return;
        }
        PlayerData data = plugin.playerDataManager().getOrCreate(player);

        boolean onGround = isOnGround(player);
        data.movement().update(to, onGround);
        data.tickGround(onGround);

        for (MovementCheck check : plugin.checkManager().movement()) {
            if (check.isEnabled()) {
                try {
                    check.handle(player, data, event);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Movement check " + check.name() + " errored: " + t);
                }
            }
        }
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = plugin.playerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markVelocity();
        }
    }

    @EventHandler
    public void onKnockback(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            PlayerData data = plugin.playerDataManager()
                    .getIfPresent(event.getEntity().getUniqueId());
            if (data != null) {
                // Damage often imparts knockback; treat like velocity for leniency.
                data.markVelocity();
            }
        }
    }

    private boolean isOnGround(Player player) {
        try {
            return player.isOnGround();
        } catch (Throwable ignored) {
            // Fallback: sample the block beneath the player.
            return player.getLocation().clone().subtract(0, 0.05, 0).getBlock().getType().isSolid();
        }
    }
}
