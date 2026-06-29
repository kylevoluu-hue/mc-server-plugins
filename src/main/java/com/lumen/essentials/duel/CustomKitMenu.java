package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Manage the three Custom kit slots. Left-click a slot to save your current inventory
 * into it; the saved kits then appear as "Custom 1/2/3" in the kit picker.
 */
public final class CustomKitMenu extends Menu {

    private static final int[] SLOTS = {11, 13, 15};

    private final UUID viewer;

    public CustomKitMenu(LumenEssentials plugin, Player viewer) {
        super(plugin, "&8Custom Kits", 3);
        this.viewer = viewer.getUniqueId();
    }

    @Override
    protected void build() {
        for (int i = 0; i < SLOTS.length; i++) {
            int slotNumber = i + 1;
            boolean set = plugin.duelManager().hasCustomKit(viewer, slotNumber);
            getInventory().setItem(SLOTS[i], new ItemBuilder(mat("CHEST"))
                    .name("&dCustom Slot " + slotNumber)
                    .lore(set ? "&aSaved" : "&7Empty",
                            "&eLeft-click to save your current inventory")
                    .build());
        }
        fillEmpty();
    }

    private Material mat(String name) {
        Material m = plugin.versionManager().adapter().resolveMaterial(name);
        return m != null ? m : Material.CHEST;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                plugin.duelManager().saveCustomKit(player, i + 1);
                getInventory().setItem(SLOTS[i], render(i + 1));
                return;
            }
        }
    }

    private ItemStack render(int slotNumber) {
        boolean set = plugin.duelManager().hasCustomKit(viewer, slotNumber);
        return new ItemBuilder(mat("CHEST"))
                .name("&dCustom Slot " + slotNumber)
                .lore(set ? "&aSaved" : "&7Empty",
                        "&eLeft-click to save your current inventory")
                .build();
    }
}
