package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.investigation.TestObject;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /luac spawnstash <player>|<world x y z>|random} — places a hidden test stash
 * for controlled staff investigations. Honors confirmation and logging settings.
 */
public final class SpawnStashCommand implements SubCommand {

    private final LumenEssentials plugin;

    public SpawnStashCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "spawnstash";
    }

    @Override
    public String permission() {
        return "luac.spawnstash";
    }

    @Override
    public String description() {
        return "Create a hidden test stash (<player>|<world x y z>|random).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cThis command must be run by a player (for context).");
            return;
        }
        Player staff = (Player) sender;
        Location location = InvestigationLocations.resolve(plugin, staff, args);
        if (location == null) {
            MessageUtil.send(sender, "&cUsage: /luac spawnstash <player> | <world x y z> | random");
            return;
        }
        if (InvestigationLocations.needsConfirmation(plugin, args)) {
            MessageUtil.send(sender, "&eAppend &fconfirm&e to proceed: &7/luac spawnstash "
                    + String.join(" ", args) + " confirm");
            return;
        }
        TestObject object = plugin.investigationManager().spawnStash(staff, location);
        if (object == null) {
            MessageUtil.send(sender, "&cCould not create stash (disabled or invalid location).");
            return;
        }
        Location o = object.origin();
        MessageUtil.send(sender, String.format("&aTest stash created &7at %s %d,%d,%d &8(id %s)",
                o.getWorld() == null ? "?" : o.getWorld().getName(),
                o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                object.id().toString().substring(0, 8)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return InvestigationLocations.tabComplete(args);
    }
}
