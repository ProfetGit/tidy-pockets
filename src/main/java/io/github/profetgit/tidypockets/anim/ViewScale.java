package io.github.profetgit.tidypockets.anim;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Scales GUI drawing through the model-view matrix, so a shader still reads every vertex at its unscaled screen
 * position (the pop under a resource pack's GUI shaders). Only before 1.21.6, where the GUI draws in call order; each
 * push flushes what is waiting and is paired with a {@link #pop}.
 */
final class ViewScale {
    //? if <1.21.6 {
    /*static final boolean SUPPORTED = true;
    *///?} else {
    static final boolean SUPPORTED = false;
    //?}

    private ViewScale() {}

    //? if <1.20.5 {
    /*static void push(GuiGraphicsExtractor g, float k, float x, float y) {
        g.flush();
        com.mojang.blaze3d.vertex.PoseStack view = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushPose();
        view.translate(x, y, 0.0F);
        view.scale(k, k, 1.0F);
        view.translate(-x, -y, 0.0F);
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    static void pop(GuiGraphicsExtractor g) {
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().popPose();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }
    *///?}

    //? if >=1.20.5 && <1.21.6 {
    /*static void push(GuiGraphicsExtractor g, float k, float x, float y) {
        g.flush();
        org.joml.Matrix4fStack view = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.translate(x, y, 0.0F);
        view.scale(k, k, 1.0F);
        view.translate(-x, -y, 0.0F);
        apply();
    }

    static void pop(GuiGraphicsExtractor g) {
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().popMatrix();
        apply();
    }
    *///?}

    // from 1.21.2 on the stack is read when drawing
    //? if >=1.20.5 && <1.21.2 {
    /*private static void apply() {
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }
    *///?}
    //? if >=1.21.2 && <1.21.6 {
    /*private static void apply() {}
    *///?}

    //? if >=1.21.6 {
    static void push(GuiGraphicsExtractor g, float k, float x, float y) {
        throw new UnsupportedOperationException();
    }

    static void pop(GuiGraphicsExtractor g) {
        throw new UnsupportedOperationException();
    }
    //?}
}
