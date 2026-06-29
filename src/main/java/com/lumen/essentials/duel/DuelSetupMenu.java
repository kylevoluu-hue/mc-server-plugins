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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Direct-challenge setup menu (1v1): cycle the kit, best-of rounds, arena weather and
 * time, then send the challenge to a specific player. (The matchmaking queue is reached
 * via {@code /duel} with no target.)
 */
public final class DuelSetupMenu extends Menu {

    private static final int KIT_SLOT = 10;
    private static final int ROUNDS_SLOT = 12;
    private static final int WEATHER_SLOT = 14;
    private static final int TIME_SLOT = 16;
    private static final int CONFIRM_SLOT = 22;

    private final UUID targetUuid;
    private final String targetName;
    private final DuelSettings settings = new DuelSettings();
    private final List<String> kits = Arrays.asList(KitFactory.KITS);

    public DuelSetupMenu(LumenEssentials plugin, Player target) {
        super(plugin, "&8Duel Setup vs " + target.getName(), 3);
        this.targetUuid = target.getUniqueId();
        this.targetName = target.getName();
    }

    @Override
    protected void build() {
        getInventory().setItem(KIT_SLOT, new ItemBuilder(mat(KitFactory.iconFor(settings.kit())))
                .name("&bKit: &f" + settings.kit())
                .lore("&7Click to change kit").build());
        getInventory().setItem(ROUNDS_SLOT, new ItemBuilder(mat("PAPER"))
                .name("&bRounds: &fBest of " + settings.rounds())
                .lore("&7Click to cycle (1 / 3 / 5)").build());
        getInventory().setItem(WEATHER_SLOT, new ItemBuilder(mat("WATER_BUCKET"))
                .name("&bWeather: &f" + settings.weather())
                .lore("&7Click to cycle").build());
        getInventory().setItem(TIME_SLOT, new ItemBuilder(mat("CLOCK"))
                .name("&bTime: &f" + settings.time())
                .lore("&7Click to cycle").build());
        getInventory().setItem(CONFIRM_SLOT, new ItemBuilder(mat("LIME_WOOL"))
                .name("&aSend Challenge")
                .lore("&7Challenge &f" + targetName).build());
        fillEmpty();
    }

    private Material mat(String name) {
        Material m = plugin.versionManager().adapter().resolveMaterial(name);
        return m != null ? m : Material.STONE;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        switch (slot) {
            case KIT_SLOT:
                int idx = (kits.indexOf(settings.kit()) + 1) % kits.size();
                settings.setKit(kits.get(idx));
                build();
                return;
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
