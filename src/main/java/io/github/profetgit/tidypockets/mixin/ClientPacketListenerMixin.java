package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.selftest.SelfTest;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void tidypockets$countResync(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (SelfTest.active()) SelfTest.contentPackets++;
    }
}
