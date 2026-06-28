package com.lumen.essentials.investigation;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Staff-only investigation tooling: creates hidden, randomized test stashes and ore
 * veins, monitors them for suspiciously direct discovery, tracks interactions, and
 * cleans them up exactly (restoring overwritten blocks) on a timer or on shutdown.
 *
 * <p>These tools are gated behind {@code luac.investigate}/{@code luac.spawnstash}/
 * {@code luac.oresummon}, every action is logged, and they intentionally place objects
 * underground/away from players so they never grant normal players an advantage.
 */
public final class InvestigationManager {

    private final LumenEssentials plugin;
    private final List<TestObject> objects = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private int monitorTaskId = -1;

    public InvestigationManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Monitor discovery/cleanup once per second; cheap with a handful of objects.
        this.monitorTaskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    public void shutdown() {
        if (monitorTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(monitorTaskId);
            monitorTaskId = -1;
        }
        // Restore every outstanding object so nothing is left in the world.
        for (TestObject object : objects) {
            cleanup(object, "shutdown");
        }
        objects.clear();
    }

    public int activeCount() {
        return objects.size();
    }

    private boolean toolsEnabled() {
        return plugin.configManager().investigation()
                .getBoolean("investigation-tools.enabled", true);
    }

    private long cleanupAtMillis() {
        var inv = plugin.configManager().investigation();
        if (!inv.getBoolean("investigation-tools.temporary-test-objects", true)) {
            return 0L;
        }
        int minutes = inv.getInt("investigation-tools.cleanup-after-minutes", 60);
        return System.currentTimeMillis() + minutes * 60_000L;
    }

    // --- Stash --------------------------------------------------------------

    /**
     * Creates a hidden randomized stash near a location. Returns the placed object,
     * or {@code null} if disabled/invalid.
     */
    public TestObject spawnStash(Player staff, Location near) {
        if (!toolsEnabled() || near == null || near.getWorld() == null) {
            return null;
        }
        var inv = plugin.configManager().investigation();
        if (!inv.getBoolean("investigation-tools.spawnstash.enabled", true)) {
            return null;
        }
        int minDist = inv.getInt("investigation-tools.spawnstash.min-distance-from-player", 32);
        int maxDist = inv.getInt("investigation-tools.spawnstash.max-distance-from-player", 128);

        Location target = randomizedNearby(near, minDist, maxDist);
        if (target == null) {
            return null;
        }

        TestObject object = new TestObject(TestObject.Type.STASH, target,
                staff.getName(), cleanupAtMillis());

        Material containerType = pick(Material.CHEST, Material.BARREL, resolveShulker());
        Block block = target.getBlock();
        object.rememberBlock(block.getLocation(), block.getType());
        block.setType(containerType, false);

        fillContainer(block, inv.getString("investigation-tools.spawnstash.max-loot-value", "medium"));
        objects.add(object);

        log(staff, "spawnstash", object, "container=" + containerType);
        return object;
    }

    private Material resolveShulker() {
        Material shulker = plugin.versionManager().adapter().resolveMaterial("SHULKER_BOX");
        return shulker != null ? shulker : Material.CHEST;
    }

    private void fillContainer(Block block, String lootValue) {
        // InventoryHolder (rather than the 1.13+ Container interface) keeps this
        // compatible all the way back to legacy chests.
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder)) {
            return;
        }
        Inventory inventory = ((InventoryHolder) state).getInventory();
        int rolls = "high".equalsIgnoreCase(lootValue) ? 8
                : "low".equalsIgnoreCase(lootValue) ? 3 : 5;
        Material[] loot = lootTable(lootValue);
        for (int i = 0; i < rolls; i++) {
            Material material = loot[random.nextInt(loot.length)];
            int slot = random.nextInt(inventory.getSize());
            inventory.setItem(slot, new ItemStack(material, 1 + random.nextInt(16)));
        }
    }

    private Material[] lootTable(String value) {
        List<Material> list = new ArrayList<>();
        addIfPresent(list, "IRON_INGOT", "GOLD_INGOT", "EXPERIENCE_BOTTLE", "ARROW", "BREAD");
        if (!"low".equalsIgnoreCase(value)) {
            addIfPresent(list, "DIAMOND", "EMERALD", "ENDER_PEARL");
        }
        if ("high".equalsIgnoreCase(value)) {
            addIfPresent(list, "NETHERITE_SCRAP", "GOLDEN_APPLE", "DIAMOND_BLOCK");
        }
        if (list.isEmpty()) {
            list.add(Material.STONE);
        }
        return list.toArray(new Material[0]);
    }

    private void addIfPresent(List<Material> list, String... names) {
        for (String name : names) {
            Material material = plugin.versionManager().adapter().resolveMaterial(name);
            if (material != null) {
                list.add(material);
            }
        }
    }

    // --- Ore vein -----------------------------------------------------------

    public TestObject oreSummon(Player staff, Location near) {
        if (!toolsEnabled() || near == null || near.getWorld() == null) {
            return null;
        }
        var inv = plugin.configManager().investigation();
        if (!inv.getBoolean("investigation-tools.oresummon.enabled", true)) {
            return null;
        }
        int minOres = inv.getInt("investigation-tools.oresummon.min-ores", 3);
        int maxOres = inv.getInt("investigation-tools.oresummon.max-ores", 12);
        int count = minOres + random.nextInt(Math.max(1, maxOres - minOres + 1));

        Location target = randomizedNearby(near, 24, 96);
        if (target == null) {
            return null;
        }

        Material ore = chooseOre(target.getWorld(),
                inv.getStringList("investigation-tools.oresummon.allowed-blocks"));
        if (ore == null) {
            return null;
        }

        TestObject object = new TestObject(TestObject.Type.ORE, target, staff.getName(), cleanupAtMillis());
        Block base = target.getBlock();
        // Build a small randomized vein around the base block.
        for (int i = 0; i < count; i++) {
            Block b = base.getRelative(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (b.getType().isSolid() || b.getType() == Material.STONE
                    || b.getType().name().contains("DEEPSLATE") || b.getType().name().contains("NETHERRACK")) {
                object.rememberBlock(b.getLocation(), b.getType());
                b.setType(ore, false);
            }
        }
        objects.add(object);
        log(staff, "oresummon", object, "ore=" + ore + " count=" + count);
        return object;
    }

    private Material chooseOre(World world, List<String> allowed) {
        List<Material> options = new ArrayList<>();
        for (String name : allowed) {
            Material material = plugin.versionManager().adapter().resolveMaterial(name);
            if (material == null) {
                continue;
            }
            boolean nether = world.getEnvironment() == World.Environment.NETHER;
            boolean isDebris = material.name().contains("ANCIENT_DEBRIS");
            // Only place ancient debris in the nether; only place ores in overworld/end.
            if (isDebris == nether) {
                options.add(material);
            }
        }
        if (options.isEmpty()) {
            Material fallback = plugin.versionManager().adapter().resolveMaterial("DIAMOND_ORE");
            return world.getEnvironment() == World.Environment.NETHER
                    ? plugin.versionManager().adapter().resolveMaterial("ANCIENT_DEBRIS")
                    : fallback;
        }
        return options.get(random.nextInt(options.size()));
    }

    // --- Monitoring & cleanup ----------------------------------------------

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<TestObject> it = objects.iterator();
        double radius = plugin.configManager().investigation()
                .getDouble("investigation-tools.discovery-radius", 4.0D);
        double radiusSq = radius * radius;

        while (it.hasNext()) {
            TestObject object = it.next();
            if (object.isExpired(now)) {
                cleanup(object, "timer");
                objects.remove(object);
                continue;
            }
            Location origin = object.origin();
            if (origin.getWorld() == null) {
                continue;
            }
            for (Player player : origin.getWorld().getPlayers()) {
                if (player.hasPermission("luac.investigate")) {
                    continue; // ignore staff
                }
                if (player.getLocation().distanceSquared(origin) <= radiusSq && !object.isDiscovered()) {
                    object.markDiscovered();
                    long seconds = (now - object.createdAt()) / 1000L;
                    plugin.alertManager().notifyStaff(String.format(
                            "&6Investigation&7: &f%s &7reached a test %s &7%ds after creation "
                                    + "(by %s) at %s %d,%d,%d",
                            player.getName(), object.type().name().toLowerCase(), seconds,
                            object.creator(), origin.getWorld().getName(),
                            origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()));
                    plugin.storageManager().logInvestigation(String.format(
                            "DISCOVERY %s reached %s (creator=%s) after %ds at %s %d,%d,%d",
                            player.getName(), object.type(), object.creator(), seconds,
                            origin.getWorld().getName(), origin.getBlockX(), origin.getBlockY(),
                            origin.getBlockZ()));
                }
            }
        }
    }

    /** Called by the world listener: counts breaks near active test objects. */
    public void handleBreakNear(Player player, Block block) {
        if (objects.isEmpty() || player.hasPermission("luac.investigate")) {
            return;
        }
        for (TestObject object : objects) {
            if (block.getWorld() != object.origin().getWorld()) {
                continue;
            }
            if (block.getLocation().distanceSquared(object.origin()) <= 36.0D) {
                object.incrementBlocksBroken();
            }
        }
    }

    private void cleanup(TestObject object, String reason) {
        for (var entry : object.originalBlocks().entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
                loc.getBlock().setType(entry.getValue(), false);
            } else if (loc.getWorld() != null) {
                // Load briefly to restore, then let the server unload normally.
                loc.getChunk().load();
                loc.getBlock().setType(entry.getValue(), false);
            }
        }
        plugin.storageManager().logInvestigation("CLEANUP " + object.type() + " id=" + object.id()
                + " reason=" + reason + " brokenNearby=" + object.blocksBrokenNearby());
    }

    // --- Helpers ------------------------------------------------------------

    private Location randomizedNearby(Location near, int minDist, int maxDist) {
        World world = near.getWorld();
        if (world == null) {
            return null;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = minDist + random.nextDouble() * Math.max(1, maxDist - minDist);
        int x = near.getBlockX() + (int) (Math.cos(angle) * dist);
        int z = near.getBlockZ() + (int) (Math.sin(angle) * dist);
        // Place a bit underground so the object is genuinely "hidden".
        int baseY = Math.max(world.getMinHeight() + 5, near.getBlockY() - (8 + random.nextInt(20)));
        return new Location(world, x + 0.5, baseY, z + 0.5);
    }

    private Material pick(Material... options) {
        return options[random.nextInt(options.length)];
    }

    private void log(Player staff, String tool, TestObject object, String extra) {
        Location o = object.origin();
        plugin.storageManager().logInvestigation(String.format(
                "CREATE %s by %s at %s %d,%d,%d cleanup=%s %s",
                tool, staff.getName(), o.getWorld() == null ? "?" : o.getWorld().getName(),
                o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                object.cleanupAt() == 0 ? "never" : "in-" + plugin.configManager().investigation()
                        .getInt("investigation-tools.cleanup-after-minutes", 60) + "m",
                extra));
        if (plugin.configManager().investigation()
                .getBoolean("investigation-tools.log-all-actions", true)) {
            plugin.alertManager().notifyStaff("&7Investigation tool used: &f" + tool
                    + " &7by &f" + staff.getName());
        }
    }

    public List<TestObject> active() {
        return new ArrayList<>(objects);
    }
}
