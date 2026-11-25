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
    // No @Redirect here: other mods (Simply Swords) redirect calls in tick, and two redirects of one call crash the game.
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "tick", at = @org.spongepowered.asm.mixin.injection.At("STORE"), ordinal = 0)
    private float tidypockets$steadyHand(float strength) {
        return io.github.profetgit.tidypockets.palette.RandomPlace.handSteady() ? 1.0f : strength;
    }
    //? if <1.21.2 {
    /^@org.spongepowered.asm.mixin.Shadow
    private net.minecraft.world.item.ItemStack mainHandItem;

    @org.spongepowered.asm.mixin.Shadow
    private net.minecraft.world.item.ItemStack offHandItem;

    @org.spongepowered.asm.mixin.injection.Inject(method = "tick", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void tidypockets$rolled(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && io.github.profetgit.tidypockets.palette.RandomPlace.recentlyRolled()) {
            mainHandItem = player.getMainHandItem();
            offHandItem = player.getOffhandItem();
        }
    }
    ^///?}
}
*///?}
