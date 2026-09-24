package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.Map;
import java.util.WeakHashMap;
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

    static final double POP_MS = 90, PUFF_MS = 80, BUMP_MS = 70, SHAKE_MS = 180;
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
            double t = (now - p.start - Ease.scaled(p.delay)) / Ease.scaled(POP_MS);
            if (t < 0) return 0;
            if (t < 1) return Math.max(0, Ease.outBack(t));
            POPS.remove(s);
        }
        Double b = BUMPS.get(s);
        if (b != null) {
            double t = Ease.progress(b, BUMP_MS);
            if (t < 1) return 1 + 0.2 * (1 - Ease.outCubic(t));
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

    public static void after(GuiGraphicsExtractor g, Slot s) {
        if (pushed) g.pose().popMatrix();
        pushed = false;
    }

    /** Draws the puff for slots in a sort pop; called whether or not the item itself was drawn. */
    public static void puff(GuiGraphicsExtractor g, Slot s) {
        Pop p = POPS.get(s);
        if (p == null || !p.puff || !Ease.enabled()) return;
        double t = (Ease.now() - p.start - Ease.scaled(p.delay) + Ease.scaled(40)) / Ease.scaled(PUFF_MS);
        if (t < 0 || t >= 1) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PUFF[(int) (t * 4)], s.x, s.y, 16, 16);
    }

    public static void clear() {
        POPS.clear();
        BUMPS.clear();
        COUNTS.clear();
        SEEN.clear();
        SHAKES.clear();
    }
}
