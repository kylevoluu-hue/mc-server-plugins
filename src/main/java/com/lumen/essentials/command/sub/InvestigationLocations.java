package com.lumen.essentials.command.sub;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared argument parsing for the investigation commands ({@code spawnstash} /
 * {@code oresummon}), which accept the same target forms. Centralized here to avoid
 * duplicating the parsing and confirmation logic in both commands.
 */
final class InvestigationLocations {

    private static final Random RANDOM = new Random();

    private InvestigationLocations() {
    }

    /**
     * Resolves a target location from one of:
     * <ul>
     *     <li>{@code <player>} — that player's location</li>
     *     <li>{@code <world> <x> <y> <z>} — explicit coordinates</li>
     *     <li>{@code random} — a random online player (or the staff member)</li>
     * </ul>
     * A trailing {@code confirm} token is ignored for parsing purposes.
     */
    static Location resolve(LumenEssentials plugin, Player staff, String[] rawArgs) {
        String[] args = stripConfirm(rawArgs);
        if (args.length == 0) {
            return null;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("random")) {
            List<? extends Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.remove(staff);
            Player base = players.isEmpty() ? staff : players.get(RANDOM.nextInt(players.size()));
            return base.getLocation();
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            return target == null ? null : target.getLocation();
        }

        if (args.length == 4) {
            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                return null;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                return new Location(world, x, y, z);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * Like {@link #resolve}, but when no target args are supplied (after stripping a
     * trailing {@code confirm}) it defaults to the staff member's own location.
     */
    static Location resolveOrSelf(LumenEssentials plugin, Player staff, String[] rawArgs) {
        if (stripConfirm(rawArgs).length == 0) {
            return staff.getLocation();
        }
        return resolve(plugin, staff, rawArgs);
    }

    /** True when confirmation is required and the {@code confirm} token is absent. */
    static boolean needsConfirmation(LumenEssentials plugin, String[] args) {
        boolean require = plugin.configManager().investigation()
                .getBoolean("investigation-tools.require-confirmation", true);
        if (!require) {
            return false;
        }
        for (String arg : args) {
            if (arg.equalsIgnoreCase("confirm")) {
                return false;
            }
        }
        return true;
    }

    private static String[] stripConfirm(String[] args) {
        if (args.length > 0 && args[args.length - 1].equalsIgnoreCase("confirm")) {
            String[] copy = new String[args.length - 1];
            System.arraycopy(args, 0, copy, 0, copy.length);
            return copy;
        }
        return args;
    }

    static List<String> tabComplete(String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("random".startsWith(prefix)) {
                out.add("random");
            }
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    out.add(p.getName());
                }
            });
        }
        return out;
    }
}
