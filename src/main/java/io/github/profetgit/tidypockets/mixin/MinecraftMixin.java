package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.selftest.SelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tidypockets$ready(CallbackInfo ci) {
        TidyPockets.LOG.info("Tidy Pockets mixins active");
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void tidypockets$frame(boolean advanceGameTime, CallbackInfo ci) {
        if (SelfTest.active()) SelfTest.onFrame((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tidypockets$tick(CallbackInfo ci) {
        TidyPockets.clientTick((Minecraft) (Object) this);
    }
}
