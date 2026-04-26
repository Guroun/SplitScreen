package net.pcal.splitscreen.common;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class LayoutPreviewButton extends AbstractButton {

    private static final int COLOR_BG_DARK = 0x99000000;
    private static final int COLOR_WHITE_15 = 0x26FFFFFF;
    private static final int COLOR_BORDER = 0x80FFFFFF;

    private final SplitscreenLayout layout;
    private final Font font;
    private final Runnable onPressCallback;

    public LayoutPreviewButton(int x, int y, int width, int height,
                               Component label, SplitscreenLayout layout,
                               Font font, Runnable onPress) {
        super(x, y, width, height, label);
        this.layout = layout;
        this.font = font;
        this.onPressCallback = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (this.onPressCallback != null) {
            this.onPressCallback.run();
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, COLOR_BG_DARK);

        int previewX = x + 8;
        int previewY = y + 8;
        int previewW = w - 16;
        int previewH = h - 32;

        drawLayoutPreview(graphics, previewX, previewY, previewW, previewH);

        int textX = x + w / 2 - this.font.width(getMessage()) / 2;
        int textY = y + h - 18;
        graphics.text(this.font, getMessage(), textX, textY, 0xFFFFFFFF, true);
    }

    private void drawLayoutPreview(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        switch (layout) {
            case HORIZONTAL -> drawHorizontalSplit(graphics, x, y, w, h);
            case VERTICAL -> drawVerticalSplit(graphics, x, y, w, h);
            case QUAD -> drawQuadSplit(graphics, x, y, w, h);
            case FULLSCREEN -> drawFullscreen(graphics, x, y, w, h);
            case WINDOWED -> drawWindowed(graphics, x, y, w, h);
        }
    }

    private void drawHorizontalSplit(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int gap = 2;
        int halfW = (w - gap) / 2;
        graphics.fill(x, y, x + halfW, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x, y, halfW, h);
        graphics.fill(x + halfW + gap, y, x + w, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x + halfW + gap, y, w - halfW - gap, h);
    }

    private void drawVerticalSplit(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int gap = 2;
        int halfH = (h - gap) / 2;
        graphics.fill(x, y, x + w, y + halfH, COLOR_WHITE_15);
        drawBorder(graphics, x, y, w, halfH);
        graphics.fill(x, y + halfH + gap, x + w, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x, y + halfH + gap, w, h - halfH - gap);
    }

    private void drawQuadSplit(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int gap = 2;
        int halfW = (w - gap) / 2;
        int halfH = (h - gap) / 2;
        graphics.fill(x, y, x + halfW, y + halfH, COLOR_WHITE_15);
        drawBorder(graphics, x, y, halfW, halfH);
        graphics.fill(x + halfW + gap, y, x + w, y + halfH, COLOR_WHITE_15);
        drawBorder(graphics, x + halfW + gap, y, w - halfW - gap, halfH);
        graphics.fill(x, y + halfH + gap, x + halfW, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x, y + halfH + gap, halfW, h - halfH - gap);
        graphics.fill(x + halfW + gap, y + halfH + gap, x + w, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x + halfW + gap, y + halfH + gap, w - halfW - gap, h - halfH - gap);
    }

    private void drawFullscreen(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, COLOR_WHITE_15);
        drawBorder(graphics, x, y, w, h);
    }

    private void drawWindowed(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int inset = w / 6;
        graphics.fill(x + inset, y + inset, x + w - inset, y + h - inset, COLOR_WHITE_15);
        drawBorder(graphics, x + inset, y + inset, w - inset * 2, h - inset * 2);
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + 1, COLOR_BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + h, COLOR_BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
