package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Redirect(method = "extractOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getMenuBackgroundBlurriness()I"))
    private int tidypockets$fadeBlur(Options options) {
        return ScreenPop.blurRadius(options.getMenuBackgroundBlurriness());
    }
}
