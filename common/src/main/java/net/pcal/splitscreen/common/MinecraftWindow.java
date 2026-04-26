package net.pcal.splitscreen.common;

public interface MinecraftWindow {

    Rectangle getWindowBounds();

    Rectangle getScreenBounds();


    void reposition(WindowStyle style, Rectangle newBounds);

    record Rectangle(int x, int y, int width, int height) {}
}
