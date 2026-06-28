package com.lumen.essentials.api.event;

import com.lumen.essentials.violations.Violation;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever a check records a violation, before alerts or punishments run.
 * Cancelling suppresses all downstream handling (alert, punishment, logging) for
 * that single detection — useful for plugins implementing custom exemption logic.
 */
public final class ViolationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Violation violation;
    private boolean cancelled;

    public ViolationEvent(Player player, Violation violation) {
        this.player = player;
        this.violation = violation;
    }

    public Player getPlayer() {
        return player;
    }

    public Violation getViolation() {
        return violation;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
