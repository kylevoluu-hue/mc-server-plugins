package com.lumen.essentials.version;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Abstraction over version-specific behavior. Implementations let the rest of the
 * plugin stay free of hardcoded version checks and degrade gracefully on platforms
 * that do not expose a given capability.
 */
public interface VersionAdapter {

    /** Human readable label, e.g. {@code "Modern (1.13-1.16)"}. */
    String name();

    /** Whether the running server exposes Paper's native anti-xray engine. */
    boolean hasNativeAntiXray();

    /** Whether {@link Player#getPing()} is available natively (vs. reflection). */
    boolean hasNativePing();

    /**
     * Resolves a {@link Material} by name in a version-safe way, returning
     * {@code null} when the material does not exist on this server version
     * (e.g. DEEPSLATE_DIAMOND_ORE on 1.16).
     */
    Material resolveMaterial(String name);

    /** The set of valuable blocks this version recognizes from a configured list. */
    Set<Material> resolveMaterials(Iterable<String> names);

    /** Whether the player is currently gliding with an elytra (false on legacy). */
    boolean isGliding(Player player);

    /** Whether the player is in a riptide/trident spin animation, if known. */
    boolean isRiptiding(Player player);
}
