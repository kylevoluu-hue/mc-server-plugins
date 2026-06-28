package com.lumen.essentials.checks.movement;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Detects horizontal movement faster than the game should allow. Accounts for the
 * Speed potion effect and uses a buffer so a single lag spike never triggers a flag;
 * the violation only fires after sustained excess speed.
 */
public final class SpeedCheck extends MovementCheck {

    private static final String BUFFER = "speed";

    public SpeedCheck(LumenEssentials plugin) {
        super(plugin, "speed");
    }

    @Override
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (shouldExempt(player, data)) {
            data.setBuffer(BUFFER, 0.0D);
            return;
        }

        double speed = data.movement().horizontalDistance();
        double limit = settings().threshold(); // base max blocks/tick (sprint-jump aware)

        // Potion speed: ~+20% per amplifier level. Be generous to avoid false flags.
        int speedAmp = effectAmplifier(player, PotionEffectType.SPEED);
        if (speedAmp >= 0) {
            limit *= (1.0D + 0.22D * (speedAmp + 1));
        }
        // Recently grounded sprint-jumps and ice produce short bursts; add headroom.
        if (data.airTicks() <= 2) {
            limit += 0.15D;
        }

        if (speed > limit) {
            double over = speed - limit;
            double buffer = data.addBuffer(BUFFER, over, 0.0D, settings().buffer() + 2.0D);
            if (buffer > settings().buffer()) {
                fail(player, data, 1.0D, String.format("speed=%.3f limit=%.3f over=%.3f buf=%.2f",
                        speed, limit, over, buffer));
            }
        } else {
            // Decay the buffer toward zero on clean ticks.
            data.addBuffer(BUFFER, -0.25D, 0.0D, settings().buffer() + 2.0D);
        }
    }

    private int effectAmplifier(Player player, PotionEffectType type) {
        try {
            return player.getPotionEffect(type) != null ? player.getPotionEffect(type).getAmplifier() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
