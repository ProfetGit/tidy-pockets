package io.github.profetgit.tidypockets.sort;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.core.ClickPlanner;
import io.github.profetgit.tidypockets.core.Stack;
import io.github.profetgit.tidypockets.inv.ClickSender;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.inv.StackKeys;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

public final class SortAction {
    private SortAction() {}

    /** Sorts the region under {@code hovered} (null = background). Returns false if nothing was done. */
    public static boolean trySort(AbstractContainerScreen<?> screen, Slot hovered) {
        TidyConfig cfg = TidyConfig.get();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!cfg.sortEnabled || Conflicts.sorting || p == null || Creative.tab(screen) == Creative.Tab.ITEMS) return false;
        AbstractContainerMenu menu = screen.getMenu();
        if (!menu.getCarried().isEmpty() || ClickSender.busy()) return false;
        Inv.Region region = Inv.sortRegion(menu, hovered, p, cfg.sortHotbar);
        if (region == null) return false;
        if (region.kind() == Inv.Kind.MAIN && !cfg.sortHotbar) pullRemembered(menu, p);

        List<Slot> slots = region.slots();
        Stack[] before = StackKeys.read(slots);
        boolean[] locked = new boolean[slots.size()];
        if (region.kind() != Inv.Kind.CONTAINER) {
            for (int i = 0; i < locked.length; i++) locked[i] = SlotLocks.isLocked(Inv.index(slots.get(i)));
        }
        ClickPlanner.Plan plan = ClickPlanner.plan(before, locked);
        ClickSender.clicks(menu, slots, plan.clicks());

        boolean[] moved = new boolean[slots.size()];
        boolean any = false;
        for (int i = 0; i < moved.length; i++) {
            moved[i] = !Objects.equals(before[i], plan.target()[i]);
            any |= moved[i];
        }
        Anims.sorted(slots, moved);
        if (cfg.sortSound) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_INSERT, any ? 1.35f : 1.7f, 0.45f));
        }
        return true;
    }

    /** Refills empty locked hotbar slots that remember an item, from the main inventory. */
    private static void pullRemembered(AbstractContainerMenu menu, LocalPlayer p) {
        List<Slot> hotbar = Inv.playerSlots(menu, p, 0, Inventory.SELECTION_SIZE);
        List<Slot> main = Inv.playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE);
        for (Slot h : hotbar) {
            if (h.hasItem() || !SlotLocks.isLocked(Inv.index(h))) continue;
            Item want = SlotLocks.remembered(Inv.index(h));
            if (want == null) continue;
            Slot best = null;
            for (Slot m : main) {
                if (SlotLocks.isLocked(Inv.index(m)) || !m.getItem().is(want)) continue;
                if (best == null || m.getItem().getCount() > best.getItem().getCount()) best = m;
            }
            if (best == null) continue;
            ClickSender.send(menu, best, 0, ContainerInput.PICKUP);
            ClickSender.send(menu, h, 0, ContainerInput.PICKUP);
        }
    }
}
