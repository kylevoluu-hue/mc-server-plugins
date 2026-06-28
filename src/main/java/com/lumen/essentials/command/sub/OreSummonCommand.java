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
 * {@code /luac oresummon <oreType> <amount> [target] [confirm]} — places a hidden,
 * randomized test ore vein. {@code oreType} accepts friendly aliases (diamond, gold,
 * ancient_debris, emerald, iron, copper, redstone, lapis, coal, *_block, random) and
 * {@code amount} is the vein size. The optional target is a player, {@code world x y z},
 * or {@code random}; omitted, it uses the staff member's location.
 */
public final class OreSummonCommand implements SubCommand {

    private static final List<String> TYPES = Arrays.asList(
            "diamond", "gold", "ancient_debris", "emerald", "iron", "copper",
            "redstone", "lapis", "coal", "diamond_block", "gold_block",
            "emerald_block", "iron_block", "netherite_block", "random");

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
        return "Create a hidden test ore vein: <oreType> <amount> [target].";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cThis command must be run by a player (for context).");
            return;
        }
        Player staff = (Player) sender;
        if (args.length < 1) {
            MessageUtil.send(sender, "&cUsage: /luac oresummon <oreType> <amount> [player|world x y z|random]");
            MessageUtil.send(sender, "&7Ores: &f" + String.join(", ", TYPES));
            return;
        }
        String oreType = args[0];
        int amount = 0; // 0 => random within config bounds
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                MessageUtil.send(sender, "&cAmount must be a number.");
                return;
            }
        }

        String[] targetArgs = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[0];
        Location location = InvestigationLocations.resolveOrSelf(plugin, staff, targetArgs);
        if (location == null) {
            MessageUtil.send(sender, "&cInvalid target. Use a player, world x y z, or random.");
            return;
        }
        if (InvestigationLocations.needsConfirmation(plugin, args)) {
            MessageUtil.send(sender, "&eAppend &fconfirm&e to proceed.");
            return;
        }

        TestObject object = plugin.investigationManager().oreSummon(staff, location, oreType, amount);
        if (object == null) {
            MessageUtil.send(sender, "&cCould not create ore vein (disabled, invalid ore, or location).");
            return;
        }
        Location o = object.origin();
        MessageUtil.send(sender, String.format("&aTest ore vein (&f%s&a) created near %s %d,%d,%d &8(id %s)",
                oreType, o.getWorld() == null ? "?" : o.getWorld().getName(),
                o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                object.id().toString().substring(0, 8)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String type : TYPES) {
                if (type.startsWith(args[0].toLowerCase())) {
                    out.add(type);
                }
            }
            return out;
        }
        if (args.length == 2) {
            return Arrays.asList("3", "6", "12", "24");
        }
        return InvestigationLocations.tabComplete(Arrays.copyOfRange(args, 2, args.length));
    }
}
