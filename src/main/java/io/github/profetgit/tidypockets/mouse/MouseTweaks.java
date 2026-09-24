package io.github.profetgit.tidypockets.mouse;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.lock.LockGuard;
import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.inv.ClickSender;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Mouse shortcuts: the wheel moves single items between the two halves of a screen, shift+drag shift-clicks every
 * slot passed over, and dragging with an item on the cursor from a matching stack collects matching stacks.
 */
public final class MouseTweaks {
    private enum Drag { NONE, SHIFT, COLLECT }

    private static Drag drag = Drag.NONE;
    private static final Set<Slot> visited = new HashSet<>();
    private static Slot dragStart;
    private static boolean leftStart;

    private MouseTweaks() {}

    private static boolean usable(AbstractContainerScreen<?> screen) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !Conflicts.mouse && !(screen instanceof CreativeModeInventoryScreen) && !ClickSender.busy();
    }

    // ---- wheel ----

    public static boolean scrolled(AbstractContainerScreen<?> screen, Slot hovered, double scrollY, boolean shift) {
        TidyConfig cfg = TidyConfig.get();
        if (!cfg.wheelMove || scrollY == 0 || hovered == null || !usable(screen)) return false;
        AbstractContainerMenu menu = screen.getMenu();
        if (!menu.getCarried().isEmpty()) return false;
        LocalPlayer p = Minecraft.getInstance().player;
        List<Slot> other = otherSide(menu, hovered, p);
        if (other.isEmpty()) return false;
        boolean push = (scrollY < 0) != cfg.wheelInvert;
        Anims.beginMove(menu);
        boolean done = push ? push(menu, hovered, other, shift) : pull(menu, hovered, other, shift);
        Anims.endMove(menu);
        return done;
    }

    /** The slots on the far side of {@code from}: container vs player, or main vs hotbar in the player screen. */
    static List<Slot> otherSide(AbstractContainerMenu menu, Slot from, LocalPlayer p) {
        boolean fromPlayer = Inv.isPlayerSlot(from, p);
        List<Slot> container = Inv.containerSlots(menu, p);
        List<Slot> out = new ArrayList<>();
        if (!container.isEmpty()) {
            if (fromPlayer) out.addAll(container);
            else {
                out.addAll(Inv.playerSlots(menu, p, 0, Inventory.SELECTION_SIZE).reversed());
                out.addAll(Inv.playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE).reversed());
            }
        } else if (menu instanceof InventoryMenu && fromPlayer) {
            int i = from.getContainerSlot();
            if (i < Inventory.SELECTION_SIZE) out.addAll(Inv.playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE));
            else if (i < Inventory.INVENTORY_SIZE) out.addAll(Inv.playerSlots(menu, p, 0, Inventory.SELECTION_SIZE));
        }
        out.removeIf(s -> Inv.isPlayerSlot(s, p) && SlotLocks.isLocked(s.getContainerSlot()));
        return out;
    }

    private static Slot target(List<Slot> other, ItemStack stack) {
        for (Slot s : other) {
            ItemStack t = s.getItem();
            if (!t.isEmpty() && ItemStack.isSameItemSameComponents(t, stack) && t.getCount() < s.getMaxStackSize(t) && s.mayPlace(stack)) return s;
        }
        for (Slot s : other) if (!s.hasItem() && s.mayPlace(stack)) return s;
        return null;
    }

    private static boolean push(AbstractContainerMenu menu, Slot from, List<Slot> other, boolean whole) {
        ItemStack stack = from.getItem();
        if (stack.isEmpty() || !from.mayPickup(Minecraft.getInstance().player)) return false;
        if (LockGuard.lockedWithItem(from)) {
            SlotAnims.shake(from);
            return true;
        }
        if (whole) {
            ClickSender.send(menu, from.index, 0, ContainerInput.QUICK_MOVE);
            return true;
        }
        Slot to = target(other, stack);
        if (to == null) return true;
        int n = stack.getCount();
        ClickSender.send(menu, from.index, 0, ContainerInput.PICKUP);
        ClickSender.send(menu, to.index, 1, ContainerInput.PICKUP);
        if (n > 1) ClickSender.send(menu, from.index, 0, ContainerInput.PICKUP);
        return true;
    }

    private static boolean pull(AbstractContainerMenu menu, Slot into, List<Slot> other, boolean whole) {
        ItemStack want = into.getItem();
        if (want.isEmpty() || want.getCount() >= into.getMaxStackSize(want)) return true;
        Slot src = null;
        for (Slot s : other) {
            if (s.hasItem() && !LockGuard.lockedWithItem(s) && ItemStack.isSameItemSameComponents(s.getItem(), want)
                && s.mayPickup(Minecraft.getInstance().player) && (src == null || s.getItem().getCount() < src.getItem().getCount())) src = s;
        }
        if (src == null) return true;
        if (whole) {
            ClickSender.send(menu, src.index, 0, ContainerInput.QUICK_MOVE);
        } else {
            int n = src.getItem().getCount();
            ClickSender.send(menu, src.index, 0, ContainerInput.PICKUP);
            ClickSender.send(menu, into.index, 1, ContainerInput.PICKUP);
            if (n > 1) ClickSender.send(menu, src.index, 0, ContainerInput.PICKUP);
        }
        return true;
    }

    // ---- drags ----

    /** Mouse down. Returns true to swallow the click (collect mode takes over the vanilla click). */
    public static boolean pressed(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent e) {
        drag = Drag.NONE;
        visited.clear();
        if (e.button() != InputConstants.MOUSE_BUTTON_LEFT || slot == null || !usable(screen)) return false;
        TidyConfig cfg = TidyConfig.get();
        ItemStack carried = screen.getMenu().getCarried();
        if (LockGuard.lockedWithItem(slot)) return false;
        if (e.hasShiftDown() && carried.isEmpty() && cfg.shiftDrag) {
            drag = Drag.SHIFT;
            visited.add(slot);
            return false;
        }
        if (!e.hasShiftDown() && cfg.collectDrag && !carried.isEmpty() && slot.hasItem()
            && ItemStack.isSameItemSameComponents(slot.getItem(), carried)) {
            drag = Drag.COLLECT;
            dragStart = slot;
            leftStart = false;
            visited.add(slot);
            return true;
        }
        return false;
    }

    public static boolean dragged(AbstractContainerScreen<?> screen, Slot slot) {
        if (drag == Drag.NONE || slot == null || visited.contains(slot)) return drag == Drag.COLLECT;
        AbstractContainerMenu menu = screen.getMenu();
        visited.add(slot);
        if (drag == Drag.SHIFT) {
            if (LockGuard.lockedWithItem(slot)) {
                SlotAnims.shake(slot);
            } else if (slot.hasItem() && slot.mayPickup(Minecraft.getInstance().player)) {
                Anims.beginMove(menu);
                ClickSender.send(menu, slot.index, 0, ContainerInput.QUICK_MOVE);
                Anims.endMove(menu);
            }
            return false;
        }
        if (!leftStart) {
            leftStart = true;
            collect(menu, dragStart);
        }
        ItemStack carried = menu.getCarried();
        if (slot.hasItem() && !LockGuard.lockedWithItem(slot) && ItemStack.isSameItemSameComponents(slot.getItem(), carried)) collect(menu, slot);
        return true;
    }

    /** Merges a slot's stack into the cursor while there is room: drop the cursor onto it, then pick it all up. */
    private static void collect(AbstractContainerMenu menu, Slot s) {
        ItemStack carried = menu.getCarried();
        if (carried.getCount() + s.getItem().getCount() > carried.getMaxStackSize()) return;
        ClickSender.send(menu, s.index, 0, ContainerInput.PICKUP);
        ClickSender.send(menu, s.index, 0, ContainerInput.PICKUP);
    }

    public static boolean released(AbstractContainerScreen<?> screen) {
        Drag d = drag;
        drag = Drag.NONE;
        visited.clear();
        if (d != Drag.COLLECT) return false;
        if (!leftStart && dragStart != null) ClickSender.send(screen.getMenu(), dragStart.index, 0, ContainerInput.PICKUP);
        dragStart = null;
        return true;
    }
}
