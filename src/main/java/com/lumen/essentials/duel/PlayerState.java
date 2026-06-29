package com.lumen.essentials.duel;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Snapshot of a player's full state taken before a duel and restored afterwards, so a
 * match never permanently changes inventory, location, health, etc. All restores are
 * defensive (wrapped in try/catch) so a single unsupported accessor can't strand a
 * player mid-restore.
 */
public final class PlayerState {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final Location location;
    private final GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final int level;
    private final float exp;
    private final List<PotionEffect> effects = new ArrayList<>();

    private PlayerState(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.location = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.health = safeHealth(player);
        this.foodLevel = readFood(player);
        this.level = readLevel(player);
        this.exp = readExp(player);
        try {
            Collection<PotionEffect> active = player.getActivePotionEffects();
            if (active != null) {
                effects.addAll(active);
            }
        } catch (Throwable ignored) {
            // effects unsupported; ignore
        }
    }

    public static PlayerState capture(Player player) {
        return new PlayerState(player);
    }

    /** Restores everything this snapshot captured. */
    public void restore(Player player) {
        DuelUtil.clearEffects(player);
        try {
            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);
        } catch (Throwable ignored) {
            // ignore
        }
        try {
            player.teleport(location);
        } catch (Throwable ignored) {
            // ignore
        }
        try {
            player.setGameMode(gameMode);
        } catch (Throwable ignored) {
            // ignore
        }
        try {
            player.setHealth(Math.min(health, DuelUtil.maxHealth(player)));
        } catch (Throwable ignored) {
            // ignore
        }
        trySet(() -> player.setFoodLevel(foodLevel));
        trySet(() -> player.setLevel(level));
        trySet(() -> player.setExp(exp));
        for (PotionEffect effect : effects) {
            try {
                player.addPotionEffect(effect);
            } catch (Throwable ignored) {
                // ignore
            }
        }
        DuelUtil.updateInventory(player);
    }

    public Location location() {
        return location;
    }

    private double safeHealth(Player player) {
        try {
            return player.getHealth();
        } catch (Throwable ignored) {
            return 20.0D;
        }
    }

    private int readFood(Player player) {
        try {
            return player.getFoodLevel();
        } catch (Throwable ignored) {
            return 20;
        }
    }

    private int readLevel(Player player) {
        try {
            return player.getLevel();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private float readExp(Player player) {
        try {
            return player.getExp();
        } catch (Throwable ignored) {
            return 0f;
        }
    }

    private void trySet(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
            // ignore
        }
    }
}
