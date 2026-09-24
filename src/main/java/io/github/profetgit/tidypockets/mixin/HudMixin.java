package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.HudAnims;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractItemHotbar", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"))
    private void tidypockets$glideSelector(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        g.pose().pushMatrix();
        g.pose().translate(HudAnims.selectorOffset(), 0);
    }

    @Inject(method = "extractItemHotbar", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"))
    private void tidypockets$glideSelectorEnd(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        g.pose().popMatrix();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tidypockets$showcaseOverlay(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active() && io.github.profetgit.tidypockets.selftest.Director.active()) {
            io.github.profetgit.tidypockets.selftest.Director.overlay(g, false);
        }
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void tidypockets$slotHead(GuiGraphicsExtractor g, int x, int y, DeltaTracker dt, Player p, ItemStack stack, int seed, CallbackInfo ci) {
        HudAnims.beforeSlot(g, x, y, p, stack);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void tidypockets$slotReturn(GuiGraphicsExtractor g, int x, int y, DeltaTracker dt, Player p, ItemStack stack, int seed, CallbackInfo ci) {
        HudAnims.afterSlot(g, x, y);
    }
}
