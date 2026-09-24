package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.anim.CreativeSlide;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.lock.LockGuard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow
    private float scrollOffs;

    @Shadow
    private Slot destroyItemSlot;

    @Unique
    private float tidypockets$before;

    @Unique
    private boolean tidypockets$hadItem;

    private static final String EFFECTS =
        "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V";

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = EFFECTS))
    private void tidypockets$popEffects(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push((Screen) (Object) this, g);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = EFFECTS, shift = At.Shift.AFTER))
    private void tidypockets$unpopEffects(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    /** The creative screen handles clicks itself, without the {@code AbstractContainerScreen} version our guard hooks. */
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void tidypockets$clickHead(Slot slot, int id, int button, ContainerInput input, CallbackInfo ci) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (LockGuard.blocks(self, slot, button, input)
            || (input == ContainerInput.QUICK_MOVE && slot != null && slot == destroyItemSlot && Creative.clearUnlocked(self))) {
            ci.cancel();
            return;
        }
        tidypockets$hadItem = slot != null && slot.hasItem();
        if (input == ContainerInput.QUICK_MOVE) Anims.beginMove(self.getMenu());
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("RETURN"))
    private void tidypockets$clickReturn(Slot slot, int id, int button, ContainerInput input, CallbackInfo ci) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (input == ContainerInput.QUICK_MOVE) Anims.endMove(self.getMenu());
        if (slot != null && Creative.tab(self) == Creative.Tab.ITEMS) Creative.poofIfDeleted(slot, tidypockets$hadItem, input);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void tidypockets$scrollHead(double x, double y, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
        tidypockets$before = scrollOffs;
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void tidypockets$scrollReturn(double x, double y, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
        if (scrollOffs != tidypockets$before) CreativeSlide.rowChanged(scrollOffs > tidypockets$before ? 1 : -1);
    }
}
