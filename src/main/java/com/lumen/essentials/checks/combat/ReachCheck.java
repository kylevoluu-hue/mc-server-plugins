package com.lumen.essentials.checks.combat;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Validates that a melee attack originates from within a plausible reach distance.
 * Distance is measured from the attacker's eye to the nearest point of the victim's
 * bounding box, with ping-based leniency added because high-latency players legitimately
 * register hits at slightly stale positions.
 */
public final class ReachCheck extends CombatCheck {

    public ReachCheck(LumenEssentials plugin) {
        super(plugin, "reach");
    }

    @Override
    public void handleAttack(Player attacker, PlayerData data, Entity victim,
                             EntityDamageByEntityEvent event) {
        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (attacker.getWorld() != victim.getWorld()) {
            return;
        }

        Location eye = attacker.getEyeLocation();
        double distance = distanceToBox(eye, victim);

        // Base survival reach is ~3.0; add tolerance for ping desync and hitbox edges.
        double base = settings().threshold() <= 0 ? 3.0D : settings().threshold();
        int ping = com.lumen.essentials.utilities.ServerUtil.getPing(attacker);
        double pingLeniency = Math.min(0.8D, ping / 1000.0D); // up to +0.8 at 800ms+
        double limit = base + 0.5D + pingLeniency;

        if (distance > limit) {
            fail(attacker, data, 1.0D, String.format("dist=%.2f limit=%.2f ping=%d",
                    distance, limit, ping));
        }
    }

    /** Distance from a point to the victim's axis-aligned bounding box (approximate). */
    private double distanceToBox(Location eye, Entity victim) {
        Location target = victim.getLocation();
        double halfWidth = victim.getWidth() / 2.0D;
        double height = victim.getHeight();

        double minX = target.getX() - halfWidth;
        double maxX = target.getX() + halfWidth;
        double minY = target.getY();
        double maxY = target.getY() + height;
        double minZ = target.getZ() - halfWidth;
        double maxZ = target.getZ() + halfWidth;

        Vector e = eye.toVector();
        double cx = clamp(e.getX(), minX, maxX);
        double cy = clamp(e.getY(), minY, maxY);
        double cz = clamp(e.getZ(), minZ, maxZ);
        return e.distance(new Vector(cx, cy, cz));
    }

    private double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }
}
