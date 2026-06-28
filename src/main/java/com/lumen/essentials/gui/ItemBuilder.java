package com.lumen.essentials.gui;

import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small fluent builder for menu {@link ItemStack}s. Centralizes color handling and
 * version-safe player-head creation so the menus stay declarative.
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material == null ? Material.STONE : material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null && lines != null) {
            List<String> colored = new ArrayList<>(lines.size());
            for (String line : lines) {
                colored.add(MessageUtil.color(line));
            }
            meta.setLore(colored);
        }
        return this;
    }

    /** Sets the head owner when this item is a player head (no-op otherwise). */
    public ItemBuilder owner(OfflinePlayer owner) {
        if (meta instanceof SkullMeta && owner != null) {
            try {
                ((SkullMeta) meta).setOwningPlayer(owner);
            } catch (Throwable ignored) {
                // Legacy servers expose setOwner(String) instead; ignore gracefully.
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
