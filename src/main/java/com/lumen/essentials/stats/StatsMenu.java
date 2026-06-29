package com.lumen.essentials.stats;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.gui.ItemBuilder;
import com.lumen.essentials.gui.Menu;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * DonutSMP-style stats menu: a blank inventory whose items reveal a single stat on
 * hover. Mob kills (zombie head), player kills (player head), deaths (skeleton head)
 * and playtime (clock).
 */
public final class StatsMenu extends Menu {

    private final OfflinePlayer target;
    private final StatsManager stats;

    public StatsMenu(LumenEssentials plugin, OfflinePlayer target) {
        super(plugin, "&8Stats: &f" + safeName(target), 3);
        this.target = target;
        this.stats = plugin.statsManager();
    }

    private static String safeName(OfflinePlayer player) {
        return player == null || player.getName() == null ? "?" : player.getName();
    }

    @Override
    protected void build() {
        Player online = target.isOnline() ? target.getPlayer() : null;

        int mobKills = online != null ? stats.mobKills(online) : 0;
        int playerKills = online != null ? stats.playerKills(online) : 0;
        int deaths = online != null ? stats.deaths(online) : 0;
        String playtime = online != null ? stats.playtimeFormatted(online) : "unknown";

        getInventory().setItem(10, head("ZOMBIE_HEAD",
                "&6Mob Kills", "&7Total mobs killed: &f" + mobKills));
        getInventory().setItem(12, new ItemBuilder(material("PLAYER_HEAD"))
                .owner(target)
                .name("&6Player Kills")
                .lore("&7Players killed: &f" + playerKills).build());
        getInventory().setItem(14, head("SKELETON_SKULL",
                "&6Deaths", "&7Times died: &f" + deaths));
        getInventory().setItem(16, new ItemBuilder(material("CLOCK"))
                .name("&6Playtime")
                .lore("&7Time played: &f" + playtime).build());

        int duelWins = plugin.duelManager().getWins(target.getUniqueId());
        getInventory().setItem(22, new ItemBuilder(material("DIAMOND_SWORD"))
                .name("&6Duel Wins")
                .lore("&7Duels won: &f" + duelWins).build());

        fillEmpty();
    }

    private ItemStack head(String headMaterial, String name, String lore) {
        return new ItemBuilder(material(headMaterial)).name(name).lore(lore).build();
    }

    private Material material(String name) {
        Material resolved = plugin.versionManager().adapter().resolveMaterial(name);
        return resolved != null ? resolved : Material.STONE;
    }

    @Override
    protected void onClick(Player player, int slot, ItemStack clicked, InventoryClickEvent event) {
        // Stats menu is informational; clicks do nothing.
    }
}
