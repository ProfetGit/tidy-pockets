package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.ScreenPop;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

// Screen pop: a chest is opened and closed, then the game menu. Each pops from 0.92 with ScreenPop's overshoot where
// its option is on (containers and all other screens have one each) and appears at once where it is off, as in vanilla.
// Closing is always instant.
public final class ScreenPopPreview extends ChestStage {
    private static final float CHEST = 250, CHEST_END = 1850, MENU = 2100, MENU_END = 3700, PERIOD = 3950;
    private static final String[] MENU_ITEMS = {"menu.returnToGame", "menu.options", "menu.returnToMenu"};

    private ItemStack[] contents;

    public ScreenPopPreview(ModDef mod) {
        super(mod);
    }

    private static double scale(float since, double speed, boolean pops) {
        return pops && speed > 0 ? ScreenPop.scaleAt(Math.min(1, Math.max(0, since * speed / ScreenPop.POP_MS))) : 1;
    }

    private void chest(Gfx g, int wx, int wy, double k) {
        g.pushScale(wx + WIN_W / 2f, wy + windowHeight(false) / 2f, (float) k, (float) k);
        window(g, wx, wy, false);
        for (int i = 0; i < contents.length; i++) stack(g, contents[i], slotX(wx, i), slotY(wy, i), 1);
        g.pop();
    }

    private static void menu(Gfx g, int cx, int top, double k) {
        g.pushScale(cx, top + 42, (float) k, (float) k);
        shadow(g, cx, top + 84, 150);
        centred(g, I18n.get("menu.game"), cx, top);
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            int by = top + 16 + i * 24;
            VanillaArt.button(g, cx - 75, by, 150);
            centred(g, I18n.get(MENU_ITEMS[i]), cx, by + 6);
        }
        g.pop();
    }

    private static void centred(Gfx g, String s, int cx, int y) {
        g.vanilla(s, cx - g.vanillaWidth(s) / 2, y, 0xFFFFFFFF, true);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean containers = flag("animScreenPop", true), all = flag("animAllScreens", false);
        double speed = speed();
        int ox = x + (w - W) / 2, wx = ox + WIN_X, wy = y + 28, cx = x + w / 2;
        backdrop(g, ox, y);
        if (!itemsReady()) {
            window(g, wx, wy, false);
            noWorld(g, x, y, w, h);
            return;
        }
        if (contents == null) contents = sort(SortPreview.stacks(27, false), 27).tidy();
        float time = time(c, PERIOD, containers, all, speed);
        boolean second = time >= (CHEST_END + MENU) / 2;
        if (time >= CHEST && time < CHEST_END) chest(g, wx, wy, scale(time - CHEST, speed, containers));
        if (time >= MENU && time < MENU_END) menu(g, cx, y + 18, scale(time - MENU, speed, all));

        if (second) {
            key(g, c, keyName("key.keyboard.escape"), x + w / 2f, y + KEY_Y, within(time, MENU - 40, 200) || within(time, MENU_END - 40, 200));
        } else if (time < CHEST_END - 300) {
            key(g, c, g.mc.options.keyUse, x + w / 2f, y + KEY_Y, within(time, CHEST - 40, 200));
        } else {
            key(g, c, g.mc.options.keyInventory, x + w / 2f, y + KEY_Y, within(time, CHEST_END - 40, 200));
        }
        // each turn names only its own option
        off(g, x, y, w, h, speed <= 0 ? "animSpeed" : second ? all ? null : "animAllScreens" : containers ? null : "animScreenPop");
    }
}
