package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.resources.language.I18n;

// Tidy Pockets' options for the shared Profet screen, bound to the same TidyConfig fields and file as the old screen.
public final class TidySettings {
    private TidySettings() {}

    private static String t(String key) {
        return I18n.get("tidypockets." + key);
    }

    private static String label(String k) {
        return t("option." + k);
    }

    private static String desc(String k) {
        return t("setting." + k + ".desc");
    }

    private static Setting bool(String k, java.util.function.Function<TidyConfig, Boolean> get, java.util.function.BiConsumer<TidyConfig, Boolean> set) {
        TidyConfig d = new TidyConfig();
        return Setting.bool(k, label(k), desc(k), () -> get.apply(TidyConfig.get()), v -> set.accept(TidyConfig.get(), v), get.apply(d));
    }

    private static Setting range(String k, int min, int max, java.util.function.ToIntFunction<TidyConfig> get, java.util.function.ObjIntConsumer<TidyConfig> set) {
        TidyConfig d = new TidyConfig();
        return Setting.range(k, label(k), desc(k), min, max, () -> get.applyAsInt(TidyConfig.get()), v -> set.accept(TidyConfig.get(), v), get.applyAsInt(d));
    }

    private static Setting choice(String k, String[] optionKeys, java.util.function.ToIntFunction<TidyConfig> get, java.util.function.ObjIntConsumer<TidyConfig> set) {
        TidyConfig d = new TidyConfig();
        String[] names = new String[optionKeys.length];
        for (int i = 0; i < names.length; i++) names[i] = t("option." + k + "." + optionKeys[i]);
        return Setting.choice(k, label(k), desc(k), names, () -> get.applyAsInt(TidyConfig.get()), v -> set.accept(TidyConfig.get(), v), get.applyAsInt(d));
    }

    public static ModDef create() {
        ModDef m = new ModDef("tidypockets", "Tidy Pockets", "Tidy", ProfetSettings.modVersion("tidypockets"), t("ui.tagline"), "tidypockets:icon.png")
            .issues("ProfetGit/tidy-pockets")
            .diagnostics(() -> {
                java.util.List<String> c = new java.util.ArrayList<>();
                if (io.github.profetgit.tidypockets.config.Conflicts.mouse) c.add("mouse");
                if (io.github.profetgit.tidypockets.config.Conflicts.fly) c.add("fly");
                if (io.github.profetgit.tidypockets.config.Conflicts.scroll) c.add("scroll");
                if (io.github.profetgit.tidypockets.config.Conflicts.screenPop) c.add("screenPop");
                if (io.github.profetgit.tidypockets.config.Conflicts.sorting) c.add("sorting");
                return c.isEmpty() ? "" : "Yielded features: " + String.join(", ", c);
            });
        m.onChange = TidyConfig::save;
        // the whole config, not just the listed values: animSpeed is a double behind a stepped slider
        m.snapshot = () -> {
            TidyConfig opening = TidyConfig.snapshot();
            return () -> {
                TidyConfig.restore(opening);
                m.onChange.run();
            };
        };
        m.scene("sort", d -> new SortPreview(d, false)).scene("sorthotbar", d -> new SortPreview(d, true)).scene("refill", RefillPreview::new).scene("protect", ProtectPreview::new)
            .scene("random", RandomPreview::new).scene("wheel", WheelPreview::new).scene("shiftdrag", ShiftDragPreview::new)
            .scene("collectdrag", CollectDragPreview::new).scene("tools", ToolsPreview::new).scene("anim", AnimPreview::new)
            .scene("safe", SafeSortPreview::new).scene("screenpop", ScreenPopPreview::new).scene("scroll", SmoothScrollPreview::new);
        TidyConfig.SortOrder[] so = TidyConfig.SortOrder.values();
        TidyConfig.Protect[] pr = TidyConfig.Protect.values();

        m.group(t("config.sorting"),
            bool("sortEnabled", c -> c.sortEnabled, (c, v) -> c.sortEnabled = v).preview("sort"),
            bool("sortHotbar", c -> c.sortHotbar, (c, v) -> c.sortHotbar = v).preview("sorthotbar"),
            choice("sortOrder", new String[] {"creative", "name", "id"}, c -> c.sortOrder.ordinal(), (c, v) -> c.sortOrder = so[v]).preview("sort"),
            bool("sortSound", c -> c.sortSound, (c, v) -> c.sortSound = v),
            bool("safeMode", c -> c.safeMode, (c, v) -> c.safeMode = v).preview("safe"),
            range("safeModeClicksPerTick", 1, 64, c -> c.safeModeClicksPerTick, (c, v) -> c.safeModeClicksPerTick = v).format(v -> v + " /tick").preview("safe"));
        m.group(t("config.refill"),
            bool("refillEnabled", c -> c.refillEnabled, (c, v) -> c.refillEnabled = v).preview("refill"),
            bool("refillExactMatch", c -> c.refillExactMatch, (c, v) -> c.refillExactMatch = v).preview("refill"),
            bool("refillOffhand", c -> c.refillOffhand, (c, v) -> c.refillOffhand = v).preview("refill"));
        m.group(t("config.random"),
            bool("randomEnabled", c -> c.randomEnabled, (c, v) -> c.randomEnabled = v).preview("random"),
            bool("randomSpread", c -> c.randomSpread, (c, v) -> c.randomSpread = v).preview("random"));
        m.group(t("config.protect"),
            choice("protect", new String[] {"all", "enchanted", "off"}, c -> c.protect.ordinal(), (c, v) -> c.protect = pr[v]).preview("protect"),
            range("protectMargin", 0, 20, c -> c.protectMargin, (c, v) -> c.protectMargin = v).preview("protect"),
            bool("protectWarn", c -> c.protectWarn, (c, v) -> c.protectWarn = v).preview("protect"));
        m.group(t("config.mouse"),
            bool("wheelMove", c -> c.wheelMove, (c, v) -> c.wheelMove = v).preview("wheel"),
            bool("wheelInvert", c -> c.wheelInvert, (c, v) -> c.wheelInvert = v).preview("wheel"),
            bool("shiftDrag", c -> c.shiftDrag, (c, v) -> c.shiftDrag = v).preview("shiftdrag"),
            bool("collectDrag", c -> c.collectDrag, (c, v) -> c.collectDrag = v).preview("collectdrag"));
        m.group(t("config.tools"),
            bool("containerButtons", c -> c.containerButtons, (c, v) -> c.containerButtons = v).preview("tools"),
            bool("search", c -> c.search, (c, v) -> c.search = v).preview("tools"));
        m.group(t("config.animation"),
            range("animSpeed", 0, 30, c -> (int) Math.round(c.animSpeed * 10), (c, v) -> c.animSpeed = v / 10.0)
                .format(v -> v == 0 ? t("ui.speedOff") : String.format(java.util.Locale.ROOT, "%.1f×", v / 10.0)).preview("anim"),
            bool("animSort", c -> c.animSort, (c, v) -> c.animSort = v).preview("sort"),
            bool("animFly", c -> c.animFly, (c, v) -> c.animFly = v).preview("anim"),
            bool("animScreenPop", c -> c.animScreenPop, (c, v) -> c.animScreenPop = v).preview("screenpop"),
            bool("animAllScreens", c -> c.animAllScreens, (c, v) -> c.animAllScreens = v).preview("screenpop"),
            bool("animHotbar", c -> c.animHotbar, (c, v) -> c.animHotbar = v).preview("anim"),
            bool("animCountBump", c -> c.animCountBump, (c, v) -> c.animCountBump = v).preview("anim"),
            bool("animPlayerHop", c -> c.animPlayerHop, (c, v) -> c.animPlayerHop = v),
            bool("smoothScroll", c -> c.smoothScroll, (c, v) -> c.smoothScroll = v).preview("scroll"),
            bool("blur", c -> c.blur, (c, v) -> c.blur = v));

        m.preset(t("ui.preset.off"), "sortEnabled", false, "refillEnabled", false, "randomEnabled", false, "protect", 2,
            "wheelMove", false, "shiftDrag", false, "collectDrag", false, "containerButtons", false, "search", false);
        m.preset(t("ui.preset.light"), "sortSound", false, "randomEnabled", false, "protect", 1,
            "wheelMove", false, "shiftDrag", false, "collectDrag", false);
        m.preset(t("ui.preset.default"));
        m.preset(t("ui.preset.full"), "sortHotbar", true, "refillExactMatch", false, "protectMargin", 4, "animAllScreens", true);
        // each preset's scenes, in the one order sort, refill, random, protect, screen pop: only where it differs from
        // Default, which shows the usual four; Off shows vanilla in the first two
        String[][] shown = {{"off", "sort", "refill"}, {"light", "random", "protect"},
            {"default", "sort", "refill", "random", "protect"}, {"full", "sorthotbar", "refill", "protect", "screenpop"}};
        for (int i = 0; i < shown.length; i++) {
            ModDef.Preset p = m.presets.get(i);
            String[] kinds = java.util.Arrays.copyOfRange(shown[i], 1, shown[i].length);
            m.presetPreview(p.name(), "preset." + shown[i][0], t("ui.preset." + shown[i][0] + ".note"));
            m.scene("preset." + shown[i][0], d -> new PresetPreview(d, p, kinds));
        }
        return m;
    }
}
