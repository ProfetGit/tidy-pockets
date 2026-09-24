package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.lock.LockGuard;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.3 {
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

/** Q (drop) can't throw the held item out of a locked hotbar slot. The drop call moved to the game mode in 26.3. */
//? if >=26.3 {
@Mixin(MultiPlayerGameMode.class)
public abstract class DropMixin {
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void tidypockets$drop(LocalPlayer player, boolean wholeStack, CallbackInfo ci) {
        if (LockGuard.blocksDrop(player)) ci.cancel();
    }
}
//?} else {
/*@Mixin(LocalPlayer.class)
public abstract class DropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void tidypockets$drop(boolean wholeStack, CallbackInfoReturnable<Boolean> cir) {
        if (LockGuard.blocksDrop((LocalPlayer) (Object) this)) cir.setReturnValue(false);
    }
}
*///?}
