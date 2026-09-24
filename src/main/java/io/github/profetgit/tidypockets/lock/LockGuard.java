package io.github.profetgit.tidypockets.lock;

import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.inv.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps locked items where they are: nothing can be picked up, shift-clicked, swapped, thrown or double-click
 * collected out of a locked slot. Adding more of the same item to a locked stack still works. Refill and tool
 * protection are exempt (refill tops a locked slot up; protection may move a tool that is about to break).
 */
public final class LockGuard {
    private LockGuard() {}

    public static boolean lockedWithItem(Slot slot) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && slot != null && slot.hasItem() && Inv.isPlayerSlot(slot, p) && SlotLocks.isLocked(slot.getContainerSlot());
    }

    private static boolean hotbarLocked(LocalPlayer p, int button) {
        int inv = button == 40 ? Inventory.SLOT_OFFHAND : button;
        return (button == 40 || (button >= 0 && button < Inventory.SELECTION_SIZE))
            && SlotLocks.isLocked(inv) && !p.getInventory().getItem(inv).isEmpty();
    }

    /** True if this vanilla click would take something out of a locked slot; the click is then cancelled. */
    public static boolean blocks(AbstractContainerScreen<?> screen, Slot slot, int button, ContainerInput input) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || screen instanceof CreativeModeInventoryScreen) return false;
        ItemStack carried = screen.getMenu().getCarried();
        boolean blocked = switch (input) {
            case PICKUP -> lockedWithItem(slot) && (carried.isEmpty() || !ItemStack.isSameItemSameComponents(carried, slot.getItem()));
            case QUICK_MOVE, THROW -> lockedWithItem(slot);
            case SWAP -> lockedWithItem(slot) || hotbarLocked(p, button);
            case PICKUP_ALL -> collectsFromLocked(screen, carried);
            default -> false;
        };
        if (blocked) refuse(slot);
        return blocked;
    }

    private static boolean collectsFromLocked(AbstractContainerScreen<?> screen, ItemStack carried) {
        if (carried.isEmpty()) return false;
        for (Slot s : screen.getMenu().slots) {
            if (lockedWithItem(s) && ItemStack.isSameItemSameComponents(s.getItem(), carried)) return true;
        }
        return false;
    }

    /** Dropping the held item with Q outside screens. */
    public static boolean blocksDrop(LocalPlayer p) {
        int sel = p.getInventory().getSelectedSlot();
        if (!SlotLocks.isLocked(sel) || p.getInventory().getItem(sel).isEmpty()) return false;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 0.3f));
        io.github.profetgit.tidypockets.anim.HudAnims.shake(sel);
        return true;
    }

    private static void refuse(Slot slot) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 0.3f));
        if (slot != null) SlotAnims.shake(slot);
    }
}
