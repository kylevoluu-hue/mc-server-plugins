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

/** Menu listing every crate, its rarity/difficulty and required key. Click to open. */
public final class CratesMenu extends Menu {

    private final UUID viewer;
    private final List<CrateManager.CrateDef> order = new ArrayList<>();

    public CratesMenu(LumenEssentials plugin, Player viewer) {
        super(plugin, "&8Crates", 3);
        this.viewer = viewer.getUniqueId();
    }

    @Override
    protected void build() {
        order.clear();
        order.addAll(plugin.crateManager().crateDefs());
        int[] slots = layout(order.size());
        for (int i = 0; i < order.size(); i++) {
            getInventory().setItem(slots[i], render(order.get(i)));
        }
        fillEmpty();
    }

    private ItemStack render(CrateManager.CrateDef crate) {
        CrateManager.KeyDef key = plugin.crateManager().keyDef(crate.keyId);
        int owned = plugin.economyManager().getKeys(viewer, crate.keyId);
        Material icon = plugin.versionManager().adapter().resolveMaterial(crate.icon);
        if (icon == null) {
            icon = Material.CHEST;
        }
        return new ItemBuilder(icon)
                .name(crate.display)
                .lore(
                        "&7Rarity: &f" + crate.rarity,
                        "&7Difficulty: &f" + crate.difficulty,
                        "&7Requires: &f" + (key == null ? crate.keyId : key.display),
                        "&7You own: &f" + owned + " key(s)",
                        "",
                        owned > 0 ? "&eClick to open" : "&cYou have no keys for this")
                .build();
    }

    private int[] layout(int count) {
        int[] slots = new int[count];
        int slot = 10;
        for (int i = 0; i < count; i++) {
            slots[i] = slot;
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }
        return slots;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        int[] slots = layout(order.size());
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                plugin.crateManager().open(player, order.get(i).id);
                getInventory().setItem(slot, render(order.get(i))); // refresh key count
                return;
            }
        }
    }
}
