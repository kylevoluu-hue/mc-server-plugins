package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.player.PlayerData;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code /luac inspectmine <player>} — mining suspicion summary for a player. */
public final class InspectMineCommand implements SubCommand {

    private final LumenEssentials plugin;

    public InspectMineCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "inspectmine";
    }

    @Override
    public String permission() {
        return "luac.investigate";
    }

    @Override
    public String description() {
        return "Inspect a player's mining suspicion.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /luac inspectmine <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not online.");
            return;
        }
        PlayerData data = plugin.playerDataManager().getIfPresent(target.getUniqueId());
        Map<String, Object> result = plugin.antiXrayManager().inspect(data, target.getUniqueId());
        MessageUtil.send(sender, "&8&m----&r &bMining: &f" + target.getName() + " &8&m----");
        result.forEach((k, v) -> MessageUtil.send(sender, "&7" + k + "&8: &f" + v));
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
