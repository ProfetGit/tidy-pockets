package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.SmoothScroller;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tidypockets$stepScroll(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        if (this instanceof SmoothScroller s) s.tidypockets$step();
    }
}
