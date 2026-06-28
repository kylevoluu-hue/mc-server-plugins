package com.lumen.essentials.version;

import org.bukkit.Bukkit;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the running server version and selects the appropriate {@link VersionAdapter}.
 * Detection is best-effort and never throws: unknown versions fall back to the
 * {@link LatestAdapter} so new server releases keep working without a code change.
 */
public final class VersionManager {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final Logger logger;
    private VersionAdapter adapter;
    private int major;
    private int minor;
    private int patch;
    private boolean purpur;

    public VersionManager(Logger logger) {
        this.logger = logger;
    }

    public void detect() {
        String bukkitVersion = Bukkit.getBukkitVersion(); // e.g. "1.20.4-R0.1-SNAPSHOT"
        Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion);
        if (matcher.find()) {
            major = parse(matcher.group(1), 1);
            minor = parse(matcher.group(2), 20);
            patch = parse(matcher.group(3), 0);
        } else {
            major = 1;
            minor = 20;
            patch = 0;
        }

        this.purpur = detectPurpur();
        this.adapter = chooseAdapter();

        logger.info("Detected server " + major + "." + minor + "." + patch
                + (purpur ? " (Purpur)" : " (Paper/Spigot)")
                + " -> using " + adapter.name() + " adapter.");
    }

    private VersionAdapter chooseAdapter() {
        // Normalize the new "26.x" style numbering: anything at/after 1.20 (or a
        // major beyond 1) is treated as "latest".
        if (major > 1) {
            return new LatestAdapter();
        }
        if (minor >= 20) {
            return new LatestAdapter();
        }
        if (minor >= 13) {
            return new ModernAdapter();
        }
        return new LegacyAdapter();
    }

    private boolean detectPurpur() {
        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int parse(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public VersionAdapter adapter() {
        return adapter;
    }

    public boolean isPurpur() {
        return purpur;
    }

    /** Returns true if the running version is at least the given 1.x minor. */
    public boolean isAtLeast(int minMinor) {
        return major > 1 || minor >= minMinor;
    }

    public String versionString() {
        return major + "." + minor + "." + patch;
    }
}
