package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.player.PlayerData;
import com.lumen.essentials.utilities.MessageUtil;
import com.lumen.essentials.utilities.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code /luac profile <player>} — a forensic snapshot of one player. */
public final class ProfileCommand implements SubCommand {

    private final LumenEssentials plugin;

    public ProfileCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public String permission() {
        return "luac.admin";
    }

    @Override
    public String description() {
        return "Show a player's anti-cheat profile.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /luac profile <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not online.");
            return;
        }
        PlayerData data = plugin.playerDataManager().getIfPresent(target.getUniqueId());
        if (data == null) {
            MessageUtil.send(sender, "&7No data tracked for that player yet.");
            return;
        }
        MessageUtil.send(sender, "&8&m----&r &bProfile: &f" + target.getName() + " &8&m----");
        MessageUtil.send(sender, "&7Ping&8: &f" + ServerUtil.getPing(target) + "ms &8| &7Brand&8: &f"
                + data.clientBrand() + " &8| &7Exempt&8: &f" + data.isExempt());

        Map<String, Double> vls = data.violations().snapshot();
        MessageUtil.send(sender, "&7Violations:");
        if (vls.isEmpty()) {
            MessageUtil.send(sender, "  &8(none)");
        } else {
            vls.forEach((k, v) -> MessageUtil.send(sender,
                    String.format("  &f%s &7= &e%.1f", k, v)));
        }

        MessageUtil.send(sender, "&7Suspicion:");
        if (data.suspicionScores().isEmpty()) {
            MessageUtil.send(sender, "  &8(none)");
        } else {
            data.suspicionScores().forEach((k, v) -> MessageUtil.send(sender,
                    String.format("  &f%s &7= &c%.1f", k, v)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> names = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    names.add(p.getName());
                }
            });
        }
        return names;
    }
}
