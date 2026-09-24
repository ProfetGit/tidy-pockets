package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.joml.Matrix3x2fStack;

/**
 * Screens pop open: scale 0.92 to 1 with a small overshoot. For containers only the panel scales (the dark
 * background, blur, tooltips, the carried item and other mods' overlays stay put); the blur fades in alongside.
 * Closing is instant.
 */
public final class ScreenPop {
    static final double POP_MS = 120;
    private static Screen current;
    private static double start;
    private static final Deque<Boolean> PUSHED = new ArrayDeque<>();

    private ScreenPop() {}

    public static void opened(Screen s) {
        if (s == current) return;
        current = s;
        start = Ease.now();
        SlotAnims.clear();
        FlyAnims.clear();
    }

    private static boolean applies(Screen s) {
        if (s == null || s != current || Conflicts.screenPop || !Ease.enabled()) return false;
        TidyConfig cfg = TidyConfig.get();
        if (s instanceof AbstractContainerScreen<?>) return cfg.animScreenPop;
        return cfg.animAllScreens && !(s instanceof TitleScreen) && !(s instanceof ChatScreen);
    }

    public static double progress(Screen s) {
        return applies(s) ? Ease.progress(start, POP_MS) : 1;
    }

    /** Current scale of a screen, 1 when not popping. */
    public static double scale(Screen s) {
        double p = progress(s);
        return p >= 1 ? 1 : 0.92 + 0.08 * Ease.outBack(p);
    }

    public static float pivotX(Screen s) {
        if (s instanceof AbstractContainerScreen<?> c) {
            AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) c;
            return a.tidypockets$left() + a.tidypockets$width() / 2f;
        }
        return s.width / 2f;
    }

    public static float pivotY(Screen s) {
        if (s instanceof AbstractContainerScreen<?> c) {
            AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) c;
            return a.tidypockets$top() + a.tidypockets$height() / 2f;
        }
        return s.height / 2f;
    }

    /** Push the pop transform (or nothing); always pair with {@link #pop}. */
    public static void push(Screen s, GuiGraphicsExtractor g) {
        double k = scale(s);
        if (k == 1) {
            PUSHED.push(false);
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float px = pivotX(s), py = pivotY(s);
        pose.translate(px, py);
        pose.scale((float) k, (float) k);
        pose.translate(-px, -py);
        PUSHED.push(true);
    }

    /** A balanced no-op for {@link #pop}. */
    public static void pushNone() {
        PUSHED.push(false);
    }

    /** Undo the pop transform for parts that must not scale (background dim, blur). */
    public static void pushIdentity(Screen s, GuiGraphicsExtractor g) {
        boolean active = !PUSHED.isEmpty() && PUSHED.peek();
        if (active) g.pose().pushMatrix().identity();
        PUSHED.push(active);
    }

    public static void pop(GuiGraphicsExtractor g) {
        if (!PUSHED.isEmpty() && PUSHED.pop()) g.pose().popMatrix();
    }

    /** The blur radius to use this frame: fades in with the pop for container screens. */
    public static int blurRadius(int radius) {
        Screen s = Minecraft.getInstance().gui.screen();
        if (!(s instanceof AbstractContainerScreen<?>) || !TidyConfig.get().blur) return radius;
        double p = progress(s);
        return p >= 1 ? radius : (int) Math.round(radius * Ease.outCubic(p));
    }
}
