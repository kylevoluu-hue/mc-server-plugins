package com.lumen.essentials.gui;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory clicks for any open {@link Menu}. Clicks in a menu are always
 * cancelled (the menus are read-only "display" inventories) and forwarded to the
 * menu's {@code onClick} handler.
 */
public final class MenuListener implements Listener {

    @SuppressWarnings("unused")
    private final LumenEssentials plugin;

    public MenuListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        // Always cancel: these menus never allow item movement.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        // Only react to clicks inside the menu itself, not the player's own inventory.
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        Menu menu = (Menu) holder;
        try {
            menu.onClickInternal((Player) event.getWhoClicked(), event.getSlot(),
                    event.getCurrentItem(), event);
        } catch (Throwable ignored) {
            // Never let a menu handler crash the click pipeline.
        }
    }
}
