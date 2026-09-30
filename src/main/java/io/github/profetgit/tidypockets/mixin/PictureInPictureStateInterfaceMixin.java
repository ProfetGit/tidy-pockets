package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21.6 {
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.2 {
@Mixin(net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState.class)
public interface PictureInPictureStateInterfaceMixin {
}
//?} else {
/*// pose() is an inherited default method: it can only be answered on the interface itself
@Mixin(net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState.class)
public interface PictureInPictureStateInterfaceMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "pose", at = @org.spongepowered.asm.mixin.injection.At("RETURN"), cancellable = true)
    private void tidypockets$pose(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<org.joml.Matrix3x2f> cir) {
        if (this instanceof io.github.profetgit.tidypockets.anim.PosedPip p && p.tidypockets$posed() != null) cir.setReturnValue(p.tidypockets$posed());
    }
}
*///?}
//?} else {
/*// the 3D-picture and stratum pipeline of the GUI came with 1.21.6
@Mixin(net.minecraft.client.Minecraft.class)
public interface PictureInPictureStateInterfaceMixin {
}
*///?}
