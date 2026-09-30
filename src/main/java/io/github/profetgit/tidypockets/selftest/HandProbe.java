package io.github.profetgit.tidypockets.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Reads what the first-person hand is showing, for the tests. The renderer moved to the player in 26.3. */
final class HandProbe {
    private HandProbe() {}

    private static Object renderer(Minecraft mc) {
        //? if >=26.3 {
        return mc.player.firstPersonHandsAndItems();
        //?} else {
        /*return mc.gameRenderer.itemInHandRenderer;
        *///?}
    }

    /** How far the main hand is raised: 0 lowered out of view, 1 in place. */
    static float height(Minecraft mc) {
        return ((io.github.profetgit.tidypockets.mixin.HandRendererAccessor) renderer(mc)).tidypockets$height();
    }

    /** The stack the hand draws right now. */
    static ItemStack shown(Minecraft mc) {
        return ((io.github.profetgit.tidypockets.mixin.HandRendererAccessor) renderer(mc)).tidypockets$item();
    }
}
