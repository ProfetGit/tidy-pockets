package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.protect.ToolProtect;
import io.github.profetgit.tidypockets.refill.Refill;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    public abstract void stopDestroyBlock();

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void tidypockets$startDestroy(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        Refill.noteAction();
        if (ToolProtect.check(InteractionHand.MAIN_HAND, ToolProtect.Cost.BLOCK)) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void tidypockets$continueDestroy(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        Refill.noteAction();
        if (ToolProtect.check(InteractionHand.MAIN_HAND, ToolProtect.Cost.BLOCK)) {
            stopDestroyBlock();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void tidypockets$attack(Player player, Entity target, CallbackInfo ci) {
        Refill.noteAction();
        if (ToolProtect.check(InteractionHand.MAIN_HAND, ToolProtect.Cost.ATTACK)) ci.cancel();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void tidypockets$useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        Refill.noteAction();
        if (ToolProtect.check(hand, ToolProtect.Cost.USE)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void tidypockets$useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Refill.noteAction();
        if (ToolProtect.check(hand, ToolProtect.Cost.USE)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void tidypockets$interact(Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Refill.noteAction();
        if (ToolProtect.check(hand, ToolProtect.Cost.USE)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void tidypockets$release(Player player, CallbackInfo ci) {
        Refill.noteAction();
        if (ToolProtect.check(player.getUsedItemHand(), ToolProtect.Cost.USE)) ci.cancel();
    }
}
