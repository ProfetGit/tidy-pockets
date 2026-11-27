package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Drag to collect: with a stack on the pointer, dragging from a matching stack gathers every matching stack passed into
// it. Off, the same drag is vanilla's: the held stack is spread over the empty and matching slots, shown tinted until
// the button is let go.
public final class CollectDragPreview extends ChestStage {
    private static final float PRESS = 800, DRAG = 950, DRAGGED = 2150, RELEASE = 2350, SLIDE_AT = 3200;
    private static final int FIRST = 10, SLOTS = 7, HELD = 24;

    private ItemStack[] start;

    public CollectDragPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(27);
        int[] path = {8, 6, -1, 9, 0, 7, 4};
        for (int i = 0; i < SLOTS; i++) {
            if (path[i] > 0) s[FIRST + i] = new ItemStack(Items.COBBLESTONE, path[i]);
        }
        s[FIRST + 2] = new ItemStack(Items.APPLE, 5);
        Item[] items = {Items.OAK_LOG, Items.BREAD, Items.IRON_INGOT, Items.TORCH, Items.COAL, Items.GOLD_INGOT, Items.ARROW, Items.BONE};
        int[] at = {1, 4, 7, 9, 17, 19, 22, 25}, n = {21, 8, 13, 30, 16, 5, 27, 11};
        for (int i = 0; i < at.length; i++) s[at[i]] = new ItemStack(items[i], n[i]);
        return s;
    }

    // when the pointer enters the path's k-th slot
    private static float entry(int k) {
        return k == 0 ? DRAG : DRAG + (k - 0.5f) / (SLOTS - 1) * (DRAGGED - DRAG);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("collectDrag", true), bump = flag("animCountBump", true);
        double speed = speed();
        int ox = x + (w - W) / 2, wx = ox + WIN_X, wy = y + 28;
        backdrop(g, ox, y);
        if (!itemsReady()) {
            window(g, wx, wy, false);
            noWorld(g, x, y, w, h);
            return;
        }
        if (start == null) start = stacks();
        float time = time(c, SLIDE_AT + SLIDE, enabled, bump, speed);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        Board board = new Board(start, time, speed, false, bump);
        int[] held = {HELD};
        // vanilla's drag: the slots it has taken so far, each shown with its share
        List<Integer> spread = new ArrayList<>();
        for (int k = 0; k < SLOTS; k++) {
            int slot = FIRST + k;
            if (enabled) {
                // the first slot is collected as the pointer leaves it
                if (k == 0) continue;
                for (int s : k == 1 ? new int[] {FIRST, slot} : new int[] {slot}) {
                    board.change(entry(k), false, stacks -> {
                        if (!stacks[s].is(Items.COBBLESTONE)) return;
                        held[0] += stacks[s].getCount();
                        stacks[s] = ItemStack.EMPTY;
                    });
                }
            } else if (time >= entry(k) && (start[slot].isEmpty() || start[slot].is(Items.COBBLESTONE)) && HELD > spread.size()) {
                spread.add(slot);
            }
        }
        // vanilla shows the shares once the drag has two slots
        boolean dragging = spread.size() > 1 && time < RELEASE;
        int share = spread.isEmpty() ? 0 : HELD / spread.size();
        if (dragging || (!spread.isEmpty() && time >= RELEASE)) {
            held[0] = HELD - share * spread.size();
            board.change(RELEASE, false, stacks -> {
                for (int s : spread) stacks[s] = cobble.copyWithCount((stacks[s].isEmpty() ? 0 : stacks[s].getCount()) + share);
            });
        }
        // while it lasts, vanilla draws each taken slot with the count it is about to have
        ItemStack[] shown = dragging ? copy(board.item) : null;
        if (shown != null) {
            for (int s : spread) shown[s] = cobble.copyWithCount((start[s].isEmpty() ? 0 : start[s].getCount()) + share);
        }

        float rowY = slotY(wy, FIRST) + 9, firstX = slotX(wx, FIRST) + 8, lastX = slotX(wx, FIRST + SLOTS - 1) + 8;
        float[] at;
        if (time < DRAG) at = path(time, 100, ox + 180, y + 106, 700, firstX, rowY);
        else if (time < RELEASE) at = new float[] {lerp(firstX, lastX, Math.min(1, (time - DRAG) / (DRAGGED - DRAG))), rowY};
        else at = path(time, RELEASE + 150, lastX, rowY, RELEASE + 650, ox + 180, y + 106);
        int hover = -1;
        for (int i = 0; i < 27 && time < SLIDE_AT; i++) {
            if (at[0] >= slotX(wx, i) - 1 && at[0] < slotX(wx, i) + 17 && at[1] >= slotY(wy, i) - 1 && at[1] < slotY(wy, i) + 17) hover = i;
        }
        int over = hover;
        float px = at[0], py = at[1];
        sliding(g, w, (time - SLIDE_AT) / SLIDE, () -> {
            window(g, wx, wy, false);
            if (shown != null) {
                if (over >= 0) highlight(g, slotX(wx, over), slotY(wy, over), false);
                for (int s : spread) g.rect(slotX(wx, s), slotY(wy, s), 16, 16, 0x80FFFFFF);
                for (int i = 0; i < 27; i++) stack(g, shown[i], slotX(wx, i), slotY(wy, i), 1);
                if (over >= 0) highlight(g, slotX(wx, over), slotY(wy, over), true);
            } else {
                board.draw(g, i -> slotX(wx, i), i -> slotY(wy, i), over);
            }
        }, () -> {
            window(g, wx, wy, false);
            for (int i = 0; i < 27; i++) stack(g, start[i], slotX(wx, i), slotY(wy, i), 1);
        });

        // the held stack stays on the pointer; a spread that used it all up leaves vanilla's yellow 0 until the release.
        // Halfway through the slide it is the next loop's stack
        if (time >= SLIDE_AT + SLIDE / 2) held[0] = HELD;
        ItemStack mine = held[0] > 0 ? cobble.copyWithCount(held[0]) : dragging ? cobble : ItemStack.EMPTY;
        boolean down = time >= PRESS && time < RELEASE;
        carried(g, mine, px, py);
        pointer(g, px, py, down ? 1 : 0);
        carriedCount(g, mine, px, py, held[0] > 0 ? null : ChatFormatting.YELLOW + "0");

        mouse(g, false, x + w / 2f, y + KEY_Y, down);
        off(g, x, y, w, h, enabled ? null : "collectDrag");
    }
}
