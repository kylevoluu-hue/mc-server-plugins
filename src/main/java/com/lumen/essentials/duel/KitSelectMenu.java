package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.UUID;

/** Lists every kit for a chosen mode; clicking one queues the player for that match. */
public final class KitSelectMenu extends Menu {

    private final DuelMode mode;
    private final UUID viewer;

    public KitSelectMenu(LumenEssentials plugin, Player viewer, DuelMode mode) {
        super(plugin, "&8" + mode.label() + " - Pick a Kit", 5);
        this.viewer = viewer.getUniqueId();
        this.mode = mode;
    }

    @Override
    protected void build() {
        int[] slots = layout(KitFactory.KITS.length);
        for (int i = 0; i < KitFactory.KITS.length; i++) {
            getInventory().setItem(slots[i], render(KitFactory.KITS[i]));
        }
        fillEmpty();
    }

    private ItemStack render(String kit) {
        Material icon = plugin.versionManager().adapter().resolveMaterial(KitFactory.iconFor(kit));
        if (icon == null) {
            icon = Material.DIAMOND_SWORD;
        }
        String lore = "&7Queue &f" + mode.label() + " &7with this kit";
        if (kit.toLowerCase(Locale.ROOT).startsWith("custom")) {
            int slot = kit.charAt(kit.length() - 1) - '0';
            boolean set = plugin.duelManager().hasCustomKit(viewer, slot);
            lore = set ? "&7Your custom kit slot " + slot : "&cEmpty - save it in Custom Kits";
        }
        return new ItemBuilder(icon).name("&e" + kit).lore(lore, "&eClick to queue").build();
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
        int[] slots = layout(KitFactory.KITS.length);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                player.closeInventory();
                plugin.duelManager().queueManager().join(player, mode, KitFactory.KITS[i]);
                return;
            }
        }
    }
}
