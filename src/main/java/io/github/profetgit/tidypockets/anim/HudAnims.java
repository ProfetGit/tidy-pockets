package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/** Hotbar: the selector glides between slots, refilled slots pop, and tool protection shakes the slot red. */
public final class HudAnims {
    static final double GLIDE_MS = 70, POP_MS = 130, SHAKE_MS = 220;
    private static final Map<Integer, Double> POPS = new HashMap<>(), SHAKES = new HashMap<>();
    private static int lastSel = -1;
    private static double fromX, glideStart;
    private static double lastDrawnX;
    private static boolean pushed;
    private static int currentSlot = -1;

    private HudAnims() {}

    private static boolean on() {
        return TidyConfig.get().animHotbar && Ease.enabled();
    }

    public static void pop(int invIndex) {
        POPS.put(invIndex, Ease.now());
    }

    public static void shake(int invIndex) {
        SHAKES.put(invIndex, Ease.now());
    }

    /** Horizontal offset in pixels for the selection sprite, which vanilla draws at the selected slot. */
    public static float selectorOffset() {
        int sel = Minecraft.getInstance().player == null ? -1 : Minecraft.getInstance().player.getInventory().getSelectedSlot();
        if (sel != lastSel) {
            fromX = lastSel < 0 ? sel : lastDrawnX;
            glideStart = Ease.now();
            lastSel = sel;
        }
        double t = on() ? Ease.progress(glideStart, GLIDE_MS) : 1;
        double drawn = t >= 1 ? sel : fromX + (sel - fromX) * Ease.outCubic(t);
        lastDrawnX = drawn;
        return (float) ((drawn - sel) * 20);
    }

    private static int slotOf(Player p, ItemStack stack) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) if (inv.getItem(i) == stack) return i;
        return inv.getItem(Inventory.SLOT_OFFHAND) == stack ? Inventory.SLOT_OFFHAND : -1;
    }

    public static void beforeSlot(GuiGraphicsExtractor g, int x, int y, Player p, ItemStack stack) {
        pushed = false;
        currentSlot = -1;
        if (!on() || p == null) return;
        int i = slotOf(p, stack);
        currentSlot = i;
        if (i < 0) return;
        double k = 1, dx = 0;
        Double popStart = POPS.get(i);
        if (popStart != null) {
            double t = Ease.progress(popStart, POP_MS);
            if (t >= 1) POPS.remove(i);
            else k = 0.6 + 0.4 * Ease.outBack(t);
        }
        Double shakeStart = SHAKES.get(i);
        if (shakeStart != null) {
            double t = Ease.progress(shakeStart, SHAKE_MS);
            if (t >= 1) SHAKES.remove(i);
            else dx = Math.sin(t * Math.PI * 6) * 2.2 * (1 - t);
        }
        if (k == 1 && dx == 0) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) (x + 8 + dx), y + 8);
        pose.scale((float) k, (float) k);
        pose.translate(-x - 8, -y - 8);
        pushed = true;
    }

    public static void afterSlot(GuiGraphicsExtractor g, int x, int y) {
        if (pushed) g.pose().popMatrix();
        pushed = false;
        Double shakeStart = currentSlot < 0 ? null : SHAKES.get(currentSlot);
        if (shakeStart != null) {
            double t = Ease.progress(shakeStart, SHAKE_MS);
            int a = (int) (110 * (1 - t));
            if (a > 0) g.fill(x, y, x + 16, y + 16, (a << 24) | 0xE02020);
        }
    }
}
