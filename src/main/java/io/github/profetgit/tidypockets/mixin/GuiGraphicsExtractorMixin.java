package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.PosedPip;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * While a screen pops, its 3D pictures take the current pose, so they scale with the panel instead of sitting still.
 * Covers every screen that draws one (enchanting table, inventory, creative, mounts, smithing, loom, skin widget).
 * Outside the pop the pose is left to vanilla (identity).
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Shadow
    public abstract Matrix3x2fStack pose();

    @ModifyArg(method = {"book", "entity", "skin", "bannerPattern"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;)V"))
    private PictureInPictureRenderState tidypockets$posePicture(PictureInPictureRenderState state) {
        if (state instanceof PosedPip posed && ScreenPop.popping()) {
            posed.tidypockets$pose(new Matrix3x2f(pose()));
            ScreenPop.posedPictures++;
        }
        return state;
    }
}
