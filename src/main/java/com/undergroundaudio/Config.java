package com.undergroundaudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** All user settings. Saved to config/underground-audio-visualizer.json. */
public final class Config {
    public enum YMode {
        UNDERGROUND("Underground only"), ABOVE("Above only"), ANY("Any height");

        public final String label;
        YMode(String label) { this.label = label; }
        public YMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static final class ChannelCfg {
        public boolean enabled;
        public int color;
        public ChannelCfg() {}
        public ChannelCfg(boolean enabled, int color) { this.enabled = enabled; this.color = color; }
    }

    // General
    public boolean enabled = true;
    public YMode yMode = YMode.UNDERGROUND;
    public int yOffset = 0;            // blocks above/below you required for the Y filter
    public int maxDistance = 64;       // blocks
    public float duration = 3.0f;      // seconds a marker stays
    public int maxMarkers = 128;
    public boolean ignoreSelf = true;  // ignore sounds within ~2.5 blocks (your own steps)
    public boolean logToChat = false;
    public String customKeywords = ""; // comma separated, matched against the sound id

    // Visuals
    public boolean boxes = true;
    public float boxSize = 1.0f;
    public boolean fade = true;
    public float opacity = 1.0f;
    public boolean tracers = true;
    public float tracerWidth = 2.0f;

    public Map<String, ChannelCfg> channels = new LinkedHashMap<>();

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("underground-audio-visualizer.json");
    }

    public static Config load() {
        Config c = null;
        try {
            Path f = file();
            if (Files.exists(f)) c = GSON.fromJson(Files.readString(f), Config.class);
        } catch (Exception ignored) {}
        if (c == null) c = new Config();
        c.fillDefaults();
        return c;
    }

    public void save() {
        try {
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException ignored) {}
    }

    public void fillDefaults() {
        if (channels == null) channels = new LinkedHashMap<>();
        if (yMode == null) yMode = YMode.UNDERGROUND;
        if (customKeywords == null) customKeywords = "";
        for (Channel ch : Channel.values()) {
            channels.computeIfAbsent(ch.name(), k -> new ChannelCfg(ch.defaultEnabled, ch.defaultColor));
        }
    }

    public ChannelCfg get(Channel ch) {
        return channels.get(ch.name());
    }
}
