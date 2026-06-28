package com.lumen.essentials.version;

import org.bukkit.entity.Player;

/**
 * Adapter for legacy servers (roughly 1.8.9 - 1.12.2). These versions predate
 * Paper's native anti-xray engine and native ping accessor, and lack elytra
 * (1.9 added it but gliding API is unreliable on the oldest builds).
 */
public final class LegacyAdapter extends AbstractVersionAdapter {

    @Override
    public String name() {
        return "Legacy (1.8.9-1.12.2)";
    }

    @Override
    public boolean hasNativeAntiXray() {
        return false;
    }

    @Override
    public boolean hasNativePing() {
        return false;
    }

    @Override
    public boolean isRiptiding(Player player) {
        // Tridents/riptide did not exist before 1.13.
        return false;
    }
}
