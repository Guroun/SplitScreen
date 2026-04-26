package net.pcal.splitscreen.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import static net.pcal.splitscreen.common.Mod.mod;

public class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        mod().onModInitialize(FabricLoader.getInstance().getConfigDir());
        LocalPositionSync.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                UdpBridge.sendPosition(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ()
                );
            }
            LocalPositionSync.checkServerConnection();
        });
    }
}
