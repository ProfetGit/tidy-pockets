package io.github.profetgit.tidypockets.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockItem.class)
public interface BlockItemInvoker {
    /** The state this item would place for the click, or null when it can't be placed there. */
    @Invoker("getPlacementState")
    BlockState tidypockets$placementState(BlockPlaceContext context);
}
