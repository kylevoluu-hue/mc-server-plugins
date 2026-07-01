package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Staff punishment commands.
 *
 * <p>{@code /punish <player> <reason> [duration] [ip-ban]} bans (and kicks) a player.
 * The reason may be a premade key from {@code punishments.yml} (which carries a default
 * duration) or free text; an explicit duration (e.g. {@code 7d}, {@code permanent})
 * overrides it. Appending {@code ip-ban} also bans the player's IP so alt accounts from
 * that address can't join. {@code /kick <player> [reason]} just kicks.
 *
 * <p>Bans use Bukkit's ban lists, so the server enforces them on join automatically.
 */
public final class PunishCommandHandler implements CommandExecutor, TabCompleter {

    private static final Pattern DURATION = Pattern.compile("(?i)^(\\d+)([smhdw])$");
    private static final List<String> DURATION_SUGGESTIONS = Arrays.asList(
            "permanent", "30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d");

    private final LumenEssentials plugin;

    public PunishCommandHandler(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("kick")) {
            return kick(sender, args);
        }
        if (command.getName().equalsIgnoreCase("unpunish")) {
            return unpunish(sender, args);
        }
        return punish(sender, args);
    }

    // --- /unpunish ---------------------------------------------------------

    private boolean unpunish(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lumen.unpunish")) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(sender, "&cUsage: /unpunish <player> [ip-ban]  |  /unpunish ip <address>");
            return true;
        }
        // /unpunish ip <address>  -> lift an IP ban directly.
        if (args[0].equalsIgnoreCase("ip") && args.length >= 2) {
            try {
                Bukkit.getBanList(BanList.Type.IP).pardon(args[1]);
                MessageUtil.send(sender, "&aLifted IP ban for &f" + args[1] + "&a.");
            } catch (Throwable t) {
                MessageUtil.send(sender, "&cFailed: " + t.getMessage());
            }
            return true;
        }

        String name = args[0];
        boolean alsoIp = args.length >= 2 && (args[1].equalsIgnoreCase("ip-ban") || args[1].equalsIgnoreCase("ip"));
        try {
            Bukkit.getBanList(BanList.Type.NAME).pardon(name);
        } catch (Throwable t) {
            MessageUtil.send(sender, "&cFailed to unban: " + t.getMessage());
            return true;
        }
        if (alsoIp) {
            // We can only lift an IP ban if we know the address; online players can't be
            // banned, so point the operator at the explicit form.
            MessageUtil.send(sender, "&7For IP unbans use &f/unpunish ip <address>&7 "
                    + "(the address is shown when you ran /punish ... ip-ban).");
        }
        MessageUtil.send(sender, "&aUnbanned &f" + name + "&a.");
        plugin.alertManager().notifyStaff("&aPunish&7: &f" + sender.getName()
                + " &7unbanned &f" + name);
        return true;
    }

    // --- /punish -----------------------------------------------------------

    private boolean punish(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lumen.punish")) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /punish <player> <reason> [duration] [ip-ban]");
            MessageUtil.send(sender, "&7Reasons: &f" + String.join(", ", reasonKeys())
                    + " &8| &7Durations: &f" + String.join(", ", DURATION_SUGGESTIONS));
            return true;
        }
        String targetName = args[0];
        List<String> rest = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean ipBan = false;
        if (!rest.isEmpty() && rest.get(rest.size() - 1).equalsIgnoreCase("ip-ban")) {
            ipBan = true;
            rest.remove(rest.size() - 1);
        }

        Long durationMillis = null;   // null until resolved; -1 sentinel = permanent
        boolean explicitDuration = false;
        if (!rest.isEmpty()) {
            Long parsed = parseDuration(rest.get(rest.size() - 1));
            if (parsed != null) {
                durationMillis = parsed;
                explicitDuration = true;
                rest.remove(rest.size() - 1);
            }
        }
        if (rest.isEmpty()) {
            MessageUtil.send(sender, "&cYou must provide a reason.");
            return true;
        }
        String reasonInput = String.join(" ", rest);

        // Resolve premade reason (display + default duration) if it matches a key.
        String display = reasonInput;
        ConfigurationSection preset = reasonSection(reasonInput.toLowerCase(Locale.ROOT));
        if (preset != null) {
            display = preset.getString("display", reasonInput);
            if (!explicitDuration) {
                durationMillis = parseDuration(preset.getString("duration", "permanent"));
            }
        } else if (!explicitDuration) {
            durationMillis = -1L; // custom reason, no duration given -> permanent
        }

        boolean permanent = durationMillis == null || durationMillis < 0;
        Date expires = permanent ? null : new Date(System.currentTimeMillis() + durationMillis);
        String durationLabel = permanent ? "permanent" : formatDuration(durationMillis);
        String source = sender.getName();
        String banMessage = MessageUtil.color("&cYou are banned!\n&7Reason: &f" + display
                + "\n&7Duration: &f" + durationLabel);

        // Apply the name ban.
        OfflinePlayer target = Bukkit.getPlayerExact(targetName) != null
                ? Bukkit.getPlayerExact(targetName) : Bukkit.getOfflinePlayer(targetName);
        try {
            Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, display, expires, source);
        } catch (Throwable t) {
            MessageUtil.send(sender, "&cFailed to apply ban: " + t.getMessage());
            return true;
        }

        Player online = Bukkit.getPlayerExact(targetName);
        if (ipBan) {
            String ip = ipOf(online);
            if (ip != null) {
                try {
                    Bukkit.getBanList(BanList.Type.IP).addBan(ip, display, expires, source);
                } catch (Throwable ignored) {
                    // IP ban best-effort
                }
            } else {
                MessageUtil.send(sender, "&eIP ban skipped: player is offline (IP unknown).");
            }
        }
        if (online != null) {
            online.kickPlayer(banMessage);
        }

        MessageUtil.send(sender, "&aBanned &f" + targetName + " &7for &f" + display
                + " &8(" + durationLabel + (ipBan ? ", ip-ban" : "") + ")");
        plugin.alertManager().notifyStaff("&cPunish&7: &f" + source + " &7banned &f" + targetName
                + " &8(" + display + ", " + durationLabel + (ipBan ? ", ip-ban" : "") + ")");
        return true;
    }

    // --- /kick -------------------------------------------------------------

    private boolean kick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lumen.kick")) {
            MessageUtil.send(sender, "&cNo permission.");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(sender, "&cUsage: /kick <player> [reason]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not online.");
            return true;
        }
        String reason = args.length > 1
                ? String.join(" ", Arrays.asList(args).subList(1, args.length))
                : "Kicked by staff";
        target.kickPlayer(MessageUtil.color("&cKicked!\n&7Reason: &f" + reason));
        MessageUtil.send(sender, "&aKicked &f" + target.getName() + " &7for &f" + reason);
        plugin.alertManager().notifyStaff("&ePunish&7: &f" + sender.getName() + " &7kicked &f"
                + target.getName() + " &8(" + reason + ")");
        return true;
    }

    // --- Helpers -----------------------------------------------------------

    private String ipOf(Player player) {
        try {
            if (player != null && player.getAddress() != null
                    && player.getAddress().getAddress() != null) {
                return player.getAddress().getAddress().getHostAddress();
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return null;
    }

    private ConfigurationSection reasonSection(String key) {
        ConfigurationSection reasons = plugin.configManager().punishments()
                .getConfigurationSection("punish.reasons");
        return reasons == null ? null : reasons.getConfigurationSection(key);
    }

    private List<String> reasonKeys() {
        ConfigurationSection reasons = plugin.configManager().punishments()
                .getConfigurationSection("punish.reasons");
        return reasons == null ? new ArrayList<>() : new ArrayList<>(reasons.getKeys(false));
    }

    /** Returns duration in ms, -1 for permanent, or null if the token is not a duration. */
    private Long parseDuration(String token) {
        if (token == null) {
            return null;
        }
        String t = token.toLowerCase(Locale.ROOT);
        if (t.equals("permanent") || t.equals("perm") || t.equals("forever")) {
            return -1L;
        }
        Matcher m = DURATION.matcher(t);
        if (!m.matches()) {
            return null;
        }
        long value = Long.parseLong(m.group(1));
        switch (m.group(2).toLowerCase(Locale.ROOT)) {
            case "s": return value * 1000L;
            case "m": return value * 60_000L;
            case "h": return value * 3_600_000L;
            case "d": return value * 86_400_000L;
            case "w": return value * 604_800_000L;
            default: return null;
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000L;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 && days == 0) {
            sb.append(minutes).append("m");
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? (seconds + "s") : out;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        boolean isPunish = command.getName().equalsIgnoreCase("punish");
        if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("unpunish")) {
                List<String> opts = onlineNames();
                opts.add("ip");
                return prefix(opts, args[0]);
            }
            return prefix(onlineNames(), args[0]);
        }
        if (isPunish && args.length == 2) {
            return prefix(reasonKeys(), args[1]);
        }
        if (isPunish && args.length == 3) {
            List<String> opts = new ArrayList<>(DURATION_SUGGESTIONS);
            opts.add("ip-ban");
            return prefix(opts, args[2]);
        }
        if (isPunish && args.length == 4) {
            return prefix(java.util.Collections.singletonList("ip-ban"), args[3]);
        }
        return new ArrayList<>();
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
        return names;
    }

    private List<String> prefix(List<String> options, String pre) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(pre.toLowerCase(Locale.ROOT))) {
                out.add(o);
            }
        }
        return out;
    }
}
