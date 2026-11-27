package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.anim.SortPop;
import io.github.profetgit.tidypockets.input.Keys;
import net.minecraft.world.item.ItemStack;

// Safe mode on the sort of a messy chest: the planner's left clicks go out N per client tick (ClickSender), so the items
// travel through the cursor tick by tick while the sort's pop (SortPop) plays from the start, as in the game. Off, every
// click leaves in the same frame. The bar under the chest has one segment per tick.
public final class SafeSortPreview extends ChestStage {
    private static final float CLICK = 700, TICK = 50, HOLD = 1100;
    private static final int BAR_W = 120;

    private ItemStack[] mess, tidy;
    private java.util.List<Integer> clicks;
    private int order = -1;

    public SafeSortPreview(ModDef mod) {
        super(mod);
    }

    private void arrange(int sortOrder) {
        if (mess != null && order == sortOrder) return;
        order = sortOrder;
        mess = SortPreview.stacks(27, false);
        Sorted s = sort(mess, 27);
        tidy = s.tidy();
        clicks = s.clicks();
    }

    // the chest after {@code ticks} ticks of clicks, ms animation ms into the sort's pop; returns the cursor's stack
    private ItemStack chest(Gfx g, int wx, int wy, boolean sorted, int ticks, int per, float time, double rate, boolean anim, boolean bump) {
        window(g, wx, wy, false);
        ItemStack[] now = copy(mess);
        ItemStack held = ItemStack.EMPTY;
        float[] bumpAt = new float[27];
        java.util.Arrays.fill(bumpAt, -1e9f);
        for (int k = 0, n = 0; k < ticks && n < clicks.size(); k++) {
            // safe mode's first batch leaves on the next tick; without it everything leaves with the key press
            float at = per >= clicks.size() ? CLICK : CLICK + (k + 1) * TICK;
            ItemStack[] before = copy(now);
            for (int j = 0; j < per && n < clicks.size(); j++, n++) held = leftClick(now, held, clicks.get(n));
            for (int i = 0; i < 27; i++) {
                int count = now[i].isEmpty() ? 0 : now[i].getCount(), was = before[i].isEmpty() ? 0 : before[i].getCount();
                boolean popped = ItemStack.matches(mess[i], tidy[i]) || !anim
                    || (at - CLICK) * rate >= SortPop.delay(i % 9, i / 9) + SlotAnims.POP_MS;
                if (bump && popped && count > 0 && count != was) bumpAt[i] = at;
            }
        }
        double ms = (time - CLICK) * rate;
        for (int i = 0; i < 27; i++) {
            double scale = 1, b = rate > 0 ? (time - bumpAt[i]) * rate / SlotAnims.BUMP_MS : 1;
            if (sorted && anim && !ItemStack.matches(mess[i], tidy[i]) && ms < SortPop.delay(i % 9, i / 9) + SlotAnims.POP_MS) {
                scale = SlotAnims.popScale(ms, SortPop.delay(i % 9, i / 9));
            } else if (b >= 0 && b < 1) {
                scale = SlotAnims.bumpScale(b);
            }
            stack(g, now[i], slotX(wx, i), slotY(wy, i), scale);
        }
        if (sorted && anim) {
            g.nextStratum();
            for (int i = 0; i < 27; i++) {
                if (ItemStack.matches(mess[i], tidy[i])) continue;
                int frame = SlotAnims.puffFrame(ms, SortPop.delay(i % 9, i / 9));
                if (frame >= 0) g.sprite(SlotAnims.puffSprite(frame), slotX(wx, i), slotY(wy, i), 16, 16);
            }
        }
        return held;
    }

    // vanilla's left click on a plain slot, with the cursor holding {@code held}; returns what it holds afterwards
    private static ItemStack leftClick(ItemStack[] s, ItemStack held, int i) {
        ItemStack here = s[i];
        if (!held.isEmpty() && same(here, held)) {
            int n = Math.min(held.getCount(), here.getMaxStackSize() - here.getCount());
            s[i] = here.copyWithCount(here.getCount() + n);
            return held.getCount() == n ? ItemStack.EMPTY : held.copyWithCount(held.getCount() - n);
        }
        s[i] = held;
        return here;
    }

    // one segment per tick, lit as its clicks leave
    private static void bar(Gfx g, int x, int y, int ticks, int sent) {
        g.rect(x - 1, y - 1, BAR_W + 2, 6, 0xFF0B0B0D);
        int gap = ticks <= BAR_W / 3 ? 1 : 0;
        for (int i = 0; i < ticks; i++) {
            int a = x + i * BAR_W / ticks, b = x + (i + 1) * BAR_W / ticks - (i < ticks - 1 ? gap : 0);
            boolean lit = i < sent;
            g.rect(a, y, b - a, 4, lit ? 0xFFD9D4D2 : 0xFF3A3634);
            g.rect(a, y, b - a, 1, lit ? 0xFFFFFFFF : 0xFF4E4947);
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("sortEnabled", true), safe = flag("safeMode", false), animSort = flag("animSort", true),
            bump = flag("animCountBump", true);
        int perTick = Math.max(1, num("safeModeClicksPerTick", 8)), sortOrder = num("sortOrder", 0);
        int ox = x + (w - W) / 2, wx = ox + WIN_X, wy = y + 24;
        backdrop(g, ox, y);
        if (!itemsReady()) {
            window(g, wx, wy, false);
            noWorld(g, x, y, w, h);
            return;
        }
        arrange(sortOrder);
        double speed = speed(), rate = animSort ? speed : 0;
        int per = safe ? perTick : clicks.size(), ticks = (clicks.size() + per - 1) / per;
        float run = safe ? ticks * TICK : 0;
        float pop = rate > 0 ? (float) ((80 + SlotAnims.POP_MS) / rate) : 0;
        float slideAt = CLICK + Math.max(run, pop) + HOLD;
        float time = time(c, slideAt + SLIDE, enabled, safe, perTick, sortOrder, animSort, bump, speed);

        boolean sorted = enabled && time >= CLICK;
        int done = !sorted ? 0 : !safe ? 1 : Math.min(ticks, (int) ((time - CLICK) / TICK));
        ItemStack[] held = {ItemStack.EMPTY};
        sliding(g, w, enabled ? (time - slideAt) / SLIDE : 0,
            () -> held[0] = chest(g, wx, wy, sorted, done, per, time, speed, rate > 0, bump),
            () -> chest(g, wx, wy, false, 0, per, 0, 0, false, false));
        bar(g, x + (w - BAR_W) / 2, y + 103, ticks, time < slideAt ? done : 0);

        // the pointer arrives, clicks on an empty corner and stays until the sort is through
        float go = smooth((time - 120) / 450), stay = go * (1 - smooth((time - slideAt + 500) / 450));
        float atX = slotX(wx, 24) + 6, atY = slotY(wy, 24) + 6;
        float px = lerp(ox + 178, atX, stay), py = lerp(y + 112, atY, stay) - (time < CLICK ? (float) Math.sin(Math.PI * go) * 5 : 0);
        carried(g, held[0], px, py);
        pointer(g, px, py, press(time, CLICK));
        carriedCount(g, held[0], px, py, null);

        key(g, c, Keys.SORT, x + w / 2f, y + KEY_Y, within(time, CLICK, 200));
        off(g, x, y, w, h, !enabled ? "sortEnabled" : !safe ? "safeMode" : null);
    }
}
