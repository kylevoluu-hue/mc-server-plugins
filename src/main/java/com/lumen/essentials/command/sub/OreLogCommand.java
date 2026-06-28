package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.antixray.OreDiscovery;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** {@code /luac orelog [player]} — recent valuable-ore discoveries. */
public final class OreLogCommand implements SubCommand {

    private final LumenEssentials plugin;

    public OreLogCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "orelog";
    }

    @Override
    public String permission() {
        return "luac.investigate";
    }

    @Override
    public String description() {
        return "Show recent ore discoveries [player].";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String filter = args.length > 0 ? args[0].toLowerCase() : null;
        List<OreDiscovery> list = new ArrayList<>(plugin.antiXrayManager().recentDiscoveries());
        Collections.reverse(list);
        MessageUtil.send(sender, "&8&m----&r &bOre Log &8&m----");
        int shown = 0;
        for (OreDiscovery d : list) {
            if (filter != null && !d.player().toLowerCase().equals(filter)) {
                continue;
            }
            MessageUtil.send(sender, String.format("&f%s &7%s &8@ &7%s %d,%d,%d &8exposed=%s",
                    d.player(), d.material().name(), d.world(), d.x(), d.y(), d.z(), d.exposed()));
            if (++shown >= 15) {
                break;
            }
        }
        if (shown == 0) {
            MessageUtil.send(sender, "&7No ore discoveries logged yet.");
        }
    }
}
