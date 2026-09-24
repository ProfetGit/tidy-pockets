package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.CreativeSlide;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow
    private float scrollOffs;

    @Unique
    private float tidypockets$before;

    private static final String EFFECTS =
        "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V";

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = EFFECTS))
    private void tidypockets$popEffects(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push((Screen) (Object) this, g);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = EFFECTS, shift = At.Shift.AFTER))
    private void tidypockets$unpopEffects(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void tidypockets$scrollHead(double x, double y, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
        tidypockets$before = scrollOffs;
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void tidypockets$scrollReturn(double x, double y, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
        if (scrollOffs != tidypockets$before) CreativeSlide.rowChanged(scrollOffs > tidypockets$before ? 1 : -1);
    }
}
