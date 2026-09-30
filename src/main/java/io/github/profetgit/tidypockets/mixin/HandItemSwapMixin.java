package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.palette.RandomPlace;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The first-person hand lowers the old item and raises the new one whenever the held item changes. A random palette
 * roll changes it on every click, so right after one the new block shows at once. The class moved in 26.3.
 */
//? if >=26.3 {
@Mixin(net.minecraft.client.player.FirstPersonHandsAndItems.class)
public abstract class HandItemSwapMixin {
    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void tidypockets$rolled(ItemStack from, ItemStack to, net.minecraft.client.player.LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (RandomPlace.recentlyRolled()) cir.setReturnValue(true);
    }
}
//?} else {
/*@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class HandItemSwapMixin {
    //? if >=1.21.2 {
    /^@Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void tidypockets$rolled(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (RandomPlace.recentlyRolled()) cir.setReturnValue(true);
    }
    ^///?}
    // 1.21.1 compares the stacks inline: HandSwapLegacyMixin
}
*///?}
