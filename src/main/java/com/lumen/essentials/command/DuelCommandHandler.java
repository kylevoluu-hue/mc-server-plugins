package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Commands for the dueling feature: {@code /duel}, {@code /duelaccept}, {@code /leave}
 * and the operator {@code /duelarena}.
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
            case "duelarena":
                return arena(sender, args);
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
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /duel <player>  (or /duel savekit)");
            return true;
        }
        if (args[0].equalsIgnoreCase("savekit")) {
            plugin.duelManager().saveCustomKit(player);
            return true;
        }
        if (plugin.duelManager().isBusy(player.getUniqueId())) {
            MessageUtil.send(sender, "&cYou are already in a duel. Use /leave first.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }
        if (plugin.duelManager().isBusy(target.getUniqueId())) {
            MessageUtil.send(sender, "&cThat player is already in a duel.");
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
            MessageUtil.send(sender, "&cYou are not in a duel.");
        }
        return true;
    }

    private boolean arena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lumen.duel.admin")) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, "&7/duelarena &fcreate <name> | pos1 <name> | pos2 <name> | remove <name> | list");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            MessageUtil.send(sender, "&7Arenas: &f" + String.join(", ", plugin.duelManager().arenaNames()));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /duelarena " + sub + " <name>");
            return true;
        }
        String name = args[1];
        switch (sub) {
            case "create":
                MessageUtil.send(sender, plugin.duelManager().createArena(name)
                        ? "&aCreated arena &f" + name + "&a. Now set pos1 and pos2."
                        : "&cAn arena with that name already exists.");
                return true;
            case "pos1":
            case "pos2":
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, "&cRun this as a player to set a spawn.");
                    return true;
                }
                int which = sub.equals("pos1") ? 1 : 2;
                MessageUtil.send(sender, plugin.duelManager().setSpawn(name, which, ((Player) sender).getLocation())
                        ? "&aSet " + sub + " for arena &f" + name + "&a." : "&cNo such arena.");
                return true;
            case "remove":
                MessageUtil.send(sender, plugin.duelManager().removeArena(name)
                        ? "&aRemoved arena &f" + name + "&a." : "&cNo such arena.");
                return true;
            default:
                MessageUtil.send(sender, "&cUnknown subcommand.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("duel") && args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("savekit");
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            return filter(out, args[0]);
        }
        if (name.equals("duelarena") && args.length == 1) {
            return filter(Arrays.asList("create", "pos1", "pos2", "remove", "list"), args[0]);
        }
        if (name.equals("duelarena") && args.length == 2) {
            return filter(plugin.duelManager().arenaNames(), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                out.add(o);
            }
        }
        return out;
    }
}
