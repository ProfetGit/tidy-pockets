package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.palette.RandomPlace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After a hotbar switch the hand rises slowly again (it follows the attack cooldown, about a quarter of a second).
 * A random palette roll switches the slot on every click, so the hand would hover half lowered while you paint.
 * For a few ticks after a roll the hand counts as fully swapped in.
 */
@Mixin(Player.class)
public abstract class PlayerSwapMixin {
    //? if >=1.21.11 {
    @Inject(method = "getItemSwapScale", at = @At("HEAD"), cancellable = true)
    private void tidypockets$rolled(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (RandomPlace.handSteady() && (Object) this == Minecraft.getInstance().player) cir.setReturnValue(1.0f);
    }
    //?}
    // before 1.21.11 the hand's rise reads the attack strength itself: HandSwapLegacyMixin
}
