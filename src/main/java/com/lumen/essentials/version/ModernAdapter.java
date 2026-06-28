package com.lumen.essentials.version;

/**
 * Adapter for the "modern" era (roughly 1.13 - 1.19). The flattening landed,
 * Paper's native anti-xray engine is present, and ping/gliding APIs are stable.
 */
public final class ModernAdapter extends AbstractVersionAdapter {

    @Override
    public String name() {
        return "Modern (1.13-1.19)";
    }

    @Override
    public boolean hasNativeAntiXray() {
        return true;
    }

    @Override
    public boolean hasNativePing() {
        return true;
    }
}
