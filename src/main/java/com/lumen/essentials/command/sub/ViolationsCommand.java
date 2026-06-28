package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import com.lumen.essentials.violations.Violation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** {@code /luac violations [player]} — shows recent violations, newest first. */
public final class ViolationsCommand implements SubCommand {

    private final LumenEssentials plugin;

    public ViolationsCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "violations";
    }

    @Override
    public String permission() {
        return "luac.admin";
    }

    @Override
    public String description() {
        return "Show recent violations [player].";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String filter = args.length > 0 ? args[0].toLowerCase() : null;
        List<Violation> list = new ArrayList<>(plugin.violationManager().recentViolations());
        Collections.reverse(list); // newest first
        MessageUtil.send(sender, "&8&m----&r &bRecent Violations &8&m----");
        int shown = 0;
        for (Violation v : list) {
            if (filter != null && !v.player().toLowerCase().equals(filter)) {
                continue;
            }
            MessageUtil.send(sender, String.format("&f%s &7%s &8vl&7=%.1f &8ping&7=%dms &8tps&7=%.1f",
                    v.player(), v.checkName(), v.violationLevel(), v.ping(), v.tps()));
            if (++shown >= 15) {
                break;
            }
        }
        if (shown == 0) {
            MessageUtil.send(sender, "&7No matching violations recorded.");
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
