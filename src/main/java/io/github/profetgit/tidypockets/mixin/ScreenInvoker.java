package io.github.profetgit.tidypockets.mixin;

import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
    // Field accessors, not an addRenderableWidget invoker: Forge 1.20.1's refmap AP cannot map that generic method.
    @Accessor("children")
    List<GuiEventListener> tidypockets$children();

    @Accessor("narratables")
    List<NarratableEntry> tidypockets$narratables();

    @Accessor("renderables")
    List<Renderable> tidypockets$renderables();

    //? if >=1.21.2 <1.21.6 {
    /*@Invoker("renderBlurredBackground")
    void tidypockets$blur();
    *///?}
    //? if >=1.20.5 <1.21.2 {
    /*@Invoker("renderBlurredBackground")
    void tidypockets$blur(float partialTick);
    *///?}
}
