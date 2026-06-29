package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the two virtual balances every player has: <b>Coins</b> (earned in AFK
 * zones, spent in the merchant) and a per-type <b>Key</b> count (earned by playtime,
 * spent opening crates). Keys and coins are pure counters - there are no physical
 * items - so this manager is the single source of truth for both.
 *
 * <p>Values are cached in memory and flushed to {@code economy-data.yml} periodically
 * and on shutdown; admin edits to offline players load that player's row on demand.
 */
public final class EconomyManager {

    private final LumenEssentials plugin;
    private final Map<UUID, Long> coins = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> keys = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private int saveTaskId = -1;

    public EconomyManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadAll();
        // Flush to disk every 2 minutes if anything changed.
        this.saveTaskId = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::saveIfDirty, 2400L, 2400L).getTaskId();
    }

    public void shutdown() {
        if (saveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        save();
    }

    private FileConfiguration data() {
        return plugin.configManager().economyData();
    }

    private void loadAll() {
        coins.clear();
        keys.clear();
        ConfigurationSection root = data().getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                coins.put(uuid, s.getLong("coins", 0));
                ConfigurationSection keySec = s.getConfigurationSection("keys");
                if (keySec != null) {
                    Map<String, Integer> map = new HashMap<>();
                    for (String keyId : keySec.getKeys(false)) {
                        map.put(keyId.toLowerCase(), keySec.getInt(keyId, 0));
                    }
                    keys.put(uuid, map);
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed uuid
            }
        }
    }

    // --- Coins -------------------------------------------------------------

    public long getCoins(UUID uuid) {
        return coins.getOrDefault(uuid, 0L);
    }

    public void setCoins(UUID uuid, long amount) {
        coins.put(uuid, Math.max(0, amount));
        dirty = true;
    }

    public void addCoins(UUID uuid, long amount) {
        setCoins(uuid, getCoins(uuid) + amount);
    }

    public boolean removeCoins(UUID uuid, long amount) {
        long current = getCoins(uuid);
        if (current < amount) {
            return false;
        }
        setCoins(uuid, current - amount);
        return true;
    }

    // --- Keys --------------------------------------------------------------

    public int getKeys(UUID uuid, String keyId) {
        Map<String, Integer> map = keys.get(uuid);
        return map == null ? 0 : map.getOrDefault(keyId.toLowerCase(), 0);
    }

    public void addKeys(UUID uuid, String keyId, int amount) {
        Map<String, Integer> map = keys.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        map.merge(keyId.toLowerCase(), amount, Integer::sum);
        if (map.get(keyId.toLowerCase()) < 0) {
            map.put(keyId.toLowerCase(), 0);
        }
        dirty = true;
    }

    public void setKeys(UUID uuid, String keyId, int amount) {
        keys.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(keyId.toLowerCase(), Math.max(0, amount));
        dirty = true;
    }

    public boolean removeKeys(UUID uuid, String keyId, int amount) {
        if (getKeys(uuid, keyId) < amount) {
            return false;
        }
        addKeys(uuid, keyId, -amount);
        return true;
    }

    // --- Persistence -------------------------------------------------------

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public synchronized void save() {
        FileConfiguration cfg = data();
        for (Map.Entry<UUID, Long> entry : coins.entrySet()) {
            cfg.set("players." + entry.getKey() + ".coins", entry.getValue());
        }
        for (Map.Entry<UUID, Map<String, Integer>> entry : keys.entrySet()) {
            for (Map.Entry<String, Integer> k : entry.getValue().entrySet()) {
                cfg.set("players." + entry.getKey() + ".keys." + k.getKey(), k.getValue());
            }
        }
        plugin.configManager().saveEconomyData();
        dirty = false;
    }
}
