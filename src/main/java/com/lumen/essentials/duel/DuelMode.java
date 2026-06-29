package com.lumen.essentials.duel;

/** Team-size modes for duels. Each mode is two teams of {@link #teamSize()} players. */
public enum DuelMode {

    ONE_V_ONE("1v1", 1),
    TWO_V_TWO("2v2", 2),
    FOUR_V_FOUR("4v4", 4),
    EIGHT_V_EIGHT("8v8", 8);

    private final String label;
    private final int teamSize;

    DuelMode(String label, int teamSize) {
        this.label = label;
        this.teamSize = teamSize;
    }

    public String label() {
        return label;
    }

    public int teamSize() {
        return teamSize;
    }

    /** Total players needed to start a match in this mode. */
    public int total() {
        return teamSize * 2;
    }

    public static DuelMode byLabel(String label) {
        for (DuelMode mode : values()) {
            if (mode.label.equalsIgnoreCase(label)) {
                return mode;
            }
        }
        return ONE_V_ONE;
    }
}
