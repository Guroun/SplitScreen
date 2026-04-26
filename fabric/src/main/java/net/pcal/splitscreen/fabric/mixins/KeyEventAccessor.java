package net.pcal.splitscreen.fabric.mixins;

import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyEvent.class)
public interface KeyEventAccessor {
    @Accessor("key")
    int getKey();
    
    @Accessor("scancode")
    int getScancode();
    
    @Accessor("modifiers")
    int getModifiers();
}
