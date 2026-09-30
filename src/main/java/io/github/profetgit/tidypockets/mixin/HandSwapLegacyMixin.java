package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Before 1.21.11 the hand's rise after a switch reads the attack strength inside ItemInHandRenderer.tick (see
 * PlayerSwapMixin for what this is for), and 1.21.1 has no shouldInstantlyReplaceVisibleItem (it compares the stacks inline).
 */
//? if >=1.21.11 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class HandSwapLegacyMixin {
}
//?} else {
/*@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class HandSwapLegacyMixin {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "tick", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float tidypockets$steadyHand(net.minecraft.client.player.LocalPlayer player, float partialTick) {
        return io.github.profetgit.tidypockets.palette.RandomPlace.handSteady() ? 1.0f : player.getAttackStrengthScale(partialTick);
    }
    //? if <1.21.2 {
    /^@org.spongepowered.asm.mixin.injection.Redirect(method = "tick", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean tidypockets$rolled(net.minecraft.world.item.ItemStack from, net.minecraft.world.item.ItemStack to) {
        return io.github.profetgit.tidypockets.palette.RandomPlace.recentlyRolled() || net.minecraft.world.item.ItemStack.matches(from, to);
    }
    ^///?}
}
*///?}
