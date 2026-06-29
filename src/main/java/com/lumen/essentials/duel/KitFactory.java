package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * The duel kit registry and applier. Predefined kits are built in code (version-safe
 * material/enchant resolution), plus {@code Wild} (keep your own inventory) and three
 * per-player {@code Custom} slots. Server owners can rename/extend the list here.
 */
public final class KitFactory {

    /** Kit names shown in the kit menu, in order. */
    public static final String[] KITS = {
            "Vanilla", "SMP", "Sword", "Axe", "Crystal", "UHC", "Gapple",
            "Diamond", "Netherite", "Wild", "Custom 1", "Custom 2", "Custom 3"
    };

    private final LumenEssentials plugin;

    public KitFactory(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /** Applies the named kit. {@code Wild} keeps the player's current inventory. */
    public void apply(Player player, String kitName) {
        String id = kitName.toLowerCase(Locale.ROOT).replace(" ", "");
        if (id.equals("wild")) {
            return; // bring your own gear
        }
        player.getInventory().clear();
        try {
            player.getInventory().setArmorContents(new ItemStack[4]);
        } catch (Throwable ignored) {
            // ignore
        }
        if (id.startsWith("custom")) {
            int slot = parseSlot(id);
            plugin.duelManager().applyCustomKit(player, slot);
            DuelUtil.updateInventory(player);
            return;
        }
        switch (id) {
            case "vanilla": vanilla(player); break;
            case "smp": smp(player); break;
            case "axe": axe(player); break;
            case "crystal": crystal(player); break;
            case "uhc": uhc(player); break;
            case "gapple": gapple(player); break;
            case "netherite": netherite(player); break;
            case "diamond": diamond(player); break;
            case "sword":
            default: sword(player); break;
        }
        DuelUtil.updateInventory(player);
    }

    /** A thematically-related menu icon material name for a kit. */
    public static String iconFor(String kit) {
        switch (kit.toLowerCase(Locale.ROOT).replace(" ", "")) {
            case "vanilla": return "IRON_SWORD";
            case "smp": return "NETHERITE_INGOT";
            case "sword": return "DIAMOND_SWORD";
            case "axe": return "DIAMOND_AXE";
            case "crystal": return "END_CRYSTAL";
            case "uhc": return "GOLDEN_APPLE";
            case "gapple": return "ENCHANTED_GOLDEN_APPLE";
            case "diamond": return "DIAMOND";
            case "netherite": return "NETHERITE_INGOT";
            case "wild": return "GRASS_BLOCK";
            case "custom1":
            case "custom2":
            case "custom3": return "CHEST";
            default: return "DIAMOND_SWORD";
        }
    }

    private int parseSlot(String id) {
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(id.substring("custom".length()))));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    // --- Kits --------------------------------------------------------------

    private void vanilla(Player player) {
        give(player, 0, item("DIAMOND_SWORD", 1));
        give(player, 1, item("BOW", 1));
        give(player, 2, item("GOLDEN_APPLE", 8, "GOLDEN_APPLE"));
        give(player, 3, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        give(player, 8, item("ARROW", 16, "ARROW"));
        armor(player, "DIAMOND", 0);
    }

    private void sword(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 2));
        give(player, 1, item("GOLDEN_APPLE", 16, "GOLDEN_APPLE"));
        give(player, 2, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        armor(player, "DIAMOND", 2);
    }

    private void diamond(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 3));
        give(player, 1, item("ENCHANTED_GOLDEN_APPLE", 16, "GOLDEN_APPLE"));
        give(player, 2, item("COOKED_BEEF", 64, "COOKED_BEEF"));
        armor(player, "DIAMOND", 3);
    }

    private void netherite(Player player) {
        give(player, 0, sharp(item("NETHERITE_SWORD", 1, "DIAMOND_SWORD"), 3));
        give(player, 1, item("SHIELD", 1, "SHIELD"));
        give(player, 2, item("ENCHANTED_GOLDEN_APPLE", 16, "GOLDEN_APPLE"));
        give(player, 3, item("TOTEM_OF_UNDYING", 2, "TOTEM_OF_UNDYING"));
        armor(player, "NETHERITE", 4, "DIAMOND");
    }

    private void uhc(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 1));
        give(player, 1, power(item("BOW", 1), 1));
        give(player, 2, item("ARROW", 32, "ARROW"));
        give(player, 3, item("GOLDEN_APPLE", 8, "GOLDEN_APPLE"));
        give(player, 4, item("COOKED_BEEF", 32, "COOKED_BEEF"));
        give(player, 5, item("COBBLESTONE", 64, "COBBLESTONE"));
        give(player, 6, item("WATER_BUCKET", 1, "WATER_BUCKET"));
        armor(player, "IRON", 1);
    }

    private void gapple(Player player) {
        give(player, 0, sharp(item("DIAMOND_SWORD", 1), 2));
        give(player, 1, item("ENCHANTED_GOLDEN_APPLE", 64, "GOLDEN_APPLE"));
        give(player, 2, item("GOLDEN_APPLE", 64, "GOLDEN_APPLE"));
        armor(player, "DIAMOND", 2);
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
        if (ench != null && level > 0) {
            try {
                item.addUnsafeEnchantment(ench, level);
            } catch (Throwable ignored) {
                // ignore
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
                // ignore
            }
        }
        return null;
    }
}
