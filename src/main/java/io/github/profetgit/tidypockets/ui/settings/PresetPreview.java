package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import java.util.Map;
import java.util.function.Function;

// A Quick setup button's preview: the scenes of what the preset changes, each played with the preset's own values and
// one loop long, the next one sliding in as the last stage leaves. Sort, refill, random palette and protection are the
// feature scenes; a preset that turns a feature off shows vanilla in it (the scene's own orange note says so).
public final class PresetPreview extends Preview {
    // test hook: show this scene of the list (frozen at Preview.debugTime), or -1 for the live rotation
    public static int debugStep = -1;

    private final ModDef mod;
    private final Map<String, Object> values;
    private final String[] kinds;
    private final ChestStage[] cache;
    private final boolean allOff;
    private ChestStage cur, nxt;
    private int idx;
    private double moveAt = -1;

    PresetPreview(ModDef mod, ModDef.Preset p, String... kinds) {
        this.mod = mod;
        this.kinds = kinds;
        cache = new ChestStage[kinds.length];
        values = new java.util.HashMap<>();
        for (Setting s : mod.all()) values.put(s.key, mod.presetValue(p, s));
        allOff = Boolean.FALSE.equals(values.get("sortEnabled")) && Boolean.FALSE.equals(values.get("refillEnabled"))
            && Boolean.FALSE.equals(values.get("randomEnabled")) && Integer.valueOf(2).equals(values.get("protect"));
    }

    private ChestStage make(int i) {
        Function<ModDef, ChestStage> f = switch (kinds[i]) {
            case "sorthotbar" -> d -> new SortPreview(d, true);
            case "refill" -> RefillPreview::new;
            case "random" -> RandomPreview::new;
            case "protect" -> ProtectPreview::new;
            case "screenpop" -> ScreenPopPreview::new;
            default -> d -> new SortPreview(d, false);
        };
        return f.apply(mod).pin(values, allOff ? ChestStage.t("preview.off.all") : null);
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        if (debugStep >= 0 || debugTime >= 0) {
            int i = Math.max(0, Math.min(kinds.length - 1, debugStep));
            if (cache[i] == null) cache[i] = make(i);
            cache[i].render(g, c, x, y, w, h);
            return;
        }
        if (cur == null) cur = make(idx);
        if (nxt == null) {
            cur.render(g, c, x, y, w, h);
            if (cur.lastPeriod > 0 && cur.lastTime >= cur.lastPeriod - ChestStage.SLIDE) {
                moveAt = c.now;
                idx = (idx + 1) % kinds.length;
                nxt = make(idx);
            }
            return;
        }
        cur.hold = true;
        float p = (float) ((c.now - moveAt) / ChestStage.SLIDE);
        ChestStage a = cur, b = nxt;
        ChestStage.sliding(g, w, p, () -> a.render(g, c, x, y, w, h), () -> b.render(g, c, x, y, w, h));
        if (p >= 1) {
            cur = nxt;
            nxt = null;
        }
    }
}
