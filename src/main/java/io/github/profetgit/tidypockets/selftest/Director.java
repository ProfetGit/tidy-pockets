package io.github.profetgit.tidypockets.selftest;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.mixin.MouseHandlerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Plays a scripted scene for the showcase clips: a visible cursor glides between points (vanilla hover follows it),
 * cues fire clicks, scrolls and keys at set times, the camera can pan, and a caption plus input hints are drawn on
 * top. Everything is in real milliseconds so the recorded frames can be resampled to any frame rate.
 */
public final class Director {
    public interface Target {
        float[] at(Minecraft mc);
    }

    private static final class Move {
        final double t0, t1;
        final Target to;
        float[] from, end;

        Move(double t0, double t1, Target to) {
            this.t0 = t0;
            this.t1 = t1;
            this.to = to;
        }
    }

    private record Look(double t0, double t1, Function<Minecraft, float[]> yawPitch) {}

    private record Cue(double t, String badge, Consumer<Minecraft> run) {}

    public static final class Timeline {
        final String name, caption;
        final double duration;
        final List<Move> moves = new ArrayList<>();
        final List<Look> looks = new ArrayList<>();
        final List<Cue> cues = new ArrayList<>();
        Function<Minecraft, int[]> crop;
        boolean cursor = true, captionTop;
        int stride = 1;
        Consumer<Minecraft> perTick;

        public Timeline(String name, String caption, double duration) {
            this.name = name;
            this.caption = caption;
            this.duration = duration;
        }

        public Timeline crop(Function<Minecraft, int[]> guiRect) {
            crop = guiRect;
            return this;
        }

        public Timeline captionTop() {
            captionTop = true;
            return this;
        }

        public Timeline noCursor() {
            cursor = false;
            return this;
        }

        public Timeline stride(int n) {
            stride = n;
            return this;
        }

        public Timeline everyTick(Consumer<Minecraft> run) {
            perTick = run;
            return this;
        }

        public Timeline move(double t0, double t1, Target to) {
            moves.add(new Move(t0, t1, to));
            return this;
        }

        public Timeline look(double t0, double t1, Function<Minecraft, float[]> yawPitch) {
            looks.add(new Look(t0, t1, yawPitch));
            return this;
        }

        public Timeline at(double t, String badge, Consumer<Minecraft> run) {
            cues.add(new Cue(t, badge, run));
            return this;
        }

        public Timeline click(double t, int button, int mods, String badge) {
            return at(t, badge, mc -> {
                doPress(mc, button, mods);
                doRelease(mc);
            });
        }

        public Timeline press(double t, int button, int mods, String badge) {
            return at(t, badge, mc -> doPress(mc, button, mods));
        }

        public Timeline release(double t) {
            return at(t, null, Director::doRelease);
        }

        public Timeline scroll(double t, double amount, String badge) {
            return at(t, badge, mc -> {
                if (mc.gui.screen() != null) mc.gui.screen().mouseScrolled(cx, cy, 0, amount);
                clickAt = now();
            });
        }
    }

    private static Timeline active;
    private static double start;
    private static float cx = 300, cy = 120;
    private static int cueIndex, lookIndex = -1;
    private static float[] lookFrom;
    private static long lastTick = Long.MIN_VALUE;
    private static String badge;
    private static double badgeAt = -1e9, clickAt = -1e9;
    private static int dragButton = -1, dragMods;
    private static boolean done;

    private Director() {}

    static double now() {
        return System.nanoTime() / 1_000_000.0;
    }

    public static void begin(Minecraft mc, Timeline t) {
        active = t;
        start = now();
        cueIndex = 0;
        lookIndex = -1;
        done = false;
        badge = null;
        for (Move m : t.moves) m.from = m.end = null;
        int[] px = null;
        if (t.crop != null) {
            int[] r = t.crop.apply(mc);
            int s = mc.getWindow().getGuiScale();
            px = new int[] {r[0] * s, r[1] * s, r[2] * s, r[3] * s};
        }
        SelfTest.startRegionCapture(t.name, px, t.stride);
        TidyPockets.LOG.info("[showcase] recording {}", t.name);
    }

    public static boolean finished(Timeline t) {
        return done && active == null;
    }

    public static boolean active() {
        return active != null;
    }

    private static MouseButtonEvent event(int button, int mods) {
        return new MouseButtonEvent(cx, cy, new MouseButtonInfo(button, mods));
    }

    private static void doPress(Minecraft mc, int button, int mods) {
        Screen s = mc.gui.screen();
        if (s != null) s.mouseClicked(event(button, mods), false);
        dragButton = button;
        dragMods = mods;
        clickAt = now();
    }

    private static void doRelease(Minecraft mc) {
        Screen s = mc.gui.screen();
        if (s != null && dragButton >= 0) s.mouseReleased(event(dragButton, dragMods));
        dragButton = -1;
    }

    public static void frame(Minecraft mc) {
        if (active == null) return;
        double t = now() - start;
        float px = cx, py = cy;
        for (Move m : active.moves) {
            if (t < m.t0) break;
            if (m.from == null) {
                m.from = new float[] {cx, cy};
                m.end = m.to.at(mc);
            }
            double k = m.t1 <= m.t0 ? 1 : Math.clamp((t - m.t0) / (m.t1 - m.t0), 0, 1);
            k = k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2;
            cx = (float) (m.from[0] + (m.end[0] - m.from[0]) * k);
            cy = (float) (m.from[1] + (m.end[1] - m.from[1]) * k);
        }
        if (active.cursor && mc.gui.screen() != null) {
            int s = mc.getWindow().getGuiScale();
            MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
            mouse.tidypockets$setX(cx * s);
            mouse.tidypockets$setY(cy * s);
            if (dragButton >= 0 && (px != cx || py != cy)) mc.gui.screen().mouseDragged(event(dragButton, dragMods), cx - px, cy - py);
        }
        int li = -1;
        for (int i = 0; i < active.looks.size(); i++) if (t >= active.looks.get(i).t0) li = i;
        if (li >= 0 && mc.player != null) {
            Look l = active.looks.get(li);
            if (li != lookIndex) {
                lookIndex = li;
                lookFrom = new float[] {mc.player.getYRot(), mc.player.getXRot()};
            }
            float[] to = l.yawPitch.apply(mc);
            double k = l.t1 <= l.t0 ? 1 : Math.clamp((t - l.t0) / (l.t1 - l.t0), 0, 1);
            k = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
            float dyaw = ((to[0] - lookFrom[0]) % 360 + 540) % 360 - 180;
            float yaw = (float) (lookFrom[0] + dyaw * k), pitch = (float) (lookFrom[1] + (to[1] - lookFrom[1]) * k);
            mc.player.setYRot(yaw);
            mc.player.yRotO = yaw;
            mc.player.setXRot(pitch);
            mc.player.xRotO = pitch;
            mc.player.setYHeadRot(yaw);
        }
        while (cueIndex < active.cues.size() && active.cues.get(cueIndex).t <= t) {
            Cue c = active.cues.get(cueIndex++);
            if (c.badge != null) {
                badge = c.badge;
                badgeAt = now();
            }
            c.run.accept(mc);
        }
        if (active.perTick != null && mc.level != null && mc.level.getGameTime() != lastTick) {
            lastTick = mc.level.getGameTime();
            active.perTick.accept(mc);
        }
        if (t >= active.duration) {
            if (dragButton >= 0) doRelease(mc);
            SelfTest.stopRegionCapture();
            active = null;
            done = true;
        }
    }

    // ---- overlay ----

    private static final String[] ARROW = {
        "K", "KK", "KWK", "KWWK", "KWWWK", "KWWWWK", "KWWWWWK", "KWWWWWWK", "KWWWWWWWK", "KWWWWWWWWK", "KWWWWWKKKKK",
        "KWWKWWK", "KWK KWWK", "KK  KWWK", "K    KWWK", "     KWWK", "      KK"};
    private static final Identifier[] PUFF = new Identifier[4];

    static {
        for (int i = 0; i < 4; i++) PUFF[i] = Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "puff_" + i);
    }

    /** Called on top of everything: from the screen when one is open, else from the HUD. */
    public static void overlay(GuiGraphicsExtractor g, boolean fromScreen) {
        Minecraft mc = Minecraft.getInstance();
        if (active == null || fromScreen != (mc.gui.screen() != null)) return;
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        if (active.caption != null) {
            int bottom = h - 8;
            if (active.crop != null) {
                int[] r = active.crop.apply(mc);
                bottom = active.captionTop ? r[1] + 21 : r[1] + r[3] - 5;
            } else if (mc.gui.screen() == null) {
                bottom = h - 50;
            }
            int tw = font.width(active.caption), x = w / 2 - tw / 2, y = bottom - 13;
            if (active.crop != null) x = active.crop.apply(mc)[0] + active.crop.apply(mc)[2] / 2 - tw / 2;
            g.fill(x - 6, y - 3, x + tw + 6, y + 11, 0xE0141420);
            g.outline(x - 7, y - 4, tw + 14, 16, 0xFF4A4A66);
            g.text(font, active.caption, x, y, 0xFFFFFFFF, true);
        }
        double since = now() - badgeAt;
        if (badge != null && since < 1100) {
            int alpha = since < 800 ? 255 : (int) (255 * (1 - (since - 800) / 300));
            int tw = font.width(badge);
            int bx = (int) cx + 9, by = (int) cy + 11;
            if (active.crop != null) {
                int[] r = active.crop.apply(mc);
                if (bx + tw + 4 > r[0] + r[2]) bx = (int) cx - tw - 6;
            }
            if (!active.cursor || mc.gui.screen() == null) {
                bx = w / 2 - tw / 2;
                by = h / 2 + 16;
            }
            g.fill(bx - 3, by - 2, bx + tw + 3, by + 10, (Math.min(alpha, 220) << 24) | 0x141420);
            g.text(font, badge, bx, by, (alpha << 24) | 0xFFD84A, true);
        }
        if (!active.cursor || mc.gui.screen() == null) return;
        double click = (now() - clickAt) / 160.0;
        if (click >= 0 && click < 1) g.blitSprite(RenderPipelines.GUI_TEXTURED, PUFF[(int) (click * 4)], (int) cx - 8, (int) cy - 8, 16, 16);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        float s = 1.6f / mc.getWindow().getGuiScale();
        pose.scale(s, s);
        for (int y = 0; y < ARROW.length; y++) {
            String row = ARROW[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == 'K') g.fill(x, y, x + 1, y + 1, 0xFF101010);
                else if (c == 'W') g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
            }
        }
        pose.popMatrix();
    }
}
