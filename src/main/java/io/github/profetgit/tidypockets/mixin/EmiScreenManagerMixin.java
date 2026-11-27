package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Before 1.21.6 the screen pop wraps the whole container render, and EMI draws its side panels and search box from inside
 * it: they must not scale with the panel. (EMI has no builds for newer versions.)
 */
//? if >=1.21.6 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class EmiScreenManagerMixin {
}
//?} else {
/*@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager", remap = false)
public abstract class EmiScreenManagerMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = {"drawBackground", "render", "drawForeground"}, remap = false, require = 0,
        at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private static void tidypockets$unscale(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.tidypockets.anim.ScreenPop.pushUnscaled();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = {"drawBackground", "render", "drawForeground"}, remap = false, require = 0,
        at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private static void tidypockets$rescale(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.tidypockets.anim.ScreenPop.popUnscaled();
    }
}
*///?}
