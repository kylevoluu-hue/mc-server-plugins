package com.lumen.essentials.listener;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Manages {@link PlayerData} lifecycle and captures the client brand (Paper exposes
 * it a tick or two after join, so we read it on a short delay).
 */
public final class ConnectionListener implements Listener {

    private final LumenEssentials plugin;

    public ConnectionListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.playerDataManager().getOrCreate(player);

        // Read the reported client brand shortly after join if the API exists.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            PlayerData data = plugin.playerDataManager().getIfPresent(player.getUniqueId());
            if (data == null) {
                return;
            }
            String brand = readBrand(player);
            data.setClientBrand(brand);
            if (plugin.configManager().config().getBoolean("logging.log-client-brand", true)) {
                plugin.storageManager().logInvestigation(
                        "BRAND " + player.getName() + " brand=" + brand);
            }
        }, 40L);
    }

    private String readBrand(Player player) {
        try {
            // Paper: Player#getClientBrandName(); resolved reflectively for portability.
            Object result = player.getClass().getMethod("getClientBrandName").invoke(player);
            return result == null ? "unknown" : result.toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.playerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markTeleport();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerDataManager().remove(event.getPlayer().getUniqueId());
        plugin.alertManager().handleQuit(event.getPlayer().getUniqueId());
        plugin.teleportManager().handleQuit(event.getPlayer().getUniqueId());
    }
}
