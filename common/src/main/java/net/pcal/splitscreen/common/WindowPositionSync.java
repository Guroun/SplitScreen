package net.pcal.splitscreen.common;

import java.util.HashMap;
import java.util.Map;

public class WindowPositionSync {

    private static final Map<String, String> COMPLEMENTARY_MODES = new HashMap<>();

    static {
        COMPLEMENTARY_MODES.put("LEFT", "RIGHT");
        COMPLEMENTARY_MODES.put("RIGHT", "LEFT");

        COMPLEMENTARY_MODES.put("TOP", "BOTTOM");
        COMPLEMENTARY_MODES.put("BOTTOM", "TOP");

        COMPLEMENTARY_MODES.put("TOP_LEFT", "BOTTOM_RIGHT");
        COMPLEMENTARY_MODES.put("TOP_RIGHT", "BOTTOM_LEFT");
        COMPLEMENTARY_MODES.put("BOTTOM_LEFT", "TOP_RIGHT");
        COMPLEMENTARY_MODES.put("BOTTOM_RIGHT", "TOP_LEFT");
    }

    public static String getComplementaryMode(String currentMode) {
        return COMPLEMENTARY_MODES.get(currentMode);
    }

    public static boolean hasComplementaryMode(String mode) {
        return COMPLEMENTARY_MODES.containsKey(mode);
    }

    public static boolean shouldAutoSync(String mode) {
        return hasComplementaryMode(mode);
    }
}
