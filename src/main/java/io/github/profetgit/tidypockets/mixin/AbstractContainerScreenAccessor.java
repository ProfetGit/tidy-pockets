package io.github.profetgit.tidypockets.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int tidypockets$left();

    @Accessor("topPos")
    int tidypockets$top();

    @Accessor("imageWidth")
    int tidypockets$width();

    @Accessor("imageHeight")
    int tidypockets$height();
}
