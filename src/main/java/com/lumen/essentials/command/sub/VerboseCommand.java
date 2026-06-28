package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /luac verbose} — toggles verbose detection output. Shares the debug
 * subscription channel so staff get per-detection diagnostics in chat.
 */
public final class VerboseCommand implements SubCommand {

    private final LumenEssentials plugin;

    public VerboseCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "verbose";
    }

    @Override
    public String permission() {
        return "luac.debug";
    }

    @Override
    public String description() {
        return "Toggle verbose detection output.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can toggle verbose.");
            return;
        }
        boolean on = plugin.alertManager().toggleDebug((Player) sender);
        MessageUtil.send(sender, "&7Verbose output is now " + (on ? "&aenabled" : "&cdisabled") + "&7.");
    }
}
