package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.PlayerHop;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The player figure is drawn as a picture-in-picture, so the hop moves its box. (The pop scales it through the
 * picture's pose, see GuiGraphicsExtractorMixin.)
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Unique
    private static boolean tidypockets$inner;

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

    @Inject(method = "extractEntityInInventoryFollowsMouse", at = @At("HEAD"), cancellable = true)
    private static void tidypockets$hopFigure(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size,
            float yOffset, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        if (tidypockets$inner) return;
        float hop = PlayerHop.offset(entity);
        if (hop == 0) return;
        tidypockets$inner = true;
        try {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, Math.round(y0 - hop), x1, Math.round(y1 - hop), size,
                yOffset, mouseX, mouseY, entity);
        } finally {
            tidypockets$inner = false;
        }
        ci.cancel();
    }
}
