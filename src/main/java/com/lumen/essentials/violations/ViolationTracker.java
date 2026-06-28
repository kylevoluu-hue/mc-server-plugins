package com.lumen.essentials.violations;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks decaying violation levels per check for a single player. Decay is applied
 * lazily (only when a score is read or written) so there is no background task and
 * no per-tick cost for idle players.
 */
public final class ViolationTracker {

    private static final class Score {
        double value;
        long lastUpdate;
    }

    private final Map<String, Score> scores = new HashMap<>();

    /**
     * Applies time-based decay then adds {@code amount} to the named check's score.
     *
     * @param check       check identifier
     * @param amount      violation level to add
     * @param decayPerSec how much score decays each second
     * @return the new (post-add) violation level
     */
    public double add(String check, double amount, double decayPerSec) {
        Score score = scores.computeIfAbsent(check, k -> new Score());
        applyDecay(score, decayPerSec);
        score.value = Math.max(0.0D, score.value + amount);
        score.lastUpdate = System.currentTimeMillis();
        return score.value;
    }

    /** Returns the current (decay-adjusted) violation level for a check. */
    public double get(String check, double decayPerSec) {
        Score score = scores.get(check);
        if (score == null) {
            return 0.0D;
        }
        applyDecay(score, decayPerSec);
        return score.value;
    }

    public void reset(String check) {
        scores.remove(check);
    }

    public void resetAll() {
        scores.clear();
    }

    public Map<String, Double> snapshot() {
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Score> entry : scores.entrySet()) {
            out.put(entry.getKey(), entry.getValue().value);
        }
        return out;
    }

    private void applyDecay(Score score, double decayPerSec) {
        if (score.lastUpdate == 0) {
            score.lastUpdate = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        double elapsedSec = (now - score.lastUpdate) / 1000.0D;
        if (elapsedSec > 0 && decayPerSec > 0) {
            score.value = Math.max(0.0D, score.value - decayPerSec * elapsedSec);
            score.lastUpdate = now;
        }
    }
}
