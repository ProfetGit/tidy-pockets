package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.anim.SortPop;
import io.github.profetgit.tidypockets.input.Keys;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// A messy chest, a click of the sort binding, and the mod's own sort: the stacks it changes puff and pop back in a
// diagonal wave (SortPop, SlotAnims), in the order its planner gives for the chosen sort order. With sorting off the
// click does nothing, as in vanilla survival. The hotbar variant shows the player's inventory, hotbar included.
public final class SortPreview extends ChestStage {
    private static final float CLICK = 800, SLIDE_AT = 1900;
    // where the chest's stacks lie before the sort
    private static final int[] SLOTS = {14, 17, 24, 26, 23, 16, 18, 6, 5, 21, 15, 20, 1, 7, 4, 2, 22, 8};
    private static final int[] BAR_SLOTS = {27, 28, 30, 33, 35};

    private final boolean inventory;
    private ItemStack[] mess, tidy;
    private int order = -1;
    private boolean withBar;

    public SortPreview(ModDef mod, boolean inventory) {
        super(mod);
        this.inventory = inventory;
    }

    static ItemStack[] stacks(int size, boolean inventory) {
        Item[] item = {Items.COBBLESTONE, Items.APPLE, Items.OAK_LOG, Items.IRON_PICKAXE, Items.GOLD_INGOT, Items.COBBLESTONE,
            Items.BREAD, Items.ARROW, Items.DIRT, Items.DIAMOND, Items.APPLE, Items.COAL, Items.STONE_SWORD, Items.IRON_INGOT,
            Items.COOKED_BEEF, Items.ENDER_PEARL, Items.OAK_LOG, Items.COAL};
        int[] n = {23, 5, 32, 1, 12, 38, 9, 24, 51, 3, 7, 18, 1, 27, 14, 6, 11, 30};
        ItemStack[] out = empty(size);
        for (int i = 0; i < SLOTS.length; i++) out[SLOTS[i]] = new ItemStack(item[i], n[i]);
        if (inventory) {
            ItemStack[] bar = {new ItemStack(Items.STONE_AXE), new ItemStack(Items.TORCH, 40), new ItemStack(Items.COOKED_BEEF, 6),
                new ItemStack(Items.COBBLESTONE, 17), new ItemStack(Items.WATER_BUCKET)};
            for (int i = 0; i < BAR_SLOTS.length; i++) out[BAR_SLOTS[i]] = bar[i];
        }
        return out;
    }

    // the chest before and after, from the mod's planner; slots past the sorted region keep their stacks
    private void arrange(int sortOrder, boolean hotbar) {
        if (mess != null && order == sortOrder && withBar == hotbar) return;
        order = sortOrder;
        withBar = hotbar;
        mess = stacks(inventory ? 36 : 27, inventory);
        tidy = sort(mess, inventory && hotbar ? 36 : 27).tidy();
    }

    // the window and its stacks; sorted, the ones the sort changed are ms animation ms into their pop
    private void chest(Gfx g, int wx, int wy, boolean sorted, double ms, double rate) {
        window(g, wx, wy, inventory);
        for (int i = 0; i < mess.length; i++) {
            boolean moved = sorted && !ItemStack.matches(mess[i], tidy[i]);
            double scale = moved && rate > 0 ? SlotAnims.popScale(ms, SortPop.delay(i % 9, i / 9)) : 1;
            stack(g, (sorted ? tidy : mess)[i], slotX(wx, i), slotY(wy, i), scale);
        }
        if (!sorted || rate <= 0) return;
        g.nextStratum();
        for (int i = 0; i < mess.length; i++) {
            if (ItemStack.matches(mess[i], tidy[i])) continue;
            int frame = SlotAnims.puffFrame(ms, SortPop.delay(i % 9, i / 9));
            if (frame >= 0) g.sprite(SlotAnims.puffSprite(frame), slotX(wx, i), slotY(wy, i), 16, 16);
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("sortEnabled", true), hotbar = flag("sortHotbar", false), anim = flag("animSort", true);
        int sortOrder = num("sortOrder", 0), speed = num("animSpeed", 10);
        int ox = x + (w - W) / 2, wx = ox + WIN_X, wy = y + (inventory ? 20 : 28);
        backdrop(g, ox, y);
        if (!itemsReady()) {
            window(g, wx, wy, inventory);
            noWorld(g, x, y, w, h);
            return;
        }
        double rate = anim ? speed / 10.0 : 0;
        // a slow wave moves the slide back, so the sorted chest is always held
        float slideAt = rate > 0 ? Math.max(SLIDE_AT, (float) (CLICK + 170 / rate + 900)) : SLIDE_AT;
        float period = slideAt + SLIDE;
        float time = time(c, period, enabled, hotbar, sortOrder, anim, speed);
        arrange(sortOrder, hotbar);

        boolean sorted = enabled && time >= CLICK;
        double ms = (time - CLICK) * rate;
        // the loop closes by sliding the next messy chest in; with sorting off there is nothing to reset
        sliding(g, w, enabled ? (time - slideAt) / SLIDE : 0, () -> chest(g, wx, wy, sorted, ms, rate), () -> chest(g, wx, wy, false, 0, 0));

        // the pointer arrives, clicks on an empty corner and leaves
        float go = smooth((time - 180) / 500), stay = go * (1 - smooth((time - slideAt + 600) / 550));
        float homeX = ox + 178, homeY = y + (inventory ? 124 : 108), atX = slotX(wx, 24) + 6, atY = slotY(wy, 24) + 6;
        float lift = time < CLICK ? (float) Math.sin(Math.PI * go) * 5 : 0;
        float down = press(time, CLICK);
        pointer(g, lerp(homeX, atX, stay), lerp(homeY, atY, stay) - lift, down);

        key(g, c, Keys.SORT, x + w / 2f, y + KEY_Y, time >= CLICK && time < CLICK + 200);
        off(g, x, y, w, h, !enabled ? "sortEnabled" : inventory ? hotbar ? null : "sortHotbar" : !anim || speed <= 0 ? "animSort" : null);
    }
}
