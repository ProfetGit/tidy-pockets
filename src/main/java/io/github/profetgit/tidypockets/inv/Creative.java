package io.github.profetgit.tidypockets.inv;

import io.github.profetgit.tidypockets.anim.FlyAnims;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.lock.LockGuard;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.mixin.CreativeSlotWrapperAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The creative inventory. Its menu exists only on the client: the "Survival Inventory" tab wraps the slots of the
 * player's inventory menu, and the item tabs show the item grid above the hotbar. Clicks are applied to the player's
 * inventory menu on the client, and its listener sends every changed slot to the server as a creative slot update,
 * the same way vanilla handles creative clicks.
 */
public final class Creative {
    public enum Tab { NONE, INVENTORY, ITEMS }

    private Creative() {}

    public static Tab tab(Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen c)) return Tab.NONE;
        return c.isInventoryOpen() ? Tab.INVENTORY : Tab.ITEMS;
    }

    public static boolean isMenu(AbstractContainerMenu menu) {
        return menu instanceof CreativeModeInventoryScreen.ItemPickerMenu;
    }

    static Slot unwrap(Slot s) {
        return s instanceof CreativeSlotWrapperAccessor w ? w.tidypockets$target() : s;
    }

    /**
     * Applies a click the way vanilla's creative screen does: an inventory-tab wrapper forwards it to the inventory-menu
     * slot it shows, and an item tab's hotbar slot takes it in the picker menu, where a shift-click deletes the item.
     * Takes the slot, not its index: vanilla adds the wrappers without {@code addSlot}, so every wrapper's
     * {@code index} is 0.
     */
    static void click(AbstractContainerMenu menu, Slot slot, int button, ContainerInput input) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !Inv.isPlayerSlot(slot, p)) return;
        Slot real = unwrap(slot);
        boolean had = slot.hasItem();
        if (real != slot) p.inventoryMenu.clicked(real.index, button, input, p);
        else menu.clicked(slot.index, button, input, p);
        p.inventoryMenu.broadcastChanges();
        if (real == slot) poofIfDeleted(slot, had, input);
    }

    /** A puff where an item-tab shift-click on the hotbar just deleted an item. */
    public static void poofIfDeleted(Slot slot, boolean had, ContainerInput input) {
        if (input == ContainerInput.QUICK_MOVE && had && !slot.hasItem() && TidyConfig.get().animFly) SlotAnims.pop(slot, 0, true);
    }

    // ---- item tabs ----

    /**
     * Shift-drag over the item grid: a full stack of the item goes into the inventory, hotbar first. A partial stack of
     * the same item is topped up instead. A locked empty slot only takes the item it remembers.
     */
    public static boolean grab(AbstractContainerScreen<?> screen, Slot from) {
        LocalPlayer p = Minecraft.getInstance().player;
        ItemStack item = from.getItem();
        if (p == null || item.isEmpty() || !from.mayPickup(p)) return false;
        Inventory inv = p.getInventory();
        int to = partial(inv, item);
        if (to < 0) to = free(inv, item);
        if (to < 0) {
            SlotAnims.shake(from);
            return false;
        }
        ItemStack put = item.copyWithCount(item.getMaxStackSize());
        inv.setItem(to, put);
        p.inventoryMenu.broadcastChanges();
        Slot shown = shown(screen.getMenu(), p, to);
        if (shown != null) FlyAnims.launch(put, from, shown);
        else SlotAnims.bump(from);
        return true;
    }

    /** First step of a grid shift-drag: vanilla's shift-click on the start slot put a full stack on the cursor. */
    public static void stow(AbstractContainerScreen<?> screen, Slot start) {
        AbstractContainerMenu menu = screen.getMenu();
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || !ItemStack.isSameItemSameComponents(carried, start.getItem())) return;
        if (grab(screen, start)) menu.setCarried(ItemStack.EMPTY);
    }

    private static int partial(Inventory inv, ItemStack item) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && ItemStack.isSameItemSameComponents(st, item) && st.getCount() < st.getMaxStackSize()) return i;
        }
        return -1;
    }

    private static int free(Inventory inv, ItemStack item) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (inv.getItem(i).isEmpty() && (!SlotLocks.isLocked(i) || item.is(SlotLocks.remembered(i)))) return i;
        }
        return -1;
    }

    private static Slot shown(AbstractContainerMenu menu, LocalPlayer p, int invIndex) {
        for (Slot s : menu.slots) if (Inv.isPlayerSlot(s, p) && Inv.index(s) == invIndex) return s;
        return null;
    }

    // ---- inventory tab ----

    /**
     * Shift-click on the trash can clears the whole inventory; locked items stay. Returns false when nothing locked is
     * there, so vanilla's own clear runs.
     */
    public static boolean clearUnlocked(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return false;
        List<Slot> kept = new ArrayList<>();
        for (Slot s : screen.getMenu().slots) if (LockGuard.lockedWithItem(s)) kept.add(s);
        if (kept.isEmpty()) return false;
        for (int i = 0; i < p.inventoryMenu.slots.size(); i++) {
            Slot s = p.inventoryMenu.getSlot(i);
            if (LockGuard.lockedWithItem(s)) continue;
            s.set(ItemStack.EMPTY);
            mc.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, i);
        }
        for (Slot s : kept) SlotAnims.shake(s);
        return true;
    }
}
