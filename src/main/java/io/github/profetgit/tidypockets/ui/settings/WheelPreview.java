package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Wheel move: four notches over a stack send its items into the chest one at a time, four the other way bring them back,
// each flying (FlyAnims) and bumping its slots, so the loop closes on the stacks it started with. "Invert" swaps which
// way is which. Off, scrolling over a plain stack does nothing, as in vanilla.
public final class WheelPreview extends ChestStage {
    private static final float PUSH = 700, PULL = 2400, EVERY = 320, PERIOD = 4100;
    private static final int NOTCHES = 4, CHEST_SLOTS = 18, HOVER = CHEST_SLOTS + 4, TARGET = 4;

    private ItemStack[] start;

    public WheelPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(36);
        Item[] items = {Items.OAK_LOG, Items.IRON_INGOT, Items.COAL, Items.BREAD, Items.GOLD_INGOT, Items.ARROW, Items.APPLE,
            Items.REDSTONE, Items.TORCH, Items.IRON_SWORD, Items.COOKED_BEEF, Items.STICK, Items.DIAMOND};
        int[] at = {0, 1, 2, 3, 6, 10, 15, 18, 20, 24, 29, 31, 34}, n = {24, 17, 33, 6, 9, 28, 4, 40, 19, 1, 7, 22, 3};
        for (int i = 0; i < at.length; i++) s[at[i]] = new ItemStack(items[i], n[i]);
        s[HOVER] = new ItemStack(Items.COBBLESTONE, 8);
        return s;
    }

    // one item from slot a to slot b, as the mod's three clicks leave them
    private static void one(ItemStack[] s, int a, int b) {
        if (s[a].isEmpty()) return;
        s[b] = s[b].isEmpty() ? s[a].copyWithCount(1) : s[b].copyWithCount(s[b].getCount() + 1);
        s[a] = s[a].getCount() == 1 ? ItemStack.EMPTY : s[a].copyWithCount(s[a].getCount() - 1);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("wheelMove", true), invert = flag("wheelInvert", false), fly = flag("animFly", true),
            bump = flag("animCountBump", true);
        double speed = speed();
        int ox = x + (w - W) / 2;
        Panel p = new Panel(ox + WIN_X, y + 14, 7, 2, 2, false);
        backdrop(g, ox, y);
        if (!itemsReady()) {
            p.draw(g);
            noWorld(g, x, y, w, h);
            return;
        }
        if (start == null) start = stacks();
        float time = time(c, PERIOD, enabled, invert, fly, bump, speed);
        p.draw(g);
        Board board = new Board(start, time, speed, fly, bump);
        for (int k = 0; k < NOTCHES; k++) board.change(PUSH + k * EVERY, true, s -> {
            if (enabled) one(s, HOVER, TARGET);
        });
        for (int k = 0; k < NOTCHES; k++) board.change(PULL + k * EVERY, true, s -> {
            if (enabled) one(s, TARGET, HOVER);
        });
        board.draw(g, p, HOVER);
        // the tip near the slot's corner, so the count stays readable
        pointer(g, p.sx(HOVER), p.sy(HOVER), 0);

        // scrolling down moves items out of the hovered slot, unless inverted
        boolean pulling = time >= PULL - 300;
        boolean down = false;
        for (int k = 0; k < NOTCHES; k++) down |= within(time, PUSH + k * EVERY, 150) || within(time, PULL + k * EVERY, 150);
        wheel(g, pulling != invert, x + w / 2f, y + KEY_Y, down);
        off(g, x, y, w, h, enabled ? null : "wheelMove");
    }
}
