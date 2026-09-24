package io.github.profetgit.tidypockets.screen;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

/** Draws on top of each slot: the padlock for locked slots. */
public final class SlotOverlay {
    private static final Identifier LOCK = Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "lock");

    private SlotOverlay() {}

    public static void afterSlot(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, Slot slot) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        String q = ContainerTools.query(screen);
        if (q != null) {
            if (ContainerTools.matches(slot.getItem(), q)) {
                g.outline(slot.x - 1, slot.y - 1, 18, 18, 0xFFFFD84A);
            } else {
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0202020);
            }
        }
        if (Inv.isPlayerSlot(slot, p) && SlotLocks.isLocked(Inv.index(slot))) {
            if (!slot.hasItem()) {
                var remembered = SlotLocks.remembered(Inv.index(slot));
                if (remembered != null) {
                    g.item(new net.minecraft.world.item.ItemStack(remembered), slot.x, slot.y);
                    g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xA88B8B8B);
                }
            }
            g.blitSprite(RenderPipelines.GUI_TEXTURED, LOCK, slot.x + 10, slot.y - 1, 7, 8);
        }
    }
}
