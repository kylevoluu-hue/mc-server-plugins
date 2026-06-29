package com.lumen.essentials.economy;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Read-only menu showing how many of each key type the player owns. */
public final class KeysMenu extends Menu {

    private final UUID viewer;

    public KeysMenu(LumenEssentials plugin, Player viewer) {
        super(plugin, "&8Your Keys", 3);
        this.viewer = viewer.getUniqueId();
    }

    @Override
    protected void build() {
        int slot = 10;
        for (CrateManager.KeyDef key : plugin.crateManager().keyDefs()) {
            int count = plugin.economyManager().getKeys(viewer, key.id);
            Material icon = plugin.versionManager().adapter().resolveMaterial(key.icon);
            if (icon == null) {
                icon = Material.STONE;
            }
            getInventory().setItem(slot, new ItemBuilder(icon)
                    .name(key.display)
                    .lore("&7Rarity: &f" + key.rarity, "&7Owned: &f" + count)
                    .build());
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            if (slot >= 26) {
                break;
            }
        }
        fillEmpty();
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        // Informational only.
    }
}
