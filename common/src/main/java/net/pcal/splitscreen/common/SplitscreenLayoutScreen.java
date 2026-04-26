package net.pcal.splitscreen.common;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.pcal.splitscreen.common.Mod.mod;

public class SplitscreenLayoutScreen extends Screen {

    private final Screen parent;
    private static final int PREVIEW_BUTTON_WIDTH = 120;
    private static final int PREVIEW_BUTTON_HEIGHT = 100;
    private static final int BUTTON_SPACING = 16;
    private static final int SMALL_BUTTON_WIDTH = 200;
    private static final int SMALL_BUTTON_HEIGHT = 20;

    public SplitscreenLayoutScreen(Screen parent) {
        super(Component.literal("Splitscreen Layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;

        int totalWidth = PREVIEW_BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
        int startX = centerX - totalWidth / 2;
        int previewY = this.height / 2 - PREVIEW_BUTTON_HEIGHT / 2;

        this.addRenderableWidget(new LayoutPreviewButton(
                startX, previewY, PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Horizontal"),
                SplitscreenLayout.HORIZONTAL,
                this.font,
                () -> selectLayout(SplitscreenLayout.HORIZONTAL)
        ));

        this.addRenderableWidget(new LayoutPreviewButton(
                startX + PREVIEW_BUTTON_WIDTH + BUTTON_SPACING, previewY,
                PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Vertical"),
                SplitscreenLayout.VERTICAL,
                this.font,
                () -> selectLayout(SplitscreenLayout.VERTICAL)
        ));

        this.addRenderableWidget(new LayoutPreviewButton(
                startX + (PREVIEW_BUTTON_WIDTH + BUTTON_SPACING) * 2, previewY,
                PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Quad"),
                SplitscreenLayout.QUAD,
                this.font,
                () -> selectLayout(SplitscreenLayout.QUAD)
        ));

        int cancelY = previewY + PREVIEW_BUTTON_HEIGHT + 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> this.minecraft.setScreen(parent)
        ).bounds(centerX - SMALL_BUTTON_WIDTH / 2, cancelY, SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        Component title = Component.literal("Splitscreen Layout");
        int titleX = this.width / 2 - this.font.width(title) / 2;
        graphics.text(this.font, title, titleX, 20, 0xFFFFFFFF, true);

        Component subtitle = Component.literal("Select a layout for local multiplayer");
        int subtitleX = this.width / 2 - this.font.width(subtitle) / 2;
        graphics.text(this.font, subtitle, subtitleX, 35, 0xFFAAAAAA, true);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void selectLayout(SplitscreenLayout layout) {
        System.out.println("SplitscreenLayoutScreen.selectLayout() called with: " + layout);
        mod().applyLayout(layout);
        System.out.println("After applyLayout, closing screen...");
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
