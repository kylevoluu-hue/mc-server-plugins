package com.lumen.essentials.flags;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import com.lumen.essentials.stats.StatsMenu;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * DonutSMP-style flag menu: one player head per flagged player. Hover shows the
 * reason and flagger; left-click teleports to the player's flagged location and
 * right-click opens their stats. Accessed via {@code /flaglist}.
 */
public final class FlagMenu extends Menu {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private List<Flag> snapshot;

    public FlagMenu(LumenEssentials plugin) {
        super(plugin, "&8Flagged Players", 6);
    }

    @Override
    protected void build() {
        snapshot = plugin.flagManager().all();
        Material headMat = plugin.versionManager().adapter().resolveMaterial("PLAYER_HEAD");
        if (headMat == null) {
            headMat = Material.STONE;
        }
        int slot = 0;
        for (Flag flag : snapshot) {
            if (slot >= 54) {
                break;
            }
            ItemStack head = new ItemBuilder(headMat)
                    .owner(Bukkit.getOfflinePlayer(flag.target()))
                    .name("&c" + flag.targetName())
                    .lore(
                            "&7Reason: &f" + flag.reason(),
                            "&7Flagged by: &f" + flag.flagger(),
                            "&7When: &f" + dateFormat.format(new Date(flag.timestamp())),
                            "&7Location: &f" + flag.world() + " "
                                    + (int) flag.x() + "," + (int) flag.y() + "," + (int) flag.z(),
                            "",
                            "&eLeft-click &7to teleport",
                            "&eRight-click &7to view stats")
                    .build();
            getInventory().setItem(slot++, head);
        }
        fillEmpty();
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        if (snapshot == null || slot < 0 || slot >= snapshot.size()) {
            return;
        }
        Flag flag = snapshot.get(slot);

        if (event.getClick() == ClickType.RIGHT) {
            new StatsMenu(plugin, Bukkit.getOfflinePlayer(flag.target())).open(player);
            return;
        }

        // Left-click: teleport to the flagged player (prefer live location).
        Player online = Bukkit.getPlayer(flag.target());
        Location destination;
        if (online != null) {
            destination = online.getLocation();
        } else {
            World world = Bukkit.getWorld(flag.world());
            if (world == null) {
                MessageUtil.send(player, "&cThat player's world is not loaded.");
                return;
            }
            destination = new Location(world, flag.x(), flag.y(), flag.z());
        }
        player.closeInventory();
        player.teleport(destination);
        MessageUtil.send(player, "&aTeleported to &f" + flag.targetName() + "&a.");
    }
}
