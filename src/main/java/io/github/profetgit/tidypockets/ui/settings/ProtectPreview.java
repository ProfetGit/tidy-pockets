package io.github.profetgit.tidypockets.ui.settings;

//? if >=26.3 {
import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.HudAnims;
import io.github.profetgit.tidypockets.core.Rules;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Tool protection: a golden pickaxe wears down use by use. Where Rules.wouldBreak says the next use would cross the
// margin, the mod cancels it, swaps in the fresh one, shakes the slot red (HudAnims) and says so. Unprotected it breaks,
// as in vanilla. With "Enchanted only" an enchanted pickaxe and then a plain one take their turn.
public final class ProtectPreview extends ChestStage {
    private static final float FIRST = 400, EVERY = 125, HOLD = 1500;
    private static final int PRESSES = 12, ROWS = 2, HAND = 18 + 4, SPARE = 6;
    private static final Identifier CRUMBS = Identifier.withDefaultNamespace("textures/item/golden_pickaxe.png");

    private ItemStack[] plain, enchanted;

    public ProtectPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack tool(boolean glint) {
        ItemStack s = new ItemStack(Items.GOLDEN_PICKAXE);
        if (glint) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    private static ItemStack[] stacks(boolean glint) {
        ItemStack[] s = empty(27);
        s[1] = new ItemStack(Items.RAW_IRON, 14);
        s[SPARE] = tool(glint);
        s[10] = new ItemStack(Items.COBBLESTONE, 52);
        s[12] = new ItemStack(Items.COAL, 30);
        s[16] = new ItemStack(Items.RAW_GOLD, 6);
        s[18] = new ItemStack(Items.IRON_SWORD);
        s[19] = new ItemStack(Items.BREAD, 12);
        s[20] = new ItemStack(Items.TORCH, 40);
        s[24] = new ItemStack(Items.COBBLESTONE, 17);
        s[HAND] = tool(glint);
        return s;
    }

    // one pickaxe's turn at t ms: what lies where, and when the mod stepped in or the tool broke
    private static final class Turn {
        ItemStack[] s;
        float saved = -1, broke = -1;
        int left;
    }

    private static Turn turn(ItemStack[] start, float t, boolean guarded, int margin) {
        Turn r = new Turn();
        r.s = copy(start);
        ItemStack held = r.s[HAND];
        int max = held.getMaxDamage();
        // the loop starts a few uses short of where something happens
        int remaining = guarded ? Math.min(max, margin + PRESSES) : PRESSES;
        held.setDamageValue(max - remaining);
        for (int k = 0; k < PRESSES; k++) {
            float at = FIRST + k * EVERY;
            if (t < at) break;
            if (r.saved >= 0 || r.broke >= 0) continue;
            ItemStack now = r.s[HAND];
            if (guarded && Rules.wouldBreak(max, now.getDamageValue(), 1, margin)) {
                r.left = max - now.getDamageValue();
                r.s[HAND] = r.s[SPARE];
                r.s[SPARE] = now;
                r.saved = at;
            } else if (now.getDamageValue() + 1 >= max) {
                r.s[HAND] = ItemStack.EMPTY;
                r.broke = at;
            } else {
                now.setDamageValue(now.getDamageValue() + 1);
            }
        }
        return r;
    }

    // when the last thing happens in a turn: with a full-length wear that is the last press
    private static float outcome(boolean guarded, int margin, int max) {
        int remaining = guarded ? Math.min(max, margin + PRESSES) : PRESSES;
        int presses = guarded ? Math.max(1, remaining - margin) : remaining;
        return FIRST + (presses - 1) * EVERY;
    }

    private void stage(Gfx g, Panel p, Turn r, float t, double rate, boolean warn) {
        p.draw(g);
        for (int i = 0; i < 27; i++) {
            if (i != HAND) stack(g, r.s[i], p.sx(i), p.sy(i), 1);
        }
        int hx = p.sx(HAND), hy = p.sy(HAND);
        selector(g, hx, hy);
        double shake = r.saved >= 0 && warn && rate > 0 ? (t - r.saved) * rate / HudAnims.SHAKE_MS : 1;
        float dx = shake < 1 ? Math.round(HudAnims.shakeOffset(shake) * 2) / 2f : 0;
        stack(g, r.s[HAND], hx + dx, hy, 1);
        if (shake < 1) {
            g.g.nextStratum();
            g.rect(hx, hy, 16, 16, (HudAnims.shakeAlpha(shake) << 24) | 0xE02020);
        }
        if (r.broke >= 0) crumbs(g, hx, hy, t - r.broke);
    }

    // the broken tool's bits, thrown as vanilla throws item particles
    private static void crumbs(Gfx g, int x, int y, float since) {
        float s = since / 1000f;
        if (s > 0.45f) return;
        float[][] from = {{3, 3, -34, -62}, {9, 2, 30, -70}, {6, 7, -8, -86}, {2, 11, -46, -30}, {11, 9, 44, -38}, {8, 12, 12, -48}};
        g.g.nextStratum();
        for (float[] f : from) {
            g.pushTranslate(x + f[0] + f[2] * s, y + f[1] + f[3] * s + 260 * s * s);
            g.g.blit(RenderPipelines.GUI_TEXTURED, CRUMBS, 0, 0, f[0] + 1, f[1], 4, 4, 16, 16);
            g.pop();
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        int mode = num("protect", 0), margin = num("protectMargin", 0);
        boolean warn = flag("protectWarn", true), hotbarAnim = flag("animHotbar", true);
        double speed = speed(), rate = hotbarAnim ? speed : 0;
        int ox = x + (w - W) / 2;
        Panel p = new Panel(ox + WIN_X, y + 8, 7, 0, ROWS, true);
        backdrop(g, ox, y);
        if (!itemsReady()) {
            p.draw(g);
            noWorld(g, x, y, w, h);
            return;
        }
        if (plain == null) {
            plain = stacks(false);
            enchanted = stacks(true);
        }
        boolean two = mode == 1;
        int max = plain[HAND].getMaxDamage();
        float first = outcome(mode != 2, margin, max) + HOLD;
        float second = two ? outcome(false, margin, max) + HOLD : 0;
        float period = first + SLIDE + (two ? second + SLIDE : 0);
        float time = time(c, period, mode, margin, warn, hotbarAnim, speed);

        boolean later = two && time >= first + SLIDE;
        float t = later ? time - first - SLIDE : time;
        float slideAt = later ? second : first;
        ItemStack[] start = mode == 1 && !later ? enchanted : plain;
        boolean guarded = mode == 0 || (mode == 1 && !later);
        Turn now = turn(start, t, guarded, margin);
        ItemStack[] nextStart = two && !later ? plain : mode == 1 ? enchanted : plain;
        Turn next = turn(nextStart, 0, mode == 0 || (mode == 1 && later), margin);
        sliding(g, w, (t - slideAt) / SLIDE, () -> stage(g, p, now, t, rate, warn), () -> stage(g, p, next, 0, 0, warn));

        if (now.saved >= 0 && warn && t < slideAt) {
            String msg = t("protect.swapped", now.s[HAND].getHoverName().getString(), now.left);
            // two lines, broken before the count where that fits, so the brackets stay whole
            int wide = Math.min(w - 8, 180), cut = msg.indexOf(" (");
            List<String> lines = cut > 0 && g.vanillaWidth(msg.substring(0, cut)) <= wide && g.vanillaWidth(msg.substring(cut + 1)) <= wide
                ? List.of(msg.substring(0, cut), msg.substring(cut + 1)) : Rows.wrap(g, msg, wide, 3);
            for (int i = 0; i < lines.size(); i++) {
                g.vanilla(lines.get(i), x + (w - g.vanillaWidth(lines.get(i))) / 2, y + 88 + i * 10, 0xFFFFFFFF, true);
            }
        }
        boolean down = false;
        for (int k = 0; k < PRESSES; k++) {
            float at = FIRST + k * EVERY;
            if (at <= outcome(guarded, margin, max)) down |= within(t, at, 80);
        }
        key(g, c, g.mc.options.keyAttack, x + w / 2f, y + KEY_Y, down);
        // with "Enchanted only" the plain pickaxe's turn is the game without protection
        off(g, x, y, w, h, mode == 2 ? "protect" : later ? "plain" : !warn ? "protectWarn" : null);
    }
}
//?}
