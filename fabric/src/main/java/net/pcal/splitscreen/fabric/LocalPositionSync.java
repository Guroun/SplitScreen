package net.pcal.splitscreen.fabric;

import net.minecraft.client.Minecraft;
import net.pcal.splitscreen.common.SplitscreenLayout;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.pcal.splitscreen.common.Mod.mod;
import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

public class LocalPositionSync {

    private static ScheduledExecutorService scheduler;
    private static volatile String currentServerAddress;

    public static void initialize() {
        UdpBridge.initialize();

        UdpBridge.setLayoutHandler((layoutName, positions, myIndex) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (!mod().isAutoSyncEnabled()) return;
                String myPosition = positions[myIndex];
                String currentMode = mod().getCurrentModeName();
                if (!myPosition.equals(currentMode)) {
                    syslog().info("Applying layout " + layoutName + ": " + myPosition + " (instance #" + UdpBridge.getMyInstanceNumber() + ")");
                    mod().setModeByName(myPosition);
                }
            });
        });

        UdpBridge.setServerHandler(address -> {
            currentServerAddress = address;
        });

        UdpBridge.setOnInstanceJoined(() -> {
            String lastLayout = mod().getLastLayoutName();
            if (lastLayout != null && !lastLayout.isEmpty()) {
                try {
                    SplitscreenLayout layout = SplitscreenLayout.valueOf(lastLayout);
                    broadcastLayout(layout);
                    syslog().info("Auto-reposition: applying saved layout " + lastLayout);
                } catch (IllegalArgumentException ignored) {}
            }
        });

        UdpBridge.setOnInstanceLeft(() -> {
            mod().setModeByName("FULLSCREEN");
            syslog().info("Auto-reposition: other instance left, switching to fullscreen");
        });

        mod().setLayoutBroadcastListener(layout -> {
            broadcastLayout(layout);
        });

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            UdpBridge.sendHeartbeat();
            UdpBridge.cleanupStaleInstances();
        }, 2, 2, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            UdpBridge.shutdown();
            if (scheduler != null) scheduler.shutdown();
        }));

        syslog().info("Local position sync initialized via UDP - I am instance #" + UdpBridge.getMyInstanceNumber());
    }

    private static void broadcastLayout(SplitscreenLayout layout) {
        mod().saveLastLayout(layout.name());
        UdpBridge.sendLayout(layout.name(), layout.getPositions());

        String[] positions = layout.getPositions();
        int myNum = UdpBridge.getMyInstanceNumber();
        if (myNum >= 0 && myNum < positions.length) {
            mod().setModeByName(positions[myNum]);
            syslog().info("Applied my position: " + positions[myNum] + " (instance #" + myNum + ")");
        }
    }

    public static void checkServerConnection() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            String address = null;
            if (mc.getCurrentServer() != null) {
                address = mc.getCurrentServer().ip;
            }

            UdpBridge.sendServerAddress(address);
        } catch (Exception ignored) {}
    }

    public static void broadcastLanServer(int port) {
        UdpBridge.sendServerAddress("localhost:" + port);
        syslog().info("Broadcasting LAN server: localhost:" + port);
    }

    public static String getLanServerAddress() {
        return currentServerAddress;
    }

    public static boolean isLanServerAvailable() {
        return currentServerAddress != null;
    }

    public static int getMyInstanceNumber() {
        return UdpBridge.getMyInstanceNumber();
    }
}
