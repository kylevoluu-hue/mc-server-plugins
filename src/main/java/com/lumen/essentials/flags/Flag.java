package com.lumen.essentials.flags;

import java.util.UUID;

/** Immutable record of a manual staff flag against a player. */
public final class Flag {

    private final UUID target;
    private final String targetName;
    private final String flagger;
    private final String reason;
    private final long timestamp;
    private final String world;
    private final double x;
    private final double y;
    private final double z;

    public Flag(UUID target, String targetName, String flagger, String reason,
                long timestamp, String world, double x, double y, double z) {
        this.target = target;
        this.targetName = targetName;
        this.flagger = flagger;
        this.reason = reason;
        this.timestamp = timestamp;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID target() {
        return target;
    }

    public String targetName() {
        return targetName;
    }

    public String flagger() {
        return flagger;
    }

    public String reason() {
        return reason;
    }

    public long timestamp() {
        return timestamp;
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}
