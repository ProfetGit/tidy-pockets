package io.github.profetgit.tidypockets.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The creative "Survival Inventory" tab shows the player's inventory menu through wrappers around its slots. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeSlotWrapperAccessor {
    @Accessor("target")
    Slot tidypockets$target();
}
