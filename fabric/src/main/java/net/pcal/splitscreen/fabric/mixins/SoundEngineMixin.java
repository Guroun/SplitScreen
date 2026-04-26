package net.pcal.splitscreen.fabric.mixins;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.pcal.splitscreen.fabric.AudioManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    private static boolean shouldMute(SoundSource source) {
        return switch (source) {
            case MASTER, MUSIC, UI -> false;
            default -> AudioManager.isMuted();
        };
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (shouldMute(instance.getSource())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void onPlayDelayed(SoundInstance instance, int delay, CallbackInfo ci) {
        if (shouldMute(instance.getSource())) {
            ci.cancel();
        }
    }
}
