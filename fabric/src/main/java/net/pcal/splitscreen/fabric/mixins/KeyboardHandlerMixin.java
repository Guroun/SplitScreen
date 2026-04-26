package net.pcal.splitscreen.fabric.mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.pcal.splitscreen.common.SplitscreenLayoutScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (((KeyEventAccessor)(Object)event).getKey() == GLFW.GLFW_KEY_F11 && 
            action == GLFW.GLFW_PRESS) {
            this.minecraft.setScreen(new SplitscreenLayoutScreen(this.minecraft.screen));
            ci.cancel();
        }
    }
}
