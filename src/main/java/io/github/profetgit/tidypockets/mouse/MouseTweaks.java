package io.github.profetgit.tidypockets.mouse;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.lock.LockGuard;
import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.inv.ClickSender;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 * <p>
 * Creative inventory: the "Survival Inventory" tab works like the survival inventory (its trash can is never dragged
 * over). In the item tabs only shift-drag works, and where it starts decides what it does: from the item grid, each
 * item passed goes into the inventory as a full stack; from the hotbar row, each hotbar item passed is deleted, like
 * vanilla's shift-click there. Neither crosses into the other row. The wheel scrolls the grid there.
 */
public final class MouseTweaks {
    private enum Drag { NONE, SHIFT, COLLECT, GRAB }

    private static Drag drag = Drag.NONE;
    private static final Set<Slot> visited = new HashSet<>();
    private static Slot dragStart;
    private static boolean leftStart;

    private MouseTweaks() {}

    private static boolean usable(AbstractContainerScreen<?> screen) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !Conflicts.mouse && !ClickSender.busy();
    }

    // ---- wheel ----

    public static boolean scrolled(AbstractContainerScreen<?> screen, Slot hovered, double scrollY, boolean shift) {
        TidyConfig cfg = TidyConfig.get();
        if (!cfg.wheelMove || scrollY == 0 || hovered == null || !usable(screen) || Creative.tab(screen) == Creative.Tab.ITEMS) return false;
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
        } else if ((menu instanceof InventoryMenu || Creative.isMenu(menu)) && fromPlayer) {
            int i = Inv.index(from);
            if (i < Inventory.SELECTION_SIZE) out.addAll(Inv.playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE));
            else if (i < Inventory.INVENTORY_SIZE) out.addAll(Inv.playerSlots(menu, p, 0, Inventory.SELECTION_SIZE));
        }
        out.removeIf(s -> Inv.isPlayerSlot(s, p) && SlotLocks.isLocked(Inv.index(s)));
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
            ClickSender.send(menu, from, 0, ContainerInput.QUICK_MOVE);
            return true;
        }
        Slot to = target(other, stack);
        if (to == null) return true;
        int n = stack.getCount();
        ClickSender.send(menu, from, 0, ContainerInput.PICKUP);
        ClickSender.send(menu, to, 1, ContainerInput.PICKUP);
        if (n > 1) ClickSender.send(menu, from, 0, ContainerInput.PICKUP);
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
            ClickSender.send(menu, src, 0, ContainerInput.QUICK_MOVE);
        } else {
            int n = src.getItem().getCount();
            ClickSender.send(menu, src, 0, ContainerInput.PICKUP);
            ClickSender.send(menu, into, 1, ContainerInput.PICKUP);
            if (n > 1) ClickSender.send(menu, src, 0, ContainerInput.PICKUP);
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
        Creative.Tab tab = Creative.tab(screen);
        if (tab != Creative.Tab.NONE && !Inv.isPlayerSlot(slot, Minecraft.getInstance().player)) {
            // the item grid, or the inventory tab's trash can; vanilla's shift-click puts a full stack on the cursor
            if (tab == Creative.Tab.ITEMS && e.hasShiftDown() && carried.isEmpty() && cfg.shiftDrag) {
                drag = Drag.GRAB;
                dragStart = slot;
                leftStart = false;
                visited.add(slot);
            }
            return false;
        }
        if (e.hasShiftDown() && carried.isEmpty() && cfg.shiftDrag) {
            drag = Drag.SHIFT;
            visited.add(slot);
            return false;
        }
        if (LockGuard.lockedWithItem(slot)) return false;
        if (!e.hasShiftDown() && cfg.collectDrag && tab != Creative.Tab.ITEMS && !carried.isEmpty() && slot.hasItem()
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
        if (drag == Drag.NONE || slot == null || visited.contains(slot)) return drag == Drag.COLLECT || drag == Drag.GRAB;
        AbstractContainerMenu menu = screen.getMenu();
        boolean playerSlot = Inv.isPlayerSlot(slot, Minecraft.getInstance().player);
        if (drag == Drag.GRAB) {
            if (playerSlot) return true;
            visited.add(slot);
            if (!leftStart) {
                leftStart = true;
                Creative.stow(screen, dragStart);
            }
            Creative.grab(screen, slot);
            return true;
        }
        visited.add(slot);
        if (drag == Drag.SHIFT) {
            if (Creative.isMenu(menu) && !playerSlot) return false;
            if (LockGuard.lockedWithItem(slot)) {
                SlotAnims.shake(slot);
            } else if (slot.hasItem() && slot.mayPickup(Minecraft.getInstance().player)) {
                Anims.beginMove(menu);
                ClickSender.send(menu, slot, 0, ContainerInput.QUICK_MOVE);
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
        ClickSender.send(menu, s, 0, ContainerInput.PICKUP);
        ClickSender.send(menu, s, 0, ContainerInput.PICKUP);
    }

    public static boolean released(AbstractContainerScreen<?> screen) {
        Drag d = drag;
        drag = Drag.NONE;
        visited.clear();
        if (d != Drag.COLLECT) return false;
        if (!leftStart && dragStart != null) ClickSender.send(screen.getMenu(), dragStart, 0, ContainerInput.PICKUP);
        dragStart = null;
        return true;
    }
}
