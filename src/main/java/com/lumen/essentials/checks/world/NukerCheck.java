package com.lumen.essentials.checks.world;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Detects "nuker" cheats that break many blocks in a single tick / very short window.
 * Counts breaks within a sliding time window and flags once the count exceeds a
 * configurable per-window limit, which is far above what manual mining can produce.
 */
public final class NukerCheck extends BreakCheck {

    private static final String WINDOW_START = "nuker_window";
    private static final String WINDOW_COUNT = "nuker_count";

    public NukerCheck(LumenEssentials plugin) {
        super(plugin, "nuker");
    }

    @Override
    public void handleBreak(Player player, PlayerData data, BlockBreakEvent event) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowMs = 250L; // quarter-second window
        long start = (long) data.buffer(WINDOW_START);
        double count = data.buffer(WINDOW_COUNT);

        if (start == 0L || now - start > windowMs) {
            data.setBuffer(WINDOW_START, now);
            data.setBuffer(WINDOW_COUNT, 1.0D);
            return;
        }

        count += 1.0D;
        data.setBuffer(WINDOW_COUNT, count);

        double limit = settings().threshold() <= 0 ? 6.0D : settings().threshold();
        if (count > limit) {
            fail(player, data, 1.0D, String.format("%.0f blocks in %dms (limit %.0f)",
                    count, windowMs, limit));
            // Reset window so we don't flag every subsequent break in the burst.
            data.setBuffer(WINDOW_START, now);
            data.setBuffer(WINDOW_COUNT, 0.0D);
        }
    }
}
