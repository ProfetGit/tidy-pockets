package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21.6 {
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderState.class)
public interface GuiRenderStateAccessor {
    @Accessor("firstStratumAfterBlur")
    int tidypockets$firstStratumAfterBlur();
}
//?} else {
/*// the 3D-picture and stratum pipeline of the GUI came with 1.21.6
@Mixin(net.minecraft.client.Minecraft.class)
public interface GuiRenderStateAccessor {
}
*///?}
