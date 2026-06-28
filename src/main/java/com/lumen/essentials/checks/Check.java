package com.lumen.essentials.checks;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Base type for every detection. Subclasses implement a single behavioral concern
 * and call {@link #fail} when a player exhibits suspicious behavior; thresholding,
 * decay, alerting and punishment are all handled centrally so individual checks
 * stay small and focused (single-responsibility).
 *
 * <p>Each check is configured under {@code checks.yml -> <category>.<name>} and
 * exposes the standard knobs (enabled, buffer, threshold, decay, confidence,
 * alert/punishment thresholds, debug).
 */
public abstract class Check {

    protected final LumenEssentials plugin;
    private final String name;
    private final String category;
    private volatile CheckSettings settings = CheckSettings.defaults();

    protected Check(LumenEssentials plugin, String category, String name) {
        this.plugin = plugin;
        this.category = category;
        this.name = name;
    }

    /** Reloads this check's settings from the supplied checks configuration. */
    public final void reload(FileConfiguration checksConfig) {
        ConfigurationSection categorySection = checksConfig.getConfigurationSection(category);
        ConfigurationSection section = categorySection == null
                ? null
                : categorySection.getConfigurationSection(name);
        this.settings = CheckSettings.from(section);
    }

    public final String name() {
        return name;
    }

    public final String category() {
        return category;
    }

    public final CheckSettings settings() {
        return settings;
    }

    public final boolean isEnabled() {
        return settings.enabled();
    }

    /**
     * Records a violation for the given player. Delegates to the central
     * {@code ViolationManager}, which applies decay, evaluates alert/punishment
     * thresholds, fires the API event and writes the log entry.
     *
     * @param player the offending player
     * @param data   their cached player data
     * @param amount the violation weight (typically scaled by confidence)
     * @param debug  human-readable diagnostic context for verbose/debug output
     */
    protected final void fail(Player player, PlayerData data, double amount, String debug) {
        plugin.violationManager().flag(this, player, data, amount, debug);
    }

    /**
     * Convenience: only emit debug output (no violation). Used when a check wants
     * to surface analysis to staff without contributing to the score.
     */
    protected final void debug(Player player, String message) {
        if (settings.debug()) {
            plugin.alertManager().debug(player.getName() + " [" + name + "] " + message);
        }
    }
}
