package io.github.profetgit.tidypockets.refill;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.core.Rules;
import io.github.profetgit.tidypockets.inv.PlayerInv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Watches the main hand and offhand. When a stack runs out (placed, eaten, thrown, broken) or turns into its empty
 * container (bucket, bowl, bottle), swaps in the best matching stack from the inventory.
 */
public final class Refill {
    private static ItemStack lastMain = ItemStack.EMPTY, lastOff = ItemStack.EMPTY;
    private static int lastSelected = -1;
    private static boolean dropHeld;
    private static int ticks, lastAction = -1000;

    private Refill() {}

    /** Called for every hand action (place, use, attack, mine); only stacks used up right after one are refilled. */
    public static void noteAction() {
        lastAction = ticks;
    }

    public static void tick(Minecraft mc) {
        ticks++;
        LocalPlayer p = mc.player;
        if (p == null) {
            lastMain = lastOff = ItemStack.EMPTY;
            lastSelected = -1;
            return;
        }
        Inventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        ItemStack main = inv.getItem(sel), off = inv.getItem(Inventory.SLOT_OFFHAND);
        boolean dropping = dropHeld || mc.options.keyDrop.isDown();
        dropHeld = mc.options.keyDrop.isDown();
        if (p.isUsingItem()) noteAction();
        boolean active = TidyConfig.get().refillEnabled && !Conflicts.sorting && mc.gui.screen() == null && !p.hasInfiniteMaterials()
            && !dropping && sel == lastSelected && ticks - lastAction <= 20;
        if (active) {
            if (ranOut(lastMain, main)) refill(p, lastMain, sel, sel);
            else if (TidyConfig.get().refillOffhand && ranOut(lastOff, off)) refill(p, lastOff, Inventory.SLOT_OFFHAND, PlayerInv.OFFHAND_BUTTON);
        }
        lastSelected = sel;
        lastMain = inv.getItem(sel).copy();
        lastOff = inv.getItem(Inventory.SLOT_OFFHAND).copy();
    }

    private static boolean ranOut(ItemStack before, ItemStack now) {
        if (before.isEmpty()) return false;
        if (now.isEmpty()) return true;
        return !now.is(before.getItem()) && before.getCount() == 1 && isEmptyContainer(now);
    }

    private static boolean isEmptyContainer(ItemStack s) {
        return s.is(Items.BUCKET) || s.is(Items.BOWL) || s.is(Items.GLASS_BOTTLE);
    }

    private static void refill(LocalPlayer p, ItemStack like, int slot, int button) {
        boolean tool = like.isDamageableItem();
        TidyConfig cfg = TidyConfig.get();
        int minRemaining = cfg.protect == TidyConfig.Protect.OFF ? 0 : cfg.protectMargin + 1;
        int from = Rules.pick(PlayerInv.candidates(p, like, slot), tool, cfg.refillExactMatch && !tool, minRemaining);
        if (from < 0) return;
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active()) {
            io.github.profetgit.tidypockets.TidyPockets.LOG.info("[refill] {} ran out in slot {}, swapping in slot {} ({}), last action {} ticks ago",
                like, slot, from, p.getInventory().getItem(from), ticks - lastAction);
        }
        PlayerInv.swapInto(p, from, button);
        Anims.refilled(slot);
    }
}
