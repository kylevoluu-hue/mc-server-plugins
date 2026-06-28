package com.lumen.essentials.version;

/**
 * Adapter for the latest era (1.20 and newer, including the 1.21+/26.x line).
 * Everything the plugin relies on is available natively here.
 */
public final class LatestAdapter extends AbstractVersionAdapter {

    @Override
    public String name() {
        return "Latest (1.20+)";
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
