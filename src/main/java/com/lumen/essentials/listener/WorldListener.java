package com.lumen.essentials.listener;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.world.BreakCheck;
import com.lumen.essentials.checks.world.PlaceCheck;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Drives world checks (break/place), feeds the anti-xray analyzer and notifies the
 * investigation manager about breaks near test objects. Runs at MONITOR priority so
 * other plugins' cancellations (claims, regions) are respected before we analyze.
 */
public final class WorldListener implements Listener {

    private final LumenEssentials plugin;

    public WorldListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.playerDataManager().getOrCreate(player);

        if (!player.hasPermission("luac.bypass")) {
            for (BreakCheck check : plugin.checkManager().breaks()) {
                if (check.isEnabled()) {
                    safeBreak(check, player, data, event);
                }
            }
        }

        // Anti-xray and investigation observation are independent of bypass so staff
        // still get ore logs, but exempt/staff players don't generate suspicion.
        plugin.antiXrayManager().handleBreak(player, data, event.getBlock());
        plugin.investigationManager().handleBreakNear(player, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("luac.bypass")) {
            return;
        }
        PlayerData data = plugin.playerDataManager().getOrCreate(player);
        for (PlaceCheck check : plugin.checkManager().places()) {
            if (check.isEnabled()) {
                try {
                    check.handlePlace(player, data, event);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Place check " + check.name() + " errored: " + t);
                }
            }
        }
    }

    private void safeBreak(BreakCheck check, Player player, PlayerData data, BlockBreakEvent event) {
        try {
            check.handleBreak(player, data, event);
        } catch (Throwable t) {
            plugin.getLogger().warning("Break check " + check.name() + " errored: " + t);
        }
    }
}
