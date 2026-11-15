package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** The menu blur fade. Before 1.20.5 there is no menu background blur, so this is an empty placeholder. */
//? if >=1.20.5 {
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class GameRendererMixin {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "extractOptions", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getMenuBackgroundBlurriness()I"))
    private int tidypockets$fadeBlur(net.minecraft.client.Options options) {
        return io.github.profetgit.tidypockets.anim.ScreenPop.blurRadius(options.getMenuBackgroundBlurriness());
    }
}
//?} else {
/*@Mixin(net.minecraft.client.Minecraft.class)
public abstract class GameRendererMixin {
}
*///?}
