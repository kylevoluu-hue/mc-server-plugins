package com.lumen.essentials.player;

import com.lumen.essentials.violations.ViolationTracker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player runtime state. One instance exists per online player and is discarded
 * on disconnect (see {@code PlayerDataManager}). Holds movement snapshots, timing
 * buffers and suspicion scores consulted by the various checks.
 *
 * <p>This class is touched from the main thread only; it is intentionally not
 * synchronized to keep the hot path cheap.
 */
public final class PlayerData {

    private final UUID uuid;
    private final String name;

    private final MovementSnapshot movement = new MovementSnapshot();
    private final ViolationTracker violations = new ViolationTracker();

    /** Recent attack intervals (ms) for autoclicker / timing analysis. */
    private final Deque<Long> attackIntervals = new ArrayDeque<>();
    private long lastAttackTime;

    /** Recent block-break / place timestamps for fast-action checks. */
    private final Deque<Long> breakTimes = new ArrayDeque<>();
    private final Deque<Long> placeTimes = new ArrayDeque<>();

    /** Generic scratch buffers keyed by check name (e.g. running counters). */
    private final Map<String, Double> buffers = new HashMap<>();

    /** Suspicion scores for non-instant-ban detections (xray/ESP/basefinder). */
    private final Map<String, Double> suspicion = new HashMap<>();

    private boolean exempt;
    private long lastTeleportTime;
    private long lastVelocityTime;
    private int airTicks;
    private int groundTicks;
    private String clientBrand = "unknown";

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public MovementSnapshot movement() {
        return movement;
    }

    public ViolationTracker violations() {
        return violations;
    }

    // --- Air / ground tick tracking ---------------------------------------

    public void tickGround(boolean onGround) {
        if (onGround) {
            groundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            groundTicks = 0;
        }
    }

    public int airTicks() {
        return airTicks;
    }

    public int groundTicks() {
        return groundTicks;
    }

    // --- Combat timing -----------------------------------------------------

    public void recordAttack(long now, int maxSamples) {
        if (lastAttackTime != 0) {
            attackIntervals.addLast(now - lastAttackTime);
            while (attackIntervals.size() > maxSamples) {
                attackIntervals.pollFirst();
            }
        }
        lastAttackTime = now;
    }

    public Deque<Long> attackIntervals() {
        return attackIntervals;
    }

    // --- World action timing ----------------------------------------------

    public void recordBreak(long now, int maxSamples) {
        breakTimes.addLast(now);
        while (breakTimes.size() > maxSamples) {
            breakTimes.pollFirst();
        }
    }

    public void recordPlace(long now, int maxSamples) {
        placeTimes.addLast(now);
        while (placeTimes.size() > maxSamples) {
            placeTimes.pollFirst();
        }
    }

    public Deque<Long> breakTimes() {
        return breakTimes;
    }

    public Deque<Long> placeTimes() {
        return placeTimes;
    }

    // --- Generic buffers ---------------------------------------------------

    public double buffer(String key) {
        return buffers.getOrDefault(key, 0.0D);
    }

    public void setBuffer(String key, double value) {
        buffers.put(key, value);
    }

    public double addBuffer(String key, double delta, double min, double max) {
        double next = Math.max(min, Math.min(max, buffer(key) + delta));
        buffers.put(key, next);
        return next;
    }

    // --- Suspicion ---------------------------------------------------------

    public double suspicion(String key) {
        return suspicion.getOrDefault(key, 0.0D);
    }

    public double addSuspicion(String key, double delta) {
        double next = Math.max(0.0D, suspicion(key) + delta);
        suspicion.put(key, next);
        return next;
    }

    public Map<String, Double> suspicionScores() {
        return suspicion;
    }

    // --- Misc flags --------------------------------------------------------

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public void markTeleport() {
        this.lastTeleportTime = System.currentTimeMillis();
    }

    public boolean recentlyTeleported(long withinMs) {
        return System.currentTimeMillis() - lastTeleportTime < withinMs;
    }

    public void markVelocity() {
        this.lastVelocityTime = System.currentTimeMillis();
    }

    public boolean recentlyTookVelocity(long withinMs) {
        return System.currentTimeMillis() - lastVelocityTime < withinMs;
    }

    public String clientBrand() {
        return clientBrand;
    }

    public void setClientBrand(String clientBrand) {
        this.clientBrand = clientBrand == null ? "unknown" : clientBrand;
    }
}
