package com.lumen.essentials.checks.movement;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Base for movement detections. Receives every positional update after the shared
 * {@link com.lumen.essentials.player.MovementSnapshot} has been refreshed, so all
 * checks see consistent delta data.
 */
public abstract class MovementCheck extends Check {

    protected MovementCheck(LumenEssentials plugin, String name) {
        super(plugin, "movement", name);
    }

    /**
     * Invoked once per processed move. Implementations should be allocation-light;
     * they run on the hot movement path.
     */
    public abstract void handle(Player player, PlayerData data, PlayerMoveEvent event);

    /**
     * Shared leniency gate. Returns true when current conditions are too unreliable
     * to flag movement (recent teleport/velocity, very low TPS, gliding, vehicle,
     * etc.), which is the primary defense against false positives.
     */
    protected boolean shouldExempt(Player player, PlayerData data) {
        if (player.isInsideVehicle() || player.isFlying() || player.getAllowFlight()) {
            return true;
        }
        if (plugin.versionManager().adapter().isGliding(player)
                || plugin.versionManager().adapter().isRiptiding(player)) {
            return true;
        }
        if (data.recentlyTeleported(2000L) || data.recentlyTookVelocity(1500L)) {
            return true;
        }
        // Severe server lag desyncs movement; skip rather than risk false flags.
        return com.lumen.essentials.utilities.ServerUtil.getTps() < 16.0D;
    }
}
