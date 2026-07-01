package com.lumen.essentials.duel;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Mutable state of a single (possibly team) duel. Team 0 = A, team 1 = B. */
public final class DuelMatch {

    public enum State { COUNTDOWN, FIGHTING, ENDED }

    private final DuelMode mode;
    private final DuelSettings settings;
    private final Integer arenaTile;
    private final List<UUID> teamA;
    private final List<UUID> teamB;
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, PlayerState> saved = new HashMap<>();
    private final Set<UUID> aliveA = new HashSet<>();
    private final Set<UUID> aliveB = new HashSet<>();

    // Every block a dueler changed (placed or broke) -> its original material, so the
    // arena can be repaired exactly each round/match.
    private final Map<Location, Material> changedBlocks = new ConcurrentHashMap<>();
    private boolean breakable;
    private int scoreA;
    private int scoreB;
    private int round = 1;
    private State state = State.COUNTDOWN;

    public DuelMatch(DuelMode mode, DuelSettings settings, Integer arenaTile,
                     List<UUID> teamA, List<UUID> teamB, Map<UUID, String> names) {
        this.mode = mode;
        this.settings = settings;
        this.arenaTile = arenaTile;
        this.teamA = new ArrayList<>(teamA);
        this.teamB = new ArrayList<>(teamB);
        this.names.putAll(names);
    }

    public DuelMode mode() {
        return mode;
    }

    public DuelSettings settings() {
        return settings;
    }

    public Integer arenaTile() {
        return arenaTile;
    }

    public List<UUID> teamA() {
        return teamA;
    }

    public List<UUID> teamB() {
        return teamB;
    }

    public List<UUID> allPlayers() {
        List<UUID> all = new ArrayList<>(teamA);
        all.addAll(teamB);
        return all;
    }

    public boolean has(UUID uuid) {
        return teamA.contains(uuid) || teamB.contains(uuid);
    }

    /** 0 for team A, 1 for team B, -1 if not in the match. */
    public int teamOf(UUID uuid) {
        if (teamA.contains(uuid)) {
            return 0;
        }
        return teamB.contains(uuid) ? 1 : -1;
    }

    public List<UUID> team(int index) {
        return index == 0 ? teamA : teamB;
    }

    public String name(UUID uuid) {
        return names.getOrDefault(uuid, "?");
    }

    public String teamName(int index) {
        List<UUID> team = team(index);
        List<String> parts = new ArrayList<>();
        for (UUID uuid : team) {
            parts.add(name(uuid));
        }
        return String.join(", ", parts);
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

    public int score(int team) {
        return team == 0 ? scoreA : scoreB;
    }

    public void addScore(int team) {
        if (team == 0) {
            scoreA++;
        } else {
            scoreB++;
        }
    }

    public void resetAlive() {
        aliveA.clear();
        aliveA.addAll(teamA);
        aliveB.clear();
        aliveB.addAll(teamB);
    }

    public Set<UUID> alive(int team) {
        return team == 0 ? aliveA : aliveB;
    }

    public void eliminate(UUID uuid) {
        aliveA.remove(uuid);
        aliveB.remove(uuid);
    }

    /** Removes a player from the match entirely (forfeit/quit). */
    public void removePlayer(UUID uuid) {
        eliminate(uuid);
        teamA.remove(uuid);
        teamB.remove(uuid);
    }

    public void setSaved(UUID uuid, PlayerState state) {
        saved.put(uuid, state);
    }

    public PlayerState saved(UUID uuid) {
        return saved.get(uuid);
    }

    public boolean breakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    /** Records a block change (only the first time per location keeps the true original). */
    public void trackChange(Location location, Material original) {
        changedBlocks.putIfAbsent(location, original == null ? Material.AIR : original);
    }

    public Map<Location, Material> changedBlocks() {
        return changedBlocks;
    }
}
