package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Locale;

/**
 * The duel kit registry and applier (PvP-Club-inspired defaults). Every kit clears the
 * inventory and applies armor, then hotbar, then bulk inventory, then offhand. Items are
 * made unbreakable and enchanted version-safely (enchants/materials that don't exist on
 * the running version are skipped rather than failing the kit). {@code Wild} keeps the
 * player's own gear; {@code Custom 1/2/3} load per-player saved inventories.
 */
public final class KitFactory {

    /** Kit names shown in the kit menu, in order. */
    public static final String[] KITS = {
            "Sword", "Axe", "UHC", "Pot", "NethPot", "NethOP", "SMP", "Crystal",
            "Mace", "SpearMace", "Creeper", "Vanilla", "Wild", "Custom 1", "Custom 2", "Custom 3"
    };

    private final LumenEssentials plugin;

    public KitFactory(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, String kitName) {
        String id = kitName.toLowerCase(Locale.ROOT).replace(" ", "");
        if (id.equals("wild")) {
            return; // bring your own gear
        }
        player.getInventory().clear();
        setArmor(player, null, null, null, null);
        if (id.startsWith("custom")) {
            plugin.duelManager().applyCustomKit(player, parseSlot(id));
            DuelUtil.updateInventory(player);
            return;
        }
        switch (id) {
            case "axe": axe(player); break;
            case "uhc": uhc(player); break;
            case "pot": pot(player); break;
            case "nethpot": nethPot(player); break;
            case "nethop": nethOp(player); break;
            case "smp": smp(player); break;
            case "crystal": crystal(player); break;
            case "mace": mace(player); break;
            case "spearmace": spearMace(player); break;
            case "creeper": creeper(player); break;
            case "vanilla": vanilla(player); break;
            case "sword":
            default: sword(player); break;
        }
        DuelUtil.updateInventory(player);
    }

    public static String iconFor(String kit) {
        switch (kit.toLowerCase(Locale.ROOT).replace(" ", "")) {
            case "sword": return "DIAMOND_SWORD";
            case "axe": return "DIAMOND_AXE";
            case "uhc": return "GOLDEN_APPLE";
            case "pot": return "SPLASH_POTION";
            case "nethpot": return "NETHERITE_INGOT";
            case "nethop": return "NETHERITE_BLOCK";
            case "smp": return "NETHERITE_SWORD";
            case "crystal": return "END_CRYSTAL";
            case "mace": return "MACE";
            case "spearmace": return "TRIDENT";
            case "creeper": return "CREEPER_HEAD";
            case "vanilla": return "IRON_SWORD";
            case "wild": return "GRASS_BLOCK";
            default: return "CHEST"; // custom slots
        }
    }

    private int parseSlot(String id) {
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(id.substring("custom".length()))));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    // ======================================================================
    //  Kits
    // ======================================================================

    private void sword(Player p) {
        diamondArmor(p, 4, true);
        give(p, 0, e(e(it("DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 1, e(it("SHIELD"), 3, "unbreaking"));
        give(p, 2, it("GOLDEN_APPLE", 16));
        give(p, 3, it("COOKED_BEEF", 64));
        give(p, 4, it("WATER_BUCKET", 1));
        give(p, 5, it("ENDER_PEARL", 8));
        give(p, 6, it("COBWEB", 16));
        give(p, 7, it("COBBLESTONE", 64));
        give(p, 8, e(e(it("BOW"), 2, "power"), 3, "unbreaking"));
        bulk(p, it("ARROW", 64));
        bulk(p, it("GOLDEN_APPLE", 16));
        offhand(p, e(it("SHIELD"), 3, "unbreaking"));
    }

    private void axe(Player p) {
        diamondArmor(p, 4, true);
        give(p, 0, e(e(e(it("DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"));
        give(p, 1, e(e(it("DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 2, e(it("SHIELD"), 3, "unbreaking"));
        give(p, 3, e(e(e(it("CROSSBOW"), 3, "quick"), 4, "pierce"), 3, "unbreaking"));
        give(p, 4, it("GOLDEN_APPLE", 16));
        give(p, 5, it("WATER_BUCKET", 1));
        give(p, 6, it("LAVA_BUCKET", 1));
        give(p, 7, it("COBWEB", 16));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("ARROW", 64));
        bulk(p, it("COBBLESTONE", 64));
        bulk(p, it("ENDER_PEARL", 8));
        offhand(p, e(it("SHIELD"), 3, "unbreaking"));
    }

    private void uhc(Player p) {
        diamondArmor(p, 3, true);
        give(p, 0, e(e(it("DIAMOND_SWORD"), 4, "sharp"), 3, "unbreaking"));
        give(p, 1, e(it("FISHING_ROD"), 3, "unbreaking"));
        give(p, 2, e(e(it("BOW"), 3, "power"), 3, "unbreaking"));
        give(p, 3, e(e(it("DIAMOND_AXE"), 3, "sharp"), 3, "unbreaking"));
        give(p, 4, it("GOLDEN_APPLE", 24));
        give(p, 5, it("GOLDEN_APPLE", 6));
        give(p, 6, it("WATER_BUCKET", 1));
        give(p, 7, it("LAVA_BUCKET", 1));
        give(p, 8, it("COBBLESTONE", 64));
        bulk(p, it("ARROW", 64));
        bulk(p, it("COBWEB", 32));
        bulk(p, it("OAK_PLANKS", 64, "OAK_WOOD"));
        bulk(p, it("COOKED_BEEF", 64));
        bulk(p, it("FLINT_AND_STEEL", 1));
        offhand(p, e(it("SHIELD"), 3, "unbreaking"));
    }

    private void pot(Player p) {
        diamondArmor(p, 4, true);
        give(p, 0, e(e(it("DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        for (int slot = 1; slot <= 6; slot++) {
            give(p, slot, potion(true, 1, true, "STRONG_HEALING", "INSTANT_HEAL", "HEALING"));
        }
        give(p, 7, potion(false, 1, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, potion(true, 24, true, "STRONG_HEALING", "INSTANT_HEAL", "HEALING"));
        bulk(p, potion(false, 3, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        bulk(p, potion(false, 1, false, "FIRE_RESISTANCE"));
        bulk(p, it("ENDER_PEARL", 4));
    }

    private void nethPot(Player p) {
        netheriteArmor(p, 4, true, false);
        give(p, 0, e(e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"), 1, "mending"));
        give(p, 1, e(e(e(e(it("NETHERITE_AXE", "DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"), 1, "mending"));
        give(p, 2, it("GOLDEN_APPLE", 64));
        give(p, 3, it("EXPERIENCE_BOTTLE", 64));
        give(p, 4, it("ENDER_PEARL", 16));
        give(p, 5, it("TOTEM_OF_UNDYING", 1));
        give(p, 6, potion(false, 1, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        give(p, 7, potion(false, 1, true, "STRONG_STRENGTH", "STRENGTH"));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("TOTEM_OF_UNDYING", 2));
        bulk(p, it("GOLDEN_APPLE", 64));
        bulk(p, it("EXPERIENCE_BOTTLE", 192));
        bulk(p, potion(false, 3, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        bulk(p, potion(false, 3, true, "STRONG_STRENGTH", "STRENGTH"));
        bulk(p, potion(false, 2, false, "FIRE_RESISTANCE"));
        bulk(p, it("COBWEB", 32));
        bulk(p, it("WATER_BUCKET", 1));
        bulk(p, it("OAK_LOG", 64, "OAK_WOOD"));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void nethOp(Player p) {
        netheriteArmor(p, 4, true, true);
        give(p, 0, e(e(e(e(e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 2, "fire"), 3, "sweep"), 1, "knock"), 3, "unbreaking"), 1, "mending"));
        give(p, 1, e(e(e(e(it("NETHERITE_AXE", "DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"), 1, "mending"));
        give(p, 2, e(e(e(it("CROSSBOW"), 3, "quick"), 4, "pierce"), 3, "unbreaking"));
        give(p, 3, it("GOLDEN_APPLE", 64));
        give(p, 4, it("TOTEM_OF_UNDYING", 1));
        give(p, 5, it("ENDER_PEARL", 16));
        give(p, 6, it("EXPERIENCE_BOTTLE", 64));
        give(p, 7, it("COBWEB", 64));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("TOTEM_OF_UNDYING", 3));
        bulk(p, it("GOLDEN_APPLE", 128));
        bulk(p, it("EXPERIENCE_BOTTLE", 192));
        bulk(p, it("WATER_BUCKET", 2));
        bulk(p, it("LAVA_BUCKET", 1));
        bulk(p, it("OBSIDIAN", 64));
        bulk(p, it("END_CRYSTAL", 32));
        bulk(p, it("OAK_LOG", 64, "OAK_WOOD"));
        bulk(p, potion(false, 3, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        bulk(p, potion(false, 3, true, "STRONG_STRENGTH", "STRENGTH"));
        bulk(p, potion(false, 2, false, "FIRE_RESISTANCE"));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void smp(Player p) {
        netheriteArmor(p, 4, true, true);
        give(p, 0, e(e(e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 3, "sweep"), 1, "knock"), 3, "unbreaking"));
        give(p, 1, e(e(e(it("NETHERITE_AXE", "DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"));
        give(p, 2, named(e(e(it("TRIDENT", "NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 1, "knock"), "&bSpear"));
        give(p, 3, it("GOLDEN_APPLE", 64));
        give(p, 4, it("TOTEM_OF_UNDYING", 1));
        give(p, 5, it("ENDER_PEARL", 16));
        give(p, 6, it("COBWEB", 32));
        give(p, 7, it("WIND_CHARGE", 64, "SNOWBALL"));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("EXPERIENCE_BOTTLE", 64));
        bulk(p, it("WATER_BUCKET", 5));
        bulk(p, it("OAK_LOG", 64, "OAK_WOOD"));
        bulk(p, it("ENDER_CHEST", 3));
        bulk(p, it("BLUE_ICE", 64, "PACKED_ICE"));
        bulk(p, potion(false, 10, true, "STRONG_STRENGTH", "STRENGTH"));
        bulk(p, potion(false, 3, false, "SWIFTNESS", "SPEED"));
        bulk(p, e(e(e(it("NETHERITE_PICKAXE", "DIAMOND_PICKAXE"), 5, "eff"), 3, "unbreaking"), 1, "mending"));
        bulk(p, it("GOLDEN_APPLE", 64));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void crystal(Player p) {
        netheriteArmor(p, 4, true, false);
        give(p, 0, e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 1, e(e(it("NETHERITE_PICKAXE", "DIAMOND_PICKAXE"), 5, "eff"), 3, "unbreaking"));
        give(p, 2, it("OBSIDIAN", 64));
        give(p, 3, it("END_CRYSTAL", 64));
        give(p, 4, it("TOTEM_OF_UNDYING", 1));
        give(p, 5, it("ENDER_PEARL", 16));
        give(p, 6, it("EXPERIENCE_BOTTLE", 64));
        give(p, 7, it("GOLDEN_APPLE", 64));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("TOTEM_OF_UNDYING", 16));
        bulk(p, it("OBSIDIAN", 128));
        bulk(p, it("END_CRYSTAL", 128));
        bulk(p, it("EXPERIENCE_BOTTLE", 192));
        bulk(p, it("GOLDEN_APPLE", 128));
        bulk(p, it("RESPAWN_ANCHOR", 64, "OBSIDIAN"));
        bulk(p, it("GLOWSTONE", 64));
        bulk(p, e(e(e(it("NETHERITE_AXE", "DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void mace(Player p) {
        netheriteArmor(p, 4, true, false);
        give(p, 0, e(e(e(it("MACE", "NETHERITE_AXE", "DIAMOND_AXE"), 5, "density"), 1, "windburst"), 3, "unbreaking"));
        give(p, 1, e(e(it("MACE", "NETHERITE_AXE", "DIAMOND_AXE"), 4, "breach"), 3, "unbreaking"));
        give(p, 2, e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 3, e(e(it("NETHERITE_AXE", "DIAMOND_AXE"), 5, "sharp"), 3, "unbreaking"));
        give(p, 4, it("WIND_CHARGE", 64, "SNOWBALL"));
        give(p, 5, it("ENDER_PEARL", 16));
        give(p, 6, it("GOLDEN_APPLE", 64));
        give(p, 7, it("TOTEM_OF_UNDYING", 1));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("WIND_CHARGE", 128, "SNOWBALL"));
        bulk(p, it("ENDER_PEARL", 64));
        bulk(p, it("GOLDEN_APPLE", 64));
        bulk(p, it("TOTEM_OF_UNDYING", 2));
        bulk(p, e(it("SHIELD"), 3, "unbreaking"));
        bulk(p, e(e(it("ELYTRA"), 3, "unbreaking"), 1, "mending"));
        bulk(p, it("FIREWORK_ROCKET", 3));
        bulk(p, potion(false, 6, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        bulk(p, potion(false, 6, true, "STRONG_STRENGTH", "STRENGTH"));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void spearMace(Player p) {
        netheriteArmor(p, 4, true, false);
        give(p, 0, named(e(e(it("TRIDENT", "NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 1, "knock"), "&bSpear"));
        give(p, 1, e(e(e(it("MACE", "NETHERITE_AXE", "DIAMOND_AXE"), 5, "density"), 1, "windburst"), 3, "unbreaking"));
        give(p, 2, e(e(it("MACE", "NETHERITE_AXE", "DIAMOND_AXE"), 4, "breach"), 3, "unbreaking"));
        give(p, 3, e(e(it("NETHERITE_SWORD", "DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 4, it("WIND_CHARGE", 64, "SNOWBALL"));
        give(p, 5, it("ENDER_PEARL", 16));
        give(p, 6, it("GOLDEN_APPLE", 64));
        give(p, 7, it("TOTEM_OF_UNDYING", 1));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("WIND_CHARGE", 128, "SNOWBALL"));
        bulk(p, it("ENDER_PEARL", 64));
        bulk(p, it("GOLDEN_APPLE", 64));
        bulk(p, it("TOTEM_OF_UNDYING", 2));
        bulk(p, potion(false, 6, true, "STRONG_SWIFTNESS", "SPEED", "SWIFTNESS"));
        bulk(p, potion(false, 6, true, "STRONG_STRENGTH", "STRENGTH"));
        bulk(p, it("WATER_BUCKET", 1));
        bulk(p, it("COBWEB", 32));
        offhand(p, it("TOTEM_OF_UNDYING", 1));
    }

    private void creeper(Player p) {
        diamondArmor(p, 4, true, true); // blast protection
        give(p, 0, e(e(it("DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 1, e(e(it("DIAMOND_AXE"), 5, "sharp"), 3, "unbreaking"));
        give(p, 2, it("CREEPER_SPAWN_EGG", 16, "TNT"));
        give(p, 3, it("FLINT_AND_STEEL", 1));
        give(p, 4, e(it("SHIELD"), 3, "unbreaking"));
        give(p, 5, it("GOLDEN_APPLE", 32));
        give(p, 6, it("ENDER_PEARL", 8));
        give(p, 7, it("COBWEB", 32));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("CREEPER_SPAWN_EGG", 16, "TNT"));
        bulk(p, it("ARROW", 64));
        bulk(p, e(e(e(it("BOW"), 3, "power"), 1, "punch"), 3, "unbreaking"));
        bulk(p, it("WATER_BUCKET", 1));
        bulk(p, it("COBBLESTONE", 64));
        offhand(p, e(it("SHIELD"), 3, "unbreaking"));
    }

    private void vanilla(Player p) {
        diamondArmor(p, 4, true);
        give(p, 0, e(e(it("DIAMOND_SWORD"), 5, "sharp"), 3, "unbreaking"));
        give(p, 1, e(e(e(it("DIAMOND_AXE"), 5, "sharp"), 5, "eff"), 3, "unbreaking"));
        give(p, 2, e(it("SHIELD"), 3, "unbreaking"));
        give(p, 3, e(e(it("BOW"), 3, "power"), 3, "unbreaking"));
        give(p, 4, e(e(it("CROSSBOW"), 3, "quick"), 3, "unbreaking"));
        give(p, 5, it("GOLDEN_APPLE", 32));
        give(p, 6, it("ENDER_PEARL", 8));
        give(p, 7, it("WATER_BUCKET", 1));
        give(p, 8, it("COOKED_BEEF", 64));
        bulk(p, it("ARROW", 64));
        bulk(p, it("COBWEB", 32));
        bulk(p, it("COBBLESTONE", 64));
        bulk(p, it("OAK_LOG", 64, "OAK_WOOD"));
        bulk(p, it("LAVA_BUCKET", 1));
        bulk(p, it("EXPERIENCE_BOTTLE", 64));
        offhand(p, e(it("SHIELD"), 3, "unbreaking"));
    }

    // ======================================================================
    //  Armor helpers
    // ======================================================================

    private void diamondArmor(Player p, int protLevel, boolean featherFall) {
        diamondArmor(p, protLevel, featherFall, false);
    }

    private void diamondArmor(Player p, int protLevel, boolean featherFall, boolean blast) {
        ItemStack helmet = armorPiece("DIAMOND_HELMET", protLevel, false, false, false, blast);
        ItemStack chest = armorPiece("DIAMOND_CHESTPLATE", protLevel, false, false, false, blast);
        ItemStack legs = armorPiece("DIAMOND_LEGGINGS", protLevel, false, false, false, blast);
        ItemStack boots = armorPiece("DIAMOND_BOOTS", protLevel, featherFall, false, false, blast);
        setArmor(p, helmet, chest, legs, boots);
    }

    private void netheriteArmor(Player p, int protLevel, boolean mending, boolean depthStrider) {
        ItemStack helmet = armorPiece("NETHERITE_HELMET", protLevel, false, mending, false, false, "DIAMOND_HELMET");
        ItemStack chest = armorPiece("NETHERITE_CHESTPLATE", protLevel, false, mending, false, false, "DIAMOND_CHESTPLATE");
        ItemStack legs = armorPiece("NETHERITE_LEGGINGS", protLevel, false, mending, false, false, "DIAMOND_LEGGINGS");
        ItemStack boots = armorPiece("NETHERITE_BOOTS", protLevel, true, mending, depthStrider, false, "DIAMOND_BOOTS");
        setArmor(p, helmet, chest, legs, boots);
    }

    private ItemStack armorPiece(String material, int protLevel, boolean featherFall, boolean mending,
                                 boolean depthStrider, boolean blast, String... fallbacks) {
        ItemStack item = it(material, 1, fallbacks);
        if (item == null) {
            return null;
        }
        e(item, protLevel, "prot");
        e(item, 3, "unbreaking");
        if (blast) {
            e(item, 4, "blast");
        }
        if (mending) {
            e(item, 1, "mending");
        }
        if (featherFall) {
            e(item, 4, "ff");
        }
        if (depthStrider) {
            e(item, 3, "depth");
        }
        return item;
    }

    private void setArmor(Player player, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        // setArmorContents order: [boots, leggings, chestplate, helmet]
        try {
            player.getInventory().setArmorContents(new ItemStack[]{boots, legs, chest, helmet});
        } catch (Throwable ignored) {
            // ignore
        }
    }

    // ======================================================================
    //  Item helpers
    // ======================================================================

    private void give(Player player, int slot, ItemStack item) {
        if (item != null) {
            player.getInventory().setItem(slot, item);
        }
    }

    /** Adds bulk items to the inventory; Bukkit splits oversized amounts into stacks. */
    private void bulk(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        try {
            player.getInventory().addItem(item);
        } catch (Throwable ignored) {
            // ignore (inventory full)
        }
    }

    private void offhand(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        try {
            player.getInventory().setItemInOffHand(item);
        } catch (Throwable ignored) {
            // offhand unsupported on this version; ignore
        }
    }

    private Material mat(String primary, String... fallbacks) {
        Material material = plugin.versionManager().adapter().resolveMaterial(primary);
        if (material == null) {
            for (String alt : fallbacks) {
                material = plugin.versionManager().adapter().resolveMaterial(alt);
                if (material != null) {
                    break;
                }
            }
        }
        return material;
    }

    /** Builds an unbreakable item, or {@code null} if no material resolves. */
    private ItemStack it(String primary, int amount, String... fallbacks) {
        Material material = mat(primary, fallbacks);
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        unbreakable(item);
        return item;
    }

    private ItemStack it(String primary) {
        return it(primary, 1);
    }

    /** Single-item with material fallbacks (amount = 1). */
    private ItemStack it(String primary, String... fallbacks) {
        return it(primary, 1, fallbacks);
    }

    private void unbreakable(ItemStack item) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // setUnbreakable unsupported on this version; ignore
        }
    }

    private ItemStack named(ItemStack item, String name) {
        if (item == null) {
            return null;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.color(name));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return item;
    }

    /** Applies an enchantment (resolved by friendly key) if present on this version. */
    private ItemStack e(ItemStack item, int level, String key) {
        if (item == null || level <= 0) {
            return item;
        }
        Enchantment ench = resolveEnchant(candidates(key));
        if (ench != null) {
            try {
                item.addUnsafeEnchantment(ench, level);
            } catch (Throwable ignored) {
                // ignore
            }
        }
        return item;
    }

    private String[] candidates(String key) {
        switch (key) {
            case "prot": return new String[]{"PROTECTION_ENVIRONMENTAL", "PROTECTION"};
            case "blast": return new String[]{"PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"};
            case "ff": return new String[]{"PROTECTION_FALL", "FEATHER_FALLING"};
            case "unbreaking": return new String[]{"DURABILITY", "UNBREAKING"};
            case "sharp": return new String[]{"DAMAGE_ALL", "SHARPNESS"};
            case "eff": return new String[]{"DIG_SPEED", "EFFICIENCY"};
            case "power": return new String[]{"ARROW_DAMAGE", "POWER"};
            case "punch": return new String[]{"ARROW_KNOCKBACK", "PUNCH"};
            case "mending": return new String[]{"MENDING"};
            case "depth": return new String[]{"DEPTH_STRIDER"};
            case "knock": return new String[]{"KNOCKBACK"};
            case "fire": return new String[]{"FIRE_ASPECT"};
            case "sweep": return new String[]{"SWEEPING_EDGE", "SWEEPING"};
            case "quick": return new String[]{"QUICK_CHARGE"};
            case "pierce": return new String[]{"PIERCING"};
            case "density": return new String[]{"DENSITY"};
            case "windburst": return new String[]{"WIND_BURST"};
            case "breach": return new String[]{"BREACH"};
            default: return new String[0];
        }
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

    /** Builds a (splash) potion of the first resolvable {@link PotionType}, or a plain potion. */
    private ItemStack potion(boolean splash, int amount, boolean upgraded, String... typeNames) {
        Material material = mat(splash ? "SPLASH_POTION" : "POTION", "POTION");
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        try {
            ItemMeta raw = item.getItemMeta();
            if (raw instanceof PotionMeta) {
                PotionType type = resolvePotionType(typeNames);
                if (type != null) {
                    boolean strong = type.name().startsWith("STRONG");
                    ((PotionMeta) raw).setBasePotionData(new PotionData(type, false, !strong && upgraded));
                    item.setItemMeta(raw);
                }
            }
        } catch (Throwable ignored) {
            // potion meta unsupported; return a plain potion
        }
        return item;
    }

    private PotionType resolvePotionType(String... names) {
        for (String name : names) {
            try {
                return PotionType.valueOf(name);
            } catch (Throwable ignored) {
                // try next
            }
        }
        return null;
    }
}
