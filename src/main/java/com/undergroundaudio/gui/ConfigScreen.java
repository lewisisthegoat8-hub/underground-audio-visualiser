package com.undergroundaudio.gui;

import com.undergroundaudio.Channel;
import com.undergroundaudio.Config;
import com.undergroundaudio.UndergroundAudioVisualizer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Tabbed settings GUI. Opened with Delete, closed with Escape. Settings save when it closes. */
public final class ConfigScreen extends Screen {
    private static final int W = 350, H = 262;
    private static final int PAD = 10, GAP = 10, ROW = 22;
    private static final int ACCENT = 0xFF55FFAA;

    private static final int[] PALETTE = {
            0xFF5555, 0xFF7733, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFAA, 0x55FFFF,
            0x5599FF, 0xAA77FF, 0xFF55FF, 0xFFFFFF, 0xAAAAAA
    };

    private enum Tab { GENERAL("General"), FILTERS("Filters"), VISUALS("Visuals");
        final String label; Tab(String l) { label = l; } }

    private static Tab currentTab = Tab.GENERAL;

    private final Screen parent;
    private int px, py;          // panel top-left
    private int colW;            // column width
    private int contentY;        // first content row y

    public ConfigScreen(Screen parent) {
        super(Component.literal("Underground Audio Visualizer"));
        this.parent = parent;
    }

    private Config cfg() { return UndergroundAudioVisualizer.CONFIG; }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        colW = (W - 2 * PAD - GAP) / 2;
        contentY = py + 62;

        // tabs
        int tabW = (W - 2 * PAD - 2 * 4) / 3;
        int i = 0;
        for (Tab t : Tab.values()) {
            final Tab tab = t;
            Button b = Button.builder(Component.literal(t.label), btn -> { currentTab = tab; rebuildWidgets(); })
                    .bounds(px + PAD + i * (tabW + 4), py + 30, tabW, 20).build();
            b.active = currentTab != t;
            addRenderableWidget(b);
            i++;
        }

        switch (currentTab) {
            case GENERAL -> buildGeneral();
            case FILTERS -> buildFilters();
            case VISUALS -> buildVisuals();
        }

        // footer
        int fy = py + H - 28;
        addRenderableWidget(Button.builder(Component.literal("Clear markers"),
                b -> UndergroundAudioVisualizer.clearMarkers())
                .bounds(px + PAD, fy, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset defaults"), b -> {
            UndergroundAudioVisualizer.CONFIG = new Config();
            UndergroundAudioVisualizer.CONFIG.fillDefaults();
            rebuildWidgets();
        }).bounds(px + PAD + 106, fy, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(px + W - PAD - 100, fy, 100, 20).build());
    }

    private int colX(int col) { return px + PAD + col * (colW + GAP); }
    private int rowY(int row) { return contentY + row * ROW; }

    // ------------------------------------------------------------------ tabs

    private void buildGeneral() {
        Config c = cfg();
        toggle(0, 0, "Enabled", () -> c.enabled, v -> c.enabled = v);
        cycle(1, 0, () -> "Height: " + c.yMode.label, () -> c.yMode = c.yMode.next());

        slider(0, 1, "Max distance", 8, 512, 1, () -> c.maxDistance, v -> c.maxDistance = (int) v, "m");
        slider(1, 1, "Height offset", 0, 40, 1, () -> c.yOffset, v -> c.yOffset = (int) v, "m");

        slider(0, 2, "Duration", 1, 15, 0.5, () -> c.duration, v -> c.duration = (float) v, "s");
        slider(1, 2, "Max markers", 8, 512, 8, () -> c.maxMarkers, v -> c.maxMarkers = (int) v, "");

        toggle(0, 3, "Ignore own sounds", () -> c.ignoreSelf, v -> c.ignoreSelf = v);
        toggle(1, 3, "Log to chat", () -> c.logToChat, v -> c.logToChat = v);
    }

    private void buildFilters() {
        Config c = cfg();
        Channel[] chs = Channel.values();
        for (int i = 0; i < chs.length; i++) {
            channelRow(i % 2, i / 2, chs[i]);
        }
        int row = (chs.length + 1) / 2;
        EditBox box = new EditBox(this.font, colX(0), rowY(row), W - 2 * PAD, 20, Component.literal("Custom keywords"));
        box.setMaxLength(200);
        box.setHint(Component.literal("Custom keywords, comma separated (e.g. bell,anvil,portal)"));
        box.setValue(c.customKeywords);
        box.setResponder(s -> c.customKeywords = s);
        addRenderableWidget(box);
    }

    private void buildVisuals() {
        Config c = cfg();
        toggle(0, 0, "Boxes", () -> c.boxes, v -> c.boxes = v);
        toggle(1, 0, "Tracers", () -> c.tracers, v -> c.tracers = v);

        slider(0, 1, "Box size", 0.25, 3.0, 0.25, () -> c.boxSize, v -> c.boxSize = (float) v, "");
        slider(1, 1, "Tracer width", 1, 8, 0.5, () -> c.tracerWidth, v -> c.tracerWidth = (float) v, "");

        toggle(0, 2, "Fade out", () -> c.fade, v -> c.fade = v);
        slider(1, 2, "Opacity", 0.1, 1.0, 0.05, () -> c.opacity, v -> c.opacity = (float) v, "");
    }

    // ------------------------------------------------------------------ widget helpers

    private void toggle(int col, int row, String label, BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        Button b = Button.builder(toggleText(label, get.getAsBoolean()), btn -> {
            set.accept(!get.getAsBoolean());
            btn.setMessage(toggleText(label, get.getAsBoolean()));
        }).bounds(colX(col), rowY(row), colW, 20).build();
        addRenderableWidget(b);
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": ").append(
                Component.literal(on ? "ON" : "OFF").withStyle(s -> s.withColor(on ? 0x55FF55 : 0xFF5555)));
    }

    private void cycle(int col, int row, java.util.function.Supplier<String> text, Runnable next) {
        addRenderableWidget(Button.builder(Component.literal(text.get()), btn -> {
            next.run();
            btn.setMessage(Component.literal(text.get()));
        }).bounds(colX(col), rowY(row), colW, 20).build());
    }

    private void channelRow(int col, int row, Channel ch) {
        Config.ChannelCfg cc = cfg().get(ch);
        int toggleW = colW - 34;

        addRenderableWidget(Button.builder(channelText(ch, cc), btn -> {
            cc.enabled = !cc.enabled;
            btn.setMessage(channelText(ch, cc));
        }).bounds(colX(col), rowY(row), toggleW, 20).build());

        addRenderableWidget(Button.builder(swatch(cc.color), btn -> {
            int idx = 0;
            for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == cc.color) idx = i;
            cc.color = PALETTE[(idx + 1) % PALETTE.length];
            btn.setMessage(swatch(cc.color));
        }).bounds(colX(col) + toggleW + 4, rowY(row), 30, 20).build());
    }

    private static Component channelText(Channel ch, Config.ChannelCfg cc) {
        return Component.literal(ch.label).withStyle(s -> s.withColor(cc.enabled ? 0x55FF55 : 0x888888));
    }

    private static Component swatch(int color) {
        return Component.literal("██").withStyle(s -> s.withColor(color));
    }

    private void slider(int col, int row, String label, double min, double max, double step,
                        DoubleSupplier get, DoubleConsumer set, String unit) {
        addRenderableWidget(new ValueSlider(colX(col), rowY(row), colW, 20, label, min, max, step, get, set, unit));
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label, unit;
        private final double min, max, step;
        private final DoubleConsumer set;

        ValueSlider(int x, int y, int w, int h, String label, double min, double max, double step,
                    DoubleSupplier get, DoubleConsumer set, String unit) {
            super(x, y, w, h, Component.empty(), (get.getAsDouble() - min) / (max - min));
            this.label = label; this.unit = unit; this.min = min; this.max = max; this.step = step; this.set = set;
            updateMessage();
        }

        private double actual() {
            double v = min + value * (max - min);
            return Math.max(min, Math.min(max, Math.round(v / step) * step));
        }

        @Override
        protected void updateMessage() {
            double v = actual();
            String s = (v == Math.floor(v)) ? String.valueOf((long) v) : String.format("%.2f", v);
            setMessage(Component.literal(label + ": " + s + unit));
        }

        @Override
        protected void applyValue() {
            set.accept(actual());
        }
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0x99000000); // dim only, no blur
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);

        // panel
        g.fill(px - 1, py - 1, px + W + 1, py + H + 1, 0xFF000000);
        g.fill(px, py, px + W, py + H, 0xFF14171C);
        g.fill(px, py, px + W, py + 24, 0xFF1D2229);
        g.fill(px, py + 24, px + W, py + 26, 0xFF000000 | ACCENT);
        g.drawString(this.font, "Underground Audio Visualizer", px + PAD, py + 8, 0xFF000000 | ACCENT);
        g.drawString(this.font, "Delete / Esc to close", px + W - PAD - this.font.width("Delete / Esc to close"),
                py + 8, 0xFF7F8A99);

        // hint line per tab
        String hint = switch (currentTab) {
            case GENERAL -> "Servers only send sounds within hearing range (usually ~16-48 blocks).";
            case FILTERS -> "Click a name to toggle it, click the swatch to change its color.";
            case VISUALS -> "Tracers draw a line from you to every active marker.";
        };
        g.drawString(this.font, hint, px + PAD, py + H - 42, 0xFF7F8A99);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        cfg().save();
    }
}
