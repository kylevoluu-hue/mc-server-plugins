package com.lumen.essentials.checks;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.combat.AutoClickerCheck;
import com.lumen.essentials.checks.combat.CombatCheck;
import com.lumen.essentials.checks.combat.ReachCheck;
import com.lumen.essentials.checks.movement.FlyCheck;
import com.lumen.essentials.checks.movement.MovementCheck;
import com.lumen.essentials.checks.movement.NoFallCheck;
import com.lumen.essentials.checks.movement.SpeedCheck;
import com.lumen.essentials.checks.movement.TimerCheck;
import com.lumen.essentials.checks.world.BreakCheck;
import com.lumen.essentials.checks.world.FastBreakCheck;
import com.lumen.essentials.checks.world.NukerCheck;
import com.lumen.essentials.checks.world.PlaceCheck;
import com.lumen.essentials.checks.world.ScaffoldCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers, configures and exposes all checks. Listeners pull the typed lists from
 * here, so adding a new check is a single line in {@link #registerAll()} — the rest
 * of the system needs no changes (open/closed principle).
 */
public final class CheckManager {

    private final LumenEssentials plugin;

    private final List<Check> all = new ArrayList<>();
    private final List<MovementCheck> movement = new ArrayList<>();
    private final List<CombatCheck> combat = new ArrayList<>();
    private final List<BreakCheck> breaks = new ArrayList<>();
    private final List<PlaceCheck> places = new ArrayList<>();

    public CheckManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        // Movement
        addMovement(new SpeedCheck(plugin));
        addMovement(new FlyCheck(plugin));
        addMovement(new NoFallCheck(plugin));
        addMovement(new TimerCheck(plugin));

        // Combat
        addCombat(new ReachCheck(plugin));
        addCombat(new AutoClickerCheck(plugin));

        // World
        addBreak(new FastBreakCheck(plugin));
        addBreak(new NukerCheck(plugin));
        addPlace(new ScaffoldCheck(plugin));

        reload();
    }

    private void addMovement(MovementCheck check) {
        movement.add(check);
        all.add(check);
    }

    private void addCombat(CombatCheck check) {
        combat.add(check);
        all.add(check);
    }

    private void addBreak(BreakCheck check) {
        breaks.add(check);
        all.add(check);
    }

    private void addPlace(PlaceCheck check) {
        places.add(check);
        all.add(check);
    }

    /** Reloads every check's settings from the (possibly freshly reloaded) config. */
    public void reload() {
        for (Check check : all) {
            check.reload(plugin.configManager().checks());
        }
    }

    public List<Check> all() {
        return Collections.unmodifiableList(all);
    }

    public List<MovementCheck> movement() {
        return movement;
    }

    public List<CombatCheck> combat() {
        return combat;
    }

    public List<BreakCheck> breaks() {
        return breaks;
    }

    public List<PlaceCheck> places() {
        return places;
    }
}
