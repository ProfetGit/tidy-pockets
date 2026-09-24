package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Anims;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void tidypockets$screenOpened(Screen screen, CallbackInfo ci) {
        Anims.screenOpened(((Gui) (Object) this).screen());
    }
}
