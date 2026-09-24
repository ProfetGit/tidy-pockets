package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Items that moved between slots fly there along a short arc and land with a squash. Moves are found by diffing the
 * menu before and after our own clicks (and vanilla shift-clicks), which the client applies locally at once.
 */
public final class FlyAnims {
    private record Flight(ItemStack stack, Slot from, Slot to, double start) {}

    static final double FLY_MS = 65;
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static AbstractContainerMenu snapMenu;
    private static ItemStack[] snap;

    private FlyAnims() {}

    public static void begin(AbstractContainerMenu menu) {
        snapMenu = menu;
        snap = new ItemStack[menu.slots.size()];
        for (int i = 0; i < snap.length; i++) snap[i] = menu.slots.get(i).getItem().copy();
    }

    public static void end(AbstractContainerMenu menu) {
        if (menu != snapMenu || snap == null || snap.length != menu.slots.size()) return;
        ItemStack[] before = snap;
        snap = null;
        if (!TidyConfig.get().animFly || Conflicts.fly || !Ease.enabled()) return;
        List<Integer> sources = new ArrayList<>(), dests = new ArrayList<>();
        for (int i = 0; i < before.length; i++) {
            ItemStack now = menu.slots.get(i).getItem();
            int was = before[i].getCount(), is = now.isEmpty() ? 0 : now.getCount();
            boolean sameItem = before[i].isEmpty() || now.isEmpty() || ItemStack.isSameItemSameComponents(before[i], now);
            if (sameItem && is < was) sources.add(i);
            else if (sameItem && is > was) dests.add(i);
            else if (!sameItem) {
                sources.add(i);
                dests.add(i);
            }
        }
        double now = Ease.now();
        for (int d : dests) {
            ItemStack arrived = menu.slots.get(d).getItem();
            if (arrived.isEmpty()) continue;
            for (int s : sources) {
                if (s == d || before[s].isEmpty() || !ItemStack.isSameItemSameComponents(before[s], arrived)) continue;
                Slot to = menu.slots.get(d);
                FLIGHTS.add(new Flight(arrived.copy(), menu.slots.get(s), to, now));
                break;
            }
        }
    }

    /** Draws flights above the slots; called by the screen just before the carried item. */
    public static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
        if (FLIGHTS.isEmpty()) return;
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
        int left = a.tidypockets$left(), top = a.tidypockets$top();
        Iterator<Flight> it = FLIGHTS.iterator();
        while (it.hasNext()) {
            Flight f = it.next();
            if (!screen.getMenu().slots.contains(f.to)) {
                it.remove();
                continue;
            }
            double t = Ease.progress(f.start, FLY_MS);
            if (t >= 1) {
                SlotAnims.bump(f.to);
                it.remove();
                continue;
            }
            double e = Ease.outCubic(t);
            double x = left + f.from.x + (f.to.x - f.from.x) * e;
            double y = top + f.from.y + (f.to.y - f.from.y) * e - Math.sin(Math.PI * t) * 4;
            double sx = 0.85, sy = 0.85;
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) x + 8, (float) y + 8);
            pose.scale((float) sx, (float) sy);
            g.item(f.stack, -8, -8);
            pose.popMatrix();
        }
    }

    public static void clear() {
        FLIGHTS.clear();
        snap = null;
    }
}
