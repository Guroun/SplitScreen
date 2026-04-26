package net.pcal.splitscreen.common;

import net.pcal.splitscreen.common.MinecraftWindow.Rectangle;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

public class Mod {

    private static class SingletonHolder {
        private static Mod INSTANCE = new Mod();
    }

    public static Mod mod() {
        return SingletonHolder.INSTANCE;
    }

    private Mod() {}

    private static final String MODE_PROP = "mode";
    private static final String GAP_PROP = "gap";
    private static final String AUTO_SYNC_PROP = "autoSync";

    private MinecraftWindow unpositionedWindow;
    private MinecraftWindow currentWindow;
    private List<WindowMode> modes;
    private int gap = 8;
    private Properties config;
    private Path configPath;
    private int currentModeIndex = 0;
    private Rectangle savedWindowRect;
    private boolean autoSyncEnabled = true;
    private ModeChangeListener modeChangeListener;
    private LayoutBroadcastListener layoutBroadcastListener;

    public void onModInitialize(final Path configDirPath) {
        this.configPath = configDirPath.resolve("splitscreen.properties");
        this.config = new Properties();
        if (this.configPath.toFile().exists()) {
            try (final FileReader fr = new FileReader(this.configPath.toFile())) {
                this.config.load(fr);
            } catch (IOException e) {
                syslog().error(e);
            }
        }
        try {
            String gapProp = config.getProperty(GAP_PROP);
            if (gapProp != null) {
                this.gap = Integer.parseInt(gapProp);
            }
            String autoSyncProp = config.getProperty(AUTO_SYNC_PROP);
            if (autoSyncProp != null) {
                this.autoSyncEnabled = Boolean.parseBoolean(autoSyncProp);
            }
            this.modes = WindowMode.getModes(gap);
            String modeConfig = config.getProperty(MODE_PROP);
            if (modeConfig != null) {
                modeConfig = modeConfig.trim();
                for (this.currentModeIndex = 0; this.currentModeIndex < this.modes.size(); this.currentModeIndex++) {
                    if (modeConfig.equals(modes.get(currentModeIndex).getName())) break;
                }
                if (currentModeIndex >= this.modes.size()) {
                    syslog().warn("unknown mode " + modeConfig);
                    currentModeIndex = 0;
                }
            }
            if (this.unpositionedWindow != null) {
                repositionWindow(this.unpositionedWindow);
                unpositionedWindow = null;
            }
        } catch (Exception e) {
            syslog().error(e);
        }
    }

    public void onWindowCreate(final MinecraftWindow window) {
        this.currentWindow = window;
        if (this.modes == null) {
            if (this.unpositionedWindow != null) syslog().error("Multiple windows created?");
            this.unpositionedWindow = requireNonNull(window);
        } else {
            repositionWindow(window);
        }
    }

    public void onToggleFullscreen(final MinecraftWindow window) {
        if (this.savedWindowRect == null || modes.get(currentModeIndex).getStyle() == WindowStyle.WINDOWED) {
            this.savedWindowRect = window.getWindowBounds();
        }
        currentModeIndex = (currentModeIndex + 1) % modes.size();
        saveConfig();
        repositionWindow(window);
        notifyModeChange();
    }

    public void openLayoutScreen() {
    }

    public void onResolutionChange(final MinecraftWindow window) {
        repositionWindow(window);
    }

    public void onSetMode(final MinecraftWindow window) {
        repositionWindow(window);
    }

    public String getCurrentModeName() {
        if (this.modes == null || this.currentModeIndex >= this.modes.size()) {
            return null;
        }
        return this.modes.get(this.currentModeIndex).getName();
    }

    public void setModeByName(String modeName) {
        if (this.modes == null) {
            syslog().warn("Cannot set mode, modes not initialized");
            return;
        }
        for (int i = 0; i < this.modes.size(); i++) {
            if (this.modes.get(i).getName().equals(modeName)) {
                this.currentModeIndex = i;
                saveConfig();
                if (this.currentWindow != null) {
                    repositionWindow(this.currentWindow);
                }
                syslog().info("Window mode set to: " + modeName);
                return;
            }
        }
        syslog().warn("Unknown mode: " + modeName);
    }

    public boolean isAutoSyncEnabled() {
        return this.autoSyncEnabled;
    }

    public void setAutoSyncEnabled(boolean enabled) {
        this.autoSyncEnabled = enabled;
        this.config.put(AUTO_SYNC_PROP, String.valueOf(enabled));
        saveConfig();
        syslog().info("Auto-sync " + (enabled ? "enabled" : "disabled"));
    }

    public void setModeChangeListener(ModeChangeListener listener) {
        this.modeChangeListener = listener;
    }

    public void setLayoutBroadcastListener(LayoutBroadcastListener listener) {
        this.layoutBroadcastListener = listener;
    }

    public void applyLayout(SplitscreenLayout layout) {
        if (this.layoutBroadcastListener != null) {
            this.layoutBroadcastListener.onLayoutSelected(layout);
        }

        String primaryPosition = layout.getPrimaryPosition();
        setModeByName(primaryPosition);

        syslog().info("Applied layout: " + layout + " with position: " + primaryPosition);
    }

    public interface ModeChangeListener {
        void onModeChanged(String newMode);
    }

    public interface LayoutBroadcastListener {
        void onLayoutSelected(SplitscreenLayout layout);
    }

    private void repositionWindow(final MinecraftWindow window) {
        if (window == null) {
            syslog().error("Window is null");
            return;
        }
        if (this.modes == null) {
            syslog().error("Modes is null");
            return;
        }
        final WindowMode mode = modes.get(this.currentModeIndex);
        if (mode == null) {
            syslog().error("Failed to determine mode.");
        } else {
            window.reposition(mode.getStyle(), mode.getRepositionedBoundsFor(window));
        }
    }

    private synchronized void saveConfig() {
        try {
            this.config.put(MODE_PROP, this.modes.get(this.currentModeIndex).getName());
            this.config.put(GAP_PROP, String.valueOf(this.gap));
            this.config.put(AUTO_SYNC_PROP, String.valueOf(this.autoSyncEnabled));
            try (final FileWriter fw = new FileWriter(this.configPath.toFile())) {
                this.config.store(fw, null);
            }
        } catch (Exception e) {
            syslog().error(e);
        }
    }

    private void notifyModeChange() {
        if (this.modeChangeListener != null) {
            String currentMode = getCurrentModeName();
            if (currentMode != null) {
                this.modeChangeListener.onModeChanged(currentMode);
            }
        }
    }
}
