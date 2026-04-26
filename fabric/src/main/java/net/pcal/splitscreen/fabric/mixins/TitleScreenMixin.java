package net.pcal.splitscreen.fabric.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.pcal.splitscreen.fabric.LocalPositionSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    
    @Unique
    private Button joinLanButton;
    
    @Unique
    private ScheduledExecutorService lanCheckScheduler;
    
    protected TitleScreenMixin(Component text) {
        super(text);
    }

    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private void beforeInit(CallbackInfo ci) {
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void addText(CallbackInfo ci) {
        final Component text = Component.literal(Minecraft.getInstance().getUser().getName());
        addRenderableWidget(new StringWidget(4, 4, font.width(text), 10, text, this.font));
        
        Button realmsButton = null;
        
        for (var widget : this.children()) {
            if (widget instanceof Button button) {
                String buttonText = button.getMessage().getString();
                if (buttonText.contains("Realms")) {
                    realmsButton = button;
                    break;
                }
            }
        }
        
        if (realmsButton != null) {
            joinLanButton = Button.builder(
                Component.literal("Join LAN"),
                button -> onJoinLanClick()
            ).pos(realmsButton.getX(), realmsButton.getY())
             .size(realmsButton.getWidth(), realmsButton.getHeight()).build();
            joinLanButton.active = false;
            removeWidget(realmsButton);
            addRenderableWidget(joinLanButton);
            
            startLanServerCheck();
        }
    }
    
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if (lanCheckScheduler != null && !lanCheckScheduler.isShutdown()) {
            lanCheckScheduler.shutdown();
        }
    }
    
    @Unique
    private void startLanServerCheck() {
        lanCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        lanCheckScheduler.scheduleAtFixedRate(() -> {
            if (joinLanButton != null) {
                String address = LocalPositionSync.getLanServerAddress();
                boolean available = address != null;
                joinLanButton.active = available;
                if (available) {
                    joinLanButton.setMessage(Component.literal("Join " + address));
                } else {
                    joinLanButton.setMessage(Component.literal("Join LAN"));
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    @Unique
    private void onJoinLanClick() {
        String serverAddress = LocalPositionSync.getLanServerAddress();
        if (serverAddress != null) {
            syslog().info("Connecting to server: " + serverAddress);
            
            ServerAddress address = ServerAddress.parseString(serverAddress);
            ServerData serverData = new ServerData("Multiplayer", serverAddress, ServerData.Type.LAN);
            
            ConnectScreen.startConnecting(
                this,
                this.minecraft,
                address,
                serverData,
                false,
                null
            );
        }
    }
}
