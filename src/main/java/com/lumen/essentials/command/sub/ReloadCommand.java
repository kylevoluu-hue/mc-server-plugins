package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;

/** {@code /luac reload} — hot-reloads all configuration files and check settings. */
public final class ReloadCommand implements SubCommand {

    private final LumenEssentials plugin;

    public ReloadCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "luac.admin";
    }

    @Override
    public String description() {
        return "Reload all configuration files.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();
        plugin.reloadConfiguration();
        MessageUtil.send(sender, "&aLumen Essentials reloaded &7("
                + (System.currentTimeMillis() - start) + "ms)");
    }
}
