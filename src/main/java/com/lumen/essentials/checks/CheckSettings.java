package com.lumen.essentials.checks;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Resolved, per-check tunables loaded from {@code checks.yml}. Every check shares
 * the same knobs so server owners get a predictable configuration surface.
 */
public final class CheckSettings {

    private final boolean enabled;
    private final double buffer;
    private final double threshold;
    private final double decay;
    private final double confidence;
    private final double alertThreshold;
    private final double punishmentThreshold;
    private final boolean debug;

    private CheckSettings(boolean enabled, double buffer, double threshold, double decay,
                          double confidence, double alertThreshold, double punishmentThreshold,
                          boolean debug) {
        this.enabled = enabled;
        this.buffer = buffer;
        this.threshold = threshold;
        this.decay = decay;
        this.confidence = confidence;
        this.alertThreshold = alertThreshold;
        this.punishmentThreshold = punishmentThreshold;
        this.debug = debug;
    }

    /** Sensible conservative defaults used when a section is missing entirely. */
    public static CheckSettings defaults() {
        return new CheckSettings(true, 0.0D, 1.0D, 0.05D, 0.5D, 5.0D, 20.0D, false);
    }

    /** Reads a check section, falling back to defaults for any missing key. */
    public static CheckSettings from(ConfigurationSection section) {
        CheckSettings d = defaults();
        if (section == null) {
            return d;
        }
        return new CheckSettings(
                section.getBoolean("enabled", d.enabled),
                section.getDouble("buffer", d.buffer),
                section.getDouble("threshold", d.threshold),
                section.getDouble("decay", d.decay),
                section.getDouble("confidence", d.confidence),
                section.getDouble("alert-threshold", d.alertThreshold),
                section.getDouble("punishment-threshold", d.punishmentThreshold),
                section.getBoolean("debug", d.debug)
        );
    }

    public boolean enabled() {
        return enabled;
    }

    public double buffer() {
        return buffer;
    }

    public double threshold() {
        return threshold;
    }

    public double decay() {
        return decay;
    }

    public double confidence() {
        return confidence;
    }

    public double alertThreshold() {
        return alertThreshold;
    }

    public double punishmentThreshold() {
        return punishmentThreshold;
    }

    public boolean debug() {
        return debug;
    }
}
