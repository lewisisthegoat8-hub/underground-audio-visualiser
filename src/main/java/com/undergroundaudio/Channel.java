package com.undergroundaudio;

import java.util.function.Predicate;

/** A group of sounds that can be toggled and colored independently. Order matters: first match wins. */
public enum Channel {
    VILLAGER("Villagers", 0x55FF55, true, id -> id.startsWith("entity.villager") || id.startsWith("entity.wandering_trader")),
    STEPS("Steps", 0xFFAA00, true, id -> id.contains(".step")),
    REDSTONE("Redstone", 0xFF5555, true,
            id -> id.contains("piston") || id.contains("lever") || id.contains("comparator") || id.contains("dispenser")),
    DOORS("Doors/Chests", 0x55FFFF, false,
            id -> id.contains("door") || id.contains("chest") || id.contains("barrel") || id.contains("shulker_box")
                    || id.contains("fence_gate")),
    BLOCKS("Break/Place", 0xFFFF55, false, id -> id.startsWith("block.") && (id.endsWith(".break") || id.endsWith(".place"))),
    EXPLOSIONS("Explosions", 0xFF7733, false, id -> id.contains("explode") || id.contains("tnt.primed")),
    COMBAT("Combat", 0xFF55FF, false,
            id -> id.startsWith("entity.player.attack") || id.startsWith("entity.player.hurt") || id.contains("arrow")),
    PLAYERS("Players", 0x5599FF, false, id -> id.startsWith("entity.player")),
    MOBS("Mobs", 0xAAAAAA, false, id -> id.startsWith("entity.")),
    CUSTOM("Custom", 0xFFFFFF, false, id -> false); // handled via keyword list in UndergroundAudioVisualizer

    public final String label;
    public final int defaultColor;
    public final boolean defaultEnabled;
    private final Predicate<String> matcher;

    Channel(String label, int defaultColor, boolean defaultEnabled, Predicate<String> matcher) {
        this.label = label;
        this.defaultColor = defaultColor;
        this.defaultEnabled = defaultEnabled;
        this.matcher = matcher;
    }

    public boolean matches(String soundPath) {
        return matcher.test(soundPath);
    }
}
