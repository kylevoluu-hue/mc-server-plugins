package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.duel.DuelMenu;
import com.lumen.essentials.duel.DuelSetupMenu;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Commands for the dueling feature: {@code /duel} (queue menu), {@code /duel <player>}
 * (direct challenge), {@code /duel savekit [slot]}, {@code /duelaccept} and
 * {@code /leave}.
 */
public final class DuelCommandHandler implements CommandExecutor, TabCompleter {

    private final LumenEssentials plugin;

    public DuelCommandHandler(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "duel":
                return duel(sender, args);
            case "duelaccept":
                return accept(sender);
            case "leave":
                return leave(sender);
            default:
                return false;
        }
    }

    private boolean duel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("savekit")) {
            int slot = 1;
            if (args.length >= 2) {
                try {
                    slot = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                    slot = 1;
                }
            }
            plugin.duelManager().saveCustomKit(player, slot);
            return true;
        }

        if (args.length == 0) {
            // Open the matchmaking menu (modes -> kits -> queue).
            new DuelMenu(plugin).open(player);
            return true;
        }

        // Direct challenge.
        if (plugin.duelManager().isBusy(player.getUniqueId())) {
            MessageUtil.send(sender, "&cYou are already in a duel or queue. Use /leave first.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }
        if (plugin.duelManager().isBusy(target.getUniqueId())) {
            MessageUtil.send(sender, "&cThat player is already busy.");
            return true;
        }
        new DuelSetupMenu(plugin, target).open(player);
        return true;
    }

    private boolean accept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        plugin.duelManager().accept((Player) sender);
        return true;
    }

    private boolean leave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        if (!plugin.duelManager().forfeit(((Player) sender).getUniqueId())) {
            MessageUtil.send(sender, "&cYou are not in a duel or queue.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("duel") && args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("savekit");
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            List<String> filtered = new ArrayList<>();
            for (String o : out) {
                if (o.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    filtered.add(o);
                }
            }
            return filtered;
        }
        return new ArrayList<>();
    }
}
