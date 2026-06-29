package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Builds and applies the predefined duel kits (UHC, Sword, Crystal, Axe, SMP) and the
 * per-player Custom kit. Materials and enchantments are resolved defensively so a kit
 * still works (degraded) on older versions that lack a block/enchant.
 */
public final class KitFactory {

    /** The kit names shown in the menu, in order. */
    public static final String[] KITS = {"UHC", "Sword", "Crystal", "Axe", "SMP", "Custom"};

    private final LumenEssentials plugin;

    public KitFactory(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /** Clears the player's inventory and applies the named kit. */
    public void apply(Player player, String kitName) {
        player.getInventory().clear();
        try {
            player.getInventory().setArmorContents(new ItemStack[4]);
        } catch (Throwable ignored) {
            // ignore
        }

        switch (kitName.toLowerCase(java.util.Locale.ROOT)) {
            case "uhc":
                uhc(player);
                break;
            case "crystal":
                crystal(player);
                break;
            case "axe":
                axe(player);
                break;
            case "smp":
                smp(player);
                break;
            case "custom":
                plugin.duelManager().applyCustomKit(player);
                break;
            case "sword":
            default:
                sword(player);
                break;
        }
        DuelUtil.updateInventory(player);
    }

    // --- Kits --------------------------------------------------------------

    private void sword(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 2));
        give(player, 1, item("GOLDEN_APPLE", 16, "ENCHANTED_GOLDEN_APPLE"));
        give(player, 2, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        armor(player, "DIAMOND", 2);
    }

    private void uhc(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 1));
        give(player, 1, power(item("BOW", 1), 1));
        give(player, 2, item("ARROW", 32, "ARROW"));
        give(player, 3, item("GOLDEN_APPLE", 8, "GOLDEN_APPLE"));
        give(player, 4, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        give(player, 5, item("COBBLESTONE", 64, "COBBLESTONE"));
        give(player, 6, item("WATER_BUCKET", 1, "WATER_BUCKET"));
        give(player, 7, item("OAK_PLANKS", 64, "PLANKS"));
        armor(player, "IRON", 1);
    }

    private void crystal(Player player) {
        give(player, 0, sharp(item("NETHERITE_SWORD", 1, "DIAMOND_SWORD"), 2));
        give(player, 1, item("END_CRYSTAL", 64, "END_CRYSTAL"));
        give(player, 2, item("OBSIDIAN", 64, "OBSIDIAN"));
        give(player, 3, item("RESPAWN_ANCHOR", 16, "OBSIDIAN"));
        give(player, 4, item("TOTEM_OF_UNDYING", 8, "TOTEM_OF_UNDYING"));
        give(player, 5, item("ENCHANTED_GOLDEN_APPLE", 16, "GOLDEN_APPLE"));
        armor(player, "NETHERITE", 2, "DIAMOND");
    }

    private void axe(Player player) {
        give(player, 0, sharp(item("NETHERITE_AXE", 1, "DIAMOND_AXE"), 2));
        give(player, 1, item("SHIELD", 1, "SHIELD"));
        give(player, 2, item("ENCHANTED_GOLDEN_APPLE", 16, "GOLDEN_APPLE"));
        give(player, 3, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        armor(player, "NETHERITE", 3, "DIAMOND");
    }

    private void smp(Player player) {
        give(player, 0, sharp(item("NETHERITE_SWORD", 1, "DIAMOND_SWORD"), 3));
        give(player, 1, item("SHIELD", 1, "SHIELD"));
        give(player, 2, item("ENDER_PEARL", 16, "ENDER_PEARL"));
        give(player, 3, item("TOTEM_OF_UNDYING", 4, "TOTEM_OF_UNDYING"));
        give(player, 4, item("ENCHANTED_GOLDEN_APPLE", 32, "GOLDEN_APPLE"));
        give(player, 5, item("COOKED_BEEF", 64, "COOKED_BEEF"));
        armor(player, "NETHERITE", 4, "DIAMOND");
    }

    // --- Helpers -----------------------------------------------------------

    private void give(Player player, int slot, ItemStack item) {
        if (item != null) {
            player.getInventory().setItem(slot, item);
        }
    }

    /** Resolves the first available material; never returns null (falls back to STONE). */
    private ItemStack item(String primary, int amount, String... fallbacks) {
        Material material = plugin.versionManager().adapter().resolveMaterial(primary);
        if (material == null) {
            for (String alt : fallbacks) {
                material = plugin.versionManager().adapter().resolveMaterial(alt);
                if (material != null) {
                    break;
                }
            }
        }
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemStack(material, Math.max(1, amount));
    }

    private ItemStack item(String primary, int amount) {
        return item(primary, amount, new String[0]);
    }

    private ItemStack sharp(ItemStack item, int level) {
        return enchant(item, level, "DAMAGE_ALL", "SHARPNESS");
    }

    private ItemStack power(ItemStack item, int level) {
        return enchant(item, level, "ARROW_DAMAGE", "POWER");
    }

    private ItemStack enchant(ItemStack item, int level, String... enchantNames) {
        Enchantment ench = resolveEnchant(enchantNames);
        if (ench != null) {
            try {
                item.addUnsafeEnchantment(ench, level);
            } catch (Throwable ignored) {
                // enchant failed; return the plain item
            }
        }
        return item;
    }

    private void armor(Player player, String tier, int protectionLevel, String... fallbackTiers) {
        String resolvedTier = tier;
        if (plugin.versionManager().adapter().resolveMaterial(tier + "_HELMET") == null) {
            for (String alt : fallbackTiers) {
                if (plugin.versionManager().adapter().resolveMaterial(alt + "_HELMET") != null) {
                    resolvedTier = alt;
                    break;
                }
            }
        }
        // setArmorContents order: [boots, leggings, chestplate, helmet]
        ItemStack[] set = new ItemStack[]{
                protect(item(resolvedTier + "_BOOTS", 1, "IRON_BOOTS"), protectionLevel),
                protect(item(resolvedTier + "_LEGGINGS", 1, "IRON_LEGGINGS"), protectionLevel),
                protect(item(resolvedTier + "_CHESTPLATE", 1, "IRON_CHESTPLATE"), protectionLevel),
                protect(item(resolvedTier + "_HELMET", 1, "IRON_HELMET"), protectionLevel)
        };
        try {
            player.getInventory().setArmorContents(set);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private ItemStack protect(ItemStack item, int level) {
        return enchant(item, level, "PROTECTION_ENVIRONMENTAL", "PROTECTION");
    }

    private Enchantment resolveEnchant(String... names) {
        for (String name : names) {
            try {
                Enchantment ench = Enchantment.getByName(name);
                if (ench != null) {
                    return ench;
                }
            } catch (Throwable ignored) {
                // getByName unsupported; try next
            }
        }
        return null;
    }
}
