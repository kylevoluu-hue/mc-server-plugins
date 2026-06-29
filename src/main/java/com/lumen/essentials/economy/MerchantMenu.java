package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The /merchant shop: buy keys and other rewards with Coins. */
public final class MerchantMenu extends Menu {

    private final UUID viewer;

    public MerchantMenu(LumenEssentials plugin, Player viewer) {
        super(plugin, plugin.merchantManager().title(), 6);
        this.viewer = viewer.getUniqueId();
    }

    @Override
    protected void build() {
        for (MerchantManager.MerchantItem item : plugin.merchantManager().items()) {
            if (item.slot >= 0 && item.slot < getInventory().getSize()) {
                getInventory().setItem(item.slot, render(item));
            }
        }
        fillEmpty();
    }

    private ItemStack render(MerchantManager.MerchantItem item) {
        Material icon = plugin.versionManager().adapter().resolveMaterial(item.icon);
        if (icon == null) {
            icon = Material.STONE;
        }
        List<String> lore = new ArrayList<>(item.lore);
        lore.add("");
        lore.add("&7Price: &6" + item.price + " Coins");
        lore.add("&7Your balance: &6" + plugin.economyManager().getCoins(viewer) + " Coins");
        lore.add("&eClick to buy");
        return new ItemBuilder(icon).name(item.name).lore(lore).build();
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        MerchantManager.MerchantItem item = plugin.merchantManager().itemAt(slot);
        if (item == null) {
            return;
        }
        plugin.merchantManager().buy(player, item);
        // Refresh all items so balances update everywhere.
        build();
    }
}
