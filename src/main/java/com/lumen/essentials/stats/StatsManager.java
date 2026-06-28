package com.lumen.essentials.stats;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

/**
 * Reads gameplay statistics from the vanilla statistic store. Statistic enum names
 * differ slightly across versions (PLAY_ONE_TICK vs PLAY_ONE_MINUTE), so playtime is
 * resolved defensively.
 */
public final class StatsManager {

    private final Statistic playtimeStatistic;

    public StatsManager() {
        this.playtimeStatistic = resolvePlaytime();
    }

    private Statistic resolvePlaytime() {
        for (String name : new String[]{"PLAY_ONE_TICK", "PLAY_ONE_MINUTE", "PLAYTIME"}) {
            try {
                return Statistic.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try the next known name.
            }
        }
        return null;
    }

    public int mobKills(Player player) {
        return read(player, "MOB_KILLS");
    }

    public int playerKills(Player player) {
        return read(player, "PLAYER_KILLS");
    }

    public int deaths(Player player) {
        return read(player, "DEATHS");
    }

    /** Playtime in ticks (20 ticks/second). */
    public long playtimeTicks(Player player) {
        if (playtimeStatistic == null) {
            return 0L;
        }
        try {
            return player.getStatistic(playtimeStatistic);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** Human readable playtime, e.g. "12h 34m". */
    public String playtimeFormatted(Player player) {
        long ticks = playtimeTicks(player);
        long totalMinutes = ticks / 20L / 60L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        long days = hours / 24L;
        hours = hours % 24L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        sb.append(hours).append("h ").append(minutes).append("m");
        return sb.toString();
    }

    private int read(Player player, String statName) {
        try {
            return player.getStatistic(Statistic.valueOf(statName));
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
