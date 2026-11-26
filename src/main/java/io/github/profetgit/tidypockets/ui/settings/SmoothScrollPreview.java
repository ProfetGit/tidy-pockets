package io.github.profetgit.tidypockets.ui.settings;

//? if >=26.3 {
import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.Ease;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

// Smooth scroll on a vanilla options list: each wheel notch moves it 12 pixels (half a row, as the game's lists do),
// eased there by the mod's own approach (Ease.approach). Off, it jumps there at once.
public final class SmoothScrollPreview extends ChestStage {
    private static final float PERIOD = 3700;
    private static final float[] NOTCHES = {500, 680, 860, 1500, 2300, 2480, 2660, 2840};
    private static final int DOWN = 4, STEP = 12, ROW = 25, LIST_W = 150, LIST_H = 100;
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button"),
        SCROLLER = Identifier.withDefaultNamespace("widget/scroller"), TRACK = Identifier.withDefaultNamespace("widget/scroller_background"),
        BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png"),
        HEADER = Identifier.withDefaultNamespace("textures/gui/header_separator.png"),
        FOOTER = Identifier.withDefaultNamespace("textures/gui/footer_separator.png");
    private static final String[][] OPTIONS = {
        {"options.fov", "90"}, {"options.renderDistance", "12"}, {"options.simulationDistance", "10"}, {"options.framerateLimit", "120"},
        {"options.vsync", "options.on"}, {"options.viewBobbing", "options.on"}, {"options.entityShadows", "options.on"},
        {"options.biomeBlendRadius", "5x5"}, {"options.mipmapLevels", "4"}, {"options.entityDistanceScaling", "100%"},
        {"options.fullscreen", "options.off"}, {"options.ao", "options.on"}};

    public SmoothScrollPreview(ModDef mod) {
        super(mod);
    }

    // where the list stands t ms into the loop; rate 0 jumps
    private static double scroll(float t, double rate) {
        double pos = 0, target = 0, last = 0, max = OPTIONS.length * ROW + 4 - LIST_H;
        for (int i = 0; i < NOTCHES.length && t >= NOTCHES[i]; i++) {
            pos = rate > 0 ? eased(pos, target, NOTCHES[i] - last, rate) : target;
            last = NOTCHES[i];
            target = Math.max(0, Math.min(max, target + (i < DOWN ? STEP : -STEP)));
        }
        return rate > 0 ? eased(pos, target, t - last, rate) : target;
    }

    private static double eased(double from, double target, double ms, double rate) {
        double next = Ease.approach(from, target, ms, rate);
        return Math.abs(target - next) < 0.3 ? target : next;
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean smooth = flag("smoothScroll", true);
        double speed = speed();
        int ox = x + (w - W) / 2, lx = x + (w - LIST_W - 12) / 2, ly = y + 11, lw = LIST_W + 12;
        backdrop(g, ox, y);
        float time = time(c, PERIOD, smooth, speed);
        int at = (int) scroll(time, smooth ? speed : 0);

        shadow(g, lx + lw / 2f, ly + LIST_H + 2, lw);
        g.g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, lx, ly, 0, at, lw, LIST_H, 32, 32);
        g.clip(lx, ly, lw, LIST_H);
        for (int i = 0; i < OPTIONS.length; i++) {
            int ry = ly + 4 + i * ROW - at;
            if (ry + 20 < ly || ry > ly + LIST_H) continue;
            g.g.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON, lx + 2, ry, LIST_W - 4, 20);
            String value = OPTIONS[i][1].startsWith("options.") ? I18n.get(OPTIONS[i][1]) : OPTIONS[i][1];
            String line = I18n.get(OPTIONS[i][0]) + ": " + value;
            g.vanilla(line, lx + LIST_W / 2 - g.vanillaWidth(line) / 2, ry + 6, 0xFFFFFFFF, true);
        }
        g.unclip();
        g.g.nextStratum();
        g.g.blit(RenderPipelines.GUI_TEXTURED, HEADER, lx, ly - 2, 0, 0, lw, 2, 32, 2);
        g.g.blit(RenderPipelines.GUI_TEXTURED, FOOTER, lx, ly + LIST_H, 0, 0, lw, 2, 32, 2);
        int content = OPTIONS.length * ROW + 4, thumb = Math.max(32, Math.min(LIST_H - 8, LIST_H * LIST_H / content));
        g.g.blitSprite(RenderPipelines.GUI_TEXTURED, TRACK, lx + LIST_W + 4, ly, 6, LIST_H);
        g.g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, lx + LIST_W + 4, ly + at * (LIST_H - thumb) / (content - LIST_H), 6, thumb);

        pointer(g, lx + LIST_W - 8, ly + 60, 0);
        boolean down = false, upwards = time >= NOTCHES[DOWN] - 350;
        for (float n : NOTCHES) down |= within(time, n, 110);
        wheel(g, upwards, x + w / 2f, y + KEY_Y, down);
        off(g, x, y, w, h, !smooth ? "smoothScroll" : speed <= 0 ? "animSpeed" : null);
    }
}
//?}
