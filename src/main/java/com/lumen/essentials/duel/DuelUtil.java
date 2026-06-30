package com.lumen.essentials.duel;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/** Small version-safe helpers shared by the duel classes. */
final class DuelUtil {

    private DuelUtil() {
    }

    static double maxHealth(Player player) {
        try {
            return player.getMaxHealth();
        } catch (Throwable ignored) {
            return 20.0D;
        }
    }

    static void clearEffects(Player player) {
        try {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        } catch (Throwable ignored) {
            // effects unsupported; ignore
        }
    }

    static void heal(Player player) {
        try {
            player.setHealth(maxHealth(player));
        } catch (Throwable ignored) {
            // ignore
        }
        try {
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExp(0f);
            player.setLevel(0);
            player.setFireTicks(0);
            player.setFallDistance(0f);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    static void updateInventory(Player player) {
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
            // ignore
        }
    }

    static void title(Player player, String title, String subtitle) {
        try {
            player.sendTitle(title, subtitle, 5, 30, 5);
        } catch (Throwable ignored) {
            // sendTitle unsupported on this version; ignore
        }
    }
}
