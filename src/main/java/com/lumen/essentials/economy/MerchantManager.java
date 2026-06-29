package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the merchant shop from {@code economy.yml} and processes purchases. Items are
 * bought with Coins and can grant keys, coins, or run a console command.
 */
public final class MerchantManager {

    /** A purchasable merchant entry. */
    public static final class MerchantItem {
        public final int slot;
        public final String icon;
        public final String name;
        public final List<String> lore;
        public final long price;
        final String type;   // key | coins | command
        final String keyId;
        final long amount;
        final String command;

        MerchantItem(int slot, String icon, String name, List<String> lore, long price,
                     String type, String keyId, long amount, String command) {
            this.slot = slot;
            this.icon = icon;
            this.name = name;
            this.lore = lore;
            this.price = price;
            this.type = type;
            this.keyId = keyId;
            this.amount = amount;
            this.command = command;
        }
    }

    private final LumenEssentials plugin;
    private final List<MerchantItem> items = new ArrayList<>();
    private String title = "&8Merchant";

    public MerchantManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        items.clear();
        ConfigurationSection section = plugin.configManager().economy()
                .getConfigurationSection("merchant");
        if (section == null) {
            return;
        }
        title = section.getString("title", "&8Merchant");
        for (Map<?, ?> raw : section.getMapList("items")) {
            items.add(parse(raw));
        }
    }

    private MerchantItem parse(Map<?, ?> raw) {
        int slot = (int) num(raw, "slot", 0);
        String icon = str(raw, "icon", "STONE");
        String name = str(raw, "name", "Item");
        List<String> lore = new ArrayList<>();
        Object loreObj = raw.get("lore");
        if (loreObj instanceof List) {
            for (Object line : (List<?>) loreObj) {
                lore.add(String.valueOf(line));
            }
        }
        long price = num(raw, "price", 0);
        String type = str(raw, "type", "command");
        String keyId = str(raw, "key", "").toLowerCase(Locale.ROOT);
        long amount = num(raw, "amount", 1);
        String command = str(raw, "command", "");
        return new MerchantItem(slot, icon, name, lore, price, type, keyId, amount, command);
    }

    public String title() {
        return title;
    }

    public List<MerchantItem> items() {
        return items;
    }

    public MerchantItem itemAt(int slot) {
        for (MerchantItem item : items) {
            if (item.slot == slot) {
                return item;
            }
        }
        return null;
    }

    /** Attempts a purchase, deducting Coins and granting the reward. */
    public boolean buy(Player player, MerchantItem item) {
        if (item == null) {
            return false;
        }
        if (!plugin.economyManager().removeCoins(player.getUniqueId(), item.price)) {
            MessageUtil.send(player, "&cYou need &f" + item.price + " Coins&c for that.");
            return false;
        }
        switch (item.type.toLowerCase(Locale.ROOT)) {
            case "key":
                plugin.economyManager().addKeys(player.getUniqueId(), item.keyId, (int) item.amount);
                break;
            case "coins":
                plugin.economyManager().addCoins(player.getUniqueId(), item.amount);
                break;
            case "command":
                if (!item.command.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            item.command.replace("{player}", player.getName()));
                }
                break;
            default:
                break;
        }
        plugin.economyManager().save();
        MessageUtil.send(player, "&aPurchased &f" + MessageUtil.strip(item.name)
                + " &afor &f" + item.price + " Coins&a.");
        return true;
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
}
