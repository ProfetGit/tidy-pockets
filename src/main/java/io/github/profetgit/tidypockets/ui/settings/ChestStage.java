package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.profetsettings.ModDef;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.Rows;
import io.github.profetgit.profetsettings.Setting;
import io.github.profetgit.tidypockets.anim.FlyAnims;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.core.ClickPlanner;
import io.github.profetgit.tidypockets.core.ItemKey;
import io.github.profetgit.tidypockets.inv.StackKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The stage of the settings previews: the game's own chest window or hotbar floating in a soft pool of light on the box's
// dark, over a contact shadow, and the pointer or key that works it. Windows, slots and items come from the player's
// resources; the slot motion is the mod's own (SlotAnims, FlyAnims).
abstract class ChestStage extends Preview {
    // the art is made for a W x H GUI pixel box; the window is 176 wide at WIN_X
    static final int W = 198, H = 150, WIN_X = 11, WIN_W = 176, HOTBAR_W = 182, KEY_Y = 119;
    static final float SLIDE = 300;
    private static final Identifier GLOW = art("glow"), SHADOW = art("shadow_item"),
        CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png"),
        // the shared settings library's mouse
        MOUSE_LEFT = Identifier.fromNamespaceAndPath("profet_settings", "mouse_left"),
        MOUSE_RIGHT = Identifier.fromNamespaceAndPath("profet_settings", "mouse_right"),
        MOUSE_WHEEL = Identifier.fromNamespaceAndPath("profet_settings", "mouse_wheel");
    private static final String[] ARROW = {
        "X.........", "XX........", "XwX.......", "XwwX......", "XwwwX.....", "XwwwwX....", "XwwwwwX...", "XwwwwwwX..",
        "XwwwwwwwX.", "XwwwwwXXXX", "XwwXwwX...", "XwX.XwwX..", "XX..XwwX..", "X....XwwX.", ".....XXX.."};
    private static final int NOTE = 0xFFF0A566;

    final ModDef mod;

    ChestStage(ModDef mod) {
        this.mod = mod;
    }

    private static Identifier art(String name) {
        return Identifier.fromNamespaceAndPath("tidypockets", "textures/gui/preview/" + name + ".png");
    }

    static String t(String key, Object... args) {
        return I18n.get("tidypockets." + key, args);
    }

    // item stacks can only be made once a world has loaded their components
    static boolean itemsReady() {
        //? if >=26.1 {
        return Items.APPLE.builtInRegistryHolder().areComponentsBound();
        //?} else {
        /*return true;
        *///?}
    }

    // ---- the settings the scenes follow ----

    // a preset scene plays with that preset's values instead of the live ones
    private java.util.Map<String, Object> pinned = java.util.Map.of();
    // set by a PresetPreview: freezes the loop just before its own closing slide
    boolean hold;
    float lastTime, lastPeriod;

    // the one note of a preset that turns everything off, in place of each feature's own
    private String allOff;
    private static final java.util.Set<String> FEATURES = java.util.Set.of("sortEnabled", "refillEnabled", "randomEnabled", "protect", "plain", "animScreenPop", "animSpeed");

    final ChestStage pin(java.util.Map<String, Object> values, String allOff) {
        pinned = values;
        this.allOff = allOff;
        return this;
    }

    final boolean flag(String key, boolean def) {
        Object v = pinned.get(key);
        if (v instanceof Boolean b) return b;
        Setting s = mod.find(key);
        return s == null ? def : s.getBool();
    }

    final int num(String key, int def) {
        Object v = pinned.get(key);
        if (v instanceof Integer n) return n;
        Setting s = mod.find(key);
        return s == null ? def : s.getInt();
    }

    // the mod's animation speed; 0 means every animation is off
    final double speed() {
        return num("animSpeed", 10) / 10.0;
    }

    // ---- time ----

    // restarts the loop when any of the values the scene depends on changes
    float time(Ctx c, float period, Object... settings) {
        float t = clock(c, Objects.hash(settings), period);
        lastTime = t;
        lastPeriod = period;
        return hold ? Math.min(t, period - SLIDE) : t;
    }

    // 0..1 with a soft start and end
    static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    // whether t is within ms after at
    static boolean within(float t, float at, float ms) {
        return t >= at && t < at + ms;
    }

    // a point moving through {time, x, y} stops, easing between them and holding at both ends
    static float[] path(float t, float... k) {
        int n = k.length / 3;
        if (t <= k[0]) return new float[] {k[1], k[2]};
        for (int i = 1; i < n; i++) {
            if (t > k[i * 3]) continue;
            float p = smooth((t - k[i * 3 - 3]) / (k[i * 3] - k[i * 3 - 3]));
            return new float[] {lerp(k[i * 3 - 2], k[i * 3 + 1], p), lerp(k[i * 3 - 1], k[i * 3 + 2], p)};
        }
        return new float[] {k[n * 3 - 2], k[n * 3 - 1]};
    }

    // how far the leaving stage has moved, {@code slide} (0..1) of the way through the slide
    static float slid(int w, float slide) {
        return Math.round(smooth(slide) * w * 2) / 2f;
    }

    // the loop closes by sliding the next stage in from the right as this one leaves
    static void sliding(Gfx g, int w, float slide, Runnable now, Runnable next) {
        float dx = slid(w, slide);
        g.pushTranslate(-dx, 0);
        now.run();
        g.pop();
        if (slide > 0) {
            g.pushTranslate(w - dx, 0);
            next.run();
            g.pop();
        }
    }

    // ---- the stage ----

    static void backdrop(Gfx g, int ox, int y) {
        g.pushTranslate(ox, y);
        g.scale(0.5f, 0.5f);
        g.blit(GLOW, 0, 0, 0, 0, W * 2, H * 2, W * 2, H * 2);
        g.pop();
    }

    // the contact shadow under something width pixels wide, centred on cx, whose bottom edge is at bottom
    static void shadow(Gfx g, float cx, float bottom, int width) {
        float a = g.alpha;
        g.alpha *= 0.95f;
        g.pushTranslate(cx + 2, bottom + 2);
        g.scale(width * (250 / 176f) / 64f, 16 / 32f);
        g.blit(SHADOW, -32, -16, 0, 0, 64, 32, 64, 32);
        g.pop();
        g.alpha = a;
    }

    // the chest window's height with that many pixels of slot rows
    static int windowHeight(boolean hotbar) {
        return hotbar ? 90 : 68;
    }

    // three rows of a chest, or with {@code hotbar} the player's three rows and hotbar, as the game draws them
    static void window(Gfx g, int x, int y, boolean hotbar) {
        shadow(g, x + WIN_W / 2f, y + windowHeight(hotbar), WIN_W);
        piece(g, x, y, 0, 7);
        if (hotbar) {
            piece(g, x, y + 7, 139, 54);
            piece(g, x, y + 61, 193, 22);
        } else {
            piece(g, x, y + 7, 17, 54);
        }
        piece(g, x, y + windowHeight(hotbar) - 7, 215, 7);
    }

    private static void piece(Gfx g, int x, int y, int v, int h) {
        g.blit(CHEST, x, y, 0, v, WIN_W, h, 256, 256);
    }

    // where slot i's item sits in the window at (x, y); slots 27 and up are the hotbar
    static int slotX(int x, int i) {
        return x + 8 + i % 9 * 18;
    }

    static int slotY(int y, int i) {
        return i < 27 ? y + 8 + i / 9 * 18 : y + 66;
    }

    // A window cut to size: a border (head 7) or a title bar (head 17), {@code top} rows of a chest, and under a gap
    // {@code rows} of the player's inventory, with the hotbar below if {@code hotbar}. Slots count from the top.
    record Panel(int x, int y, int head, int top, int rows, boolean hotbar) {
        int height() {
            return head + (top > 0 ? top * 18 + (rows > 0 || hotbar ? 8 : 0) : 0) + rows * 18 + (hotbar ? 22 : 0) + 7;
        }

        private int lower() {
            return y + head + (top > 0 ? top * 18 + 8 : 0);
        }

        int size() {
            return (top + rows + (hotbar ? 1 : 0)) * 9;
        }

        int sx(int i) {
            return x + 8 + i % 9 * 18;
        }

        int sy(int i) {
            int row = i / 9;
            if (row < top) return y + head + 1 + row * 18;
            if (row < top + rows) return lower() + 1 + (row - top) * 18;
            return lower() + rows * 18 + 5;
        }

        void draw(Gfx g) {
            shadow(g, x + WIN_W / 2f, y + height(), WIN_W);
            piece(g, x, y, 0, head);
            int at = y + head;
            if (top > 0) {
                piece(g, x, at, 17, top * 18);
                at += top * 18;
                if (rows > 0 || hotbar) {
                    piece(g, x, at, 128, 8);
                    at += 8;
                }
            }
            if (rows > 0) {
                piece(g, x, at, 139, rows * 18);
                at += rows * 18;
            }
            if (hotbar) {
                piece(g, x, at, 193, 22);
                at += 22;
            }
            piece(g, x, at, 215, 7);
        }
    }

    // a window holding a single slot, 32 x 32, as the offhand's
    static void single(Gfx g, int x, int y) {
        shadow(g, x + 16, y + 32, 32);
        int[][] rows = {{0, 7}, {17, 18}, {215, 7}};
        int at = y;
        for (int[] r : rows) {
            g.blit(CHEST, x, at, 0, r[0], 25, r[1], 256, 256);
            g.blit(CHEST, x + 25, at, 169, r[0], 7, r[1], 256, 256);
            at += r[1];
        }
    }

    // ---- the hotbar of the HUD ----

    static void hotbar(Gfx g, int x, int y) {
        shadow(g, x + HOTBAR_W / 2f, y + 22, HOTBAR_W);
        VanillaArt.hotbar(g, x, y, HOTBAR_W);
    }

    // where the item of the HUD hotbar's slot i sits
    static int hudX(int x, int i) {
        return x + 3 + i * 20;
    }

    // the selector around an item position, which may be between two slots while it glides
    static void selector(Gfx g, float ix, float iy) {
        g.pushTranslate(Math.round(ix * 2) / 2f - 4, iy - 4);
        VanillaArt.selection(g, 0, 0);
        g.pop();
    }

    // ---- items ----

    // a slot's item and count, scaled about the slot's centre as the mod's own slot animations do it
    static void stack(Gfx g, ItemStack s, float x, float y, double scale) {
        if (s.isEmpty() || scale <= 0.02) return;
        g.pushTranslate(x + 8, y + 8);
        g.scale((float) scale, (float) scale);
        g.translate(-8, -8);
        g.item(s, 0, 0);
        g.itemDecorations(s, 0, 0);
        g.pop();
    }

    // vanilla's highlight of the slot under the pointer: one sprite behind its item, one over it
    static void highlight(Gfx g, int ix, int iy, boolean front) {
        if (front) g.nextStratum();
        VanillaArt.highlight(g, ix, iy, front);
    }

    static ItemStack[] empty(int size) {
        ItemStack[] out = new ItemStack[size];
        Arrays.fill(out, ItemStack.EMPTY);
        return out;
    }

    static ItemStack[] copy(ItemStack[] s) {
        ItemStack[] out = new ItemStack[s.length];
        for (int i = 0; i < s.length; i++) out[i] = s[i].copy();
        return out;
    }

    static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    // vanilla's shift-click: slot from merges into matching stacks among {@code to}, then takes the first empty one
    static void quickMove(ItemStack[] s, int from, int[] to) {
        ItemStack m = s[from];
        if (m.isEmpty()) return;
        for (int i : to) {
            if (m.isEmpty()) break;
            if (!same(s[i], m)) continue;
            int n = Math.min(m.getCount(), s[i].getMaxStackSize() - s[i].getCount());
            if (n <= 0) continue;
            s[i] = s[i].copyWithCount(s[i].getCount() + n);
            m = m.getCount() == n ? ItemStack.EMPTY : m.copyWithCount(m.getCount() - n);
        }
        for (int i : to) {
            if (m.isEmpty()) break;
            if (!s[i].isEmpty()) continue;
            s[i] = m;
            m = ItemStack.EMPTY;
        }
        s[from] = m;
    }

    static int[] range(int from, int to) {
        int[] out = new int[to - from];
        for (int i = 0; i < out.length; i++) out[i] = from + i;
        return out;
    }

    // what the mod's planner makes of slots 0..region-1: the stacks afterwards and the left clicks that get them there
    record Sorted(ItemStack[] tidy, List<Integer> clicks) {}

    static Sorted sort(ItemStack[] mess, int region) {
        StackKeys.ensureOrder();
        io.github.profetgit.tidypockets.core.Stack[] before = new io.github.profetgit.tidypockets.core.Stack[region];
        Map<ItemKey, ItemStack> items = new HashMap<>();
        for (int i = 0; i < region; i++) {
            if (mess[i].isEmpty()) continue;
            ItemKey key = StackKeys.key(mess[i], null);
            items.putIfAbsent(key, mess[i]);
            before[i] = new io.github.profetgit.tidypockets.core.Stack(key, mess[i].getCount());
        }
        ClickPlanner.Plan plan = ClickPlanner.plan(before, new boolean[region]);
        io.github.profetgit.tidypockets.core.Stack[] after = plan.target();
        ItemStack[] tidy = mess.clone();
        for (int i = 0; i < region; i++) tidy[i] = after[i] == null ? ItemStack.EMPTY : items.get(after[i].key()).copyWithCount(after[i].count());
        return new Sorted(tidy, plan.clicks());
    }

    // ---- slots over time ----

    private record Flight(ItemStack stack, ItemStack under, int from, int to, float at) {}

    // A stage's slots played from the start of the loop up to {@code now}: every change of the stacks brings the mod's
    // animations with it, as in the game (a count bump, a flight to where the stack went, a bump as it lands).
    static final class Board {
        final ItemStack[] item;
        private final float now;
        private final double rate;
        private final boolean fly, bump;
        private final float[] bumpAt;
        private final List<Flight> flights = new ArrayList<>();

        Board(ItemStack[] start, float now, double rate, boolean fly, boolean bump) {
            this.item = copy(start);
            this.now = now;
            this.rate = rate;
            this.fly = fly;
            this.bump = bump;
            this.bumpAt = new float[start.length];
            Arrays.fill(bumpAt, -1e9f);
        }

        // at time at, if that has come: change the stacks; {@code moved} when it was a move the mod flies
        void change(float at, boolean moved, Consumer<ItemStack[]> change) {
            if (now < at) return;
            ItemStack[] before = copy(item);
            change.accept(item);
            if (rate <= 0) return;
            for (int i = 0; i < item.length; i++) {
                int count = item[i].isEmpty() ? 0 : item[i].getCount(), was = before[i].isEmpty() ? 0 : before[i].getCount();
                if (bump && count > 0 && count != was) bumpAt[i] = at;
            }
            if (!moved || !fly) return;
            for (int[] m : FlyAnims.moves(before, item)) {
                flights.add(new Flight(item[m[1]].copy(), FlyAnims.stays(before[m[1]], item[m[1]]), m[0], m[1], at));
            }
        }

        void draw(Gfx g, Panel p, int hover) {
            draw(g, p::sx, p::sy, hover);
        }

        void draw(Gfx g, java.util.function.IntUnaryOperator sx, java.util.function.IntUnaryOperator sy, int hover) {
            double flyMs = rate > 0 ? FlyAnims.FLY_MS / rate : 0;
            // what a slot shows while something is still flying to it; null when nothing is
            ItemStack[] under = new ItemStack[item.length];
            float[] bumped = bumpAt.clone();
            for (Flight f : flights) {
                if (now - f.at < flyMs) {
                    if (under[f.to] == null) under[f.to] = f.under;
                }
                else bumped[f.to] = Math.max(bumped[f.to], (float) (f.at + flyMs));
            }
            if (hover >= 0) highlight(g, sx.applyAsInt(hover), sy.applyAsInt(hover), false);
            for (int i = 0; i < item.length; i++) {
                if (under[i] != null) {
                    stack(g, under[i], sx.applyAsInt(i), sy.applyAsInt(i), 1);
                    continue;
                }
                double b = rate > 0 ? (now - bumped[i]) * rate / SlotAnims.BUMP_MS : 1;
                stack(g, item[i], sx.applyAsInt(i), sy.applyAsInt(i), b >= 0 && b < 1 ? SlotAnims.bumpScale(b) : 1);
            }
            if (hover >= 0) highlight(g, sx.applyAsInt(hover), sy.applyAsInt(hover), true);
            g.nextStratum();
            for (Flight f : flights) {
                if (now - f.at >= flyMs) continue;
                FlyAnims.item(g, f.stack, sx.applyAsInt(f.from), sy.applyAsInt(f.from), sx.applyAsInt(f.to), sy.applyAsInt(f.to),
                    (now - f.at) / flyMs);
            }
        }
    }

    // ---- the pointer ----

    // a pixel arrow one pixel thick with its tip at (x, y), over its shadow; {@code down} 0..1 presses it onto it
    static void pointer(Gfx g, float x, float y, float down) {
        g.nextStratum();
        // whole device pixels at the hub's scale of 2
        x = Math.round(x * 2) / 2f;
        y = Math.round(y * 2) / 2f;
        float push = Math.round(down * 2) / 2f;
        arrow(g, x + 3 - push, y + 3 - push, 0x59000000, 0x59000000);
        if (down < 0.5f) arrow(g, x + 1, y + 1, 0xFF0B0B0D, 0xFF6F6E6C);
        arrow(g, x + push, y + push, 0xFF1A1A1C, 0xFFF7F5F2);
    }

    // how far a click at {@code at} has the pointer pressed down
    static float press(float t, float at) {
        return t < at + 110 ? Math.max(0, 1 - Math.abs(t - at - 20) / 90) : 0;
    }

    // A stack carried on the pointer, centred on the tip as the game draws it. Its count is drawn by a second call after
    // the pointer, so that the arrow never hides it.
    static void carried(Gfx g, ItemStack s, float x, float y) {
        if (s.isEmpty()) return;
        g.nextStratum();
        g.pushTranslate(Math.round(x * 2) / 2f - 8, Math.round(y * 2) / 2f - 8);
        g.item(s, 0, 0);
        g.pop();
    }

    // {@code text} replaces the number, as for vanilla's yellow 0
    static void carriedCount(Gfx g, ItemStack s, float x, float y, String text) {
        if (s.isEmpty()) return;
        g.nextStratum();
        g.pushTranslate(Math.round(x * 2) / 2f - 8, Math.round(y * 2) / 2f - 8);
        g.itemDecorations(s, 0, 0, text);
        g.pop();
    }

    private static void arrow(Gfx g, float x, float y, int edge, int fill) {
        g.pushTranslate(x, y);
        for (int v = 0; v < ARROW.length; v++) {
            String line = ARROW[v];
            for (int u = 0; u < line.length(); u++) {
                char ch = line.charAt(u);
                if (ch != '.') g.rect(u, v, 1, 1, ch == 'X' ? edge : fill);
            }
        }
        g.pop();
    }

    // ---- the frame around it ----

    static String keyName(String key) {
        return I18n.get(key).toUpperCase(java.util.Locale.ROOT);
    }

    private static int keyWidth(Gfx g, Ctx c, String label) {
        return (int) Math.ceil(g.width(Gfx.Face.FIVE_B, label, c.sz.fs)) + 14;
    }

    // the player's own binding centred on cx: a mouse button as the mouse with that button lit, a key as its cap
    static void key(Gfx g, Ctx c, KeyMapping mapping, float cx, int y, boolean pressed) {
        g.binding(mapping, cx, y, c.sz.fs, pressed);
    }

    // a key cap for a key that is not a binding
    static void key(Gfx g, Ctx c, String label, float cx, int y, boolean pressed) {
        g.keycap(label, Math.round(cx - keyWidth(g, c, label) / 2f), y, c.sz.fs, pressed);
    }

    // two key caps side by side, as keys held together
    static void keys(Gfx g, Ctx c, float cx, int y, String a, boolean aDown, String b, boolean bDown) {
        int wa = keyWidth(g, c, a), wb = keyWidth(g, c, b), x = Math.round(cx - (wa + wb + 4) / 2f);
        g.keycap(a, x, y, c.sz.fs, aDown);
        g.keycap(b, x + wa + 4, y, c.sz.fs, bDown);
    }

    // the mouse with its left or right button lit, for a click the screen takes from the mouse itself
    static void mouse(Gfx g, boolean right, float cx, int y, boolean pressed) {
        g.sprite(right ? MOUSE_RIGHT : MOUSE_LEFT, Math.round(cx - 8), y + (pressed ? 1 : 0), 16, 16);
    }

    // a held key and the left button
    static void keyAndMouse(Gfx g, Ctx c, float cx, int y, String key, boolean keyDown, boolean mouseDown) {
        int wk = keyWidth(g, c, key), x = Math.round(cx - (wk + 20) / 2f);
        g.keycap(key, x, y, c.sz.fs, keyDown);
        mouse(g, false, x + wk + 12, y, mouseDown);
    }

    // the mouse with its wheel lit and an arrow for the way it turns, which nudges along on each notch
    static void wheel(Gfx g, boolean up, float cx, int y, boolean notch) {
        int x = Math.round(cx - 12);
        g.sprite(MOUSE_WHEEL, x, y, 16, 16);
        int top = y + 5 + (notch ? up ? -1 : 1 : 0);
        for (int row = 0; row < 4; row++) {
            int half = up ? row : 3 - row;
            g.rect(x + 19 - half, top + row + 1, half * 2 + 1, 1, 0xFF0B0B0D);
            g.rect(x + 19 - half, top + row, half * 2 + 1, 1, 0xFFD9D4D2);
        }
    }

    // The only text a preview carries: along the bottom edge, in the accent colour, which option is switched off (the
    // scene then shows the game without it). The motion explains the rest.
    static void note(Gfx g, int x, int y, int w, int h, String line) {
        List<String> lines = Rows.wrap(g, line, w - 8, 2);
        int top = y + h - 3 - lines.size() * 10;
        for (int i = 0; i < lines.size(); i++) {
            g.vanilla(lines.get(i), x + (w - g.vanillaWidth(lines.get(i))) / 2, top + i * 10 + 1, NOTE, true);
        }
    }

    // the note of an option that is switched off, or nothing. A preset scene names only a whole feature that is off (or,
    // when the preset turns everything off, the one general note), never a sub-option.
    final void off(Gfx g, int x, int y, int w, int h, String option) {
        if (option == null) return;
        if (!pinned.isEmpty() && !FEATURES.contains(option)) return;
        note(g, x, y, w, h, allOff != null ? allOff : t("preview.off." + option));
    }

    // the note of a scene that cannot show its items yet
    static void noWorld(Gfx g, int x, int y, int w, int h) {
        note(g, x, y, w, h, t("preview.noWorld"));
    }
}
