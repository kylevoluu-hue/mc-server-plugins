package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /luac debug} — toggles receiving low-level debug output. */
public final class DebugCommand implements SubCommand {

    private final LumenEssentials plugin;

    public DebugCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String permission() {
        return "luac.debug";
    }

    @Override
    public String description() {
        return "Toggle debug output.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can toggle debug.");
            return;
        }
        boolean on = plugin.alertManager().toggleDebug((Player) sender);
        MessageUtil.send(sender, "&7Debug output is now " + (on ? "&aenabled" : "&cdisabled") + "&7.");
    }
}
