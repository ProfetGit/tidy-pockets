package io.github.profetgit.tidypockets.inv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;

/** Which slots of an open menu belong together: the player's main inventory, the hotbar, or a storage container. */
public final class Inv {
    public enum Kind { MAIN, HOTBAR, CONTAINER }

    public record Region(Kind kind, List<Slot> slots) {}

    private Inv() {}

    public static boolean isPlayerSlot(Slot s, Player p) {
        return s.container == p.getInventory();
    }

    /** A player slot's index in {@link Inventory}. Creative's inventory-tab wrappers report their menu index instead. */
    public static int index(Slot s) {
        return Creative.unwrap(s).getContainerSlot();
    }

    public static boolean isStorage(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu
            || menu instanceof DispenserMenu;
    }

    /** Player inventory slots whose inventory index is in [from, to), ordered by that index. */
    public static List<Slot> playerSlots(AbstractContainerMenu menu, Player p, int from, int to) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : menu.slots) {
            int i = index(s);
            if (isPlayerSlot(s, p) && i >= from && i < to) out.add(s);
        }
        out.sort(Comparator.comparingInt(Inv::index));
        return out;
    }

    public static List<Slot> containerSlots(AbstractContainerMenu menu, Player p) {
        List<Slot> out = new ArrayList<>();
        if (!isStorage(menu)) return out;
        for (Slot s : menu.slots) if (!isPlayerSlot(s, p)) out.add(s);
        return out;
    }

    /** The region to sort for a middle click on {@code hovered} (may be null: the panel background). */
    public static Region sortRegion(AbstractContainerMenu menu, Slot hovered, Player p, boolean withHotbar) {
        if (hovered != null && isPlayerSlot(hovered, p)) {
            int i = index(hovered);
            if (i < 0 || i >= Inventory.INVENTORY_SIZE) return null;
            return mainRegion(menu, p, withHotbar);
        }
        List<Slot> c = containerSlots(menu, p);
        if (!c.isEmpty() && (hovered == null || c.contains(hovered))) return new Region(Kind.CONTAINER, c);
        if (hovered == null) return mainRegion(menu, p, withHotbar);
        return null;
    }

    public static Region mainRegion(AbstractContainerMenu menu, Player p, boolean withHotbar) {
        List<Slot> slots = playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE);
        if (withHotbar) slots.addAll(playerSlots(menu, p, 0, Inventory.SELECTION_SIZE));
        return new Region(Kind.MAIN, slots);
    }
}
