package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Vanilla only blurs menus outside the game; this blurs the world behind inventories too. */
public final class Blur {
    private Blur() {}

    public static void containerBackground(Screen s, GuiGraphicsExtractor g, float partialTick) {
        if (!(s instanceof AbstractContainerScreen<?>) || !TidyConfig.get().blur) return;
        if (Minecraft.getInstance().options.getMenuBackgroundBlurriness() < 1) return;
        //? if >=1.21.6 {
        io.github.profetgit.tidypockets.mixin.GuiRenderStateAccessor state = (io.github.profetgit.tidypockets.mixin.GuiRenderStateAccessor)
            ((io.github.profetgit.tidypockets.mixin.GuiGraphicsExtractorAccessor) g).tidypockets$state();
        if (state.tidypockets$firstStratumAfterBlur() != Integer.MAX_VALUE) return;
        g.blurBeforeThisStratum();
        //?}
        //? if >=1.21.2 <1.21.6 {
        /*((io.github.profetgit.tidypockets.mixin.ScreenInvoker) s).tidypockets$blur();
        *///?}
        //? if <1.21.2 {
        /*((io.github.profetgit.tidypockets.mixin.ScreenInvoker) s).tidypockets$blur(partialTick);
        *///?}
    }
}
