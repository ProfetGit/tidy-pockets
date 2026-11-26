package io.github.profetgit.tidypockets.ui.settings;

//? if >=26.3 {
import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.HudAnims;
import io.github.profetgit.tidypockets.core.Dice;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Random palette: a wall is built bottom row first, one use of the key per block. Three hotbar slots carry the die; the
// mod's Dice picks which of them places, the selector glides there and the slot hops (HudAnims). Off, only the held
// block places.
public final class RandomPreview extends ChestStage {
    private static final float FIRST = 500, EVERY = 140, HOLD = 1100;
    private static final int COLS = 9, ROWS = 3, BLOCKS = COLS * ROWS, PALETTE = 3;
    // a fixed run of dice rolls, so the loop builds the same wall every time
    private static final long SEED = 51;
    private static final String[] TEXTURE = {"cobblestone", "bricks", "oak_planks"};
    private static final Identifier DICE = Identifier.fromNamespaceAndPath("tidypockets", "dice");

    private ItemStack[] bar;

    public RandomPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(9);
        s[0] = new ItemStack(Items.COBBLESTONE, 64);
        s[1] = new ItemStack(Items.BRICKS, 48);
        s[2] = new ItemStack(Items.OAK_PLANKS, 56);
        s[5] = new ItemStack(Items.IRON_PICKAXE);
        s[6] = new ItemStack(Items.IRON_SHOVEL);
        s[7] = new ItemStack(Items.TORCH, 40);
        s[8] = new ItemStack(Items.BREAD, 12);
        return s;
    }

    // which palette slot places each block, in building order
    private static int[] rolls(boolean spread) {
        java.util.Random random = new java.util.Random(SEED);
        int[] out = new int[BLOCKS];
        int last = -1;
        for (int n = 0; n < BLOCKS; n++) {
            int col = n % COLS, row = n / COLS;
            List<Dice.Option> options = new ArrayList<>();
            for (int slot = 0; slot < PALETTE; slot++) {
                int faces = 0, edges = 0;
                if (col > 0 && out[n - 1] == slot) faces++;
                if (row > 0 && out[n - COLS] == slot) faces++;
                if (row > 0 && col > 0 && out[n - COLS - 1] == slot) edges++;
                if (row > 0 && col < COLS - 1 && out[n - COLS + 1] == slot) edges++;
                options.add(new Dice.Option(slot, faces, edges, slot == last));
            }
            out[n] = last = Dice.pick(options, spread, random.nextDouble());
        }
        return out;
    }

    private void stage(Gfx g, int ox, int y, int hx, float t, boolean enabled, int[] rolls, double rate) {
        int wallX = ox + (W - COLS * 16) / 2, wallY = y + 24;
        int placed = t < FIRST ? 0 : Math.min(BLOCKS, 1 + (int) ((t - FIRST) / EVERY)), base = Math.min(COLS, placed) * 16;
        // the shadow grows with the wall's foot
        if (base > 0) shadow(g, wallX + base / 2f, wallY + ROWS * 16, base);
        // the block outline where the next one goes, as the game outlines what you aim at
        if (placed < BLOCKS) {
            int cx = wallX + placed % COLS * 16, cy = wallY + (ROWS - 1 - placed / COLS) * 16;
            g.frame(cx + 1, cy + 1, 14, 14, 0x66FFFFFF);
        }
        for (int n = 0; n < placed; n++) {
            int slot = enabled ? rolls[n] : 0;
            Identifier tex = Identifier.withDefaultNamespace("textures/block/" + TEXTURE[slot] + ".png");
            g.g.blit(RenderPipelines.GUI_TEXTURED, tex, wallX + n % COLS * 16, wallY + (ROWS - 1 - n / COLS) * 16, 0, 0, 16, 16, 16, 16, g.a(0xFFFFFFFF));
        }

        // replay the picks for the selector and the slot hops
        int selected = 0;
        double from = 0, to = 0;
        float glideAt = -1e9f;
        float[] hopped = {-1e9f, -1e9f, -1e9f};
        int[] used = new int[PALETTE];
        for (int n = 0; n < placed; n++) {
            float at = FIRST + n * EVERY;
            int slot = enabled ? rolls[n] : 0;
            used[slot]++;
            if (slot == selected) continue;
            double glided = rate > 0 ? Math.min(1, (at - glideAt) * rate / HudAnims.GLIDE_MS) : 1;
            from = HudAnims.glide(from, to, glided);
            to = slot;
            glideAt = at;
            hopped[slot] = at;
            selected = slot;
        }
        double glided = rate > 0 ? Math.min(1, Math.max(0, (t - glideAt) * rate / HudAnims.GLIDE_MS)) : 1;
        float drawn = (float) HudAnims.glide(from, to, glided);

        hotbar(g, hx, y + 86);
        selector(g, hudX(hx, 0) + drawn * 20, y + 89);
        for (int i = 0; i < 9; i++) {
            ItemStack s = bar[i];
            if (i < PALETTE && used[i] > 0) s = s.copyWithCount(s.getCount() - used[i]);
            double hop = i < PALETTE && rate > 0 ? (t - hopped[i]) * rate / HudAnims.ROLL_MS : 1;
            stack(g, s, hudX(hx, i), y + 89, hop >= 0 && hop < 1 ? HudAnims.rollScale(hop) : 1);
        }
        if (enabled) {
            g.g.nextStratum();
            for (int i = 0; i < PALETTE; i++) g.g.blitSprite(RenderPipelines.GUI_TEXTURED, DICE, hudX(hx, i) - 1, y + 88, 7, 7);
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean enabled = flag("randomEnabled", true), spread = flag("randomSpread", true), hotbarAnim = flag("animHotbar", true);
        double speed = speed(), rate = hotbarAnim ? speed : 0;
        int ox = x + (w - W) / 2, hx = x + (w - HOTBAR_W) / 2;
        backdrop(g, ox, y);
        if (!itemsReady()) {
            hotbar(g, hx, y + 86);
            noWorld(g, x, y, w, h);
            return;
        }
        if (bar == null) bar = stacks();
        float slideAt = FIRST + (BLOCKS - 1) * EVERY + HOLD;
        float time = time(c, slideAt + SLIDE, enabled, spread, hotbarAnim, speed);
        int[] rolls = rolls(spread);
        sliding(g, w, (time - slideAt) / SLIDE, () -> stage(g, ox, y, hx, time, enabled, rolls, rate),
            () -> stage(g, ox, y, hx, 0, enabled, rolls, 0));

        boolean down = time >= FIRST && time < FIRST + BLOCKS * EVERY && (time - FIRST) % EVERY < 80;
        key(g, c, g.mc.options.keyUse, x + w / 2f, y + KEY_Y, down);
        off(g, x, y, w, h, !enabled ? "randomEnabled" : !spread ? "randomSpread" : null);
    }
}
//?}
