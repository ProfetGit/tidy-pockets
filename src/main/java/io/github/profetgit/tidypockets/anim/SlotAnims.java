package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Per-slot scale animations inside container screens: the sort pop (item hidden, then pops in with a puff), the
 * arrival bump after an item flies in, and a small bump when a stack's count changes.
 */
public final class SlotAnims {
    private record Pop(double start, double delay, boolean puff) {}

    public static final double POP_MS = 90, PUFF_MS = 80, BUMP_MS = 70;
    static final double SHAKE_MS = 180;
    private static final Identifier[] PUFF = new Identifier[4];

    static {
        for (int i = 0; i < 4; i++) PUFF[i] = Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "puff_" + i);
    }

    private static final Map<Slot, Pop> POPS = new WeakHashMap<>();
    private static final Map<Slot, Double> BUMPS = new WeakHashMap<>();
    private static final Map<Slot, Double> SHAKES = new WeakHashMap<>();
    private static final Map<Slot, Integer> COUNTS = new WeakHashMap<>();
    private static final Map<Slot, Boolean> SEEN = new WeakHashMap<>();
    private static boolean pushed;

    private SlotAnims() {}

    /** Real ms as animation ms: the times below are for speed 1. */
    private static double effectMs(double realMs) {
        return realMs * TidyConfig.get().animSpeed;
    }

    /** A popping item's scale, {@code ms} animation ms after the sort: hidden until its delay, then in with an overshoot. */
    public static double popScale(double ms, double delayMs) {
        double t = (ms - delayMs) / POP_MS;
        if (t < 0) return 0;
        return t < 1 ? Math.max(0, Ease.outBack(t)) : 1;
    }

    /** The puff sprite showing at that moment, or -1: it leads its item's pop by 40 ms. */
    public static int puffFrame(double ms, double delayMs) {
        double t = (ms - delayMs + 40) / PUFF_MS;
        return t < 0 || t >= 1 ? -1 : (int) (t * 4);
    }

    /** A bumped item's scale, {@code t} (0..1) of the way through the bump. */
    public static double bumpScale(double t) {
        return 1 + 0.2 * (1 - Ease.outCubic(t));
    }

    public static Identifier puffSprite(int frame) {
        return PUFF[frame];
    }

    public static void pop(Slot s, double delayMs, boolean puff) {
        POPS.put(s, new Pop(Ease.now(), delayMs, puff));
    }

    public static void bump(Slot s) {
        BUMPS.put(s, Ease.now());
    }

    /** A quick side-to-side "no": a locked slot refusing a click. */
    public static void shake(Slot s) {
        SHAKES.put(s, Ease.now());
    }

    private static double shakeOffset(Slot s) {
        Double start = SHAKES.get(s);
        if (start == null || !Ease.enabled()) return 0;
        double t = Ease.progress(start, SHAKE_MS);
        if (t >= 1) {
            SHAKES.remove(s);
            return 0;
        }
        return Math.sin(t * Math.PI * 6) * 2.2 * (1 - t);
    }

    private static double scale(Slot s) {
        if (!Ease.enabled()) return 1;
        double now = Ease.now();
        Pop p = POPS.get(s);
        if (p != null) {
            double ms = effectMs(now - p.start);
            if (ms < p.delay + POP_MS) return popScale(ms, p.delay);
            POPS.remove(s);
        }
        Double b = BUMPS.get(s);
        if (b != null) {
            double t = Ease.progress(b, BUMP_MS);
            if (t < 1) return bumpScale(t);
            BUMPS.remove(s);
        }
        return 1;
    }

    private static void trackCount(Slot s) {
        int count = s.getItem().getCount();
        Integer before = COUNTS.put(s, count);
        if (SEEN.put(s, true) == null) return;
        if (before != null && before != count && count > 0 && TidyConfig.get().animCountBump && !POPS.containsKey(s)) bump(s);
    }

    /** Before vanilla draws the slot's item. Returns false to skip drawing it (fully shrunk). */
    public static boolean before(GuiGraphicsExtractor g, Slot s) {
        pushed = false;
        trackCount(s);
        double k = scale(s), dx = shakeOffset(s);
        if (k <= 0.02) return false;
        ItemStack under = FlyAnims.under(s);
        if (under != null) {
            if (!under.isEmpty()) {
                g.item(under, s.x, s.y);
                g.itemDecorations(Minecraft.getInstance().font, under, s.x, s.y);
            }
            return false;
        }
        if (k != 1 || dx != 0) {
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) (s.x + 8 + dx), s.y + 8);
            pose.scale((float) k, (float) k);
            pose.translate(-s.x - 8, -s.y - 8);
            pushed = true;
        }
        return true;
    }

    /** Whether a slot {@link #before} skipped still shows its old stack, under an item flying onto it. */
    public static boolean showsUnderFlight(Slot s) {
        ItemStack under = FlyAnims.under(s);
        return under != null && !under.isEmpty();
    }

    public static void after(GuiGraphicsExtractor g, Slot s) {
        if (pushed) g.pose().popMatrix();
        pushed = false;
    }

    /** Draws the puff for slots in a sort pop; called whether or not the item itself was drawn. */
    public static void puff(GuiGraphicsExtractor g, Slot s) {
        Pop p = POPS.get(s);
        if (p == null || !p.puff || !Ease.enabled()) return;
        int frame = puffFrame(effectMs(Ease.now() - p.start), p.delay);
        if (frame < 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PUFF[frame], s.x, s.y, 16, 16);
    }

    public static void clear() {
        POPS.clear();
        BUMPS.clear();
        COUNTS.clear();
        SEEN.clear();
        SHAKES.clear();
    }
}
