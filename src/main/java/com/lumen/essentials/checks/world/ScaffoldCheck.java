package com.lumen.essentials.checks.world;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Detects scaffold/bridging cheats: rapidly placing blocks beneath oneself while
 * moving, often without looking down. Combines a fast-place timing component with a
 * "placed below feet while not looking down" heuristic, and buffers results so
 * legitimate fast bridging by skilled players is unlikely to flag.
 */
public final class ScaffoldCheck extends PlaceCheck {

    private static final String BUFFER = "scaffold";
    private static final int MAX_SAMPLES = 10;

    public ScaffoldCheck(LumenEssentials plugin) {
        super(plugin, "scaffold");
    }

    @Override
    public void handlePlace(Player player, PlayerData data, BlockPlaceEvent event) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        long now = System.currentTimeMillis();
        long previous = data.placeTimes().isEmpty() ? 0L : data.placeTimes().peekLast();
        data.recordPlace(now, MAX_SAMPLES);

        Block placed = event.getBlockPlaced();
        boolean belowFeet = placed.getY() < player.getLocation().getY();
        // Bridging hacks usually keep the pitch high (looking forward) while placing below.
        boolean notLookingDown = player.getLocation().getPitch() < 30.0F;
        boolean fast = previous != 0L && (now - previous) < (settings().threshold() <= 0 ? 110L
                : (long) settings().threshold());

        if (belowFeet && notLookingDown && fast) {
            double buffer = data.addBuffer(BUFFER, 1.0D, 0.0D, settings().buffer() + 4.0D);
            if (buffer > settings().buffer()) {
                fail(player, data, 1.0D, String.format("bridge place pitch=%.0f interval<%s buf=%.1f",
                        player.getLocation().getPitch(), "limit", buffer));
            }
        } else {
            data.addBuffer(BUFFER, -1.0D, 0.0D, settings().buffer() + 4.0D);
        }
    }
}
