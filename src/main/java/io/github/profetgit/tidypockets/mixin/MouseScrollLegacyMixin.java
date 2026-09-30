package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Before 1.21.2 AbstractContainerScreen has no mouseScrolled of its own: the wheel is hooked where the mouse handler calls the screen. */
//? if >=1.21.2 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MouseScrollLegacyMixin {
}
//?} else {
/*@Mixin(net.minecraft.client.MouseHandler.class)
public abstract class MouseScrollLegacyMixin {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "onScroll", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean tidypockets$scrolled(net.minecraft.client.gui.screens.Screen screen, double x, double y, double scrollX, double scrollY) {
        boolean handled = screen.mouseScrolled(x, y, scrollX, scrollY);
        if (!handled && screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c) {
            handled = io.github.profetgit.tidypockets.mouse.MouseTweaks.scrolledAt(c, x, y, scrollY);
        }
        return handled;
    }
}
*///?}
