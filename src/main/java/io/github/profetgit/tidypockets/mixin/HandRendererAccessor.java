package io.github.profetgit.tidypockets.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Self-test only: what the first-person hand shows, by accessor so it works under any runtime names. */
//? if >=26.3 {
@Mixin(net.minecraft.client.player.FirstPersonHandsAndItems.class)
//?} else {
/*@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
*///?}
public interface HandRendererAccessor {
    @Accessor("mainHandHeight")
    float tidypockets$height();

    @Accessor("mainHandItem")
    ItemStack tidypockets$item();
}
