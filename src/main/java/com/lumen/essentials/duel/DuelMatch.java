package com.lumen.essentials.duel;

import java.util.UUID;

/** Mutable state of a single duel between two players. */
public final class DuelMatch {

    public enum State { COUNTDOWN, FIGHTING, ENDED }

    private final DuelArena arena;
    private final UUID player1;
    private final UUID player2;
    private final String name1;
    private final String name2;
    private final DuelSettings settings;

    private PlayerState saved1;
    private PlayerState saved2;
    private int score1;
    private int score2;
    private int round = 1;
    private State state = State.COUNTDOWN;

    public DuelMatch(DuelArena arena, UUID player1, String name1, UUID player2, String name2,
                     DuelSettings settings) {
        this.arena = arena;
        this.player1 = player1;
        this.name1 = name1;
        this.player2 = player2;
        this.name2 = name2;
        this.settings = settings;
    }

    public DuelArena arena() {
        return arena;
    }

    public UUID player1() {
        return player1;
    }

    public UUID player2() {
        return player2;
    }

    public String name(UUID uuid) {
        return uuid.equals(player1) ? name1 : name2;
    }

    public UUID opponent(UUID uuid) {
        return uuid.equals(player1) ? player2 : player1;
    }

    public boolean has(UUID uuid) {
        return uuid.equals(player1) || uuid.equals(player2);
    }

    public DuelSettings settings() {
        return settings;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int round() {
        return round;
    }

    public void nextRound() {
        this.round++;
    }

    public int score(UUID uuid) {
        return uuid.equals(player1) ? score1 : score2;
    }

    public void addScore(UUID uuid) {
        if (uuid.equals(player1)) {
            score1++;
        } else {
            score2++;
        }
    }

    public void setSaved(UUID uuid, PlayerState state) {
        if (uuid.equals(player1)) {
            saved1 = state;
        } else {
            saved2 = state;
        }
    }

    public PlayerState saved(UUID uuid) {
        return uuid.equals(player1) ? saved1 : saved2;
    }
}
