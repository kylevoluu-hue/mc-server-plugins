package com.lumen.essentials.listener;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Silk-touch spawner harvesting. Breaking a mob spawner with a Silk Touch tool
 * drops a spawner item that remembers its mob type; placing that item restores the
 * correct mob. All behavior is configurable under {@code silk-spawners} in
 * {@code features.yml} and gated behind {@code lumen.silkspawner}.
 *
 * <p>Mob type is stored on the item via {@link BlockStateMeta} (the same mechanism
 * the vanilla client uses), so spawners stack and survive relog correctly. Servers
 * too old for {@link BlockStateMeta} simply drop a generic spawner.
 */
public final class SilkSpawnerListener implements Listener {

    private final LumenEssentials plugin;

    public SilkSpawnerListener(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.configManager().features().getBoolean("silk-spawners.enabled", true);
    }

    private Material spawnerMaterial() {
        Material m = plugin.versionManager().adapter().resolveMaterial("SPAWNER");
        if (m == null) {
            m = plugin.versionManager().adapter().resolveMaterial("MOB_SPAWNER");
        }
        return m;
    }

    private boolean worldAllowed(String world) {
        Set<String> worlds = new HashSet<>(
                plugin.configManager().features().getStringList("silk-spawners.worlds"));
        if (worlds.isEmpty()) {
            return true;
        }
        boolean whitelist = plugin.configManager().features()
                .getBoolean("silk-spawners.world-whitelist", false);
        return whitelist == worlds.contains(world);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!enabled()) {
            return;
        }
        Block block = event.getBlock();
        Material spawner = spawnerMaterial();
        if (spawner == null || block.getType() != spawner) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("lumen.silkspawner") || !worldAllowed(block.getWorld().getName())) {
            return;
        }

        boolean requireSilk = plugin.configManager().features()
                .getBoolean("silk-spawners.require-silk-touch", true);
        int minLevel = plugin.configManager().features().getInt("silk-spawners.min-silk-level", 1);
        if (requireSilk && silkLevel(player) < minLevel) {
            return; // no silk touch: leave vanilla behavior (spawner drops nothing)
        }

        // Read the mob type before the block is removed.
        EntityType mobType = null;
        BlockState state = block.getState();
        if (state instanceof CreatureSpawner) {
            try {
                mobType = ((CreatureSpawner) state).getSpawnedType();
            } catch (Throwable ignored) {
                // Some forks return null/throw for unset spawners.
            }
        }

        // Suppress vanilla drops/XP and drop our spawner item instead.
        suppressVanillaDrops(event);
        ItemStack item = createSpawnerItem(spawner, mobType);
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        try {
            block.getWorld().dropItemNaturally(loc, item);
        } catch (Throwable ignored) {
            // World#dropItemNaturally should always exist; ignore defensively.
        }
        MessageUtil.send(player, "&aYou harvested a "
                + (mobType == null ? "" : prettyName(mobType) + " ") + "spawner.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!enabled()) {
            return;
        }
        Block block = event.getBlockPlaced();
        Material spawner = spawnerMaterial();
        if (spawner == null || block.getType() != spawner) {
            return;
        }
        EntityType stored = readStoredType(event.getItemInHand());
        if (stored == null) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof CreatureSpawner) {
            try {
                CreatureSpawner cs = (CreatureSpawner) state;
                cs.setSpawnedType(stored);
                cs.update();
            } catch (Throwable ignored) {
                // Could not apply stored type; placed spawner keeps its default.
            }
        }
    }

    private int silkLevel(Player player) {
        try {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool == null) {
                return 0;
            }
            return tool.getEnchantmentLevel(Enchantment.SILK_TOUCH);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void suppressVanillaDrops(BlockBreakEvent event) {
        boolean dropXp = plugin.configManager().features().getBoolean("silk-spawners.drop-xp", false);
        try {
            event.setDropItems(false);
        } catch (Throwable ignored) {
            // setDropItems is 1.12+; older servers already drop nothing for spawners.
        }
        if (!dropXp) {
            try {
                event.setExpToDrop(0);
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    private ItemStack createSpawnerItem(Material spawnerMaterial, EntityType mobType) {
        ItemStack item = new ItemStack(spawnerMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        // Store the mob type in the item's block state so it survives stacking/relog.
        if (meta instanceof BlockStateMeta && mobType != null) {
            try {
                BlockStateMeta blockMeta = (BlockStateMeta) meta;
                BlockState bs = blockMeta.getBlockState();
                if (bs instanceof CreatureSpawner) {
                    ((CreatureSpawner) bs).setSpawnedType(mobType);
                    blockMeta.setBlockState(bs);
                }
            } catch (Throwable ignored) {
                // Fall back to a generic (typeless) spawner item.
            }
        }
        if (mobType != null) {
            meta.setDisplayName(MessageUtil.color("&f" + prettyName(mobType) + " Spawner"));
        }
        item.setItemMeta(meta);
        return item;
    }

    private EntityType readStoredType(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BlockStateMeta) {
                BlockState bs = ((BlockStateMeta) meta).getBlockState();
                if (bs instanceof CreatureSpawner) {
                    return ((CreatureSpawner) bs).getSpawnedType();
                }
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return null;
    }

    private String prettyName(EntityType type) {
        String name = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder(name.length());
        boolean capitalize = true;
        for (char c : name.toCharArray()) {
            if (capitalize && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(c);
                if (c == ' ') {
                    capitalize = true;
                }
            }
        }
        return sb.toString();
    }
}
