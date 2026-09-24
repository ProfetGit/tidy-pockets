package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;

/**
 * The creative grid still scrolls row by row (that is what the menu holds), but each row change slides the grid in
 * from the scroll direction instead of jumping.
 */
public final class CreativeSlide {
    static final double SLIDE_MS = 90;
    private static double start = -1e9;
    private static int direction;
    private static boolean active;

    private CreativeSlide() {}

    public static void rowChanged(int delta) {
        if (!TidyConfig.get().smoothScroll || Conflicts.scroll || !Ease.enabled() || delta == 0) return;
        start = Ease.now();
        direction = Integer.signum(delta);
    }

    /**
     * The slot grid is part of the tab's background texture, so it would stay still under sliding items. While a
     * slide runs, a matching grid (vanilla slot colours) is drawn over it, moving with the items.
     */
    public static void grid(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
        if (!(screen instanceof CreativeModeInventoryScreen)) return;
        double t = Ease.progress(start, SLIDE_MS);
        if (t >= 1) return;
        int dy = (int) Math.round(direction * 18 * (1 - Ease.outCubic(t)));
        g.enableScissor(8, 17, 8 + 162, 17 + 90);
        for (int row = -1; row <= 5; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + col * 18, y = 17 + row * 18 + dy;
                g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
                g.fill(x, y, x + 17, y + 1, 0xFF373737);
                g.fill(x, y, x + 1, y + 17, 0xFF373737);
                g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
                g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
            }
        }
        g.disableScissor();
    }

    public static void before(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, Slot slot) {
        active = false;
        if (!(screen instanceof CreativeModeInventoryScreen) || Minecraft.getInstance().player == null
            || slot.container == Minecraft.getInstance().player.getInventory()) return;
        double t = Ease.progress(start, SLIDE_MS);
        if (t >= 1) return;
        float dy = (float) (direction * 18 * (1 - Ease.outCubic(t)));
        g.enableScissor(9, 18, 9 + 162, 18 + 90);
        g.pose().pushMatrix();
        g.pose().translate(0, dy);
        active = true;
    }

    public static void after(GuiGraphicsExtractor g) {
        if (!active) return;
        g.pose().popMatrix();
        g.disableScissor();
        active = false;
    }
}
