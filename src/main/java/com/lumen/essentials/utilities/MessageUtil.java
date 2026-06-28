package com.lumen.essentials.utilities;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Small helper for colorizing and sending chat messages. Centralizing this keeps
 * color handling consistent and avoids scattering {@link ChatColor} calls everywhere.
 */
public final class MessageUtil {

    private MessageUtil() {
    }

    /** Translates {@code &}-style color codes into Minecraft section codes. */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /** Sends a single colorized line to the target. */
    public static void send(CommandSender target, String message) {
        if (target != null && message != null && !message.isEmpty()) {
            target.sendMessage(color(message));
        }
    }

    /** Strips all color codes from a string (useful for log files). */
    public static String strip(String input) {
        return input == null ? "" : ChatColor.stripColor(color(input));
    }
}
