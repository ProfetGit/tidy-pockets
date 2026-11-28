package io.github.profetgit.tidypockets.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** The passes of a post effect, for the blur radius before 1.20.5 (later versions: an empty placeholder). */
//? if <1.20.5 {
/*@Mixin(net.minecraft.client.renderer.PostChain.class)
public interface PostChainAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("passes")
    java.util.List<net.minecraft.client.renderer.PostPass> tidypockets$passes();
}
*///?} else {
@Mixin(net.minecraft.client.Minecraft.class)
public interface PostChainAccessor {
}
//?}
