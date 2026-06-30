package com.lumen.essentials.settings;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Iterator;

/**
 * Enforces the server-side {@code /settings} toggles: loads/saves per-player state,
 * hides global chat, blocks natural hostile-mob spawns around opted-out players, and
 * instantly respawns players who opt in.
 */
public final class SettingsListener implements Listener {

    private static final double MOB_RADIUS = 48.0D;

    private final LumenEssentials plugin;

    public SettingsListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.settingsManager().load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.settingsManager().unload(event.getPlayer().getUniqueId());
    }

    /** Death clears potion effects, so re-apply night vision a tick after respawn. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.settingsManager().applyNightVision(event.getPlayer()), 2L);
    }

    /** Hide Global Chat: drop opted-out players from the recipient set. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Iterator<Player> it = event.getRecipients().iterator();
        while (it.hasNext()) {
            Player recipient = it.next();
            // Never hide a player's own message from themselves.
            if (recipient.equals(event.getPlayer())) {
                continue;
            }
            if (plugin.settingsManager().isEnabled(recipient.getUniqueId(), SettingType.HIDE_CHAT)) {
                it.remove();
            }
        }
    }

    /**
     * Hostile Mob Spawning: cancel a natural hostile spawn only when every player near
     * the spawn has opted out (so it still spawns if a nearby player allows mobs).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        Location loc = event.getEntity().getLocation();
        if (loc.getWorld() == null) {
            return;
        }
        boolean anyNearby = false;
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) > MOB_RADIUS * MOB_RADIUS) {
                continue;
            }
            anyNearby = true;
            // If a single nearby player allows mobs, let the spawn proceed.
            if (!plugin.settingsManager().isEnabled(player.getUniqueId(), SettingType.NO_HOSTILE_MOBS)) {
                return;
            }
        }
        if (anyNearby) {
            event.setCancelled(true);
        }
    }

    /** Instant Respawn: trigger an immediate respawn a tick after death. */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.settingsManager().isEnabled(player.getUniqueId(), SettingType.INSTANT_RESPAWN)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> instantRespawn(player), 1L);
    }

    private void instantRespawn(Player player) {
        if (!player.isOnline()) {
            return;
        }
        try {
            // Spigot exposes player.spigot().respawn(); resolved reflectively for portability.
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            spigot.getClass().getMethod("respawn").invoke(spigot);
        } catch (Throwable ignored) {
            // Respawn helper unavailable on this build; the player uses the normal screen.
        }
    }
}
