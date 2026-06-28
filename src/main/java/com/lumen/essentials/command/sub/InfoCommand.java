package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import com.lumen.essentials.utilities.ServerUtil;
import org.bukkit.command.CommandSender;

/** {@code /luac info} — shows plugin/version/runtime status. */
public final class InfoCommand implements SubCommand {

    private final LumenEssentials plugin;

    public InfoCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "luac.admin";
    }

    @Override
    public String description() {
        return "Show plugin status and environment info.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageUtil.send(sender, "&8&m----&r &bLumen Essentials &8&m----");
        MessageUtil.send(sender, "&7Version&8: &f" + plugin.getDescription().getVersion());
        MessageUtil.send(sender, "&7Server&8: &f" + plugin.versionManager().versionString()
                + (plugin.versionManager().isPurpur() ? " (Purpur)" : " (Paper/Spigot)"));
        MessageUtil.send(sender, "&7Adapter&8: &f" + plugin.versionManager().adapter().name());
        MessageUtil.send(sender, "&7Checks&8: &f" + plugin.checkManager().all().size());
        MessageUtil.send(sender, "&7Tracked players&8: &f" + plugin.playerDataManager().size());
        MessageUtil.send(sender, "&7Active test objects&8: &f" + plugin.investigationManager().activeCount());
        MessageUtil.send(sender, "&7Anti-xray&8: &f" + (plugin.antiXrayManager().isEnabled() ? "on" : "off"));
        MessageUtil.send(sender, "&7TPS&8: &f" + String.format("%.1f", ServerUtil.getTps()));
    }
}
