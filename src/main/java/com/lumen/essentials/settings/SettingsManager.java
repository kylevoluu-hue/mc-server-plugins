package com.lumen.essentials.settings;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns per-player {@code /settings} toggles and the server-side enforcement that is
 * feasible without a companion client (night vision refresh in particular). State is
 * loaded on join, kept in memory, and persisted to {@code settings.yml} on change.
 */
public final class SettingsManager {

    // Effectively infinite (~3.4 years of ticks). Re-applied on join/respawn and by the
    // refresh task so it survives deaths (which clear potion effects) without flashing.
    private static final int NIGHT_VISION_DURATION = Integer.MAX_VALUE;

    private final LumenEssentials plugin;
    private final Map<UUID, EnumMap<SettingType, Boolean>> cache = new ConcurrentHashMap<>();
    private int refreshTaskId = -1;

    public SettingsManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Refresh night vision every 30s so the infinite effect never lapses.
        this.refreshTaskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::refreshAll, 200L, 600L).getTaskId();
    }

    public void shutdown() {
        if (refreshTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
    }

    /** Loads a player's saved settings into memory and applies any active effects. */
    public void load(Player player) {
        EnumMap<SettingType, Boolean> map = new EnumMap<>(SettingType.class);
        ConfigurationSection section = plugin.configManager().settings()
                .getConfigurationSection("players." + player.getUniqueId());
        for (SettingType type : SettingType.values()) {
            boolean value = section != null
                    ? section.getBoolean(type.key(), type.defaultValue())
                    : type.defaultValue();
            map.put(type, value);
        }
        cache.put(player.getUniqueId(), map);
        apply(player, SettingType.NIGHT_VISION);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isEnabled(UUID uuid, SettingType type) {
        EnumMap<SettingType, Boolean> map = cache.get(uuid);
        if (map == null) {
            return type.defaultValue();
        }
        return map.getOrDefault(type, type.defaultValue());
    }

    /** Flips a setting, persists it, applies any immediate effect, and returns the new value. */
    public boolean toggle(Player player, SettingType type) {
        EnumMap<SettingType, Boolean> map =
                cache.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(SettingType.class));
        boolean next = !map.getOrDefault(type, type.defaultValue());
        map.put(type, next);
        persist(player.getUniqueId(), type, next);
        apply(player, type);
        return next;
    }

    private void persist(UUID uuid, SettingType type, boolean value) {
        FileConfiguration cfg = plugin.configManager().settings();
        cfg.set("players." + uuid + "." + type.key(), value);
        plugin.configManager().saveSettings();
    }

    /** Re-applies night vision (e.g. after respawn, which clears potion effects). */
    public void applyNightVision(Player player) {
        apply(player, SettingType.NIGHT_VISION);
    }

    /** Applies the live server-side effect of a setting (currently night vision). */
    private void apply(Player player, SettingType type) {
        if (type != SettingType.NIGHT_VISION) {
            return;
        }
        try {
            if (isEnabled(player.getUniqueId(), SettingType.NIGHT_VISION)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                        NIGHT_VISION_DURATION, 0, true, false));
            } else {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        } catch (Throwable ignored) {
            // Night vision unavailable on this server build; ignore.
        }
    }

    private void refreshAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isEnabled(player.getUniqueId(), SettingType.NIGHT_VISION)) {
                apply(player, SettingType.NIGHT_VISION);
            }
        }
    }
}
