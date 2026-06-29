package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.economy.CratesMenu;
import com.lumen.essentials.economy.KeysMenu;
import com.lumen.essentials.economy.MerchantMenu;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
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
 * Handles the economy commands: {@code /coins}, {@code /keys}, {@code /key},
 * {@code /crates}, {@code /crate}, {@code /merchant}, {@code /afkzone} and
 * {@code /economy}. Player-facing commands open menus or show balances; admin
 * subcommands manage balances, keys, AFK zones and reloading.
 */
public final class EconomyCommandHandler implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "lumen.economy.admin";
    private static final String AFK_ADMIN = "lumen.afkzone.admin";

    private final LumenEssentials plugin;

    public EconomyCommandHandler(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "coins":
                return coins(sender, args);
            case "keys":
                return openKeys(sender);
            case "key":
                return key(sender, args);
            case "crates":
                return openCrates(sender);
            case "crate":
                return crate(sender, args);
            case "merchant":
                return openMerchant(sender);
            case "afkzone":
                return afkzone(sender, args);
            case "economy":
                return economy(sender, args);
            default:
                return false;
        }
    }

    // --- Coins -------------------------------------------------------------

    private boolean coins(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageUtil.send(sender, "&cConsole must specify a player.");
                return true;
            }
            long bal = plugin.economyManager().getCoins(((Player) sender).getUniqueId());
            MessageUtil.send(sender, "&7Balance: &6" + bal + " Coins");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (!sender.hasPermission(ADMIN)) {
                MessageUtil.send(sender, "&cNo permission.");
                return true;
            }
            if (args.length < 3) {
                MessageUtil.send(sender, "&cUsage: /coins " + sub + " <player> <amount>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            long amount = parseLong(args[2], -1);
            if (amount < 0) {
                MessageUtil.send(sender, "&cAmount must be a positive number.");
                return true;
            }
            switch (sub) {
                case "give":
                    plugin.economyManager().addCoins(target.getUniqueId(), amount);
                    break;
                case "take":
                    plugin.economyManager().removeCoins(target.getUniqueId(), amount);
                    break;
                case "set":
                    plugin.economyManager().setCoins(target.getUniqueId(), amount);
                    break;
                default:
                    break;
            }
            plugin.economyManager().save();
            MessageUtil.send(sender, "&aUpdated &f" + args[1] + "&a's coins (" + sub + " " + amount + ").");
            return true;
        }
        // /coins <player>
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        MessageUtil.send(sender, "&7" + args[0] + ": &6"
                + plugin.economyManager().getCoins(target.getUniqueId()) + " Coins");
        return true;
    }

    // --- Keys --------------------------------------------------------------

    private boolean openKeys(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        new KeysMenu(plugin, (Player) sender).open((Player) sender);
        return true;
    }

    private boolean key(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length < 4) {
            MessageUtil.send(sender, "&cUsage: /key <give|take|set> <player> <keyId> <amount>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String keyId = args[2].toLowerCase(Locale.ROOT);
        int amount = (int) parseLong(args[3], -1);
        if (amount < 0) {
            MessageUtil.send(sender, "&cAmount must be a positive number.");
            return true;
        }
        if (plugin.crateManager().keyDef(keyId) == null) {
            MessageUtil.send(sender, "&cUnknown key '" + keyId + "'. Defined keys: " + keyIds());
            return true;
        }
        switch (sub) {
            case "give":
                plugin.economyManager().addKeys(target.getUniqueId(), keyId, amount);
                break;
            case "take":
                plugin.economyManager().removeKeys(target.getUniqueId(), keyId, amount);
                break;
            case "set":
                plugin.economyManager().setKeys(target.getUniqueId(), keyId, amount);
                break;
            default:
                MessageUtil.send(sender, "&cUsage: /key <give|take|set> <player> <keyId> <amount>");
                return true;
        }
        plugin.economyManager().save();
        MessageUtil.send(sender, "&aUpdated &f" + args[1] + "&a's " + keyId + " keys.");
        return true;
    }

    // --- Crates ------------------------------------------------------------

    private boolean openCrates(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        new CratesMenu(plugin, (Player) sender).open((Player) sender);
        return true;
    }

    private boolean crate(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN)) {
                MessageUtil.send(sender, "&cNo permission.");
                return true;
            }
            plugin.reloadConfiguration();
            MessageUtil.send(sender, "&aEconomy/crates reloaded.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("open")) {
            if (!(sender instanceof Player)) {
                MessageUtil.send(sender, "&cPlayers only.");
                return true;
            }
            plugin.crateManager().open((Player) sender, args[1]);
            return true;
        }
        return openCrates(sender);
    }

    private boolean openMerchant(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cPlayers only.");
            return true;
        }
        new MerchantMenu(plugin, (Player) sender).open((Player) sender);
        return true;
    }

    // --- AFK zones ---------------------------------------------------------

    private boolean afkzone(CommandSender sender, String[] args) {
        if (!sender.hasPermission(AFK_ADMIN)) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, "&7/afkzone &fpos1|pos2|create <name>|remove <name>|setreward <name> <coins>|list");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean isPlayer = sender instanceof Player;
        switch (sub) {
            case "pos1":
                if (isPlayer) {
                    plugin.afkZoneManager().setPos1((Player) sender);
                }
                return true;
            case "pos2":
                if (isPlayer) {
                    plugin.afkZoneManager().setPos2((Player) sender);
                }
                return true;
            case "create":
                if (args.length < 2 || !isPlayer) {
                    MessageUtil.send(sender, "&cUsage (as a player): /afkzone create <name>");
                    return true;
                }
                if (plugin.afkZoneManager().create((Player) sender, args[1])) {
                    MessageUtil.send(sender, "&aCreated AFK zone &f" + args[1] + "&a.");
                }
                return true;
            case "remove":
                if (args.length < 2) {
                    MessageUtil.send(sender, "&cUsage: /afkzone remove <name>");
                    return true;
                }
                MessageUtil.send(sender, plugin.afkZoneManager().remove(args[1])
                        ? "&aRemoved AFK zone &f" + args[1] + "&a." : "&cNo such zone.");
                return true;
            case "setreward":
                if (args.length < 3) {
                    MessageUtil.send(sender, "&cUsage: /afkzone setreward <name> <coins>");
                    return true;
                }
                long coins = parseLong(args[2], -1);
                if (coins < 0) {
                    MessageUtil.send(sender, "&cCoins must be a positive number.");
                    return true;
                }
                MessageUtil.send(sender, plugin.afkZoneManager().setReward(args[1], coins)
                        ? "&aSet &f" + args[1] + "&a reward to &6" + coins + " Coins&a." : "&cNo such zone.");
                return true;
            case "list":
                MessageUtil.send(sender, "&7AFK zones: &f" + String.join(", ", plugin.afkZoneManager().zoneNames()));
                return true;
            default:
                MessageUtil.send(sender, "&cUnknown subcommand.");
                return true;
        }
    }

    private boolean economy(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfiguration();
            MessageUtil.send(sender, "&aEconomy reloaded.");
            return true;
        }
        MessageUtil.send(sender, "&7/economy reload");
        return true;
    }

    // --- Helpers -----------------------------------------------------------

    private String keyIds() {
        List<String> ids = new ArrayList<>();
        plugin.crateManager().keyDefs().forEach(k -> ids.add(k.id));
        return String.join(", ", ids);
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("coins") && args.length == 1) {
            return filter(Arrays.asList("give", "take", "set"), args[0]);
        }
        if (name.equals("key") && args.length == 1) {
            return filter(Arrays.asList("give", "take", "set"), args[0]);
        }
        if (name.equals("key") && args.length == 3) {
            return filter(new ArrayList<>(Arrays.asList(keyIds().split(", "))), args[2]);
        }
        if (name.equals("afkzone") && args.length == 1) {
            return filter(Arrays.asList("pos1", "pos2", "create", "remove", "setreward", "list"), args[0]);
        }
        if ((name.equals("coins") || name.equals("key")) && args.length == 2) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return filter(players, args[1]);
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
