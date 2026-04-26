package net.pcal.splitscreen.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

public class AudioManager {

    private static final double PROXIMITY_THRESHOLD = 16.0;
    private static final Map<UUID, PlayerPositionsPacket.Entry> otherPlayerPositions = new HashMap<>();
    private static boolean muted = false;
    private static UUID myUuid;

    public static void setMyUuid(UUID uuid) {
        myUuid = uuid;
    }

    public static void onReceivePositions(List<PlayerPositionsPacket.Entry> entries) {
        otherPlayerPositions.clear();
        for (PlayerPositionsPacket.Entry entry : entries) {
            if (myUuid == null || !entry.uuid().equals(myUuid)) {
                otherPlayerPositions.put(entry.uuid(), entry);
            }
        }
        recalculate();
    }

    public static boolean isMuted() {
        return muted;
    }

    private static void recalculate() {
        if (myUuid == null) {
            muted = false;
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            muted = false;
            return;
        }

        double myX = player.getX();
        double myY = player.getY();
        double myZ = player.getZ();

        for (Map.Entry<UUID, PlayerPositionsPacket.Entry> e : otherPlayerPositions.entrySet()) {
            PlayerPositionsPacket.Entry other = e.getValue();
            UUID otherUuid = e.getKey();

            double dx = myX - other.x();
            double dy = myY - other.y();
            double dz = myZ - other.z();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= PROXIMITY_THRESHOLD * PROXIMITY_THRESHOLD) {
                boolean wasMuted = muted;
                muted = myUuid.compareTo(otherUuid) > 0;
                if (muted != wasMuted) {
                    syslog().info("Audio " + (muted ? "muted" : "unmuted") + " (distance: " + String.format("%.1f", Math.sqrt(distSq)) + ")");
                }
                return;
            }
        }

        if (muted) {
            syslog().info("Audio unmuted (no nearby players)");
        }
        muted = false;
    }
}
