package com.lumen.essentials.command;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.command.sub.AlertsCommand;
import com.lumen.essentials.command.sub.ChecksCommand;
import com.lumen.essentials.command.sub.DebugCommand;
import com.lumen.essentials.command.sub.ExemptCommand;
import com.lumen.essentials.command.sub.InfoCommand;
import com.lumen.essentials.command.sub.InspectMineCommand;
import com.lumen.essentials.command.sub.OreLogCommand;
import com.lumen.essentials.command.sub.OreSummonCommand;
import com.lumen.essentials.command.sub.ProfileCommand;
import com.lumen.essentials.command.sub.ReloadCommand;
import com.lumen.essentials.command.sub.SpawnStashCommand;
import com.lumen.essentials.command.sub.VerboseCommand;
import com.lumen.essentials.command.sub.ViolationsCommand;
import com.lumen.essentials.command.sub.XrayTopCommand;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thin dispatcher for {@code /luac} (alias {@code /lumen}). Owns the subcommand
 * registry and provides permission-aware tab completion and help.
 */
public final class CommandManager implements CommandExecutor, TabCompleter {

    private final LumenEssentials plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public CommandManager(LumenEssentials plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        add(new ReloadCommand(plugin));
        add(new InfoCommand(plugin));
        add(new ChecksCommand(plugin));
        add(new AlertsCommand(plugin));
        add(new DebugCommand(plugin));
        add(new VerboseCommand(plugin));
        add(new ViolationsCommand(plugin));
        add(new ProfileCommand(plugin));
        add(new ExemptCommand(plugin));
        add(new OreLogCommand(plugin));
        add(new InspectMineCommand(plugin));
        add(new XrayTopCommand(plugin));
        add(new SpawnStashCommand(plugin));
        add(new OreSummonCommand(plugin));
    }

    private void add(SubCommand command) {
        subCommands.put(command.name().toLowerCase(Locale.ROOT), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            MessageUtil.send(sender, "&cUnknown subcommand &f" + args[0] + "&c. Try &f/luac help");
            return true;
        }
        if (!hasPermission(sender, sub)) {
            MessageUtil.send(sender, plugin.configManager().message("no-permission",
                    "&cYou do not have permission to use that."));
            return true;
        }
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        try {
            sub.execute(sender, subArgs);
        } catch (Throwable t) {
            MessageUtil.send(sender, "&cAn error occurred running that command. Check console.");
            plugin.getLogger().warning("Error in /luac " + sub.name() + ": " + t);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                if (hasPermission(sender, sub) && sub.name().startsWith(prefix)) {
                    out.add(sub.name());
                }
            }
            return out;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub != null && hasPermission(sender, sub)) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            return sub.tabComplete(sender, subArgs);
        }
        return new ArrayList<>();
    }

    private boolean hasPermission(CommandSender sender, SubCommand sub) {
        return sub.permission() == null || sender.hasPermission(sub.permission());
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "&8&m----&r &bLumen Essentials &8&m----");
        for (SubCommand sub : subCommands.values()) {
            if (hasPermission(sender, sub)) {
                MessageUtil.send(sender, "&b/luac " + sub.name() + " &8- &7" + sub.description());
            }
        }
    }
}
