package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.SubCommand;
import com.lumen.essentials.investigation.TestObject;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code /luac spawnstash <type> <size> [target] [confirm]} — places a hidden,
 * randomized test stash. {@code type} is chest|barrel|shulker|spawner|base|random,
 * {@code size} is small|regular|large|huge. The optional target is a player,
 * {@code world x y z}, or {@code random}; omitted, it uses the staff member's spot.
 */
public final class SpawnStashCommand implements SubCommand {

    private static final List<String> TYPES =
            Arrays.asList("chest", "barrel", "shulker", "spawner", "base", "random");
    private static final List<String> SIZES =
            Arrays.asList("small", "regular", "large", "huge");

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
        return "Create a hidden test stash: <type> <size> [target].";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cThis command must be run by a player (for context).");
            return;
        }
        Player staff = (Player) sender;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /luac spawnstash <type> <size> [player|world x y z|random]");
            MessageUtil.send(sender, "&7Types: &f" + String.join(", ", TYPES) + " &8| &7Sizes: &f"
                    + String.join(", ", SIZES));
            return;
        }
        String type = args[0];
        String size = args[1];

        String[] targetArgs = Arrays.copyOfRange(args, 2, args.length);
        Location location = InvestigationLocations.resolveOrSelf(plugin, staff, targetArgs);
        if (location == null) {
            MessageUtil.send(sender, "&cInvalid target. Use a player, world x y z, or random.");
            return;
        }
        if (InvestigationLocations.needsConfirmation(plugin, args)) {
            MessageUtil.send(sender, "&eAppend &fconfirm&e to proceed.");
            return;
        }

        TestObject object = plugin.investigationManager().spawnStash(staff, location, type, size);
        if (object == null) {
            MessageUtil.send(sender, "&cCould not create stash (disabled or invalid location).");
            return;
        }
        Location o = object.origin();
        MessageUtil.send(sender, String.format("&aTest stash (&f%s/%s&a) created at %s %d,%d,%d &8(id %s)",
                type, size, o.getWorld() == null ? "?" : o.getWorld().getName(),
                o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                object.id().toString().substring(0, 8)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(TYPES, args[0]);
        }
        if (args.length == 2) {
            return filter(SIZES, args[1]);
        }
        return InvestigationLocations.tabComplete(Arrays.copyOfRange(args, 2, args.length));
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix.toLowerCase())) {
                out.add(option);
            }
        }
        return out;
    }
}
