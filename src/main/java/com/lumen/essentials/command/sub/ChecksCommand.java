package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;

/** {@code /luac checks} — lists registered checks and their enabled/threshold state. */
public final class ChecksCommand implements SubCommand {

    private final LumenEssentials plugin;

    public ChecksCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "checks";
    }

    @Override
    public String permission() {
        return "luac.admin";
    }

    @Override
    public String description() {
        return "List all checks and their state.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageUtil.send(sender, "&8&m----&r &bChecks &8&m----");
        for (Check check : plugin.checkManager().all()) {
            String state = check.isEnabled() ? "&aON" : "&cOFF";
            MessageUtil.send(sender, "&7" + check.category() + "/&f" + check.name()
                    + " " + state + " &8(alert " + check.settings().alertThreshold()
                    + ", punish " + check.settings().punishmentThreshold() + ")");
        }
    }
}
