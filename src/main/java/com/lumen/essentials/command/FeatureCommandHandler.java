package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.flags.Flag;
import com.lumen.essentials.flags.FlagMenu;
import com.lumen.essentials.stats.StatsMenu;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
 * Single executor/tab-completer for the standalone player-facing commands (flags,
 * stats, warps, RTP, teleport requests). Dispatching by command name keeps these
 * many small commands in one cohesive place while each still has its own permission
 * and registration in {@code plugin.yml}.
 */
public final class FeatureCommandHandler implements CommandExecutor, TabCompleter {

    private final LumenEssentials plugin;

    public FeatureCommandHandler(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "flag":
                return flag(sender, args);
            case "flaglist":
                return flagList(sender);
            case "stats":
                return stats(sender, args);
            case "warp":
                return warp(sender, args);
            case "sswarp":
                return setWarp(sender, args);
            case "rtp":
                return rtp(sender, args);
            case "rtpworld":
                return rtpWorld(sender, args);
            case "rtpamount":
                return rtpAmount(sender, args);
            case "tpa":
                return tpa(sender, args, false);
            case "tpahere":
                return tpa(sender, args, true);
            case "tpaccept":
                return tpAccept(sender);
            case "tpauto":
                return tpAuto(sender);
            case "tp":
                return tp(sender, args);
            case "settings":
                return settings(sender);
            default:
                return false;
        }
    }

    // --- Flags -------------------------------------------------------------

    private boolean flag(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /flag <player> <reason>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not online.");
            return true;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Flag flag = plugin.flagManager().add(target, sender.getName(), reason);
        MessageUtil.send(sender, "&aFlagged &f" + target.getName() + " &7for: &f" + reason);
        plugin.alertManager().notifyStaff("&cFlag&7: &f" + sender.getName() + " &7flagged &f"
                + target.getName() + " &7- " + flag.reason());
        return true;
    }

    private boolean flagList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can open the flag list.");
            return true;
        }
        new FlagMenu(plugin).open((Player) sender);
        return true;
    }

    // --- Stats -------------------------------------------------------------

    private boolean stats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can open the stats menu.");
            return true;
        }
        Player viewer = (Player) sender;
        OfflinePlayer target = viewer;
        if (args.length >= 1) {
            if (!viewer.hasPermission("lumen.stats.others")) {
                MessageUtil.send(sender, "&cYou may only view your own stats.");
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
        }
        new StatsMenu(plugin, target).open(viewer);
        return true;
    }

    // --- Warps -------------------------------------------------------------

    private boolean warp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can warp.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            MessageUtil.send(sender, "&7Warps: &f" + String.join(", ", plugin.warpManager().names()));
            return true;
        }
        String warpName = args[0].toLowerCase(Locale.ROOT);
        Location destination = plugin.warpManager().get(warpName);
        if (destination == null) {
            MessageUtil.send(sender, "&cUnknown warp. Available: &f"
                    + String.join(", ", plugin.warpManager().names()));
            return true;
        }
        final String label = warpName;
        plugin.teleportManager().beginWarmup(player, destination,
                () -> plugin.teleportManager().sendTeleportMessage(player, "&aWarped to &f" + label + "&a."));
        return true;
    }

    private boolean setWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can set warps.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /sswarp <name>");
            return true;
        }
        Player player = (Player) sender;
        String name = args[0].toLowerCase(Locale.ROOT);
        plugin.warpManager().set(name, player.getLocation());
        MessageUtil.send(sender, "&aWarp &f" + name + " &aset to your current location.");
        return true;
    }

    // --- RTP ---------------------------------------------------------------

    private boolean rtp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can use RTP.");
            return true;
        }
        plugin.rtpManager().teleport((Player) sender);
        return true;
    }

    private boolean rtpWorld(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /rtpworld <world>");
            return true;
        }
        if (Bukkit.getWorld(args[0]) == null) {
            MessageUtil.send(sender, "&cThat world is not loaded.");
            return true;
        }
        plugin.rtpManager().setWorld(args[0]);
        plugin.configManager().reloadAll(); // persist not needed; runtime config holds it
        MessageUtil.send(sender, "&aRTP world set to &f" + args[0] + "&a.");
        return true;
    }

    private boolean rtpAmount(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /rtpamount <max-radius>");
            return true;
        }
        try {
            int radius = Integer.parseInt(args[0]);
            plugin.rtpManager().setMaxRadius(Math.max(50, radius));
            MessageUtil.send(sender, "&aRTP max radius set to &f" + radius + "&a.");
        } catch (NumberFormatException ex) {
            MessageUtil.send(sender, "&cRadius must be a number.");
        }
        return true;
    }

    // --- Teleport requests -------------------------------------------------

    private boolean tpa(CommandSender sender, String[] args, boolean here) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can send teleport requests.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /" + (here ? "tpahere" : "tpa") + " <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target == sender) {
            MessageUtil.send(sender, "&cPlayer not found.");
            return true;
        }
        plugin.teleportManager().sendRequest((Player) sender, target, here);
        return true;
    }

    private boolean tpAccept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        plugin.teleportManager().accept((Player) sender);
        return true;
    }

    private boolean tpAuto(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        boolean on = plugin.teleportManager().toggleAuto((Player) sender);
        MessageUtil.send(sender, "&7Auto-accept teleports: " + (on ? "&aON" : "&cOFF"));
        return true;
    }

    // --- Settings ----------------------------------------------------------

    private boolean settings(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can open settings.");
            return true;
        }
        Player player = (Player) sender;
        new com.lumen.essentials.settings.SettingsMenu(plugin, player).open(player);
        return true;
    }

    // --- Admin tp ----------------------------------------------------------

    private boolean tp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can use /tp here.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                MessageUtil.send(sender, "&cPlayer not found.");
                return true;
            }
            player.teleport(target.getLocation());
            MessageUtil.send(sender, "&aTeleported to &f" + target.getName() + "&a.");
        } else if (args.length == 2) {
            Player a = Bukkit.getPlayerExact(args[0]);
            Player b = Bukkit.getPlayerExact(args[1]);
            if (a == null || b == null) {
                MessageUtil.send(sender, "&cPlayer not found.");
                return true;
            }
            a.teleport(b.getLocation());
            MessageUtil.send(sender, "&aTeleported &f" + a.getName() + " &ato &f" + b.getName() + "&a.");
        } else if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                player.teleport(new Location(player.getWorld(), x, y, z));
                MessageUtil.send(sender, "&aTeleported.");
            } catch (NumberFormatException ex) {
                MessageUtil.send(sender, "&cInvalid coordinates.");
            }
        } else {
            MessageUtil.send(sender, "&cUsage: /tp <player> | /tp <p1> <p2> | /tp <x> <y> <z>");
        }
        return true;
    }

    // --- Tab completion ----------------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            switch (name) {
                case "warp":
                    return prefix(plugin.warpManager().names(), args[0]);
                case "flag":
                case "stats":
                case "tpa":
                case "tpahere":
                case "tp":
                    return onlinePlayers(args[0]);
                case "rtpworld":
                    return worlds(args[0]);
                default:
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private List<String> onlinePlayers(String pre) {
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(pre.toLowerCase())) {
                out.add(p.getName());
            }
        }
        return out;
    }

    private List<String> worlds(String pre) {
        List<String> out = new ArrayList<>();
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            if (w.getName().toLowerCase().startsWith(pre.toLowerCase())) {
                out.add(w.getName());
            }
        }
        return out;
    }

    private List<String> prefix(List<String> options, String pre) {
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(pre.toLowerCase())) {
                out.add(option);
            }
        }
        return out;
    }
}
