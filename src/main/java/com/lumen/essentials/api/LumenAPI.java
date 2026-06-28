package com.lumen.essentials.api;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import com.lumen.essentials.violations.Violation;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable, public-facing API for third-party plugins. Obtain an instance via
 * {@code ((LumenEssentials) Bukkit.getPluginManager().getPlugin("LumenEssentials")).api()}.
 *
 * <p>This is the only class external plugins should depend on; everything else is
 * implementation detail and may change between releases. Combined with the
 * {@link com.lumen.essentials.api.event.ViolationEvent} and
 * {@link com.lumen.essentials.api.event.PunishmentEvent} Bukkit events, it lets
 * other plugins read violations, register exemptions, cancel punishments, and read
 * mining/suspicion data.
 */
public final class LumenAPI {

    private final LumenEssentials plugin;

    public LumenAPI(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /** The current violation level for a player on a specific check. */
    public double getViolationLevel(Player player, String checkName) {
        PlayerData data = plugin.playerDataManager().getIfPresent(player.getUniqueId());
        if (data == null) {
            return 0.0D;
        }
        return data.violations().get(checkName, 0.0D);
    }

    /** A snapshot of all current violation levels for a player. */
    public Map<String, Double> getViolations(Player player) {
        PlayerData data = plugin.playerDataManager().getIfPresent(player.getUniqueId());
        return data == null ? Map.of() : data.violations().snapshot();
    }

    /** Recent system-wide violations (newest last), capped to a rolling window. */
    public Collection<Violation> getRecentViolations() {
        return plugin.violationManager().recentViolations();
    }

    /** Marks a player exempt from all checks until cleared or they disconnect. */
    public void setExempt(UUID uuid, boolean exempt) {
        PlayerData data = plugin.playerDataManager().getIfPresent(uuid);
        if (data != null) {
            data.setExempt(exempt);
        }
    }

    public boolean isExempt(UUID uuid) {
        PlayerData data = plugin.playerDataManager().getIfPresent(uuid);
        return data != null && data.isExempt();
    }

    /** Reads the current xray/ESP/basefinder suspicion scores for a player. */
    public Map<String, Double> getSuspicionScores(Player player) {
        PlayerData data = plugin.playerDataManager().getIfPresent(player.getUniqueId());
        return data == null ? Map.of() : Map.copyOf(data.suspicionScores());
    }

    /** The client brand reported by a player, if known ("unknown" otherwise). */
    public String getClientBrand(Player player) {
        PlayerData data = plugin.playerDataManager().getIfPresent(player.getUniqueId());
        return data == null ? "unknown" : data.clientBrand();
    }

    /** Active investigation test objects (stashes / ore veins) currently placed. */
    public int getActiveInvestigationObjects() {
        return plugin.investigationManager().activeCount();
    }

    /** The raw {@link PlayerData} for advanced consumers (may be empty). */
    public Optional<PlayerData> getPlayerData(UUID uuid) {
        return Optional.ofNullable(plugin.playerDataManager().getIfPresent(uuid));
    }
}
