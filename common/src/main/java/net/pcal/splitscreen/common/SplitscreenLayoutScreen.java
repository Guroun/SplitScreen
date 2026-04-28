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

    private LayoutPreviewButton horizontalBtn;
    private LayoutPreviewButton verticalBtn;
    private LayoutPreviewButton quadBtn;

    public SplitscreenLayoutScreen(Screen parent) {
        super(Component.literal("Splitscreen Layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        int totalWidth = PREVIEW_BUTTON_WIDTH * 3 + BUTTON_SPACING * 2;
        int startX = centerX - totalWidth / 2;
        int previewY = this.height / 2 - PREVIEW_BUTTON_HEIGHT / 2;

        horizontalBtn = new LayoutPreviewButton(
                startX, previewY, PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Horizontal"),
                SplitscreenLayout.HORIZONTAL,
                this.font,
                () -> selectLayout(SplitscreenLayout.HORIZONTAL)
        );

        verticalBtn = new LayoutPreviewButton(
                startX + PREVIEW_BUTTON_WIDTH + BUTTON_SPACING, previewY,
                PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Vertical"),
                SplitscreenLayout.VERTICAL,
                this.font,
                () -> selectLayout(SplitscreenLayout.VERTICAL)
        );

        quadBtn = new LayoutPreviewButton(
                startX + (PREVIEW_BUTTON_WIDTH + BUTTON_SPACING) * 2, previewY,
                PREVIEW_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT,
                Component.literal("Quad"),
                SplitscreenLayout.QUAD,
                this.font,
                () -> selectLayout(SplitscreenLayout.QUAD)
        );

        this.addRenderableWidget(horizontalBtn);
        this.addRenderableWidget(verticalBtn);
        this.addRenderableWidget(quadBtn);

        int cancelY = previewY + PREVIEW_BUTTON_HEIGHT + 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> this.minecraft.setScreen(parent)
        ).bounds(centerX - SMALL_BUTTON_WIDTH / 2, cancelY, SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

        updateButtonStates();
    }

    private void updateButtonStates() {
        int count = mod().getInstanceCount();
        horizontalBtn.active = count == 2;
        verticalBtn.active = count == 2;
        quadBtn.active = count >= 3;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        updateButtonStates();

        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        Component title = Component.literal("Splitscreen Layout");
        int titleX = this.width / 2 - this.font.width(title) / 2;
        graphics.text(this.font, title, titleX, 20, 0xFFFFFFFF, true);

        int count = mod().getInstanceCount();
        Component subtitle;
        if (count <= 1) {
            subtitle = Component.literal("Start another instance to enable split-screen");
        } else if (count == 2) {
            subtitle = Component.literal("2 instances detected - select a 2-player layout");
        } else {
            subtitle = Component.literal(count + " instances detected - quad split available");
        }
        int subtitleX = this.width / 2 - this.font.width(subtitle) / 2;
        graphics.text(this.font, subtitle, subtitleX, 35, 0xFFAAAAAA, true);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void selectLayout(SplitscreenLayout layout) {
        mod().applyLayout(layout);
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
