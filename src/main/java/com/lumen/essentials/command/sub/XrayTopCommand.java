package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/** {@code /luac xraytop} — leaderboard of valuable-ore discoverers this session. */
public final class XrayTopCommand implements SubCommand {

    private final LumenEssentials plugin;

    public XrayTopCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "xraytop";
    }

    @Override
    public String permission() {
        return "luac.investigate";
    }

    @Override
    public String description() {
        return "Top valuable-ore discoverers.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<Map.Entry<String, Integer>> top = plugin.antiXrayManager().topDiscoverers(10);
        MessageUtil.send(sender, "&8&m----&r &bXray Top &8&m----");
        if (top.isEmpty()) {
            MessageUtil.send(sender, "&7No discoveries recorded yet.");
            return;
        }
        int rank = 1;
        for (Map.Entry<String, Integer> entry : top) {
            MessageUtil.send(sender, "&8#" + (rank++) + " &f" + entry.getKey()
                    + " &7- &e" + entry.getValue() + " finds");
        }
    }
}
