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
     * Creates a hidden randomized stash near a location. Every invocation produces a
     * different base, container mix and loot.
     *
     * @param stashType chest | barrel | shulker | spawner | base | random
     * @param size      small | regular | large | huge (scales loot/containers)
     */
    public TestObject spawnStash(Player staff, Location near, String stashType, String size) {
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

        int scale = sizeScale(size);
        String type = (stashType == null || stashType.isEmpty()) ? "random"
                : stashType.toLowerCase(java.util.Locale.ROOT);
        if (type.equals("random")) {
            String[] all = {"chest", "barrel", "shulker", "spawner", "base"};
            type = all[random.nextInt(all.length)];
        }

        switch (type) {
            case "spawner":
                placeSpawner(object, target.getBlock(), scale);
                break;
            case "base":
                placeBase(object, target, scale, size);
                break;
            default:
                placeContainer(object, target.getBlock(), type, scale, size);
                break;
        }

        objects.add(object);
        log(staff, "spawnstash", object, "type=" + type + " size=" + size);
        return object;
    }

    private int sizeScale(String size) {
        if (size == null) {
            return 2;
        }
        switch (size.toLowerCase(java.util.Locale.ROOT)) {
            case "small":
                return 1;
            case "large":
                return 4;
            case "huge":
                return 8;
            case "regular":
            default:
                return 2;
        }
    }

    private Material containerMaterial(String type) {
        switch (type) {
            case "barrel": {
                Material m = plugin.versionManager().adapter().resolveMaterial("BARREL");
                return m != null ? m : Material.CHEST;
            }
            case "shulker": {
                Material m = plugin.versionManager().adapter().resolveMaterial("SHULKER_BOX");
                return m != null ? m : Material.CHEST;
            }
            case "chest":
            default:
                return Material.CHEST;
        }
    }

    /**
     * Builds a realistic hidden storage stash: a small dug-out, lit room with the
     * chosen container type arranged in neat rows along one or more walls - the way a
     * player actually organizes storage. The room size, which walls are lined, the
     * spacing, the lighting and the loot are all randomized so no two stashes look the
     * same (less obvious to a watching moderator).
     */
    private void placeContainer(TestObject object, Block base, String type, int scale, String size) {
        int half = Math.max(1, baseHalf(scale));
        int height = Math.max(3, baseHeight(scale) - 1);
        Material[] mats = buildingMaterials(base.getWorld());
        buildRoomShell(object, base, half, height, mats[0], mats[1]);

        // Doorway in one wall.
        setBlock(object, base.getRelative(half + 1, 0, 0), Material.AIR);
        setBlock(object, base.getRelative(half + 1, 1, 0), Material.AIR);

        Material container = containerMaterial(type);
        Material light = firstMaterial("LANTERN", "TORCH", "GLOWSTONE", "SHROOMLIGHT");

        // Pick a random subset of the four walls to line with storage.
        List<Integer> wallOrder = new ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3));
        java.util.Collections.shuffle(wallOrder, random);
        int wallCount = 1 + random.nextInt(4);
        boolean[] lined = new boolean[4];
        for (int i = 0; i < wallCount; i++) {
            lined[wallOrder.get(i)] = true;
        }

        int torchEvery = 3 + random.nextInt(3);
        int placed = 0;
        for (int w = 0; w < 4; w++) {
            if (!lined[w]) {
                continue;
            }
            for (int t = -half; t <= half; t++) {
                int dx;
                int dz;
                switch (w) {
                    case 0: dx = -half; dz = t; break;
                    case 1: dx = half; dz = t; break;
                    case 2: dx = t; dz = -half; break;
                    default: dx = t; dz = half; break;
                }
                // Keep the doorway clear.
                if (dx == half && dz == 0) {
                    continue;
                }
                Block at = base.getRelative(dx, 0, dz);
                if (light != null && Math.floorMod(t + half, torchEvery) == 0) {
                    setBlock(object, at, light); // a light every few blocks
                } else if (random.nextInt(7) != 0) { // occasional gap for realism
                    setBlock(object, at, container);
                    fillContainer(at, size, scale);
                    placed++;
                }
            }
        }

        // Guarantee at least a couple of containers even on a tiny/unlucky roll.
        if (placed < 2) {
            for (int d = -half; d <= half && placed < 2; d++) {
                Block at = base.getRelative(d, 0, -half);
                setBlock(object, at, container);
                fillContainer(at, size, scale);
                placed++;
            }
        }

        // A central ceiling light.
        if (light != null) {
            setBlock(object, base.getRelative(0, height - 1, 0), light);
        }
    }

    /** Carves a hollow room (floor, four walls, ceiling) out of the terrain. */
    private void buildRoomShell(TestObject object, Block base, int half, int height,
                                Material wall, Material floor) {
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -half - 1; dz <= half + 1; dz++) {
                for (int dy = -1; dy <= height; dy++) {
                    Block b = base.getRelative(dx, dy, dz);
                    boolean edgeX = dx == -half - 1 || dx == half + 1;
                    boolean edgeZ = dz == -half - 1 || dz == half + 1;
                    boolean isFloor = dy == -1;
                    boolean isCeiling = dy == height;
                    if (edgeX || edgeZ || isFloor || isCeiling) {
                        setBlock(object, b, isFloor ? floor : wall);
                    } else {
                        setBlock(object, b, Material.AIR);
                    }
                }
            }
        }
    }

    private void placeSpawner(TestObject object, Block block, int scale) {
        Material spawner = plugin.versionManager().adapter().resolveMaterial("SPAWNER");
        if (spawner == null) {
            placeContainer(object, block, "chest", scale, "regular");
            return;
        }
        object.rememberBlock(block.getLocation(), block.getType());
        block.setType(spawner, false);
        try {
            BlockState state = block.getState();
            if (state instanceof org.bukkit.block.CreatureSpawner) {
                org.bukkit.block.CreatureSpawner cs = (org.bukkit.block.CreatureSpawner) state;
                org.bukkit.entity.EntityType[] mobs = randomSpawnerMobs();
                cs.setSpawnedType(mobs[random.nextInt(mobs.length)]);
                cs.update();
            }
        } catch (Throwable ignored) {
            // CreatureSpawner API differences across versions; the block is still placed.
        }
    }

    private org.bukkit.entity.EntityType[] randomSpawnerMobs() {
        List<org.bukkit.entity.EntityType> list = new ArrayList<>();
        for (String name : new String[]{"ZOMBIE", "SKELETON", "SPIDER", "CAVE_SPIDER", "BLAZE", "CREEPER"}) {
            try {
                list.add(org.bukkit.entity.EntityType.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Mob not present on this version.
            }
        }
        if (list.isEmpty()) {
            list.add(org.bukkit.entity.EntityType.ZOMBIE);
        }
        return list.toArray(new org.bukkit.entity.EntityType[0]);
    }

    /**
     * Builds an actual hidden base: a hollow room (floor, walls, ceiling) carved out
     * of the terrain, with a doorway, lighting and furniture, and the loot containers
     * placed inside. Size scales the interior footprint and height. Every block is
     * recorded so cleanup restores the terrain exactly.
     */
    private void placeBase(TestObject object, Location target, int scale, String size) {
        Block base = target.getBlock(); // interior floor center
        int half = baseHalf(scale);     // interior reaches +/- half on x/z
        int height = baseHeight(scale); // interior wall height
        Material[] mats = buildingMaterials(base.getWorld());

        // 1) Shell + hollow interior.
        buildRoomShell(object, base, half, height, mats[0], mats[1]);

        // 2) Doorway: a 1x2 opening in the middle of one wall.
        int wallX = half + 1;
        setBlock(object, base.getRelative(wallX, 0, 0), Material.AIR);
        setBlock(object, base.getRelative(wallX, 1, 0), Material.AIR);

        // 3) Interior floor cells available for furnishings (shuffled).
        List<int[]> cells = new ArrayList<>();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                cells.add(new int[]{dx, dz});
            }
        }
        java.util.Collections.shuffle(cells, random);
        int index = 0;

        // 4) Loot containers.
        int containers = Math.min(cells.size() - 2, 2 + scale);
        String[] kinds = {"chest", "barrel", "shulker"};
        for (int i = 0; i < containers && index < cells.size(); i++, index++) {
            int[] c = cells.get(index);
            Block b = base.getRelative(c[0], 0, c[1]);
            setBlock(object, b, containerMaterial(kinds[random.nextInt(kinds.length)]));
            fillContainer(b, size, scale);
        }

        // 5) Furniture for a lived-in look.
        for (String furniture : new String[]{"CRAFTING_TABLE", "FURNACE", "BOOKSHELF"}) {
            if (index >= cells.size()) {
                break;
            }
            Material m = plugin.versionManager().adapter().resolveMaterial(furniture);
            if (m != null && random.nextBoolean()) {
                int[] c = cells.get(index++);
                setBlock(object, base.getRelative(c[0], 0, c[1]), m);
            }
        }

        // 6) Lighting so the room reads as inhabited.
        Material light = firstMaterial("LANTERN", "GLOWSTONE", "TORCH", "SHROOMLIGHT");
        if (light != null) {
            int lights = 1 + scale / 2;
            for (int i = 0; i < lights && index < cells.size(); i++, index++) {
                int[] c = cells.get(index);
                setBlock(object, base.getRelative(c[0], height - 1, c[1]), light);
            }
        }

        // 7) A spawner sometimes, tucked in a corner.
        if (random.nextBoolean()) {
            placeSpawner(object, base.getRelative(half, 0, half), scale);
        }
    }

    private void setBlock(TestObject object, Block block, Material material) {
        if (material == null) {
            return;
        }
        object.rememberBlock(block.getLocation(), block.getType());
        block.setType(material, false);
    }

    private int baseHalf(int scale) {
        switch (scale) {
            case 1: return 1;  // small  -> 3x3 interior
            case 2: return 2;  // regular-> 5x5
            case 4: return 3;  // large  -> 7x7
            default: return 4; // huge   -> 9x9
        }
    }

    private int baseHeight(int scale) {
        switch (scale) {
            case 1: return 3;
            case 2: return 4;
            case 4: return 5;
            default: return 6;
        }
    }

    private Material[] buildingMaterials(World world) {
        boolean nether = world.getEnvironment() == World.Environment.NETHER;
        Material wall;
        Material floor;
        if (nether) {
            wall = firstMaterial("NETHER_BRICKS", "BLACKSTONE", "NETHERRACK", "COBBLESTONE", "STONE");
            floor = firstMaterial("BLACKSTONE", "NETHER_BRICKS", "NETHERRACK", "STONE");
        } else {
            String[] walls = {"STONE_BRICKS", "COBBLESTONE", "DEEPSLATE_BRICKS",
                    "MOSSY_COBBLESTONE", "OAK_PLANKS", "POLISHED_ANDESITE"};
            String[] floors = {"OAK_PLANKS", "STONE_BRICKS", "COBBLESTONE", "SPRUCE_PLANKS"};
            wall = firstMaterial(walls[random.nextInt(walls.length)], "COBBLESTONE", "STONE");
            floor = firstMaterial(floors[random.nextInt(floors.length)], "COBBLESTONE", "STONE");
        }
        return new Material[]{wall == null ? Material.STONE : wall, floor == null ? Material.STONE : floor};
    }

    private Material firstMaterial(String... names) {
        for (String name : names) {
            Material material = plugin.versionManager().adapter().resolveMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return null;
    }

    private void fillContainer(Block block, String size, int scale) {
        // InventoryHolder (rather than the 1.13+ Container interface) keeps this
        // compatible all the way back to legacy chests.
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder)) {
            return;
        }
        Inventory inventory = ((InventoryHolder) state).getInventory();
        int rolls = (3 + random.nextInt(4)) * Math.max(1, scale);
        Material[] loot = lootTable(size);
        for (int i = 0; i < rolls; i++) {
            Material material = loot[random.nextInt(loot.length)];
            int slot = random.nextInt(inventory.getSize());
            inventory.setItem(slot, new ItemStack(material, 1 + random.nextInt(16)));
        }
    }

    private Material[] lootTable(String size) {
        List<Material> list = new ArrayList<>();
        addIfPresent(list, "IRON_INGOT", "GOLD_INGOT", "EXPERIENCE_BOTTLE", "ARROW", "BREAD", "COAL");
        String s = size == null ? "regular" : size.toLowerCase(java.util.Locale.ROOT);
        if (!s.equals("small")) {
            addIfPresent(list, "DIAMOND", "EMERALD", "ENDER_PEARL", "GOLDEN_CARROT");
        }
        if (s.equals("large") || s.equals("huge")) {
            addIfPresent(list, "NETHERITE_SCRAP", "NETHERITE_INGOT", "GOLDEN_APPLE",
                    "DIAMOND_BLOCK", "ENCHANTED_GOLDEN_APPLE");
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

    /**
     * Creates a hidden randomized ore vein. Each call random-walks a fresh blob so no
     * two veins share a shape.
     *
     * @param oreType    friendly ore alias (diamond, gold, ancient_debris, emerald,
     *                   iron, copper, redstone, lapis, coal, *_block, ...) or "random"
     * @param veinAmount number of ore blocks to place; &lt;= 0 picks a random amount
     */
    public TestObject oreSummon(Player staff, Location near, String oreType, int veinAmount) {
        if (!toolsEnabled() || near == null || near.getWorld() == null) {
            return null;
        }
        var inv = plugin.configManager().investigation();
        if (!inv.getBoolean("investigation-tools.oresummon.enabled", true)) {
            return null;
        }
        int minOres = inv.getInt("investigation-tools.oresummon.min-ores", 3);
        int maxOres = inv.getInt("investigation-tools.oresummon.max-ores", 12);
        int count = veinAmount > 0 ? veinAmount
                : minOres + random.nextInt(Math.max(1, maxOres - minOres + 1));
        count = Math.max(1, Math.min(count, 256)); // safety cap

        Location target = randomizedNearby(near, 24, 96);
        if (target == null) {
            return null;
        }

        Material ore = resolveOreType(target.getWorld(), oreType,
                inv.getStringList("investigation-tools.oresummon.allowed-blocks"));
        if (ore == null) {
            return null;
        }

        TestObject object = new TestObject(TestObject.Type.ORE, target, staff.getName(), cleanupAtMillis());
        Block base = target.getBlock();
        // Random-walk a blob vein from the base, placing ore into replaceable blocks.
        Block cursor = base;
        int placed = 0;
        int guard = 0;
        while (placed < count && guard < count * 8) {
            guard++;
            if (isReplaceable(cursor.getType())) {
                object.rememberBlock(cursor.getLocation(), cursor.getType());
                cursor.setType(ore, false);
                placed++;
            }
            cursor = cursor.getRelative(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (random.nextInt(5) == 0) {
                cursor = base; // occasionally restart near the origin for a tighter blob
            }
        }
        objects.add(object);
        log(staff, "oresummon", object, "ore=" + ore + " count=" + placed);
        return object;
    }

    private boolean isReplaceable(Material type) {
        if (type == null || type == Material.AIR) {
            return false;
        }
        String n = type.name();
        return type.isSolid() || n.contains("STONE") || n.contains("DEEPSLATE")
                || n.contains("NETHERRACK") || n.contains("DIRT") || n.contains("GRANITE")
                || n.contains("ANDESITE") || n.contains("DIORITE") || n.contains("TUFF");
    }

    private Material resolveOreType(World world, String oreType, List<String> allowed) {
        boolean nether = world.getEnvironment() == World.Environment.NETHER;
        if (oreType != null && !oreType.isEmpty() && !oreType.equalsIgnoreCase("random")) {
            Material mapped = mapOreAlias(oreType, nether);
            if (mapped != null) {
                return mapped;
            }
        }
        // Random from the configured allow-list, respecting the dimension for debris.
        List<Material> options = new ArrayList<>();
        for (String name : allowed) {
            Material material = plugin.versionManager().adapter().resolveMaterial(name);
            if (material == null) {
                continue;
            }
            boolean isDebris = material.name().contains("ANCIENT_DEBRIS");
            if (isDebris == nether) {
                options.add(material);
            }
        }
        if (options.isEmpty()) {
            return plugin.versionManager().adapter()
                    .resolveMaterial(nether ? "ANCIENT_DEBRIS" : "DIAMOND_ORE");
        }
        return options.get(random.nextInt(options.size()));
    }

    /** Maps a friendly ore alias to a concrete material, deepslate/dimension-aware. */
    private Material mapOreAlias(String alias, boolean nether) {
        com.lumen.essentials.version.VersionAdapter adapter = plugin.versionManager().adapter();
        switch (alias.toLowerCase(java.util.Locale.ROOT)) {
            case "ancient_debris":
            case "ancientdebris":
            case "debris":
                return first(adapter, "ANCIENT_DEBRIS");
            case "netherite":
            case "netherite_block":
                return first(adapter, "NETHERITE_BLOCK");
            case "diamond":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_DIAMOND_ORE" : "DIAMOND_ORE", "DIAMOND_ORE");
            case "diamond_block":
                return first(adapter, "DIAMOND_BLOCK");
            case "gold":
                return nether ? first(adapter, "NETHER_GOLD_ORE", "GOLD_ORE")
                        : first(adapter, random.nextBoolean() ? "DEEPSLATE_GOLD_ORE" : "GOLD_ORE", "GOLD_ORE");
            case "gold_block":
                return first(adapter, "GOLD_BLOCK");
            case "emerald":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_EMERALD_ORE" : "EMERALD_ORE", "EMERALD_ORE");
            case "emerald_block":
                return first(adapter, "EMERALD_BLOCK");
            case "iron":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_IRON_ORE" : "IRON_ORE", "IRON_ORE");
            case "iron_block":
                return first(adapter, "IRON_BLOCK");
            case "copper":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_COPPER_ORE" : "COPPER_ORE", "COPPER_ORE");
            case "redstone":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_REDSTONE_ORE" : "REDSTONE_ORE", "REDSTONE_ORE");
            case "lapis":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_LAPIS_ORE" : "LAPIS_ORE", "LAPIS_ORE");
            case "coal":
                return first(adapter, random.nextBoolean() ? "DEEPSLATE_COAL_ORE" : "COAL_ORE", "COAL_ORE");
            default:
                return adapter.resolveMaterial(alias); // try a direct material name
        }
    }

    private Material first(com.lumen.essentials.version.VersionAdapter adapter, String... names) {
        for (String name : names) {
            Material material = adapter.resolveMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return null;
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
        int baseY = Math.max(minWorldHeight(world) + 5, near.getBlockY() - (8 + random.nextInt(20)));
        return new Location(world, x + 0.5, baseY, z + 0.5);
    }

    /** World#getMinHeight is 1.17+; fall back to 0 on older servers. */
    private int minWorldHeight(World world) {
        try {
            return world.getMinHeight();
        } catch (Throwable ignored) {
            return 0;
        }
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
