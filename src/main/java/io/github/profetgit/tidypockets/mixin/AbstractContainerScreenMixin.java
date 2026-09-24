package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.anim.CreativeSlide;
import io.github.profetgit.tidypockets.anim.FlyAnims;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.lock.LockGuard;
import io.github.profetgit.tidypockets.mouse.MouseTweaks;
import io.github.profetgit.tidypockets.screen.ScreenInput;
import io.github.profetgit.tidypockets.screen.SlotOverlay;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    private Slot getHoveredSlot(double x, double y) {
        throw new AssertionError();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseClicked(MouseButtonEvent e, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Slot slot = getHoveredSlot(e.x(), e.y());
        if (ScreenInput.mouseClicked(self, slot, e) || MouseTweaks.pressed(self, slot, e)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseDragged(MouseButtonEvent e, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.dragged((AbstractContainerScreen<?>) (Object) this, getHoveredSlot(e.x(), e.y()))) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseReleased(MouseButtonEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.released((AbstractContainerScreen<?>) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"), cancellable = true)
    private void tidypockets$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        boolean shift = io.github.profetgit.tidypockets.Compat.shiftDown();
        if (MouseTweaks.scrolled((AbstractContainerScreen<?>) (Object) this, getHoveredSlot(x, y), scrollY, shift)) cir.setReturnValue(true);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void tidypockets$quietShowcase(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active() && io.github.profetgit.tidypockets.selftest.Director.active()) ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tidypockets$init(CallbackInfo ci) {
        ContainerTools.init((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tidypockets$keyPressed(KeyEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (ScreenInput.keyPressed((AbstractContainerScreen<?>) (Object) this, hoveredSlot, e)) cir.setReturnValue(true);
    }

    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void tidypockets$creativeGrid(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        CreativeSlide.grid((AbstractContainerScreen<?>) (Object) this, g);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void tidypockets$slotHead(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        CreativeSlide.before(self, g, slot);
        if (!SlotAnims.before(g, slot)) {
            CreativeSlide.after(g);
            SlotAnims.puff(g, slot);
            ci.cancel();
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void tidypockets$slotTail(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        SlotAnims.after(g, slot);
        CreativeSlide.after(g);
        SlotAnims.puff(g, slot);
        SlotOverlay.afterSlot((AbstractContainerScreen<?>) (Object) this, g, slot);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$popContents(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push((AbstractContainerScreen<?>) (Object) this, g);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void tidypockets$unpopContents(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void tidypockets$flights(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        FlyAnims.draw((AbstractContainerScreen<?>) (Object) this, g);
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void tidypockets$clickHead(Slot slot, int id, int button, ContainerInput input, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (LockGuard.blocks(self, slot, button, input)) {
            ci.cancel();
            return;
        }
        if (input == ContainerInput.QUICK_MOVE) Anims.beginMove(self.getMenu());
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("TAIL"))
    private void tidypockets$clickTail(Slot slot, int id, int button, ContainerInput input, CallbackInfo ci) {
        if (input == ContainerInput.QUICK_MOVE) Anims.endMove(((AbstractContainerScreen<?>) (Object) this).getMenu());
    }
}
