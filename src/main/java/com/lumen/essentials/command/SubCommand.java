package com.lumen.essentials.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * One {@code /luac <name>} subcommand. Keeping each subcommand behind this small
 * interface lets {@link CommandManager} stay a thin dispatcher and makes adding a
 * command a matter of registering one more implementation.
 */
public interface SubCommand {

    /** Primary name, e.g. {@code "reload"}. */
    String name();

    /** Required permission node, or {@code null} for "available to anyone". */
    String permission();

    /** Short usage/description line shown in {@code /luac help}. */
    String description();

    /** Runs the command. {@code args} excludes the subcommand name itself. */
    void execute(CommandSender sender, String[] args);

    /** Tab completions for the current argument index (default: none). */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
