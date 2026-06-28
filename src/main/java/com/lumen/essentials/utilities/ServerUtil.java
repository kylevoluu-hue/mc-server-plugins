package com.lumen.essentials.utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Server-state helpers (TPS, ping) implemented defensively so the plugin works
 * across many server versions where method signatures differ.
 */
public final class ServerUtil {

    private static Method getTpsMethod;
    private static boolean tpsResolved;

    private ServerUtil() {
    }

    /**
     * Returns the most recent (1m) server TPS, clamped to a sane range.
     * Falls back to 20.0 on servers that do not expose {@code getTPS()}.
     */
    public static double getTps() {
        try {
            if (!tpsResolved) {
                tpsResolved = true;
                try {
                    getTpsMethod = Bukkit.getServer().getClass().getMethod("getTPS");
                } catch (NoSuchMethodException ignored) {
                    getTpsMethod = null;
                }
            }
            if (getTpsMethod != null) {
                Object result = getTpsMethod.invoke(Bukkit.getServer());
                if (result instanceof double[]) {
                    double[] tps = (double[]) result;
                    if (tps.length > 0) {
                        return clampTps(tps[0]);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through to default.
        }
        return 20.0D;
    }

    private static double clampTps(double tps) {
        if (Double.isNaN(tps) || tps < 0) {
            return 20.0D;
        }
        return Math.min(tps, 20.0D);
    }

    /**
     * Returns a player's ping in milliseconds. Paper exposes {@link Player#getPing()};
     * older servers require reflection into the NMS entity, handled gracefully.
     */
    public static int getPing(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            return Math.max(0, player.getPing());
        } catch (Throwable ignored) {
            // Player#getPing absent on very old APIs; treat as unknown.
            return 0;
        }
    }
}
