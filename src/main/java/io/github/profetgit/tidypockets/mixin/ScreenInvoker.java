package io.github.profetgit.tidypockets.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T tidypockets$addRenderableWidget(T widget);

    //? if >=1.21.2 <1.21.6 {
    /*@Invoker("renderBlurredBackground")
    void tidypockets$blur();
    *///?}
    //? if <1.21.2 {
    /*@Invoker("renderBlurredBackground")
    void tidypockets$blur(float partialTick);
    *///?}
}
