package io.github.profetgit.tidypockets.inv;

import io.github.profetgit.tidypockets.core.Rules;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/** Moves between the player's own slots while no screen is open, through the always-open inventory menu. */
public final class PlayerInv {
    public static final int OFFHAND_BUTTON = 40;

    private PlayerInv() {}

    /** Menu slot of an inventory index in {@link InventoryMenu}. */
    public static int menuSlot(int invIndex) {
        if (invIndex < Inventory.SELECTION_SIZE) return InventoryMenu.USE_ROW_SLOT_START + invIndex;
        if (invIndex == Inventory.SLOT_OFFHAND) return InventoryMenu.SHIELD_SLOT;
        return invIndex;
    }

    /** Swaps inventory slot {@code from} with hotbar slot {@code hotbar} (0-8) or the offhand ({@link #OFFHAND_BUTTON}). */
    public static void swapInto(LocalPlayer p, int from, int hotbarOrOffhand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active()) {
            io.github.profetgit.tidypockets.TidyPockets.LOG.info("[click] swap inv {} into {} from {}", from, hotbarOrOffhand,
                StackWalker.getInstance().walk(f -> f.skip(1).limit(3).map(x -> x.getClassName().replaceAll(".*\\.", "") + "." + x.getMethodName()).toList()));
        }
        mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, menuSlot(from), hotbarOrOffhand, ContainerInput.SWAP, p);
    }

    public static List<Rules.Candidate> candidates(LocalPlayer p, ItemStack like, int skip) {
        Inventory inv = p.getInventory();
        List<Rules.Candidate> out = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (i == skip || SlotLocks.isLocked(i)) continue;
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(like.getItem())) continue;
            boolean exact = ItemStack.isSameItemSameComponents(s, like);
            boolean sameEnch = Objects.equals(s.get(DataComponents.ENCHANTMENTS), like.get(DataComponents.ENCHANTMENTS));
            int remaining = s.isDamageableItem() ? s.getMaxDamage() - s.getDamageValue() : Integer.MAX_VALUE;
            out.add(new Rules.Candidate(i, exact, sameEnch, remaining, s.getCount()));
        }
        return out;
    }

    public static int firstEmptyMain(LocalPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = Inventory.SELECTION_SIZE; i < Inventory.INVENTORY_SIZE; i++) {
            if (inv.getItem(i).isEmpty() && !SlotLocks.isLocked(i)) return i;
        }
        return -1;
    }
}
