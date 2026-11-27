package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.anim.HudAnims;
import io.github.profetgit.tidypockets.core.Rules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Refill, in three uses of the use key: the splash potion in hand is thrown and its twin swaps in; thrown again with only
// a look-alike left (Exact match decides); then the offhand's last torches go and a new stack comes. Rules.pick chooses,
// HudAnims pops the slot. Off, a used-up hand stays empty, as in vanilla.
public final class RefillPreview extends ChestStage {
    private static final float FIRST = 600, SECOND = 1600, TORCH_A = 2450, TORCH_B = 2750, SLIDE_AT = 3750, TICK = 50;
    // slots of the window: two inventory rows, then the hotbar; the offhand has its own panel
    private static final int ROWS = 2, HAND = 18 + 4, OFF = 27;

    private ItemStack[] start;

    public RefillPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack potion(boolean healing) {
        return Compat.splashPotion(!healing);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(28);
        Item[] items = {Items.BREAD, Items.IRON_INGOT, Items.COBBLESTONE, Items.OAK_LOG, Items.ARROW, Items.APPLE};
        int[] at = {1, 2, 6, 11, 13, 16}, n = {12, 30, 48, 22, 17, 5};
        for (int i = 0; i < at.length; i++) s[at[i]] = new ItemStack(items[i], n[i]);
        s[4] = potion(true);
        s[15] = potion(false);
        s[8] = new ItemStack(Items.TORCH, 24);
        s[18] = new ItemStack(Items.IRON_SWORD);
        s[19] = new ItemStack(Items.IRON_PICKAXE);
        s[20] = new ItemStack(Items.COOKED_BEEF, 9);
        s[24] = new ItemStack(Items.OAK_PLANKS, 40);
        s[HAND] = potion(true);
        s[OFF] = new ItemStack(Items.TORCH, 2);
        return s;
    }

    // the player's inventory index of a window slot
    private static int invIndex(int slot) {
        return slot >= 18 ? slot - 18 : 9 + slot;
    }

    // the mod's pick for a used-up stack of {@code like}, as a window slot, or -1
    private static int pick(ItemStack[] s, ItemStack like, int skip, boolean exact) {
        List<Rules.Candidate> out = new ArrayList<>();
        for (int i = 0; i < OFF; i++) {
            if (i == skip || s[i].isEmpty() || !s[i].is(like.getItem())) continue;
            out.add(new Rules.Candidate(invIndex(i), ItemStack.isSameItemSameComponents(s[i], like), false, Integer.MAX_VALUE, s[i].getCount()));
        }
        int from = Rules.pick(out, false, exact, 0);
        return from < 0 ? -1 : from < 9 ? 18 + from : from - 9;
    }

    private static final class State {
        ItemStack[] s;
        float handPop = -1e9f, offPop = -1e9f;
    }

    // the stacks at that moment of the loop
    private State at(float time, boolean enabled, boolean exact, boolean offhand) {
        State st = new State();
        st.s = copy(start);
        for (float use : new float[] {FIRST, SECOND}) {
            if (time < use) break;
            ItemStack like = st.s[HAND];
            if (like.isEmpty()) continue;
            st.s[HAND] = ItemStack.EMPTY;
            if (!enabled || time < use + TICK) continue;
            int from = pick(st.s, like, HAND, exact);
            if (from >= 0) {
                st.s[HAND] = st.s[from];
                st.s[from] = ItemStack.EMPTY;
                st.handPop = use + TICK;
            }
        }
        if (time >= TORCH_A) st.s[OFF] = st.s[OFF].copyWithCount(1);
        if (time >= TORCH_B) {
            ItemStack like = new ItemStack(Items.TORCH);
            st.s[OFF] = ItemStack.EMPTY;
            if (enabled && offhand && time >= TORCH_B + TICK) {
                int from = pick(st.s, like, OFF, exact);
                if (from >= 0) {
                    st.s[OFF] = st.s[from];
                    st.s[from] = ItemStack.EMPTY;
                    st.offPop = TORCH_B + TICK;
                }
            }
        }
        return st;
    }

    private void stage(Gfx g, Panel p, int offX, int offY, State st, float time, double rate) {
        p.draw(g);
        single(g, offX, offY);
        for (int i = 0; i < OFF; i++) {
            if (i != HAND) stack(g, st.s[i], p.sx(i), p.sy(i), 1);
        }
        stack(g, st.s[HAND], p.sx(HAND), p.sy(HAND), popped(time, st.handPop, rate));
        selector(g, p.sx(HAND), p.sy(HAND));
        if (st.s[OFF].isEmpty()) VanillaArt.shield(g, offX + 8, offY + 8);
        else stack(g, st.s[OFF], offX + 8, offY + 8, popped(time, st.offPop, rate));
    }

    private static double popped(float time, float since, double rate) {
        if (rate <= 0) return 1;
        double p = (time - since) * rate / HudAnims.POP_MS;
        return p >= 0 && p < 1 ? HudAnims.popScale(p) : 1;
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("refillEnabled", true), exact = flag("refillExactMatch", true), offhand = flag("refillOffhand", true),
            hotbarAnim = flag("animHotbar", true);
        double speed = speed(), rate = hotbarAnim ? speed : 0;
        int ox = x + (w - W) / 2;
        Panel p = new Panel(ox + WIN_X, y + 10, 7, 0, ROWS, true);
        int offX = ox + WIN_X, offY = y + 87;
        backdrop(g, ox, y);
        if (!itemsReady()) {
            p.draw(g);
            single(g, offX, offY);
            noWorld(g, x, y, w, h);
            return;
        }
        if (start == null) start = stacks();
        float time = time(c, SLIDE_AT + SLIDE, enabled, exact, offhand, hotbarAnim, speed);
        State st = at(time, enabled, exact, offhand), fresh = at(0, enabled, exact, offhand);
        // with refill off the hands stay empty, so the next full stage still slides in
        sliding(g, w, (time - SLIDE_AT) / SLIDE, () -> stage(g, p, offX, offY, st, time, rate),
            () -> stage(g, p, offX, offY, fresh, 0, 0));

        boolean down = within(time, FIRST, 180) || within(time, SECOND, 180) || within(time, TORCH_A, 150) || within(time, TORCH_B, 150);
        key(g, c, g.mc.options.keyUse, x + w / 2f, y + KEY_Y, down);
        off(g, x, y, w, h, !enabled ? "refillEnabled" : !offhand ? "refillOffhand" : !exact ? "refillExactMatch" : null);
    }
}
