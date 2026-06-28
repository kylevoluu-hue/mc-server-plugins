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

/** {@code /luac exempt <player>} — toggles a runtime check exemption for a player. */
public final class ExemptCommand implements SubCommand {

    private final LumenEssentials plugin;

    public ExemptCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "exempt";
    }

    @Override
    public String permission() {
        return "luac.exempt";
    }

    @Override
    public String description() {
        return "Toggle check exemption for a player.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, "&cUsage: /luac exempt <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(sender, "&cPlayer not online.");
            return;
        }
        PlayerData data = plugin.playerDataManager().getOrCreate(target);
        boolean newState = !data.isExempt();
        data.setExempt(newState);
        MessageUtil.send(sender, "&7" + target.getName() + " is now "
                + (newState ? "&aexempt" : "&cnot exempt") + "&7 from checks.");
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
