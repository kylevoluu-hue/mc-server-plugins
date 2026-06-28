package com.lumen.essentials.checks.world;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Detects breaking blocks faster than legitimately possible by measuring the time
 * between consecutive breaks of non-instant blocks. Creative mode and instant-break
 * materials are ignored. A buffer requires several fast breaks in a row before a
 * flag, so a couple of quick crops or instamine edge-cases never trip it.
 */
public final class FastBreakCheck extends BreakCheck {

    private static final String BUFFER = "fastbreak";
    private static final int MAX_SAMPLES = 8;

    public FastBreakCheck(LumenEssentials plugin) {
        super(plugin, "fastbreak");
    }

    @Override
    public void handleBreak(Player player, PlayerData data, BlockBreakEvent event) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Ignore trivially-instant blocks (crops, grass, tall flowers, etc.).
        // Material#getHardness is not available on all supported API versions, so we
        // filter by a name heuristic that is safe everywhere.
        if (isInstantBreak(event.getBlock().getType().name())) {
            return;
        }

        long now = System.currentTimeMillis();
        long previous = data.breakTimes().isEmpty() ? 0L : data.breakTimes().peekLast();
        data.recordBreak(now, MAX_SAMPLES);
        if (previous == 0L) {
            return;
        }

        long interval = now - previous;
        // Minimum plausible interval between two real breaks of solid blocks.
        long minInterval = (long) (settings().threshold() <= 0 ? 90L : settings().threshold());
        if (interval < minInterval) {
            double buffer = data.addBuffer(BUFFER, 1.0D, 0.0D, settings().buffer() + 4.0D);
            if (buffer > settings().buffer()) {
                fail(player, data, 1.0D, String.format("interval=%dms min=%dms buf=%.1f",
                        interval, minInterval, buffer));
            }
        } else {
            data.addBuffer(BUFFER, -1.0D, 0.0D, settings().buffer() + 4.0D);
        }
    }

    private boolean isInstantBreak(String name) {
        return name.contains("GRASS") || name.contains("FERN") || name.contains("FLOWER")
                || name.contains("SAPLING") || name.contains("TORCH") || name.contains("WHEAT")
                || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEET")
                || name.contains("TULIP") || name.contains("MUSHROOM") || name.contains("SEAGRASS")
                || name.contains("KELP") || name.contains("SNOW") || name.equals("AIR");
    }
}
