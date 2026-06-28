package com.lumen.essentials.checks.combat;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.math.MathUtil;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Analyzes the timing distribution of melee attacks to detect autoclickers. Rather
 * than a naive CPS cap (which lags-flags fast legitimate players), it looks for
 * inhuman <em>consistency</em>: a very low coefficient of variation across many
 * samples indicates a machine-generated click pattern. A minimum sample count and
 * a high CPS gate keep false positives low.
 */
public final class AutoClickerCheck extends CombatCheck {

    private static final int MAX_SAMPLES = 20;
    private static final int MIN_SAMPLES = 12;

    public AutoClickerCheck(LumenEssentials plugin) {
        super(plugin, "autoclicker");
    }

    @Override
    public void handleAttack(Player attacker, PlayerData data, Entity victim,
                             EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        data.recordAttack(now, MAX_SAMPLES);
        if (data.attackIntervals().size() < MIN_SAMPLES) {
            return;
        }

        double meanInterval = MathUtil.average(data.attackIntervals());
        if (meanInterval <= 0) {
            return;
        }
        double cps = 1000.0D / meanInterval;
        // Only scrutinize sustained high click rates.
        double cpsGate = settings().threshold() <= 0 ? 9.0D : settings().threshold();
        if (cps < cpsGate) {
            return;
        }

        double cv = MathUtil.coefficientOfVariation(data.attackIntervals());
        // Human clicking has natural jitter (cv typically > 0.08). Machine timing
        // is far more regular; flag very low variation at high CPS.
        if (cv < 0.06D) {
            fail(attacker, data, 1.0D, String.format("cps=%.1f cv=%.3f samples=%d",
                    cps, cv, data.attackIntervals().size()));
        } else {
            debug(attacker, String.format("cps=%.1f cv=%.3f (ok)", cps, cv));
        }
    }
}
