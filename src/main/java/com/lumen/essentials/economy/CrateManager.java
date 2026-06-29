package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Loads key and crate definitions from {@code economy.yml} and handles opening
 * crates. Keys and crates are virtual: opening a crate consumes one of its key type
 * and rolls a weighted reward (coins, another key, or a console command). There are
 * no physical key items - {@link EconomyManager} tracks the counts.
 */
public final class CrateManager {

    /** A configured key type. */
    public static final class KeyDef {
        public final String id;
        public final String display;
        public final String rarity;
        public final String icon;

        KeyDef(String id, String display, String rarity, String icon) {
            this.id = id;
            this.display = display;
            this.rarity = rarity;
            this.icon = icon;
        }
    }

    /** One weighted reward inside a crate. */
    public static final class Reward {
        final String type;     // coins | key | command
        final int weight;
        final long amount;
        final String keyId;
        final String command;
        final String message;

        Reward(String type, int weight, long amount, String keyId, String command, String message) {
            this.type = type;
            this.weight = weight;
            this.amount = amount;
            this.keyId = keyId;
            this.command = command;
            this.message = message;
        }
    }

    /** A configured crate. */
    public static final class CrateDef {
        public final String id;
        public final String display;
        public final String keyId;
        public final String icon;
        public final String rarity;
        public final String difficulty;
        final List<Reward> rewards;
        final int totalWeight;

        CrateDef(String id, String display, String keyId, String icon, String rarity,
                 String difficulty, List<Reward> rewards) {
            this.id = id;
            this.display = display;
            this.keyId = keyId;
            this.icon = icon;
            this.rarity = rarity;
            this.difficulty = difficulty;
            this.rewards = rewards;
            int total = 0;
            for (Reward r : rewards) {
                total += Math.max(0, r.weight);
            }
            this.totalWeight = total;
        }
    }

    private final LumenEssentials plugin;
    private final Random random = new Random();
    private final Map<String, KeyDef> keyDefs = new LinkedHashMap<>();
    private final Map<String, CrateDef> crateDefs = new LinkedHashMap<>();

    public CrateManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        keyDefs.clear();
        crateDefs.clear();

        ConfigurationSection keys = plugin.configManager().economy().getConfigurationSection("keys");
        if (keys != null) {
            for (String id : keys.getKeys(false)) {
                ConfigurationSection s = keys.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                keyDefs.put(id.toLowerCase(Locale.ROOT), new KeyDef(id.toLowerCase(Locale.ROOT),
                        s.getString("display", id), s.getString("rarity", "COMMON"),
                        s.getString("icon", "TRIPWIRE_HOOK")));
            }
        }

        ConfigurationSection crates = plugin.configManager().economy().getConfigurationSection("crates");
        if (crates != null) {
            for (String id : crates.getKeys(false)) {
                ConfigurationSection s = crates.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                List<Reward> rewards = new ArrayList<>();
                List<Map<?, ?>> rewardList = s.getMapList("rewards");
                for (Map<?, ?> raw : rewardList) {
                    rewards.add(parseReward(raw));
                }
                crateDefs.put(id.toLowerCase(Locale.ROOT), new CrateDef(id.toLowerCase(Locale.ROOT),
                        s.getString("display", id), s.getString("key", id).toLowerCase(Locale.ROOT),
                        s.getString("icon", "CHEST"), s.getString("rarity", "COMMON"),
                        s.getString("difficulty", "EASY"), rewards));
            }
        }
    }

    private Reward parseReward(Map<?, ?> raw) {
        String type = str(raw, "type", "coins");
        int weight = (int) num(raw, "weight", 1);
        long amount = num(raw, "amount", 1);
        String keyId = str(raw, "key", "");
        String command = str(raw, "command", "");
        String message = str(raw, "message", "");
        return new Reward(type, weight, amount, keyId.toLowerCase(Locale.ROOT), command, message);
    }

    private String str(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private long num(Map<?, ?> map, String key, long def) {
        Object v = map.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return v == null ? def : Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public java.util.Collection<KeyDef> keyDefs() {
        return keyDefs.values();
    }

    public KeyDef keyDef(String id) {
        return id == null ? null : keyDefs.get(id.toLowerCase(Locale.ROOT));
    }

    public java.util.Collection<CrateDef> crateDefs() {
        return crateDefs.values();
    }

    public CrateDef crateDef(String id) {
        return id == null ? null : crateDefs.get(id.toLowerCase(Locale.ROOT));
    }

    /** Attempts to open a crate for the player, consuming one of its key. */
    public boolean open(Player player, String crateId) {
        CrateDef crate = crateDef(crateId);
        if (crate == null) {
            MessageUtil.send(player, "&cUnknown crate.");
            return false;
        }
        if (!plugin.economyManager().removeKeys(player.getUniqueId(), crate.keyId, 1)) {
            KeyDef key = keyDef(crate.keyId);
            MessageUtil.send(player, "&cYou need a " + (key == null ? crate.keyId : key.display)
                    + " &cto open this crate.");
            return false;
        }
        Reward reward = roll(crate);
        if (reward == null) {
            MessageUtil.send(player, "&cThis crate has no rewards configured.");
            // Refund the key since nothing was given.
            plugin.economyManager().addKeys(player.getUniqueId(), crate.keyId, 1);
            return false;
        }
        applyReward(player, reward);
        if (!reward.message.isEmpty()) {
            MessageUtil.send(player, reward.message);
        }
        plugin.economyManager().save();
        return true;
    }

    private Reward roll(CrateDef crate) {
        if (crate.rewards.isEmpty() || crate.totalWeight <= 0) {
            return crate.rewards.isEmpty() ? null : crate.rewards.get(0);
        }
        int roll = random.nextInt(crate.totalWeight);
        int cumulative = 0;
        for (Reward reward : crate.rewards) {
            cumulative += Math.max(0, reward.weight);
            if (roll < cumulative) {
                return reward;
            }
        }
        return crate.rewards.get(crate.rewards.size() - 1);
    }

    private void applyReward(Player player, Reward reward) {
        switch (reward.type.toLowerCase(Locale.ROOT)) {
            case "coins":
                plugin.economyManager().addCoins(player.getUniqueId(), reward.amount);
                break;
            case "key":
                plugin.economyManager().addKeys(player.getUniqueId(), reward.keyId, (int) reward.amount);
                break;
            case "command":
                if (!reward.command.isEmpty()) {
                    String command = reward.command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                break;
            default:
                break;
        }
    }
}
