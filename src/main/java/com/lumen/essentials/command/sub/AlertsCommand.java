package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /luac alerts} — toggles receiving staff anti-cheat alerts. */
public final class AlertsCommand implements SubCommand {

    private final LumenEssentials plugin;

    public AlertsCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "alerts";
    }

    @Override
    public String permission() {
        return "luac.alerts";
    }

    @Override
    public String description() {
        return "Toggle anti-cheat alerts.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cOnly players can toggle alerts.");
            return;
        }
        boolean on = plugin.alertManager().toggleAlerts((Player) sender);
        MessageUtil.send(sender, "&7Alerts are now " + (on ? "&aenabled" : "&cdisabled") + "&7.");
    }
}
