package com.lumen.essentials.investigation;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A temporary investigation object (a hidden test stash or test ore vein) placed by
 * staff. Remembers the original blocks it overwrote so cleanup can restore the world
 * exactly, and tracks discovery/interaction signals for forensic alerts.
 */
public final class TestObject {

    public enum Type {
        STASH,
        ORE
    }

    private final UUID id = UUID.randomUUID();
    private final Type type;
    private final Location origin;
    private final String creator;
    private final long createdAt;
    private final long cleanupAt;
    /** Original block materials keyed by location, for exact restoration. */
    private final Map<Location, Material> originalBlocks = new HashMap<>();

    private boolean discovered;
    private int blocksBrokenNearby;
    private int containerOpens;

    public TestObject(Type type, Location origin, String creator, long cleanupAt) {
        this.type = type;
        this.origin = origin;
        this.creator = creator;
        this.createdAt = System.currentTimeMillis();
        this.cleanupAt = cleanupAt;
    }

    public UUID id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public Location origin() {
        return origin;
    }

    public String creator() {
        return creator;
    }

    public long createdAt() {
        return createdAt;
    }

    public long cleanupAt() {
        return cleanupAt;
    }

    public Map<Location, Material> originalBlocks() {
        return originalBlocks;
    }

    public void rememberBlock(Location location, Material material) {
        originalBlocks.put(location, material);
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void markDiscovered() {
        this.discovered = true;
    }

    public int blocksBrokenNearby() {
        return blocksBrokenNearby;
    }

    public void incrementBlocksBroken() {
        this.blocksBrokenNearby++;
    }

    public int containerOpens() {
        return containerOpens;
    }

    public void incrementContainerOpens() {
        this.containerOpens++;
    }

    public boolean isExpired(long now) {
        return cleanupAt > 0 && now >= cleanupAt;
    }
}
