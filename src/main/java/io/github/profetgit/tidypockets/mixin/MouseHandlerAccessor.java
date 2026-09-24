package io.github.profetgit.tidypockets.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void tidypockets$setX(double x);

    @Accessor("ypos")
    void tidypockets$setY(double y);
}
