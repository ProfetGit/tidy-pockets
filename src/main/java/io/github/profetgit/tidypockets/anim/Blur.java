package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.mixin.GuiGraphicsExtractorAccessor;
import io.github.profetgit.tidypockets.mixin.GuiRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Vanilla only blurs menus outside the game; this blurs the world behind inventories too. */
public final class Blur {
    private Blur() {}

    public static void containerBackground(Screen s, GuiGraphicsExtractor g) {
        if (!(s instanceof AbstractContainerScreen<?>) || !TidyConfig.get().blur) return;
        if (Minecraft.getInstance().options.getMenuBackgroundBlurriness() < 1) return;
        GuiRenderStateAccessor state = (GuiRenderStateAccessor) ((GuiGraphicsExtractorAccessor) g).tidypockets$state();
        if (state.tidypockets$firstStratumAfterBlur() != Integer.MAX_VALUE) return;
        g.blurBeforeThisStratum();
    }
}
