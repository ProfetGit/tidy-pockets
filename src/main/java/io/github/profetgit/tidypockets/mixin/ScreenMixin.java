package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Blur;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$popBackground(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push((Screen) (Object) this, g);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$unpopBackground(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$popContents(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (self instanceof AbstractContainerScreen<?>) ScreenPop.pushNone();
        else ScreenPop.push(self, g);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$unpopContents(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void tidypockets$showcaseOverlay(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active() && io.github.profetgit.tidypockets.selftest.Director.active()) {
            g.nextStratum();
            io.github.profetgit.tidypockets.selftest.Director.overlay(g, true);
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void tidypockets$backgroundHead(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        ScreenPop.pushIdentity(self, g);
        Blur.containerBackground(self, g);
    }

    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void tidypockets$backgroundReturn(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }
}
