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
    private record Flight(ItemStack stack, ItemStack under, Slot from, Slot to, double start) {}

    public static final double FLY_MS = 65;
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
        ItemStack[] after = new ItemStack[before.length];
        for (int i = 0; i < after.length; i++) after[i] = menu.slots.get(i).getItem();
        double now = Ease.now();
        for (int[] move : moves(before, after)) {
            int d = move[1];
            FLIGHTS.add(new Flight(after[d].copy(), stays(before[d], after[d]), menu.slots.get(move[0]), menu.slots.get(d), now));
        }
    }

    /** The flights between two states of the same slots, as {from, to} index pairs. */
    public static List<int[]> moves(ItemStack[] before, ItemStack[] after) {
        List<Integer> sources = new ArrayList<>(), dests = new ArrayList<>();
        for (int i = 0; i < before.length; i++) {
            ItemStack now = after[i];
            int was = before[i].getCount(), is = now.isEmpty() ? 0 : now.getCount();
            boolean sameItem = before[i].isEmpty() || now.isEmpty() || ItemStack.isSameItemSameComponents(before[i], now);
            if (sameItem && is < was) sources.add(i);
            else if (sameItem && is > was) dests.add(i);
            else if (!sameItem) {
                sources.add(i);
                dests.add(i);
            }
        }
        List<int[]> out = new ArrayList<>();
        for (int d : dests) {
            ItemStack arrived = after[d];
            if (arrived.isEmpty()) continue;
            for (int s : sources) {
                if (s == d || before[s].isEmpty() || !ItemStack.isSameItemSameComponents(before[s], arrived)) continue;
                out.add(new int[] {s, d});
                break;
            }
        }
        return out;
    }

    /** A flight that isn't a diff of two menu states: a creative grid item going into the hotbar. */
    public static void launch(ItemStack stack, ItemStack was, Slot from, Slot to) {
        if (!TidyConfig.get().animFly || Conflicts.fly || !Ease.enabled()) return;
        FLIGHTS.add(new Flight(stack.copy(), stays(was, stack), from, to, Ease.now()));
    }

    /** What of the target's old stack stays in it while a flight adds to it: nothing if it was empty or held something else. */
    public static ItemStack stays(ItemStack was, ItemStack arrived) {
        return !was.isEmpty() && ItemStack.isSameItemSameComponents(was, arrived) ? was : ItemStack.EMPTY;
    }

    /**
     * For a slot a flight is still heading for, the stack to show there until it lands (maybe empty); null otherwise.
     * Vanilla already put the arriving items there, so its own drawing is skipped meanwhile.
     */
    public static ItemStack under(Slot s) {
        for (Flight f : FLIGHTS) if (f.to == s && Ease.progress(f.start, FLY_MS) < 1) return f.under;
        return null;
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
            item(g, f.stack, left + f.from.x, top + f.from.y, left + f.to.x, top + f.to.y, t);
        }
    }

    /** A flying item {@code t} (0..1) of the way from one slot's item position to another's: a short arc, slightly small. */
    public static void item(GuiGraphicsExtractor g, ItemStack stack, double fromX, double fromY, double toX, double toY, double t) {
        double e = Ease.outCubic(t);
        double x = fromX + (toX - fromX) * e;
        double y = fromY + (toY - fromY) * e - Math.sin(Math.PI * t) * 4;
        double sx = 0.85, sy = 0.85;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x + 8, (float) y + 8);
        pose.scale((float) sx, (float) sy);
        g.item(stack, -8, -8);
        pose.popMatrix();
    }

    public static void clear() {
        FLIGHTS.clear();
        snap = null;
    }
}
