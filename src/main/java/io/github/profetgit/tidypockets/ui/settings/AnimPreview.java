package io.github.profetgit.tidypockets.ui.settings;

//? if >=26.3 {
import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.HudAnims;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The mod's small animations in turn: three shift-clicked stacks fly into the chest (FlyAnims); a carried stack is
// placed one item at a time and the slot bumps with each (SlotAnims); then number keys move the hotbar selector
// (HudAnims). Speed scales them all; each one switched off happens at once, as in vanilla.
public final class AnimPreview extends ChestStage {
    private static final float SLIDE_AT = 5150, EVERY = 400, TAKE = 2150, PUT = 3200;
    private static final float[] CLICKS = {550, 1000, 1450}, PLACES = {2500, 2720, 2940}, KEYS = {3600, 3950, 4300, 4650};
    private static final int[] FROM = {10, 12, 14}, PICKS = {4, 7, 2, 0};
    private static final int CHEST_SLOTS = 9, TARGET = 7, SOURCE = 16;

    private ItemStack[] start, bar;

    public AnimPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(18);
        s[0] = new ItemStack(Items.COBBLESTONE, 40);
        s[1] = new ItemStack(Items.APPLE, 20);
        s[TARGET] = new ItemStack(Items.IRON_INGOT, 5);
        s[FROM[0]] = new ItemStack(Items.APPLE, 9);
        s[FROM[1]] = new ItemStack(Items.GOLD_INGOT, 14);
        s[FROM[2]] = new ItemStack(Items.BREAD, 12);
        s[SOURCE] = new ItemStack(Items.IRON_INGOT, 9);
        return s;
    }

    private static ItemStack[] hand() {
        ItemStack[] s = empty(9);
        Item[] items = {Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.BOW, Items.COOKED_BEEF, Items.TORCH, Items.COBBLESTONE,
            Items.WATER_BUCKET};
        int[] at = {0, 1, 2, 3, 4, 5, 7, 8}, n = {1, 1, 1, 1, 11, 38, 52, 1};
        for (int i = 0; i < at.length; i++) s[at[i]] = new ItemStack(items[i], n[i]);
        return s;
    }

    private void hud(Gfx g, int hx, int y, float time, double rate) {
        int selected = 0;
        double from = 0, to = 0;
        float glideAt = -1e9f;
        for (int k = 0; k < KEYS.length; k++) {
            if (time < KEYS[k]) break;
            from = HudAnims.glide(from, to, rate > 0 ? Math.min(1, (KEYS[k] - glideAt) * rate / HudAnims.GLIDE_MS) : 1);
            to = selected = PICKS[k];
            glideAt = KEYS[k];
        }
        double glided = rate > 0 ? Math.min(1, Math.max(0, (time - glideAt) * rate / HudAnims.GLIDE_MS)) : 1;
        hotbar(g, hx, y);
        selector(g, hudX(hx, 0) + (float) HudAnims.glide(from, to, glided) * 20, y + 3);
        for (int i = 0; i < 9; i++) stack(g, bar[i], hudX(hx, i), y + 3, 1);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean fly = flag("animFly", true), bump = flag("animCountBump", true), glide = flag("animHotbar", true);
        double speed = speed();
        int ox = x + (w - W) / 2, hx = x + (w - HOTBAR_W) / 2;
        Panel p = new Panel(ox + WIN_X, y + 12, 7, 1, 1, false);
        backdrop(g, ox, y);
        if (!itemsReady()) {
            p.draw(g);
            hotbar(g, hx, y + 87);
            noWorld(g, x, y, w, h);
            return;
        }
        if (start == null) {
            start = stacks();
            bar = hand();
        }
        float time = time(c, SLIDE_AT + SLIDE, fly, bump, glide, speed);
        Board board = new Board(start, time, speed, fly, bump);
        for (int k = 0; k < CLICKS.length; k++) {
            int slot = FROM[k];
            board.change(CLICKS[k], true, s -> quickMove(s, slot, range(0, CHEST_SLOTS)));
        }
        // a stack is picked up, placed one item at a time and the rest put back: plain clicks, so nothing flies
        board.change(TAKE, false, s -> s[SOURCE] = ItemStack.EMPTY);
        for (float at : PLACES) board.change(at, false, s -> s[TARGET] = s[TARGET].copyWithCount(s[TARGET].getCount() + 1));
        int placed = 0;
        for (float at : PLACES) if (time >= at) placed++;
        board.change(PUT, false, s -> s[SOURCE] = new ItemStack(Items.IRON_INGOT, 9 - PLACES.length));
        ItemStack held = time >= TAKE && time < PUT ? new ItemStack(Items.IRON_INGOT, 9 - placed) : ItemStack.EMPTY;

        sliding(g, w, (time - SLIDE_AT) / SLIDE, () -> {
            p.draw(g);
            board.draw(g, p, -1);
        }, () -> {
            p.draw(g);
            new Board(start, 0, speed, fly, bump).draw(g, p, -1);
        });
        hud(g, hx, y + 87, time, glide ? speed : 0);

        float homeX = ox + 180, homeY = y + 74;
        float[] at = path(time, 100, homeX, homeY,
            CLICKS[0] - 60, p.sx(FROM[0]) + 9, p.sy(FROM[0]) + 9, CLICKS[0] + 150, p.sx(FROM[0]) + 9, p.sy(FROM[0]) + 9,
            CLICKS[1] - 60, p.sx(FROM[1]) + 9, p.sy(FROM[1]) + 9, CLICKS[1] + 150, p.sx(FROM[1]) + 9, p.sy(FROM[1]) + 9,
            CLICKS[2] - 60, p.sx(FROM[2]) + 9, p.sy(FROM[2]) + 9, CLICKS[2] + 150, p.sx(FROM[2]) + 9, p.sy(FROM[2]) + 9,
            TAKE - 60, p.sx(SOURCE) + 9, p.sy(SOURCE) + 9, TAKE + 60, p.sx(SOURCE) + 9, p.sy(SOURCE) + 9,
            // over the target's top corner, so its count stays readable beside the carried stack
            PLACES[0] - 60, p.sx(TARGET) + 1, p.sy(TARGET) + 1, PLACES[2] + 60, p.sx(TARGET) + 1, p.sy(TARGET) + 1,
            PUT - 40, p.sx(SOURCE) + 9, p.sy(SOURCE) + 9, PUT + 100, p.sx(SOURCE) + 9, p.sy(SOURCE) + 9, PUT + 450, homeX, homeY);
        float down = 0;
        for (float click : new float[] {CLICKS[0], CLICKS[1], CLICKS[2], TAKE, PLACES[0], PLACES[1], PLACES[2], PUT}) {
            down = Math.max(down, press(time, click));
        }
        carried(g, held, at[0], at[1]);
        pointer(g, at[0], at[1], down);
        carriedCount(g, held, at[0], at[1], null);

        if (time < TAKE - 300) {
            boolean clicked = within(time, CLICKS[0], 160) || within(time, CLICKS[1], 160) || within(time, CLICKS[2], 160);
            keyAndMouse(g, c, x + w / 2f, y + KEY_Y, keyName("tidypockets.ui.key.shift"), within(time, CLICKS[0] - 200, CLICKS[2] - CLICKS[0] + 400),
                clicked);
        } else if (time < PLACES[0] - 150 || (time >= PUT - 150 && time < KEYS[0] - 250)) {
            mouse(g, false, x + w / 2f, y + KEY_Y, within(time, TAKE, 160) || within(time, PUT, 160));
        } else if (time < PUT - 150) {
            boolean clicked = within(time, PLACES[0], 110) || within(time, PLACES[1], 110) || within(time, PLACES[2], 110);
            mouse(g, true, x + w / 2f, y + KEY_Y, clicked);
        } else {
            int k = 0;
            while (k < KEYS.length - 1 && time >= KEYS[k + 1] - EVERY / 2) k++;
            key(g, c, g.mc.options.keyHotbarSlots[PICKS[k]], x + w / 2f, y + KEY_Y, within(time, KEYS[k], 180));
        }
        off(g, x, y, w, h, speed <= 0 ? "animSpeed" : !fly ? "animFly" : !bump ? "animCountBump" : !glide ? "animHotbar" : null);
    }
}
//?}
