package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Before 1.21.9 vanilla asks the keyboard whether Shift is down (InputConstants.isKeyDown), not the click event. The
 * self-test presses keys that no keyboard holds, so the pretended Shift is answered here.
 */
//? if >=1.21.9 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class ScreenShiftMixin {
}
//?} else {
/*@Mixin(com.mojang.blaze3d.platform.InputConstants.class)
public abstract class ScreenShiftMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "isKeyDown(JI)Z", at = @org.spongepowered.asm.mixin.injection.At("RETURN"), cancellable = true)
    private static void tidypockets$pretendShift(long window, int key, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (key == 340 || key == 344) && io.github.profetgit.tidypockets.Compat.pretendedShift()) cir.setReturnValue(true);
    }
}
*///?}
