package com.lumen.essentials.checks.movement;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Detects sustained upward or hovering motion in the air that is inconsistent with
 * gravity. Exempts levitation, slow-falling, liquids, climbables, webs and recent
 * velocity so legitimate vertical movement is never penalized.
 */
public final class FlyCheck extends MovementCheck {

    private static final String BUFFER = "fly";

    public FlyCheck(LumenEssentials plugin) {
        super(plugin, "fly");
    }

    @Override
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (shouldExempt(player, data) || isVerticallyExempt(player)) {
            data.setBuffer(BUFFER, 0.0D);
            return;
        }
        // Only meaningful while airborne for a few ticks.
        if (data.airTicks() < 4 || data.movement().onGround()) {
            data.addBuffer(BUFFER, -1.0D, 0.0D, settings().buffer() + 4.0D);
            return;
        }

        double dy = data.movement().deltaY();
        // After a few air ticks gravity should always pull the player down.
        // Ascending or hovering (dy >= ~0) for sustained ticks is suspicious.
        if (dy >= -0.05D) {
            double buffer = data.addBuffer(BUFFER, 1.0D, 0.0D, settings().buffer() + 4.0D);
            if (buffer > settings().buffer()) {
                fail(player, data, 1.0D, String.format("dy=%.3f airTicks=%d buf=%.1f",
                        dy, data.airTicks(), buffer));
            }
        } else {
            data.addBuffer(BUFFER, -1.0D, 0.0D, settings().buffer() + 4.0D);
        }
    }

    private boolean isVerticallyExempt(Player player) {
        try {
            if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                    || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Older versions may lack these effect types; ignore.
        }
        Material in = player.getLocation().getBlock().getType();
        Material feet = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        return isSpecial(in) || isSpecial(feet);
    }

    private boolean isSpecial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return material == Material.WATER || material == Material.LAVA
                || name.contains("BUBBLE") || name.contains("WEB") || name.contains("COBWEB")
                || name.contains("LADDER") || name.contains("VINE")
                || name.contains("SCAFFOLDING") || name.contains("HONEY")
                || name.contains("SLIME") || name.contains("POWDER_SNOW");
    }
}
