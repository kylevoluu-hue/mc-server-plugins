package com.lumen.essentials.checks.movement;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Detects "timer" hacks that speed up the client clock by measuring how much real
 * time elapses per movement packet against the expected ~50ms tick. A balance
 * accumulator is used: the player earns time at the real rate and spends it per
 * packet, so transient lag self-corrects and only a persistently fast clock drifts
 * the balance into violation territory.
 */
public final class TimerCheck extends MovementCheck {

    private static final String BALANCE = "timer_balance";
    private static final String LAST = "timer_last";
    private static final double EXPECTED_MS = 50.0D;

    public TimerCheck(LumenEssentials plugin) {
        super(plugin, "timer");
    }

    @Override
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        long now = System.currentTimeMillis();
        double last = data.buffer(LAST);
        data.setBuffer(LAST, now);
        if (last == 0.0D || data.recentlyTeleported(2000L)) {
            data.setBuffer(BALANCE, 0.0D);
            return;
        }

        double elapsed = now - last;
        // Add real elapsed time, subtract the expected per-packet cost.
        double balance = data.buffer(BALANCE) + elapsed - EXPECTED_MS;
        // Clamp the balance so it cannot run away after a long pause.
        double cap = settings().buffer() * EXPECTED_MS + 500.0D;
        balance = Math.max(-cap, Math.min(cap, balance));
        data.setBuffer(BALANCE, balance);

        // A persistently negative balance means packets arrive faster than real time.
        double violationFloor = -EXPECTED_MS * (settings().threshold() <= 0 ? 6.0D : settings().threshold());
        if (balance < violationFloor) {
            fail(player, data, 1.0D, String.format("balance=%.0fms (clock too fast)", balance));
            // Pull balance back toward zero after flagging to avoid spamming.
            data.setBuffer(BALANCE, 0.0D);
        }
    }
}
