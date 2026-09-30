package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Before 1.21.2 the inventory screens share EffectRenderingInventoryScreen, which draws the effect panel after super.render. */
//? if >=1.21.2 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class EffectScreenLegacyMixin {
}
//?} else {
/*@Mixin(net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen.class)
public abstract class EffectScreenLegacyMixin {
    private static final String EFFECTS = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;renderEffects(Lnet/minecraft/client/gui/GuiGraphics;II)V";

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = EFFECTS))
    private void tidypockets$popEffects(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float dt, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.tidypockets.anim.ScreenPop.push((net.minecraft.client.gui.screens.Screen) (Object) this, g);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = EFFECTS,
        shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void tidypockets$unpopEffects(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float dt, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.profetgit.tidypockets.anim.ScreenPop.pop(g);
    }
}
*///?}
