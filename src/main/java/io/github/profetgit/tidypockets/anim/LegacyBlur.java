package io.github.profetgit.tidypockets.anim;

//? if <1.20.5 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import io.github.profetgit.tidypockets.TidyPockets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
*///?}

/**
 * The inventory blur before 1.20.5, where the game has no menu blur: its old "blur" post effect, run over what is
 * drawn so far.
 */
final class LegacyBlur {
    private LegacyBlur() {}

    //? if <1.20.5 {
    /*static final int DEFAULT_RADIUS = 5;
    private static final Identifier EFFECT = Identifier.withDefaultNamespace("shaders/post/blur.json");
    private static PostChain chain;
    private static int width, height;
    private static boolean failed;
    private static long drawnIn = -1;

    static void process(GuiGraphicsExtractor g, float partialTick, int radius) {
        // once a frame, even if a screen draws its background twice
        if (failed || radius < 1 || drawnIn == Ease.frames()) return;
        drawnIn = Ease.frames();
        Minecraft mc = Minecraft.getInstance();
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), EFFECT);
                width = height = 0;
            }
            if (width != mc.getWindow().getWidth() || height != mc.getWindow().getHeight()) {
                width = mc.getWindow().getWidth();
                height = mc.getWindow().getHeight();
                chain.resize(width, height);
            }
        } catch (java.io.IOException | com.google.gson.JsonSyntaxException e) {
            failed = true;
            TidyPockets.LOG.warn("No inventory blur, couldn't load {}: {}", EFFECT, e.toString());
            return;
        }
        g.flush();
        // two box passes of radius * 1.4 come out about as soft as the newer game's blur of the same strength
        setRadius(Math.round(radius * 1.4f));
        chain.process(partialTick);
        chain.process(partialTick);
        mc.getMainRenderTarget().bindWrite(true);
        // the effect's own quads leave depth behind that would hide the screen drawn next
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Blur.drawn++;
    }

    private static void setRadius(float radius) {
        for (var pass : ((io.github.profetgit.tidypockets.mixin.PostChainAccessor) chain).tidypockets$passes()) {
            pass.getEffect().safeGetUniform("Radius").set(radius);
        }
    }
    *///?}
}
