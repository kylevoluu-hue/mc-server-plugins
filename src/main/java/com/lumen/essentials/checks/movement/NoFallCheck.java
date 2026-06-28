package com.lumen.essentials.checks.movement;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Flags the classic NoFall pattern: a player reporting {@code onGround=true} in a
 * packet while there is no supporting block beneath them and they are clearly
 * descending. This is a heuristic that errs on the side of caution (it only fires
 * when the player is well above any block and still claims to be grounded).
 */
public final class NoFallCheck extends MovementCheck {

    public NoFallCheck(LumenEssentials plugin) {
        super(plugin, "nofall");
    }

    @Override
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (shouldExempt(player, data)) {
            return;
        }
        // Player claims ground but is descending and there's air below for several blocks.
        if (data.movement().onGround() && data.movement().deltaY() < -0.1D) {
            if (!hasGroundWithin(player, 1.2D)) {
                fail(player, data, 1.0D, String.format("claimedGround dy=%.3f noSupport",
                        data.movement().deltaY()));
            }
        }
    }

    private boolean hasGroundWithin(Player player, double depth) {
        double y = player.getLocation().getY();
        for (double d = 0; d <= depth; d += 0.4D) {
            Material below = player.getLocation().clone().subtract(0, d, 0).getBlock().getType();
            if (below != null && below.isSolid()) {
                return true;
            }
        }
        // Don't penalize near water/lava/climbables either.
        Material here = player.getLocation().getBlock().getType();
        return here == Material.WATER || here == Material.LAVA
                || (here != null && (here.name().contains("LADDER") || here.name().contains("VINE")
                || here.name().contains("WEB") || here.name().contains("SCAFFOLDING")));
    }
}
