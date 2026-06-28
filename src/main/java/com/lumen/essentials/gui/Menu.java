package com.lumen.essentials.gui;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for the plugin's chest-style menus. Implements {@link InventoryHolder}
 * so {@link MenuListener} can route clicks back to the originating menu and cancel
 * item movement (the "blank inventory menu" interaction model).
 *
 * <p>Subclasses build their contents in {@link #build()} and react in
 * {@link #onClick(Player, int, ItemStack, InventoryClickEvent)}.
 */
public abstract class Menu implements InventoryHolder {

    protected final LumenEssentials plugin;
    private final Inventory inventory;

    protected Menu(LumenEssentials plugin, String title, int rows) {
        this.plugin = plugin;
        int clamped = Math.max(1, Math.min(6, rows));
        this.inventory = Bukkit.createInventory(this, clamped * 9, MessageUtil.color(title));
    }

    /** Populate the inventory. Called once before opening. */
    protected abstract void build();

    /** Handle a click on a populated slot. The event is already cancelled. */
    protected abstract void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event);

    /** Package-private bridge used by {@link MenuListener} to reach {@link #onClick}. */
    void onClickInternal(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        onClick(player, slot, clicked, event);
    }

    /** Fills every empty slot with a neutral filler pane for a clean look. */
    protected void fillEmpty() {
        Material pane = plugin.versionManager().adapter().resolveMaterial("BLACK_STAINED_GLASS_PANE");
        if (pane == null) {
            return; // Legacy versions without stained-glass-pane material: leave blank.
        }
        ItemStack filler = new ItemBuilder(pane).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void open(Player player) {
        build();
        player.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
