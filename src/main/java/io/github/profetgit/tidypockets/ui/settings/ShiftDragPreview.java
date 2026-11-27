package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Shift-drag: Shift and the left button held, the pointer runs along a row; each stack it enters is shift-clicked and flies
// (FlyAnims) to where vanilla's shift-click puts it. Off, only the stack pressed on goes, as in vanilla.
public final class ShiftDragPreview extends ChestStage {
    private static final float PRESS = 800, DRAG = 950, DRAGGED = 2150, RELEASE = 2250, SLIDE_AT = 3000;
    private static final int CHEST_SLOTS = 18, ROW = CHEST_SLOTS + 1, STACKS = 7;

    private ItemStack[] start;

    public ShiftDragPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(36);
        Item[] mine = {Items.BREAD, Items.APPLE, Items.TORCH, Items.GOLD_INGOT, Items.COOKED_BEEF, Items.ARROW, Items.OAK_LOG};
        int[] n = {9, 12, 24, 7, 14, 32, 18};
        for (int i = 0; i < STACKS; i++) s[ROW + i] = new ItemStack(mine[i], n[i]);
        s[0] = new ItemStack(Items.COBBLESTONE, 52);
        s[1] = new ItemStack(Items.APPLE, 20);
        s[2] = new ItemStack(Items.COAL, 31);
        s[5] = new ItemStack(Items.IRON_INGOT, 16);
        s[27] = new ItemStack(Items.IRON_PICKAXE);
        s[28] = new ItemStack(Items.STONE_AXE);
        s[33] = new ItemStack(Items.WATER_BUCKET);
        return s;
    }

    // when the pointer enters the row's k-th stack
    private static float entry(int k) {
        return k == 0 ? PRESS : DRAG + (k - 0.5f) / (STACKS - 1) * (DRAGGED - DRAG);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("shiftDrag", true), fly = flag("animFly", true), bump = flag("animCountBump", true);
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
        float time = time(c, SLIDE_AT + SLIDE, enabled, fly, bump, speed);
        Board board = new Board(start, time, speed, fly, bump);
        for (int k = 0; k < STACKS; k++) {
            int slot = ROW + k;
            if (k == 0 || enabled) board.change(entry(k), true, s -> quickMove(s, slot, range(0, CHEST_SLOTS)));
        }

        float rowY = p.sy(ROW) + 9, firstX = p.sx(ROW) + 8, lastX = p.sx(ROW + STACKS - 1) + 8;
        float[] at;
        if (time < DRAG) at = path(time, 150, ox + 180, y + 106, 700, firstX, rowY);
        else if (time < RELEASE) at = new float[] {lerp(firstX, lastX, Math.min(1, (time - DRAG) / (DRAGGED - DRAG))), rowY};
        else at = path(time, RELEASE + 100, lastX, rowY, RELEASE + 600, ox + 180, y + 106);
        int hover = -1;
        for (int i = 0; i < p.size(); i++) {
            if (at[0] >= p.sx(i) - 1 && at[0] < p.sx(i) + 17 && at[1] >= p.sy(i) - 1 && at[1] < p.sy(i) + 17) hover = i;
        }
        int over = hover;
        sliding(g, w, (time - SLIDE_AT) / SLIDE, () -> {
            p.draw(g);
            board.draw(g, p, time < SLIDE_AT ? over : -1);
        }, () -> {
            p.draw(g);
            new Board(start, 0, speed, fly, bump).draw(g, p, -1);
        });
        boolean held = time >= PRESS && time < RELEASE;
        pointer(g, at[0], at[1], held ? 1 : 0);

        keyAndMouse(g, c, x + w / 2f, y + KEY_Y, keyName("tidypockets.ui.key.shift"), within(time, PRESS - 200, RELEASE - PRESS + 250), held);
        off(g, x, y, w, h, enabled ? null : "shiftDrag");
    }
}
