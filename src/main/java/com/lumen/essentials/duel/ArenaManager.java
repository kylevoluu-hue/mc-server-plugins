package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the dedicated flat duel world and allocates 100x100 walled arenas inside it on
 * demand. A fresh arena is built lazily on a free grid tile when a match needs one and
 * released back when the match ends, so the world stays small and there is no
 * per-match world creation (which would lag the server).
 */
public final class ArenaManager {

    private static final String WORLD_NAME = "lumen_duels";
    private static final int SIZE = 100;        // arena interior is 100x100
    private static final int TILE = 256;        // spacing between arenas
    private static final int FLOOR_Y = 100;     // platform height
    private static final int WALL_HEIGHT = 6;
    private static final int GRID = 16;         // up to 16x16 = 256 concurrent arenas

    private final LumenEssentials plugin;
    private final Set<Integer> usedTiles = new HashSet<>();
    private final Set<Integer> builtTiles = new HashSet<>();
    private World world;

    public ArenaManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    /** Creates/loads the flat duel world if needed. Returns null if creation failed. */
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
        // Best-effort: keep the arena world quiet and static.
        trySetRule(w, "doMobSpawning", "false");
        trySetRule(w, "doDaylightCycle", "false");
        trySetRule(w, "doWeatherCycle", "false");
        trySetRule(w, "mobGriefing", "false");
        trySetRule(w, "doFireTick", "false");
        trySetRule(w, "fallDamage", "true");
        trySetRule(w, "announceAdvancements", "false");
    }

    private void trySetRule(World w, String rule, String value) {
        try {
            w.setGameRuleValue(rule, value);
        } catch (Throwable ignored) {
            // gamerule unsupported on this version; ignore
        }
    }

    /** Allocates a free arena tile and ensures its platform is built. */
    public synchronized Integer allocate() {
        if (world() == null) {
            return null;
        }
        for (int tile = 0; tile < GRID * GRID; tile++) {
            if (!usedTiles.contains(tile)) {
                usedTiles.add(tile);
                ensureBuilt(tile);
                return tile;
            }
        }
        return null; // all arenas in use
    }

    public synchronized void release(Integer tile) {
        if (tile != null) {
            usedTiles.remove(tile);
        }
    }

    private void ensureBuilt(int tile) {
        if (builtTiles.contains(tile)) {
            return;
        }
        World w = world();
        int[] origin = tileOrigin(tile);
        int half = SIZE / 2;
        // Indestructible shell: bedrock floor + invisible barrier walls, so the arena
        // can never be broken out of or griefed regardless of the breakable setting.
        Material floor = resolve("BEDROCK", "OBSIDIAN", "STONE");
        Material wall = resolve("BARRIER", "GLASS", "STONE");
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                w.getBlockAt(origin[0] + dx, FLOOR_Y, origin[1] + dz).setType(floor, false);
                boolean edge = dx == -half || dx == half || dz == -half || dz == half;
                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    Material m = edge ? wall : Material.AIR;
                    w.getBlockAt(origin[0] + dx, FLOOR_Y + dy, origin[1] + dz).setType(m, false);
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

    /**
     * Spawn points for a tile: {@code count} per team, lined along opposite edges and
     * facing the center.
     */
    public List<Location>[] spawns(int tile, int count) {
        World w = world();
        int[] origin = tileOrigin(tile);
        int half = SIZE / 2 - 4;
        List<Location> teamA = new ArrayList<>();
        List<Location> teamB = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0 : (i - (count - 1) / 2.0) * 6;
            Location a = new Location(w, origin[0] - half + 0.5, FLOOR_Y + 1, origin[1] + spread + 0.5, 90f, 0f);
            Location b = new Location(w, origin[0] + half + 0.5, FLOOR_Y + 1, origin[1] + spread + 0.5, -90f, 0f);
            teamA.add(a);
            teamB.add(b);
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
