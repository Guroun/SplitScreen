package net.pcal.splitscreen.common;

public enum SplitscreenLayout {
    HORIZONTAL("LEFT", "RIGHT"),
    VERTICAL("TOP", "BOTTOM"),
    QUAD("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"),
    FULLSCREEN("FULLSCREEN"),
    WINDOWED("WINDOWED");

    private final String[] positions;

    SplitscreenLayout(String... positions) {
        this.positions = positions;
    }

    public String[] getPositions() {
        return positions;
    }

    public String getPrimaryPosition() {
        return positions[0];
    }

    public String getSecondaryPosition() {
        return positions.length > 1 ? positions[1] : positions[0];
    }
}
