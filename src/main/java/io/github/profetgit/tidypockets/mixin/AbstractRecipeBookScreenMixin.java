package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.FlyAnims;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recipe-book screens (inventory, crafting table, furnaces) replace the container's render method, so the pop and
 * the item flights are hooked here too. The recipe book panel and its toggle button pop with the inventory.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    private static final String CONTENTS =
        "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";
    private static final String BACKGROUND =
        "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";
    private static final String BOOK =
        "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";
    private static final String CARRIED =
        "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V";

    private AbstractContainerScreen<?> tidypockets$self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    @Inject(method = "extractRenderState", at = {@At(value = "INVOKE", target = CONTENTS), @At(value = "INVOKE", target = BACKGROUND),
        @At(value = "INVOKE", target = BOOK)})
    private void tidypockets$pop(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.push(tidypockets$self(), g);
    }

    @Inject(method = "extractRenderState", at = {@At(value = "INVOKE", target = CONTENTS, shift = At.Shift.AFTER),
        @At(value = "INVOKE", target = BACKGROUND, shift = At.Shift.AFTER), @At(value = "INVOKE", target = BOOK, shift = At.Shift.AFTER)})
    private void tidypockets$unpop(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        ScreenPop.pop(g);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = CARRIED))
    private void tidypockets$flights(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        FlyAnims.draw(tidypockets$self(), g);
    }
}
