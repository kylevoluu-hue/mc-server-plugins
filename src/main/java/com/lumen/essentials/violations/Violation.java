package com.lumen.essentials.violations;

/**
 * Immutable record describing a single detection event. Captures the full context
 * required for staff alerts, logging and the developer API.
 */
public final class Violation {

    private final String player;
    private final String checkName;
    private final String category;
    private final double violationLevel;
    private final double confidence;
    private final int ping;
    private final double tps;
    private final double x;
    private final double y;
    private final double z;
    private final String world;
    private final long timestamp;
    private final String serverVersion;
    private final String pluginVersion;
    private final String debugInfo;

    private Violation(Builder builder) {
        this.player = builder.player;
        this.checkName = builder.checkName;
        this.category = builder.category;
        this.violationLevel = builder.violationLevel;
        this.confidence = builder.confidence;
        this.ping = builder.ping;
        this.tps = builder.tps;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.world = builder.world;
        this.timestamp = builder.timestamp;
        this.serverVersion = builder.serverVersion;
        this.pluginVersion = builder.pluginVersion;
        this.debugInfo = builder.debugInfo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String player() {
        return player;
    }

    public String checkName() {
        return checkName;
    }

    public String category() {
        return category;
    }

    public double violationLevel() {
        return violationLevel;
    }

    public double confidence() {
        return confidence;
    }

    public int ping() {
        return ping;
    }

    public double tps() {
        return tps;
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

    public String world() {
        return world;
    }

    public long timestamp() {
        return timestamp;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public String pluginVersion() {
        return pluginVersion;
    }

    public String debugInfo() {
        return debugInfo;
    }

    /** Mutable builder; {@link #timestamp} defaults to now if unset. */
    public static final class Builder {
        private String player = "unknown";
        private String checkName = "unknown";
        private String category = "unknown";
        private double violationLevel;
        private double confidence;
        private int ping;
        private double tps = 20.0D;
        private double x, y, z;
        private String world = "unknown";
        private long timestamp = System.currentTimeMillis();
        private String serverVersion = "unknown";
        private String pluginVersion = "unknown";
        private String debugInfo = "";

        public Builder player(String player) {
            this.player = player;
            return this;
        }

        public Builder checkName(String checkName) {
            this.checkName = checkName;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder violationLevel(double violationLevel) {
            this.violationLevel = violationLevel;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder ping(int ping) {
            this.ping = ping;
            return this;
        }

        public Builder tps(double tps) {
            this.tps = tps;
            return this;
        }

        public Builder location(double x, double y, double z, String world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder serverVersion(String serverVersion) {
            this.serverVersion = serverVersion;
            return this;
        }

        public Builder pluginVersion(String pluginVersion) {
            this.pluginVersion = pluginVersion;
            return this;
        }

        public Builder debugInfo(String debugInfo) {
            this.debugInfo = debugInfo == null ? "" : debugInfo;
            return this;
        }

        public Violation build() {
            return new Violation(this);
        }
    }
}
