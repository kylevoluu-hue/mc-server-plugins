package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The main duel menu: pick a mode (1v1, 2v2, 4v4, 8v8) to choose a kit and queue, or
 * open the Custom Kits editor.
 */
public final class DuelMenu extends Menu {

    private static final int[] MODE_SLOTS = {10, 12, 14, 16};
    private static final int CUSTOM_SLOT = 22;

    public DuelMenu(LumenEssentials plugin) {
        super(plugin, "&8Duels", 3);
    }

    @Override
    protected void build() {
        DuelMode[] modes = DuelMode.values();
        String[] icons = {"IRON_SWORD", "DIAMOND_SWORD", "GOLDEN_SWORD", "NETHERITE_SWORD"};
        for (int i = 0; i < modes.length && i < MODE_SLOTS.length; i++) {
            getInventory().setItem(MODE_SLOTS[i], new ItemBuilder(mat(icons[i]))
                    .name("&b" + modes[i].label() + " Duel")
                    .lore("&7Queue a &f" + modes[i].label() + " &7match.", "&eClick to pick a kit")
                    .build());
        }
        getInventory().setItem(CUSTOM_SLOT, new ItemBuilder(mat("CRAFTING_TABLE"))
                .name("&dCustom Kits")
                .lore("&7Save up to 3 custom kits", "&7to duel with.").build());
        fillEmpty();
    }

    private Material mat(String name) {
        Material m = plugin.versionManager().adapter().resolveMaterial(name);
        return m != null ? m : Material.STONE;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        DuelMode[] modes = DuelMode.values();
        for (int i = 0; i < MODE_SLOTS.length && i < modes.length; i++) {
            if (slot == MODE_SLOTS[i]) {
                new KitSelectMenu(plugin, player, modes[i]).open(player);
                return;
            }
        }
        if (slot == CUSTOM_SLOT) {
            new CustomKitMenu(plugin, player).open(player);
        }
    }
}
