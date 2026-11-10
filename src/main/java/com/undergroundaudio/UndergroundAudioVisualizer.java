package com.undergroundaudio;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.undergroundaudio.gui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Underground Audio Visualizer: draws fading boxes (and tracers) where selected sounds were played.
 * Press Delete to open the settings GUI (rebindable in Controls).
 */
public final class UndergroundAudioVisualizer implements ClientModInitializer {
    public static Config CONFIG = new Config();

    private static KeyMapping openKey;

    private static final class Marker {
        final Channel channel;
        final int color;
        final double x, y, z;
        long time;
        Marker(Channel channel, int color, double x, double y, double z, long time) {
            this.channel = channel; this.color = color; this.x = x; this.y = y; this.z = z; this.time = time;
        }
    }

    private static final List<Marker> MARKERS = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.uav.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                mc.setScreen(new ConfigScreen(mc.screen));
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(UndergroundAudioVisualizer::onRender3D);
    }

    public static void clearMarkers() {
        MARKERS.clear();
    }

    // ------------------------------------------------------------------ packet side

    /** Called from the mixin. The packet handler runs once on the netty thread and once on the main thread; only the main one counts. */
    public static void onSound(ClientboundSoundPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;
        Config c = CONFIG;
        if (!c.enabled || mc.player == null) return;

        String id = packet.getSound().value().location().getPath();
        double x = packet.getX(), y = packet.getY(), z = packet.getZ();

        Vec3 me = mc.player.position();
        double dx = x - me.x, dy = y - me.y, dz = z - me.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > c.maxDistance) return;
        if (c.ignoreSelf && dist < 2.5) return;

        switch (c.yMode) {
            case UNDERGROUND -> { if (!(y < me.y - c.yOffset)) return; }
            case ABOVE -> { if (!(y > me.y + c.yOffset)) return; }
            case ANY -> { }
        }

        Channel channel = classify(id, c);
        if (channel == null) return;

        long now = System.currentTimeMillis();
        int color = c.get(channel).color;

        // refresh an existing marker at (almost) the same spot instead of stacking duplicates
        for (Marker m : MARKERS) {
            if (m.channel == channel && Math.abs(m.x - x) < 0.75 && Math.abs(m.y - y) < 0.75 && Math.abs(m.z - z) < 0.75) {
                m.time = now;
                return;
            }
        }
        MARKERS.add(new Marker(channel, color, x, y, z, now));
        while (MARKERS.size() > Math.max(1, c.maxMarkers)) MARKERS.remove(0);

        if (c.logToChat) {
            mc.player.displayClientMessage(Component.literal(String.format(
                    "[UAV] %s  %s  at %.0f %.0f %.0f  (%.0fm)", channel.label, id, x, y, z, dist)), false);
        }
    }

    private static Channel classify(String id, Config c) {
        for (Channel ch : Channel.values()) {
            if (!c.get(ch).enabled) continue;
            if (ch == Channel.CUSTOM) {
                if (matchesCustom(id, c.customKeywords)) return ch;
            } else if (ch.matches(id)) {
                return ch;
            }
        }
        return null;
    }

    private static boolean matchesCustom(String id, String keywords) {
        if (keywords == null || keywords.isBlank()) return false;
        for (String k : keywords.toLowerCase().split(",")) {
            k = k.trim();
            if (!k.isEmpty() && id.contains(k)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ render side

    private static void onRender3D(WorldRenderContext ctx) {
        if (MARKERS.isEmpty()) return;
        Config c = CONFIG;
        Minecraft mc = Minecraft.getInstance();
        if (!c.enabled || mc.player == null || (!c.boxes && !c.tracers)) return;

        long now = System.currentTimeMillis();
        long durationMs = (long) (c.duration * 1000f);

        PoseStack matrices = ctx.matrices();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 look = mc.player.getViewVector(1.0f);
        PoseStack.Pose pose = matrices.last();
        VertexConsumer lines = ctx.consumers().getBuffer(RenderType.lines());

        Iterator<Marker> it = MARKERS.iterator();
        while (it.hasNext()) {
            Marker m = it.next();
            long age = now - m.time;
            if (age >= durationMs) { it.remove(); continue; }

            float a = c.opacity * (c.fade ? 1.0f - (float) age / durationMs : 1.0f);
            int argb = ((int) (Math.max(0f, Math.min(1f, a)) * 255f) << 24) | (m.color & 0xFFFFFF);

            float cx = (float) (m.x - cam.x), cy = (float) (m.y - cam.y), cz = (float) (m.z - cam.z);

            if (c.boxes) {
                float h = c.boxSize / 2f;
                box(lines, pose, cx - h, cy - h, cz - h, cx + h, cy + h, cz + h, argb, 2.0f);
            }
            if (c.tracers) {
                line(lines, pose, (float) look.x, (float) look.y, (float) look.z, cx, cy, cz, argb, c.tracerWidth);
            }
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p,
                            float x1, float y1, float z1, float x2, float y2, float z2, int argb, float w) {
        // bottom
        line(vc, p, x1, y1, z1, x2, y1, z1, argb, w);
        line(vc, p, x2, y1, z1, x2, y1, z2, argb, w);
        line(vc, p, x2, y1, z2, x1, y1, z2, argb, w);
        line(vc, p, x1, y1, z2, x1, y1, z1, argb, w);
        // top
        line(vc, p, x1, y2, z1, x2, y2, z1, argb, w);
        line(vc, p, x2, y2, z1, x2, y2, z2, argb, w);
        line(vc, p, x2, y2, z2, x1, y2, z2, argb, w);
        line(vc, p, x1, y2, z2, x1, y2, z1, argb, w);
        // verticals
        line(vc, p, x1, y1, z1, x1, y2, z1, argb, w);
        line(vc, p, x2, y1, z1, x2, y2, z1, argb, w);
        line(vc, p, x2, y1, z2, x2, y2, z2, argb, w);
        line(vc, p, x1, y1, z2, x1, y2, z2, argb, w);
    }

    private static void line(VertexConsumer vc, PoseStack.Pose p,
                             float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-6f) return;
        nx /= len; ny /= len; nz /= len;
        // if the next two lines fail to compile on your build, drop .setLineWidth(width)
        vc.addVertex(p, x1, y1, z1).setColor(argb).setNormal(p, nx, ny, nz).setLineWidth(width);
        vc.addVertex(p, x2, y2, z2).setColor(argb).setNormal(p, nx, ny, nz).setLineWidth(width);
    }
}
