package com.lumen.essentials.settings;

import java.util.Locale;

/**
 * The toggleable per-player settings shown in {@code /settings}. Each entry carries
 * a config key, display name, a thematically-related menu icon, descriptive lore and
 * a default value.
 *
 * <p>The {@code clientSide} flag marks settings that are inherently a client concern
 * (particles, FOV, fullbright). The server persists them as preferences a companion
 * client/resource layer can read; the rest are enforced server-side.
 */
public enum SettingType {

    NIGHT_VISION("night-vision", "Night Vision", "GOLDEN_CARROT", false, false,
            "Permanent night vision (fullbright)."),

    NO_EXPLOSION_PARTICLES("no-explosion-particles", "No Explosion Particles", "TNT", false, true,
            "Hide explosion particle effects."),

    FAST_CRYSTAL("fast-crystal", "Fast Crystal", "END_CRYSTAL", false, true,
            "Reduced end-crystal interaction delay."),

    NO_HOSTILE_MOBS("no-hostile-mobs", "Hostile Mob Spawning", "ZOMBIE_HEAD", false, false,
            "Stop hostile mobs spawning around you.",
            "They can still spawn if a nearby player allows them."),

    HIDE_CHAT("hide-chat", "Hide Global Chat", "PAPER", false, false,
            "Hide global chat messages from your screen."),

    TEAM_CHAT("team-chat", "Team Chat", "WHITE_BANNER", false, true,
            "Route your chat to your team only."),

    BLOCK_TPA("block-tpa", "Teleport Requests", "ENDER_PEARL", false, false,
            "Prevent others from sending you teleport requests."),

    SPOOF_COORDS("spoof-coords", "Spoof Coords", "COMPASS", false, true,
            "Prevent accidental coordinate leaks."),

    INSTANT_RESPAWN("instant-respawn", "Instant Respawn", "RED_BED", false, false,
            "Respawn instantly when you die."),

    HURT_CAM("hurt-cam", "Hurt Cam", "REDSTONE", false, true,
            "Disable FOV effects and reduce hurt motion."),

    TELEPORT_MESSAGES("teleport-messages", "Teleport Messages", "ENDER_EYE", true, false,
            "Show a confirmation message when you teleport.");

    private final String key;
    private final String displayName;
    private final String iconMaterial;
    private final boolean defaultValue;
    private final boolean clientSide;
    private final String[] description;

    SettingType(String key, String displayName, String iconMaterial, boolean defaultValue,
                boolean clientSide, String... description) {
        this.key = key;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.defaultValue = defaultValue;
        this.clientSide = clientSide;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String iconMaterial() {
        return iconMaterial;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public boolean clientSide() {
        return clientSide;
    }

    public String[] description() {
        return description;
    }

    public static SettingType byKey(String key) {
        for (SettingType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    public String enumName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
