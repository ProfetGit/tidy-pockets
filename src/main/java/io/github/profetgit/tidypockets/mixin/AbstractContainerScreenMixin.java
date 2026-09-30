package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.anim.CreativeSlide;
import io.github.profetgit.tidypockets.anim.FlyAnims;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.lock.LockGuard;
import io.github.profetgit.tidypockets.input.KeyEvt;
import io.github.profetgit.tidypockets.input.MouseEvt;
import io.github.profetgit.tidypockets.mouse.MouseTweaks;
import io.github.profetgit.tidypockets.screen.ScreenInput;
import io.github.profetgit.tidypockets.screen.SlotOverlay;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
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

    //? if >=1.21.9 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseClicked(MouseButtonEvent e, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Slot slot = getHoveredSlot(e.x(), e.y());
        MouseEvt ev = new MouseEvt(e.x(), e.y(), e.button(), e.modifiers());
        if (ScreenInput.mouseClicked(self, slot, ev) || MouseTweaks.pressed(self, slot, ev)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseDragged(MouseButtonEvent e, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.dragged((AbstractContainerScreen<?>) (Object) this, getHoveredSlot(e.x(), e.y()))) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseReleased(MouseButtonEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.released((AbstractContainerScreen<?>) (Object) this)) cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseClicked(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Slot slot = getHoveredSlot(x, y);
        MouseEvt ev = new MouseEvt(x, y, button, io.github.profetgit.tidypockets.Compat.modifiers());
        if (ScreenInput.mouseClicked(self, slot, ev) || MouseTweaks.pressed(self, slot, ev)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseDragged(double x, double y, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.dragged((AbstractContainerScreen<?>) (Object) this, getHoveredSlot(x, y))) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void tidypockets$mouseReleased(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaks.released((AbstractContainerScreen<?>) (Object) this)) cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.2 {
    @Inject(method = "mouseScrolled", at = @At("RETURN"), cancellable = true)
    private void tidypockets$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        boolean shift = io.github.profetgit.tidypockets.Compat.shiftDown();
        if (MouseTweaks.scrolled((AbstractContainerScreen<?>) (Object) this, getHoveredSlot(x, y), scrollY, shift)) cir.setReturnValue(true);
    }
    //?}

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void tidypockets$quietShowcase(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active() && io.github.profetgit.tidypockets.selftest.Director.active()) ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tidypockets$init(CallbackInfo ci) {
        ContainerTools.init((AbstractContainerScreen<?>) (Object) this);
    }

    //? if >=1.21.9 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tidypockets$keyPressed(KeyEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (ScreenInput.keyPressed((AbstractContainerScreen<?>) (Object) this, hoveredSlot, io.github.profetgit.tidypockets.Compat.keyEvt(e))) cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tidypockets$keyPressed(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ScreenInput.keyPressed((AbstractContainerScreen<?>) (Object) this, hoveredSlot, new KeyEvt(key, scancode, modifiers))) cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.2 {
    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void tidypockets$creativeGrid(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        CreativeSlide.grid((AbstractContainerScreen<?>) (Object) this, g);
    }
    //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/NonNullList;size()I"))
    private void tidypockets$creativeGrid(GuiGraphicsExtractor g, int mouseX, int mouseY, float dt, CallbackInfo ci) {
        CreativeSlide.grid((AbstractContainerScreen<?>) (Object) this, g);
    }
    *///?}

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

    //? if >=1.21.6 {
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
    //?} else {
    /*// before 1.21.6 render() draws the background, the widgets and then the contents itself: the pop wraps all of it, and
    // vanilla's own pose push (the panel offset) closes at the first popPose
    @Inject(method = "render", at = @At("HEAD"))
    private void tidypockets$popAll(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push((AbstractContainerScreen<?>) (Object) this, g);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0,
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void tidypockets$unpopAll(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
        FlyAnims.draw((AbstractContainerScreen<?>) (Object) this, g);
    }
    *///?}

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
