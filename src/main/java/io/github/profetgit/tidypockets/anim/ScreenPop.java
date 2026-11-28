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
 * Closing is instant. The scale goes on the GUI pose, or through {@link ViewScale} while a resource pack replaces the
 * GUI shaders; where that isn't possible the pop stays off.
 */
public final class ScreenPop {
    public static final double POP_MS = 120;
    private static Screen current;
    private static double start;
    /** What a push left behind: nothing, a transform on the pose or on the view, or one that cancels the view's scale. */
    private enum Layer { NONE, POSE, VIEW, VIEW_CANCEL }

    private static final Deque<Layer> PUSHED = new ArrayDeque<>();
    private static final Deque<Layer> UNSCALED = new ArrayDeque<>();
    private static GuiGraphicsExtractor popped;
    private static float poppedScale, poppedX, poppedY;
    /** 3D pictures given the pop pose so far (read by the self-test). */
    public static int posedPictures;

    private ScreenPop() {}

    public static void opened(Screen s) {
        if (s == current) return;
        current = s;
        start = Ease.now();
        if (s != null) Conflicts.checkGuiShaders();
        SlotAnims.clear();
        FlyAnims.clear();
    }

    private static boolean applies(Screen s) {
        if (s == null || s != current || Conflicts.screenPop || !Ease.enabled()) return false;
        if (Conflicts.guiShaders && !ViewScale.SUPPORTED) return false;
        TidyConfig cfg = TidyConfig.get();
        if (s instanceof AbstractContainerScreen<?>) return cfg.animScreenPop;
        return cfg.animAllScreens && !(s instanceof TitleScreen) && !(s instanceof ChatScreen);
    }

    public static double progress(Screen s) {
        return applies(s) ? Ease.progress(start, POP_MS) : 1;
    }

    /** Whether the open screen is mid-pop. */
    public static boolean popping() {
        Screen s = Minecraft.getInstance().gui.screen();
        return s != null && scale(s) != 1;
    }

    /** Current scale of a screen, 1 when not popping. */
    public static double scale(Screen s) {
        return scaleAt(progress(s));
    }

    /** A popping screen's scale, {@code p} (0..1) of the way through the pop. */
    public static double scaleAt(double p) {
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
            PUSHED.push(Layer.NONE);
            return;
        }
        popped = g;
        poppedScale = (float) k;
        poppedX = pivotX(s);
        poppedY = pivotY(s);
        Layer layer = Conflicts.guiShaders ? Layer.VIEW : Layer.POSE;
        transform(layer, g, poppedScale);
        PUSHED.push(layer);
    }

    private static void transform(Layer layer, GuiGraphicsExtractor g, float k) {
        if (layer == Layer.POSE) {
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate(poppedX, poppedY);
            pose.scale(k, k);
            pose.translate(-poppedX, -poppedY);
        } else {
            ViewScale.push(g, k, poppedX, poppedY);
        }
    }

    private static void undo(Layer layer, GuiGraphicsExtractor g) {
        if (layer == Layer.POSE) g.pose().popMatrix();
        else if (layer != Layer.NONE) ViewScale.pop(g);
    }

    private static Layer scaled() {
        Layer top = PUSHED.peek();
        return top == Layer.POSE || top == Layer.VIEW ? top : Layer.NONE;
    }

    /** Cancel a pop for another mod's overlay drawn in the middle of the scaled parts; always pair with {@link #popUnscaled}. */
    public static void pushUnscaled() {
        Layer active = scaled();
        if (active != Layer.NONE) transform(active, popped, 1 / poppedScale);
        PUSHED.push(Layer.NONE);
        UNSCALED.push(active);
    }

    public static void popUnscaled() {
        if (UNSCALED.isEmpty()) return;
        PUSHED.pop();
        undo(UNSCALED.pop(), popped);
    }

    /** A balanced no-op for {@link #pop}. */
    public static void pushNone() {
        PUSHED.push(Layer.NONE);
    }

    /** Undo the pop transform for parts that must not scale (background dim, blur). */
    public static void pushIdentity(Screen s, GuiGraphicsExtractor g) {
        Layer active = scaled();
        if (active == Layer.POSE) {
            g.pose().pushMatrix().identity();
            PUSHED.push(Layer.POSE);
        } else if (active == Layer.VIEW) {
            ViewScale.push(g, 1 / poppedScale, poppedX, poppedY);
            PUSHED.push(Layer.VIEW_CANCEL);
        } else {
            PUSHED.push(Layer.NONE);
        }
    }

    public static void pop(GuiGraphicsExtractor g) {
        if (!PUSHED.isEmpty()) undo(PUSHED.pop(), g);
    }

    /** The blur radius to use this frame: in a world, screens take our strength; containers fade it in with the pop. */
    public static int blurRadius(int radius) {
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.gui.screen();
        if (s == null || mc.level == null || !TidyConfig.get().blur) return radius;
        int full = Blur.strength(radius);
        if (!(s instanceof AbstractContainerScreen<?>)) return full;
        double p = progress(s);
        return p >= 1 ? full : (int) Math.round(full * Ease.outCubic(p));
    }
}
