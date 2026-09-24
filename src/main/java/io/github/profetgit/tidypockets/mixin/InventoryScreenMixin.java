package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.PlayerHop;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player figure is drawn as a picture-in-picture that ignores the pose, so the pop and hop move its box instead. */
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
    private static void tidypockets$transformFigure(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size,
            float yOffset, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        if (tidypockets$inner) return;
        Screen s = Minecraft.getInstance().gui.screen();
        double k = s == null ? 1 : ScreenPop.scale(s);
        float hop = PlayerHop.offset(entity);
        if (k == 1 && hop == 0) return;
        float px = s == null ? 0 : ScreenPop.pivotX(s), py = s == null ? 0 : ScreenPop.pivotY(s);
        int nx0 = Math.round(px + (x0 - px) * (float) k), ny0 = Math.round(py + (y0 - hop - py) * (float) k);
        int nx1 = Math.round(px + (x1 - px) * (float) k), ny1 = Math.round(py + (y1 - hop - py) * (float) k);
        tidypockets$inner = true;
        try {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, nx0, ny0, nx1, ny1, Math.max(1, (int) Math.round(size * k)),
                yOffset, mouseX, mouseY, entity);
        } finally {
            tidypockets$inner = false;
        }
        ci.cancel();
    }
}
