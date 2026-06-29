package com.lumen.essentials.duel;

/**
 * The configurable options chosen in the duel setup menu before a match: which kit,
 * how many rounds (best-of), the arena weather and the time of day.
 */
public final class DuelSettings {

    public enum Weather { CLEAR, RAIN, THUNDER }

    public enum Time { DAY, NIGHT }

    private String kit = "Sword";
    private int rounds = 3;          // best-of (odd)
    private Weather weather = Weather.CLEAR;
    private Time time = Time.DAY;

    public String kit() {
        return kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public int rounds() {
        return rounds;
    }

    /** Cycles 1 -> 3 -> 5 -> 1. */
    public void cycleRounds() {
        rounds = rounds == 1 ? 3 : rounds == 3 ? 5 : 1;
    }

    /** Wins required to take the match (best-of majority). */
    public int winsNeeded() {
        return rounds / 2 + 1;
    }

    public Weather weather() {
        return weather;
    }

    public void cycleWeather() {
        Weather[] values = Weather.values();
        weather = values[(weather.ordinal() + 1) % values.length];
    }

    public Time time() {
        return time;
    }

    public void cycleTime() {
        time = time == Time.DAY ? Time.NIGHT : Time.DAY;
    }
}
