package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Anims;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void tidypockets$screenOpened(Screen screen, CallbackInfo ci) {
        Anims.screenOpened(((net.minecraft.client.gui.Gui) (Object) this).screen());
    }
}
//?} else {
/*// before 26.2 the screen lives on Minecraft itself
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class GuiMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void tidypockets$screenOpened(Screen screen, CallbackInfo ci) {
        Anims.screenOpened(((net.minecraft.client.Minecraft) (Object) this).screen);
    }
}
*///?}
