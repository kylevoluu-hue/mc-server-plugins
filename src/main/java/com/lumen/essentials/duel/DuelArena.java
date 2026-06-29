package com.lumen.essentials.duel;

import org.bukkit.Location;

/** A duel arena with two spawn points. Only one match may use an arena at a time. */
public final class DuelArena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean inUse;

    public DuelArena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public String name() {
        return name;
    }

    public Location spawn1() {
        return spawn1;
    }

    public Location spawn2() {
        return spawn2;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}
