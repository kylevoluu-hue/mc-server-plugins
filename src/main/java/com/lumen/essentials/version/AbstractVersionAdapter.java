package com.lumen.essentials.version;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared adapter logic. Material resolution is identical across versions (it is
 * inherently null-safe via {@link Material#matchMaterial(String)}), so it lives here.
 */
public abstract class AbstractVersionAdapter implements VersionAdapter {

    @Override
    public Material resolveMaterial(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Material.matchMaterial(name.trim().toUpperCase());
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public Set<Material> resolveMaterials(Iterable<String> names) {
        Set<Material> result = new LinkedHashSet<>();
        if (names == null) {
            return result;
        }
        for (String name : names) {
            Material material = resolveMaterial(name);
            if (material != null) {
                result.add(material);
            }
        }
        return result;
    }

    @Override
    public boolean isGliding(Player player) {
        try {
            return player != null && player.isGliding();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean isRiptiding(Player player) {
        try {
            return player != null && player.isRiptiding();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Utility for subclasses: filter a known material name list to those that exist. */
    protected Set<Material> existing(String... names) {
        Set<Material> set = new HashSet<>();
        for (String name : names) {
            Material material = resolveMaterial(name);
            if (material != null) {
                set.add(material);
            }
        }
        return set;
    }
}
