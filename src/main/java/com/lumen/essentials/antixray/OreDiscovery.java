package com.lumen.essentials.antixray;

import org.bukkit.Material;

/** Immutable record of a single valuable-block discovery, used for {@code /luac orelog}. */
public final class OreDiscovery {

    private final String player;
    private final Material material;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final boolean exposed;
    private final long timestamp;

    public OreDiscovery(String player, Material material, String world,
                        int x, int y, int z, boolean exposed, long timestamp) {
        this.player = player;
        this.material = material;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.exposed = exposed;
        this.timestamp = timestamp;
    }

    public String player() {
        return player;
    }

    public Material material() {
        return material;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public boolean exposed() {
        return exposed;
    }

    public long timestamp() {
        return timestamp;
    }
}
