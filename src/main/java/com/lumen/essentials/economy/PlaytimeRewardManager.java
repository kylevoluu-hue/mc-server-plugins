package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Awards keys based on time played. Each online player accrues a minute every minute;
 * once they reach the configured interval (default 60 minutes) they receive the keys
 * listed under {@code playtime-rewards.keys} in {@code economy.yml}. This is the only
 * way to earn keys - there is no shard/real-money path.
 */
public final class PlaytimeRewardManager {

    private final LumenEssentials plugin;
    private final Map<UUID, Integer> minutes = new HashMap<>();
    private int taskId = -1;

    public PlaytimeRewardManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Tick once per minute.
        this.taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, 1200L, 1200L).getTaskId();
    }

    public void shutdown() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void clear(UUID uuid) {
        minutes.remove(uuid);
    }

    private void tick() {
        ConfigurationSection rewards = plugin.configManager().economy()
                .getConfigurationSection("playtime-rewards");
        if (rewards == null || !rewards.getBoolean("enabled", true)) {
            return;
        }
        int interval = Math.max(1, rewards.getInt("interval-minutes", 60));
        ConfigurationSection keySection = rewards.getConfigurationSection("keys");

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int accrued = minutes.getOrDefault(player.getUniqueId(), 0) + 1;
            if (accrued >= interval) {
                accrued -= interval;
                grant(player, keySection);
            }
            minutes.put(player.getUniqueId(), accrued);
        }
    }

    private void grant(Player player, ConfigurationSection keySection) {
        if (keySection == null) {
            return;
        }
        boolean awarded = false;
        for (String keyId : keySection.getKeys(false)) {
            int amount = keySection.getInt(keyId, 0);
            if (amount > 0) {
                plugin.economyManager().addKeys(player.getUniqueId(), keyId, amount);
                CrateManager.KeyDef def = plugin.crateManager().keyDef(keyId);
                MessageUtil.send(player, "&6+" + amount + " "
                        + (def == null ? keyId : def.display) + " &7(playtime reward)");
                awarded = true;
            }
        }
        if (awarded) {
            plugin.economyManager().save();
        }
    }
}
