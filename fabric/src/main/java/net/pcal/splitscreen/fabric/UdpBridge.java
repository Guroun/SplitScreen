package net.pcal.splitscreen.fabric;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

public class UdpBridge {

    private static final int PORT = 25570;
    private static final long INSTANCE_TIMEOUT_MS = 5000;

    private static final UUID myId = UUID.randomUUID();
    private static DatagramSocket socket;
    private static boolean isServer;
    private static volatile boolean running;
    private static int myInstanceNumber = -1;

    private static final Map<UUID, double[]> remotePositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> instanceHeartbeats = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> instanceNumbers = new ConcurrentHashMap<>();
    private static final Set<SocketAddress> clients = ConcurrentHashMap.newKeySet();

    private static volatile String lastServerAddress;
    private static volatile String lastLayoutRaw;
    private static String lastAppliedLayout;

    private static LayoutHandler layoutHandler;
    private static ServerHandler serverHandler;
    private static Runnable onInstanceJoined;
    private static Runnable onInstanceLeft;

    public interface LayoutHandler {
        void onLayoutReceived(String layoutName, String[] positions, int myIndex);
    }

    public interface ServerHandler {
        void onServerAddressChanged(String address);
    }

    public static UUID getMyId() { return myId; }
    public static int getMyInstanceNumber() { return myInstanceNumber; }
    public static void setLayoutHandler(LayoutHandler h) { layoutHandler = h; }
    public static void setServerHandler(ServerHandler h) { serverHandler = h; }
    public static void setOnInstanceJoined(Runnable r) { onInstanceJoined = r; }
    public static void setOnInstanceLeft(Runnable r) { onInstanceLeft = r; }
    public static String getLastServerAddress() { return lastServerAddress; }

    public static void initialize() {
        running = true;
        instanceNumbers.put(myId, -1);

        try {
            socket = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
            isServer = true;
            myInstanceNumber = 0;
            instanceNumbers.put(myId, 0);
            syslog().info("UDP bridge: SERVER on port " + PORT + ", I am #0");
        } catch (SocketException e) {
            try {
                socket = new DatagramSocket();
                isServer = false;
                syslog().info("UDP bridge: CLIENT, connecting to server...");
            } catch (SocketException ex) {
                syslog().error("UDP bridge failed to start", ex);
                return;
            }
        }

        AudioManager.setMyUuid(myId);

        Thread receiver = new Thread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (running) {
                try {
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    if (isServer) {
                        handleServerMessage(msg, packet.getSocketAddress());
                    } else {
                        handleClientMessage(msg);
                    }
                } catch (IOException e) {
                    if (running) syslog().error("UDP receive error", e);
                }
            }
        }, "UDP-Bridge");
        receiver.setDaemon(true);
        receiver.start();

        if (!isServer) {
            sendToServer("HELLO|" + myId);
        }
    }

    private static void handleServerMessage(String msg, SocketAddress sender) {
        for (String line : msg.split("\n")) {
            String[] parts = line.trim().split("\\|");
            if (parts.length < 1) continue;

            switch (parts[0]) {
                case "HELLO" -> {
                    if (parts.length >= 2) {
                        UUID uuid = UUID.fromString(parts[1]);
                        int num = assignInstanceNumber(uuid);
                        instanceHeartbeats.put(uuid, System.currentTimeMillis());
                        clients.add(sender);
                        sendToClient(sender, "WELCOME|" + uuid + "|" + num);
                        sendToClient(sender, "SERVER|" + (lastServerAddress != null ? lastServerAddress : ""));
                        if (lastLayoutRaw != null) {
                            sendToClient(sender, lastLayoutRaw);
                        }
                        syslog().info("UDP: Assigned instance #" + num + " to " + uuid.toString().substring(0, 8));
                        if (onInstanceJoined != null) {
                            Minecraft.getInstance().execute(onInstanceJoined);
                        }
                    }
                }
                case "HEARTBEAT" -> {
                    if (parts.length >= 2) {
                        UUID uuid = UUID.fromString(parts[1]);
                        instanceHeartbeats.put(uuid, System.currentTimeMillis());
                    }
                }
                case "POS" -> {
                    if (parts.length >= 5) {
                        relayToOthers(line, sender);
                        processPosition(parts);
                    }
                }
                case "LAYOUT" -> {
                    lastLayoutRaw = line;
                    relayToOthers(line, sender);
                    processLayout(parts, true);
                }
                case "SERVER" -> {
                    if (parts.length >= 2) {
                        lastServerAddress = parts[1].isEmpty() ? null : parts[1];
                        relayToOthers(line, sender);
                        notifyServerHandler(lastServerAddress);
                    }
                }
            }
        }
    }

    private static void handleClientMessage(String msg) {
        for (String line : msg.split("\n")) {
            String[] parts = line.trim().split("\\|");
            if (parts.length < 1) continue;

            switch (parts[0]) {
                case "WELCOME" -> {
                    if (parts.length >= 3) {
                        UUID uuid = UUID.fromString(parts[1]);
                        if (uuid.equals(myId)) {
                            myInstanceNumber = Integer.parseInt(parts[2]);
                            instanceNumbers.put(myId, myInstanceNumber);
                            syslog().info("UDP: I am instance #" + myInstanceNumber);
                        }
                    }
                }
                case "POS" -> processPosition(parts);
                case "LAYOUT" -> processLayout(parts, false);
                case "SERVER" -> {
                    if (parts.length >= 2) {
                        lastServerAddress = parts[1].isEmpty() ? null : parts[1];
                        notifyServerHandler(lastServerAddress);
                    }
                }
            }
        }
    }

    private static int assignInstanceNumber(UUID uuid) {
        if (instanceNumbers.containsKey(uuid)) {
            return instanceNumbers.get(uuid);
        }
        Set<Integer> taken = new HashSet<>(instanceNumbers.values());
        int num = 0;
        while (taken.contains(num)) num++;
        instanceNumbers.put(uuid, num);
        return num;
    }

    private static void processPosition(String[] parts) {
        try {
            UUID uuid = UUID.fromString(parts[1]);
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double z = Double.parseDouble(parts[4]);
            if (!uuid.equals(myId)) {
                remotePositions.put(uuid, new double[]{x, y, z});
                updateAudioManager();
            }
        } catch (Exception ignored) {}
    }

    private static void processLayout(String[] parts, boolean fromRelay) {
        try {
            String layoutName = parts[1];
            if (layoutName.equals(lastAppliedLayout)) return;

            String[] positions = parts[2].split(",");
            int myNum = myInstanceNumber;
            if (layoutHandler != null && myNum >= 0 && myNum < positions.length) {
                lastAppliedLayout = layoutName;
                layoutHandler.onLayoutReceived(layoutName, positions, myNum);
            }
        } catch (Exception ignored) {}
    }

    private static void updateAudioManager() {
        List<PlayerPositionsPacket.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, double[]> e : remotePositions.entrySet()) {
            double[] pos = e.getValue();
            entries.add(new PlayerPositionsPacket.Entry(e.getKey(), pos[0], pos[1], pos[2]));
        }
        AudioManager.onReceivePositions(entries);
    }

    private static void notifyServerHandler(String address) {
        if (serverHandler != null) {
            serverHandler.onServerAddressChanged(address);
        }
    }

    private static void relayToOthers(String line, SocketAddress sender) {
        byte[] data = line.getBytes(StandardCharsets.UTF_8);
        for (SocketAddress client : clients) {
            if (!client.equals(sender)) {
                try {
                    socket.send(new DatagramPacket(data, data.length, client));
                } catch (IOException ignored) {}
            }
        }
    }

    private static void sendToServer(String msg) {
        if (socket == null || running == false || isServer) return;
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, InetAddress.getLoopbackAddress(), PORT));
        } catch (IOException ignored) {}
    }

    private static void sendToClient(SocketAddress client, String msg) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, client));
        } catch (IOException ignored) {}
    }

    private static void broadcast(String msg) {
        if (socket == null || !running) return;
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        if (isServer) {
            processMessageLocally(msg);
            for (SocketAddress client : clients) {
                try {
                    socket.send(new DatagramPacket(data, data.length, client));
                } catch (IOException ignored) {}
            }
        } else {
            try {
                socket.send(new DatagramPacket(data, data.length, InetAddress.getLoopbackAddress(), PORT));
            } catch (IOException ignored) {}
        }
    }

    private static void processMessageLocally(String msg) {
        for (String line : msg.split("\n")) {
            String[] parts = line.trim().split("\\|");
            if (parts.length >= 1) {
                if ("POS".equals(parts[0])) processPosition(parts);
                else if ("LAYOUT".equals(parts[0])) processLayout(parts, false);
                else if ("SERVER".equals(parts[0]) && parts.length >= 2) {
                    lastServerAddress = parts[1].isEmpty() ? null : parts[1];
                    notifyServerHandler(lastServerAddress);
                }
            }
        }
    }

    public static void sendPosition(double x, double y, double z) {
        broadcast("POS|" + myId + "|" + x + "|" + y + "|" + z);
    }

    public static void sendLayout(String layoutName, String[] positions) {
        broadcast("LAYOUT|" + layoutName + "|" + String.join(",", positions));
    }

    public static void sendServerAddress(String address) {
        broadcast("SERVER|" + (address != null ? address : ""));
    }

    public static void sendHeartbeat() {
        sendToServer("HEARTBEAT|" + myId);
    }

    public static void cleanupStaleInstances() {
        if (!isServer) return;
        long now = System.currentTimeMillis();
        boolean removed = false;
        instanceHeartbeats.entrySet().removeIf(e -> {
            if (now - e.getValue() > INSTANCE_TIMEOUT_MS && !e.getKey().equals(myId)) {
                instanceNumbers.remove(e.getKey());
                remotePositions.remove(e.getKey());
                syslog().info("UDP: Removed stale instance " + e.getKey().toString().substring(0, 8));
                return true;
            }
            return false;
        });
        if (removed && onInstanceLeft != null) {
            Minecraft.getInstance().execute(onInstanceLeft);
        }
    }

    public static void shutdown() {
        running = false;
        if (socket != null) socket.close();
    }
}
