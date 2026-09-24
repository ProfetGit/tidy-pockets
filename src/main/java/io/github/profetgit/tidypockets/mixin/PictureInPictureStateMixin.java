package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.PosedPip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Lets the GUI's 3D pictures (enchanting book, inventory and smithing figures, loom banner, skin widget) carry a pose.
 * {@code PictureInPictureRenderer.blitTexture} already blits through {@code pose()}; vanilla just always returns identity.
 */
@Mixin({GuiBookModelRenderState.class, GuiEntityRenderState.class, GuiSkinRenderState.class, GuiBannerResultRenderState.class})
public abstract class PictureInPictureStateMixin implements PosedPip {
    // remap = false: Mixin refuses remappable shadows in a multi-target mixin (26.x is unobfuscated anyway)
    @Shadow(remap = false) @Final private int x0;
    @Shadow(remap = false) @Final private int y0;
    @Shadow(remap = false) @Final private int x1;
    @Shadow(remap = false) @Final private int y1;
    @Shadow(remap = false) @Final private ScreenRectangle scissorArea;
    @Shadow(remap = false) @Final @Mutable private ScreenRectangle bounds;

    @Unique
    private Matrix3x2fc tidypockets$pose = PictureInPictureRenderState.IDENTITY_POSE;

    @Override
    public void tidypockets$pose(Matrix3x2fc pose) {
        tidypockets$pose = pose;
        // 26.3's getBounds(..., pose, scissor), spelled out because 26.2 only has the pose-less overload
        ScreenRectangle box = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        bounds = scissorArea != null ? scissorArea.intersection(box) : box;
    }

    public Matrix3x2fc pose() {
        return tidypockets$pose;
    }
}
