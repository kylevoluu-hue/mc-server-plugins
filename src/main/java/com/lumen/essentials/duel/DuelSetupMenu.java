package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * The pre-match configuration menu: pick a kit (UHC, Sword, Crystal, Axe, SMP, Custom),
 * choose best-of rounds, weather and time, optionally save your Custom kit, then send
 * the challenge.
 */
public final class DuelSetupMenu extends Menu {

    private static final int[] KIT_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int ROUNDS_SLOT = 20;
    private static final int WEATHER_SLOT = 22;
    private static final int TIME_SLOT = 24;
    private static final int SAVEKIT_SLOT = 30;
    private static final int CONFIRM_SLOT = 40;

    private final UUID targetUuid;
    private final String targetName;
    private final DuelSettings settings = new DuelSettings();

    public DuelSetupMenu(LumenEssentials plugin, Player target) {
        super(plugin, "&8Duel Setup vs " + target.getName(), 5);
        this.targetUuid = target.getUniqueId();
        this.targetName = target.getName();
    }

    @Override
    protected void build() {
        for (int i = 0; i < KitFactory.KITS.length && i < KIT_SLOTS.length; i++) {
            getInventory().setItem(KIT_SLOTS[i], kitIcon(KitFactory.KITS[i]));
        }
        getInventory().setItem(ROUNDS_SLOT, new ItemBuilder(mat("PAPER"))
                .name("&bRounds: &fBest of " + settings.rounds())
                .lore("&7Click to cycle (1 / 3 / 5)").build());
        getInventory().setItem(WEATHER_SLOT, new ItemBuilder(mat("WATER_BUCKET"))
                .name("&bWeather: &f" + settings.weather())
                .lore("&7Click to cycle (Clear / Rain / Thunder)").build());
        getInventory().setItem(TIME_SLOT, new ItemBuilder(mat("CLOCK"))
                .name("&bTime: &f" + settings.time())
                .lore("&7Click to cycle (Day / Night)").build());
        getInventory().setItem(SAVEKIT_SLOT, new ItemBuilder(mat("CRAFTING_TABLE"))
                .name("&dSave Custom Kit")
                .lore("&7Saves your current inventory", "&7as the Custom kit.").build());
        getInventory().setItem(CONFIRM_SLOT, new ItemBuilder(mat("LIME_WOOL"))
                .name("&aSend Challenge")
                .lore("&7Challenge &f" + targetName, "&7with the chosen settings.").build());
        fillEmpty();
    }

    private ItemStack kitIcon(String kit) {
        boolean selected = settings.kit().equalsIgnoreCase(kit);
        return new ItemBuilder(mat(kitMaterial(kit)))
                .name((selected ? "&a" : "&e") + kit + " Kit")
                .lore(selected ? "&aSelected" : "&7Click to select")
                .build();
    }

    private String kitMaterial(String kit) {
        switch (kit.toLowerCase(java.util.Locale.ROOT)) {
            case "uhc": return "GOLDEN_APPLE";
            case "crystal": return "END_CRYSTAL";
            case "axe": return "DIAMOND_AXE";
            case "smp": return "NETHERITE_INGOT";
            case "custom": return "CHEST";
            case "sword":
            default: return "DIAMOND_SWORD";
        }
    }

    private Material mat(String name) {
        Material m = plugin.versionManager().adapter().resolveMaterial(name);
        return m != null ? m : Material.STONE;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        for (int i = 0; i < KIT_SLOTS.length && i < KitFactory.KITS.length; i++) {
            if (slot == KIT_SLOTS[i]) {
                settings.setKit(KitFactory.KITS[i]);
                build();
                return;
            }
        }
        switch (slot) {
            case ROUNDS_SLOT:
                settings.cycleRounds();
                build();
                return;
            case WEATHER_SLOT:
                settings.cycleWeather();
                build();
                return;
            case TIME_SLOT:
                settings.cycleTime();
                build();
                return;
            case SAVEKIT_SLOT:
                player.closeInventory();
                plugin.duelManager().saveCustomKit(player);
                return;
            case CONFIRM_SLOT:
                player.closeInventory();
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    MessageUtil.send(player, "&c" + targetName + " is no longer online.");
                    return;
                }
                plugin.duelManager().sendRequest(player, target, settings);
                return;
            default:
                break;
        }
    }
}
