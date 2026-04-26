package net.pcal.splitscreen.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.pcal.splitscreen.common.WindowStyle.FULLSCREEN;
import static net.pcal.splitscreen.common.WindowStyle.SPLITSCREEN;
import static net.pcal.splitscreen.common.WindowStyle.WINDOWED;

record WindowMode(
        String name,
        WindowStyle style,
        Function<MinecraftWindow, Integer> xFn,
        Function<MinecraftWindow, Integer> yFn,
        Function<MinecraftWindow, Integer> widthFn,
        Function<MinecraftWindow, Integer> heightFn) {

    WindowStyle getStyle() {
        return style;
    }


    String getName() {
        return name;
    }

    MinecraftWindow.Rectangle getRepositionedBoundsFor(MinecraftWindow screenBounds) {
        return new MinecraftWindow.Rectangle(xFn.apply(screenBounds), yFn.apply(screenBounds), widthFn.apply(screenBounds), heightFn.apply(screenBounds));
    }

    static List<WindowMode> getModes(int gap) {
        final List<WindowMode> modes = new ArrayList<>();
        addMode(modes, "WINDOWED", WINDOWED, r -> r.getWindowBounds().x(), r -> r.getWindowBounds().y(),
                r -> r.getWindowBounds().width(), r -> r.getWindowBounds().height());
        addMode(modes, "LEFT", SPLITSCREEN, r -> 0, r -> 0, r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height());
        addMode(modes, "RIGHT", SPLITSCREEN, r -> r.getScreenBounds().width() / 2 + gap, r -> 0, r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height());
        addMode(modes, "TOP", SPLITSCREEN, r -> 0, r -> 0, r -> r.getScreenBounds().width(), r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "BOTTOM", SPLITSCREEN, r -> 0, r -> r.getScreenBounds().height() / 2 + gap, r -> r.getScreenBounds().width(), r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "TOP_LEFT", SPLITSCREEN, r -> 0, r -> 0,
                r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "TOP_RIGHT", SPLITSCREEN, r -> r.getScreenBounds().width() / 2 + gap, r -> 0,
                r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "BOTTOM_LEFT", SPLITSCREEN, r -> 0, r -> r.getScreenBounds().height() / 2 + gap,
                r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "BOTTOM_RIGHT", SPLITSCREEN, r -> r.getScreenBounds().width() / 2 + gap, r -> r.getScreenBounds().height() / 2 + gap,
                r -> r.getScreenBounds().width() / 2 - gap, r -> r.getScreenBounds().height() / 2 - gap);
        addMode(modes, "FULLSCREEN", FULLSCREEN, no(), no(), no(), no());
        return modes;
    }

    private static void addMode(List<WindowMode> modes,
                                String name,
                                WindowStyle style,
                                Function<MinecraftWindow, Integer> xFn,
                                Function<MinecraftWindow, Integer> yFn,
                                Function<MinecraftWindow, Integer> widthFn,
                                Function<MinecraftWindow, Integer> heightFn) {
        modes.add(new WindowMode(name, style, xFn, yFn, widthFn, heightFn));
    }

    private static Function<MinecraftWindow, Integer> no() {
        return r -> -1;
    }

}
