package com.lumen.essentials.player;

import org.bukkit.Location;

/**
 * Immutable-ish snapshot of a player's positional state for a single tick. Reused
 * via {@link #update} to avoid per-tick allocations on the movement hot path.
 */
public final class MovementSnapshot {

    private double x, y, z;
    private double lastX, lastY, lastZ;
    private float yaw, pitch;
    private boolean onGround;
    private boolean lastOnGround;
    private long time;
    private boolean initialized;

    public void update(Location location, boolean onGround) {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.lastOnGround = this.onGround;

        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.onGround = onGround;
        this.time = System.currentTimeMillis();
        this.initialized = true;
    }

    public double deltaX() {
        return x - lastX;
    }

    public double deltaY() {
        return y - lastY;
    }

    public double deltaZ() {
        return z - lastZ;
    }

    /** Horizontal speed (blocks moved on the XZ plane this tick). */
    public double horizontalDistance() {
        double dx = deltaX();
        double dz = deltaZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean onGround() {
        return onGround;
    }

    public boolean lastOnGround() {
        return lastOnGround;
    }

    public double y() {
        return y;
    }

    public double lastY() {
        return lastY;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public long time() {
        return time;
    }
}
