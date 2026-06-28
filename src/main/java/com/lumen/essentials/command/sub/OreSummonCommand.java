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
 * {@code /luac oresummon <player>|<world x y z>|random} — places a hidden randomized
 * test ore vein for xray investigations.
 */
public final class OreSummonCommand implements SubCommand {

    private final LumenEssentials plugin;

    public OreSummonCommand(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "oresummon";
    }

    @Override
    public String permission() {
        return "luac.oresummon";
    }

    @Override
    public String description() {
        return "Create a hidden test ore vein (<player>|<world x y z>|random).";
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
            MessageUtil.send(sender, "&cUsage: /luac oresummon <player> | <world x y z> | random");
            return;
        }
        if (InvestigationLocations.needsConfirmation(plugin, args)) {
            MessageUtil.send(sender, "&eAppend &fconfirm&e to proceed: &7/luac oresummon "
                    + String.join(" ", args) + " confirm");
            return;
        }
        TestObject object = plugin.investigationManager().oreSummon(staff, location);
        if (object == null) {
            MessageUtil.send(sender, "&cCould not create ore vein (disabled or invalid location).");
            return;
        }
        Location o = object.origin();
        MessageUtil.send(sender, String.format("&aTest ore vein created &7near %s %d,%d,%d &8(id %s)",
                o.getWorld() == null ? "?" : o.getWorld().getName(),
                o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                object.id().toString().substring(0, 8)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return InvestigationLocations.tabComplete(args);
    }
}
