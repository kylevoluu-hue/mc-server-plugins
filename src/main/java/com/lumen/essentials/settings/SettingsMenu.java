package com.lumen.essentials.settings;

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

/**
 * DonutSMP-style settings menu: one item per {@link SettingType}, each with a
 * thematically-related icon, the current ON/OFF state, and click-to-toggle. The slot
 * layout is derived deterministically so clicks map back to the right setting.
 */
public final class SettingsMenu extends Menu {

    private final UUID viewer;

    public SettingsMenu(LumenEssentials plugin, Player viewer) {
        super(plugin, "&8Settings", 3);
        this.viewer = viewer.getUniqueId();
    }

    @Override
    protected void build() {
        SettingType[] types = SettingType.values();
        int[] slots = layout(types.length);
        for (int i = 0; i < types.length; i++) {
            getInventory().setItem(slots[i], render(types[i]));
        }
        fillEmpty();
    }

    /** Lays toggles out across the inner columns (skipping the border columns). */
    private int[] layout(int count) {
        int[] slots = new int[count];
        int slot = 10;
        for (int i = 0; i < count; i++) {
            slots[i] = slot;
            slot++;
            if (slot % 9 == 8) { // skip right border, jump to next row's first inner column
                slot += 2;
            }
        }
        return slots;
    }

    private ItemStack render(SettingType type) {
        boolean on = plugin.settingsManager().isEnabled(viewer, type);
        Material icon = plugin.versionManager().adapter().resolveMaterial(type.iconMaterial());
        if (icon == null) {
            icon = Material.STONE;
        }
        List<String> lore = new ArrayList<>();
        for (String line : type.description()) {
            lore.add("&7" + line);
        }
        lore.add("");
        lore.add(on ? "&aEnabled" : "&cDisabled");
        if (type.clientSide()) {
            lore.add("&8(client-side preference)");
        }
        lore.add("&eClick to toggle");
        return new ItemBuilder(icon)
                .name((on ? "&a" : "&c") + type.displayName())
                .lore(lore)
                .build();
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        SettingType type = typeAtSlot(slot);
        if (type == null) {
            return;
        }
        plugin.settingsManager().toggle(player, type);
        getInventory().setItem(slot, render(type)); // re-render in place; menu stays open
    }

    private SettingType typeAtSlot(int slot) {
        SettingType[] types = SettingType.values();
        int[] slots = layout(types.length);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return types[i];
            }
        }
        return null;
    }
}
