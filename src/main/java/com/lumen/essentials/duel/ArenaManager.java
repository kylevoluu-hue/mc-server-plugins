package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the dedicated flat duel world and allocates arenas of varying size inside it.
 * Bigger, more mobile kits (crystal, pearls, elytra) get bigger arenas. Each arena is a
 * fully-enclosed shell — a layered grass/stone/bedrock floor with invisible barrier
 * walls and roof — so nobody can fly, pearl, or elytra out. Tiles are laid on a grid,
 * partitioned into size bands so a tile always keeps the same size (its shell is built
 * once and reused).
 */
public final class ArenaManager {

    public static final int SMALL = 55;
    public static final int MEDIUM = 80;
    public static final int LARGE = 120;

    private static final String WORLD_NAME = "lumen_duels";
    private static final int TILE = 320;        // spacing between arenas (fits LARGE + gap)
    private static final int FLOOR_Y = 100;     // grass surface height
    private static final int GRID = 16;         // 16x16 tiles

    private final LumenEssentials plugin;
    private final Set<Integer> usedTiles = new HashSet<>();
    private final Set<Integer> builtTiles = new HashSet<>();
    private final Map<Integer, Integer> tileSize = new HashMap<>();
    private World world;

    public ArenaManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public World world() {
        if (world != null) {
            return world;
        }
        world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            try {
                world = new WorldCreator(WORLD_NAME)
                        .type(WorldType.FLAT)
                        .generateStructures(false)
                        .createWorld();
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not create duel world: " + t.getMessage());
                return null;
            }
        }
        configureWorld(world);
        return world;
    }

    private void configureWorld(World w) {
        trySetRule(w, "doMobSpawning", "false");
        trySetRule(w, "doDaylightCycle", "false");
        trySetRule(w, "doWeatherCycle", "false");
        trySetRule(w, "mobGriefing", "false");
        trySetRule(w, "doFireTick", "false");
        trySetRule(w, "announceAdvancements", "false");
    }

    private void trySetRule(World w, String rule, String value) {
        try {
            w.setGameRuleValue(rule, value);
        } catch (Throwable ignored) {
            // gamerule unsupported; ignore
        }
    }

    /** Vertical fighting room (wall height) scales with arena size. */
    private int wallHeight(int size) {
        return size >= LARGE ? 32 : size >= MEDIUM ? 24 : 18;
    }

    /** The grid rows reserved for a given size band, so each tile keeps one size. */
    private int[] rowsFor(int size) {
        if (size >= LARGE) {
            return new int[]{0, 4};      // rows 0-4
        }
        if (size >= MEDIUM) {
            return new int[]{5, 10};     // rows 5-10
        }
        return new int[]{11, 15};        // rows 11-15
    }

    /** Allocates a free arena tile sized for the match, building its shell if needed. */
    public synchronized Integer allocate(int size) {
        if (world() == null) {
            return null;
        }
        int[] rows = rowsFor(size);
        for (int gz = rows[0]; gz <= rows[1]; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                int tile = gz * GRID + gx;
                if (!usedTiles.contains(tile)) {
                    usedTiles.add(tile);
                    tileSize.put(tile, size);
                    ensureBuilt(tile, size);
                    return tile;
                }
            }
        }
        return null; // all arenas of this size in use
    }

    public synchronized void release(Integer tile) {
        if (tile != null) {
            usedTiles.remove(tile);
        }
    }

    private void ensureBuilt(int tile, int size) {
        if (builtTiles.contains(tile)) {
            return;
        }
        World w = world();
        int[] origin = tileOrigin(tile);
        int half = size / 2;
        int roofY = FLOOR_Y + wallHeight(size) + 1;

        Material grass = resolve("GRASS_BLOCK", "GRASS", "DIRT");
        Material dirt = resolve("DIRT", "STONE");
        Material stone = resolve("STONE", "COBBLESTONE");
        Material bedrock = resolve("BEDROCK", "OBSIDIAN", "STONE");
        Material barrier = resolve("BARRIER", "GLASS", "STONE");

        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = origin[0] + dx;
                int z = origin[1] + dz;
                // Layered floor: grass surface over dirt, stone, then indestructible bedrock.
                w.getBlockAt(x, FLOOR_Y, z).setType(grass, false);
                w.getBlockAt(x, FLOOR_Y - 1, z).setType(dirt, false);
                w.getBlockAt(x, FLOOR_Y - 2, z).setType(stone, false);
                w.getBlockAt(x, FLOOR_Y - 3, z).setType(bedrock, false);
                // Barrier roof over the whole area.
                w.getBlockAt(x, roofY, z).setType(barrier, false);
                // Barrier walls around the perimeter.
                if (dx == -half || dx == half || dz == -half || dz == half) {
                    for (int dy = 1; dy <= wallHeight(size); dy++) {
                        w.getBlockAt(x, FLOOR_Y + dy, z).setType(barrier, false);
                    }
                }
            }
        }
        builtTiles.add(tile);
    }

    private int[] tileOrigin(int tile) {
        int gx = tile % GRID;
        int gz = tile / GRID;
        return new int[]{gx * TILE, gz * TILE};
    }

    /** Spawn points: {@code count} per team, lined along opposite edges facing center. */
    public List<Location>[] spawns(int tile, int count) {
        World w = world();
        int size = tileSize.getOrDefault(tile, MEDIUM);
        int[] origin = tileOrigin(tile);
        int half = size / 2 - 4;
        List<Location> teamA = new ArrayList<>();
        List<Location> teamB = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0 : (i - (count - 1) / 2.0) * 6;
            teamA.add(new Location(w, origin[0] - half + 0.5, FLOOR_Y + 1, origin[1] + spread + 0.5, 90f, 0f));
            teamB.add(new Location(w, origin[0] + half + 0.5, FLOOR_Y + 1, origin[1] + spread + 0.5, -90f, 0f));
        }
        @SuppressWarnings("unchecked")
        List<Location>[] result = new List[]{teamA, teamB};
        return result;
    }

    public Location center(int tile) {
        int[] origin = tileOrigin(tile);
        return new Location(world(), origin[0] + 0.5, FLOOR_Y + 1, origin[1] + 0.5);
    }

    private Material resolve(String primary, String... fallbacks) {
        Material m = plugin.versionManager().adapter().resolveMaterial(primary);
        if (m == null) {
            for (String alt : fallbacks) {
                m = plugin.versionManager().adapter().resolveMaterial(alt);
                if (m != null) {
                    break;
                }
            }
        }
        return m != null ? m : Material.STONE;
    }
}
