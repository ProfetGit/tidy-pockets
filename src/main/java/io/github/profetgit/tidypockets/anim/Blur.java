package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Blurs the world behind inventories; before 1.20.5, where the game blurs nothing, behind its in-game menus too (later
 * versions blur those themselves, with our strength: see {@link ScreenPop#blurRadius}).
 */
public final class Blur {
    private Blur() {}

    /** Blurs this mod asked for or drew itself (read by the self-test). */
    public static int drawn;

    /** The inventory blur's radius: our own setting, else the game's Menu Background Blur ({@code gameRadius}). */
    public static int strength(int gameRadius) {
        int own = TidyConfig.get().blurStrength;
        return own > 0 ? own : gameRadius;
    }

    public static void background(Screen s, GuiGraphicsExtractor g, float partialTick) {
        if (!TidyConfig.get().blur) return;
        //? if >=1.20.5 {
        if (!(s instanceof AbstractContainerScreen<?>)) return;
        //?} else {
        /*if (!(s instanceof AbstractContainerScreen<?>) && Minecraft.getInstance().level == null) return;
        *///?}
        //? if >=1.20.5 {
        if (strength(Minecraft.getInstance().options.getMenuBackgroundBlurriness()) < 1) return;
        //?} else {
        /*LegacyBlur.process(g, partialTick, ScreenPop.blurRadius(strength(LegacyBlur.DEFAULT_RADIUS)));
        *///?}
        //? if >=1.21.6 {
        io.github.profetgit.tidypockets.mixin.GuiRenderStateAccessor state = (io.github.profetgit.tidypockets.mixin.GuiRenderStateAccessor)
            ((io.github.profetgit.tidypockets.mixin.GuiGraphicsExtractorAccessor) g).tidypockets$state();
        if (state.tidypockets$firstStratumAfterBlur() != Integer.MAX_VALUE) return;
        g.blurBeforeThisStratum();
        //?}
        //? if >=1.21.2 <1.21.6 {
        /*((io.github.profetgit.tidypockets.mixin.ScreenInvoker) s).tidypockets$blur();
        *///?}
        //? if >=1.20.5 <1.21.2 {
        /*((io.github.profetgit.tidypockets.mixin.ScreenInvoker) s).tidypockets$blur(partialTick);
        *///?}
        //? if >=1.20.5 {
        drawn++;
        //?}
    }
}
