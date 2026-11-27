package io.github.profetgit.tidypockets.selftest;

import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.ProfetSettingsScreen;
import io.github.profetgit.tidypockets.config.ConfigScreens;
import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Consumer;

// The settings previews at fixed moments of their loops (frames ui-pv-*), and with -Dtidypockets.selftest.pvdense=true
// (or a list of preview kinds) every 60 fps moment (ui-dense-*) for the flicker check.
final class UiPreviews {
    private UiPreviews() {}

    // before any world has loaded there are no item components: the previews must cope
    static void atTitle(Script s) {
        s.run(mc -> {
            String made;
            try {
                made = "made " + new ItemStack(Items.APPLE);
            } catch (RuntimeException e) {
                made = "throws " + e.getMessage();
            }
            SelfTest.record("ui: item stacks at the title screen", true, made);
        });
        shot(s, "sortEnabled", "sort", "-title", 1800);
        shot(s, "safeMode", "safe", "-title", 900);
        shot(s, "refillEnabled", "refill", "-title", 700);
        shot(s, "protect", "protect", "-title", 900);
        shot(s, "randomEnabled", "random", "-title", 900);
        shot(s, "wheelMove", "wheel", "-title", 900);
        shot(s, "shiftDrag", "shiftdrag", "-title", 900);
        shot(s, "collectDrag", "collectdrag", "-title", 900);
        shot(s, "containerButtons", "tools", "-title", 900);
        shot(s, "animFly", "anim", "-title", 900);
        shot(s, "animScreenPop", "screenpop", "-title", 900);
        shot(s, "smoothScroll", "scroll", "-title", 900);
        s.run(mc -> {
            Preview.debugTime = -1;
            mc.gui.setScreen(null);
            mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }).waitTicks(10);
    }

    static void add(Script s) {
        TidyConfig[] opening = {null};
        s.run(mc -> {
            opening[0] = TidyConfig.snapshot();
            reset();
        });
        sortShots(s);
        safeShots(s);
        refillShots(s);
        protectShots(s);
        randomShots(s);
        mouseShots(s);
        toolShots(s);
        animShots(s);
        screenShots(s);
        presetShots(s);
        s.run(mc -> reset());
        dense(s, "sortEnabled", "sort", 2200);
        dense(s, "sortHotbar", "sorthotbar", 2200);
        with(s, c -> c.safeMode = true);
        dense(s, "safeMode", "safe", 2350);
        s.run(mc -> reset());
        dense(s, "refillEnabled", "refill", 4050);
        dense(s, "protect", "protect", 3575);
        dense(s, "randomEnabled", "random", 5540);
        dense(s, "wheelMove", "wheel", 4100);
        dense(s, "shiftDrag", "shiftdrag", 3300);
        dense(s, "collectDrag", "collectdrag", 3500);
        dense(s, "containerButtons", "tools", 5500);
        dense(s, "animFly", "anim", 5450);
        with(s, c -> c.animAllScreens = true);
        dense(s, "animScreenPop", "screenpop", 3950);
        s.run(mc -> reset());
        dense(s, "smoothScroll", "scroll", 3700);
        s.run(mc -> {
            TidyConfig.restore(opening[0]);
            Preview.debugTime = -1;
            mc.gui.setScreen(null);
        });
    }

    // the defaults, with these changes
    private static void with(Script s, Consumer<TidyConfig> change) {
        s.run(mc -> {
            reset();
            change.accept(TidyConfig.get());
        });
    }

    private static void sortShots(Script s) {
        shot(s, "sortEnabled", "sort", "", 100, 500, 800, 830, 880, 940, 1500, 2050, 2150);
        with(s, c -> c.sortOrder = TidyConfig.SortOrder.NAME);
        shot(s, "sortOrder", "sort", "-name", 1800);
        with(s, c -> c.sortOrder = TidyConfig.SortOrder.ID);
        shot(s, "sortOrder", "sort", "-id", 1800);
        with(s, c -> c.animSort = false);
        shot(s, "animSort", "sort", "-noanim", 780, 830, 1800);
        with(s, c -> c.animSpeed = 0);
        shot(s, "sortEnabled", "sort", "-speed0", 830);
        with(s, c -> c.animSpeed = 3.0);
        shot(s, "sortEnabled", "sort", "-speed3", 815, 845);
        with(s, c -> c.animSpeed = 0.5);
        shot(s, "sortEnabled", "sort", "-slow", 900, 1050);
        with(s, c -> c.sortEnabled = false);
        shot(s, "sortEnabled", "sort", "-off", 830, 1800);
        s.run(mc -> reset());
        shot(s, "sortHotbar", "sorthotbar", "-off", 500, 880, 1800);
        with(s, c -> c.sortHotbar = true);
        shot(s, "sortHotbar", "sorthotbar", "", 500, 880, 1800);
    }

    private static void safeShots(Script s) {
        with(s, c -> c.safeMode = true);
        shot(s, "safeMode", "safe", "", 400, 720, 800, 860, 940, 1500, 2200);
        with(s, c -> {
            c.safeMode = true;
            c.safeModeClicksPerTick = 1;
        });
        shot(s, "safeModeClicksPerTick", "safe", "-one", 800, 1200, 1700, 2300, 3000, 3650);
        with(s, c -> {
            c.safeMode = true;
            c.safeModeClicksPerTick = 64;
        });
        shot(s, "safeModeClicksPerTick", "safe", "-all", 760, 1000);
        s.run(mc -> reset());
        shot(s, "safeMode", "safe", "-off", 720, 780, 1000, 1800);
        with(s, c -> {
            c.safeMode = true;
            c.sortEnabled = false;
        });
        shot(s, "safeMode", "safe", "-nosort", 860);
    }

    private static void refillShots(Script s) {
        s.run(mc -> reset());
        shot(s, "refillEnabled", "refill", "", 100, 620, 680, 720, 1200, 1620, 1700, 2100, 2500, 2760, 2830, 3300, 3900);
        with(s, c -> c.refillExactMatch = false);
        shot(s, "refillExactMatch", "refill", "-loose", 1620, 1700, 2100);
        with(s, c -> c.refillEnabled = false);
        shot(s, "refillEnabled", "refill", "-off", 700, 1700, 2830);
        with(s, c -> c.refillOffhand = false);
        shot(s, "refillOffhand", "refill", "-nooff", 2830, 3300);
        with(s, c -> c.animHotbar = false);
        shot(s, "refillEnabled", "refill", "-noanim", 660);
    }

    private static void protectShots(Script s) {
        s.run(mc -> reset());
        shot(s, "protect", "protect", "", 100, 700, 1200, 1720, 1780, 1820, 1900, 2400, 3350);
        with(s, c -> c.protect = TidyConfig.Protect.OFF);
        shot(s, "protect", "protect", "-off", 1200, 1720, 1800, 1900, 2600);
        with(s, c -> c.protect = TidyConfig.Protect.ENCHANTED);
        shot(s, "protect", "protect", "-enchanted", 1000, 1820, 3350, 4500, 5350, 5450, 5900);
        with(s, c -> c.protectMargin = 20);
        shot(s, "protectMargin", "protect", "-margin", 400, 1820, 2400);
        with(s, c -> c.protectWarn = false);
        shot(s, "protectWarn", "protect", "-silent", 1820, 2400);
    }

    private static void randomShots(Script s) {
        s.run(mc -> reset());
        shot(s, "randomEnabled", "random", "", 100, 520, 545, 900, 1800, 3000, 4200, 5000, 5400);
        with(s, c -> c.randomSpread = false);
        shot(s, "randomSpread", "random", "-pure", 1800, 5000);
        with(s, c -> c.randomEnabled = false);
        shot(s, "randomEnabled", "random", "-off", 1800, 5000);
        with(s, c -> c.animHotbar = false);
        shot(s, "randomEnabled", "random", "-noanim", 920);
    }

    private static void mouseShots(Script s) {
        s.run(mc -> reset());
        shot(s, "wheelMove", "wheel", "", 100, 720, 760, 900, 1750, 2100, 2420, 2460, 3500, 3900);
        with(s, c -> c.wheelInvert = true);
        shot(s, "wheelInvert", "wheel", "-invert", 760, 2460);
        with(s, c -> c.wheelMove = false);
        shot(s, "wheelMove", "wheel", "-off", 760, 2460);
        with(s, c -> c.animSpeed = 0.3);
        shot(s, "wheelMove", "wheel", "-slow", 800, 1100, 2500);
        s.run(mc -> reset());
        shot(s, "shiftDrag", "shiftdrag", "", 100, 780, 820, 860, 1200, 1400, 1700, 2200, 2700, 3150);
        with(s, c -> c.shiftDrag = false);
        shot(s, "shiftDrag", "shiftdrag", "-off", 820, 1700, 2700);
        with(s, c -> c.animFly = false);
        shot(s, "shiftDrag", "shiftdrag", "-nofly", 1400);
        s.run(mc -> reset());
        shot(s, "collectDrag", "collectdrag", "", 100, 780, 1000, 1150, 1450, 1800, 2200, 2500, 2900, 3350);
        with(s, c -> c.collectDrag = false);
        shot(s, "collectDrag", "collectdrag", "-off", 1000, 1150, 1450, 1800, 2200, 2400, 2900, 3350);
    }

    private static void toolShots(Script s) {
        s.run(mc -> reset());
        shot(s, "containerButtons", "tools", "", 100, 500, 800, 1000, 1500, 2200, 2760, 2870, 2900, 3100, 3570, 3600, 3900, 4270, 4310, 4700, 5350);
        with(s, c -> c.containerButtons = false);
        shot(s, "containerButtons", "tools", "-nobuttons", 1500, 2900, 4310);
        with(s, c -> c.search = false);
        shot(s, "search", "tools", "-nosearch", 600, 1500, 4700);
        with(s, c -> {
            c.search = false;
            c.containerButtons = false;
        });
        shot(s, "search", "tools", "-off", 1500, 2900);
    }

    private static void animShots(Script s) {
        s.run(mc -> reset());
        shot(s, "animFly", "anim", "", 100, 560, 590, 640, 1010, 1460, 1700, 2200, 2520, 2760, 2960, 3250, 3620, 3650, 4000, 4700, 5300);
        with(s, c -> c.animSpeed = 0.3);
        shot(s, "animSpeed", "anim", "-slow", 650, 1100, 2600, 3660);
        with(s, c -> c.animSpeed = 0);
        shot(s, "animSpeed", "anim", "-speed0", 560);
        with(s, c -> c.animFly = false);
        shot(s, "animFly", "anim", "-nofly", 560);
        with(s, c -> c.animCountBump = false);
        shot(s, "animCountBump", "anim", "-nobump", 2520);
        with(s, c -> c.animHotbar = false);
        shot(s, "animHotbar", "anim", "-noglide", 3620);
    }

    private static void screenShots(Script s) {
        s.run(mc -> reset());
        shot(s, "animScreenPop", "screenpop", "", 100, 260, 280, 310, 450, 1200, 1950, 2110, 3000, 3850);
        with(s, c -> c.animAllScreens = true);
        shot(s, "animAllScreens", "screenpop", "-all", 2110, 2140, 2180, 3000);
        with(s, c -> c.animScreenPop = false);
        shot(s, "animScreenPop", "screenpop", "-off", 260, 280, 1200);
        with(s, c -> c.animSpeed = 0.3);
        shot(s, "animScreenPop", "screenpop", "-slow", 300, 400);
        s.run(mc -> reset());
        shot(s, "smoothScroll", "scroll", "", 100, 520, 560, 600, 700, 900, 1100, 1600, 2320, 2400, 2900, 3400);
        with(s, c -> c.smoothScroll = false);
        shot(s, "smoothScroll", "scroll", "-off", 520, 560, 700, 900, 1100);
    }

    // every Quick setup button plays its own scene list with that preset's values: kind, title, note, and a still of each scene
    private static final String[][] PRESETS = {
        {"0", "off", "Off", "1800 1800", "Turns off Tidy Pockets"}, {"1", "light", "Light", "3000 1000", "Quiet, simpler: no random palette or mouse shortcuts"},
        {"2", "default", "Default", "1800 1800 3000 1000", "The recommended setup"}, {"3", "full", "Full", "1800 1800 1000 1000", "Everything on, hotbar sorted too"}};

    private static void presetShots(Script s) {
        for (String[] p : PRESETS) {
            int i = Integer.parseInt(p[0]);
            String[] times = p[3].split(" ");
            s.run(mc -> {
                    reset();
                    mc.gui.setScreen(ConfigScreens.create(null));
                    Preview.debugTime = Integer.parseInt(times[0]);
                    io.github.profetgit.tidypockets.ui.settings.PresetPreview.debugStep = 0;
                })
                .waitTicks(20)
                .run(mc -> hub(mc).debugScroll(0))
                .waitTicks(6)
                .run(mc -> {
                    int[] pt = hub(mc).debugPresetPoint(i);
                    hub(mc).debugHover(pt[0], pt[1]);
                })
                .waitTicks(12)
                .check("ui: Quick setup " + p[2] + " shows its own preview", mc -> ("preset." + p[1]).equals(hub(mc).debugPreviewKind()) ? null : "preview " + hub(mc).debugPreviewKind())
                .check("ui: Quick setup " + p[2] + " titles its preview", mc -> p[2].equals(hub(mc).debugPreviewTitle()) ? null : "title " + hub(mc).debugPreviewTitle())
                .check("ui: Quick setup " + p[2] + " has its own plain-words note, no numbers", mc -> {
                    String n = hub(mc).debugPreviewNote();
                    return p[4].equals(n) && n.chars().noneMatch(Character::isDigit) ? null : "note '" + n + "'";
                });
            for (int k = 0; k < times.length; k++) {
                int step = k, t = Integer.parseInt(times[k]);
                s.run(mc -> {
                    io.github.profetgit.tidypockets.ui.settings.PresetPreview.debugStep = step;
                    Preview.debugTime = t;
                }).waitTicks(3).capture("ui-pv-preset-" + p[1] + "-" + step, 1).waitTicks(3);
            }
        }
        // a hover restarts the rotation: a new scene each time, also back onto the same preset after leaving it
        double[] mount = {0};
        for (int i : new int[] {1, 3, 1}) {
            int idx = i;
            s.run(mc -> hub(mc).debugHover(-1, -1))
                .waitTicks(4)
                .run(mc -> {
                    int[] pt = hub(mc).debugPresetPoint(idx);
                    hub(mc).debugHover(pt[0], pt[1]);
                })
                .waitTicks(8)
                .check("ui: hovering " + PRESETS[i][2] + " again starts its rotation over", mc -> {
                    double m = hub(mc).debugSceneMount();
                    boolean fresh = m > mount[0];
                    mount[0] = m;
                    return fresh ? null : "scene was not recreated";
                })
                .waitTicks(8)
                .check("ui: " + PRESETS[i][2] + "'s rotation keeps running while hovered", mc -> hub(mc).debugSceneMount() == mount[0] ? null : "scene recreated");
        }
        s.run(mc -> {
            io.github.profetgit.tidypockets.ui.settings.PresetPreview.debugStep = -1;
            Preview.debugTime = -1;
        });
    }

    private static void reset() {
        TidyConfig.restore(new TidyConfig());
    }

    private static ProfetSettingsScreen hub(Minecraft mc) {
        return (ProfetSettingsScreen) mc.gui.screen();
    }

    // opens the screen, hovers the setting's row and leaves its preview frozen at the first moment
    private static void open(Script s, String key, String kind, String suffix, int first) {
        s.run(mc -> {
                mc.gui.setScreen(ConfigScreens.create(null));
                Preview.debugTime = first;
            })
            .waitTicks(20)
            .run(mc -> hub(mc).debugRevealRow(key))
            .waitTicks(4)
            .run(mc -> {
                // a switch has one centre, a slider's point is a fraction along it
                boolean slider = key.equals("safeModeClicksPerTick") || key.equals("protectMargin") || key.equals("animSpeed");
                int[] p = hub(mc).debugPoint(key, slider ? 0.5f : -1f);
                hub(mc).debugHover(p[0], p[1]);
            })
            .waitTicks(12)
            .check("ui: " + key + " shows the " + kind + " preview" + suffix, mc -> kind.equals(hub(mc).debugPreviewKind()) ? null : "preview " + hub(mc).debugPreviewKind());
    }

    private static void shot(Script s, String key, String kind, String suffix, int... times) {
        open(s, key, kind, suffix, times[0]);
        for (int t : times) {
            s.run(mc -> Preview.debugTime = t).waitTicks(3).capture("ui-pv-" + kind + suffix + "-" + t, 1).waitTicks(3);
        }
    }

    // every 60 fps moment of a loop, if -Dtidypockets.selftest.pvdense is true or names this kind
    private static void dense(Script s, String key, String kind, int period) {
        String want = System.getProperty("tidypockets.selftest.pvdense", "false");
        if (!want.equals("true") && !java.util.List.of(want.split(",")).contains(kind)) return;
        open(s, key, kind, "-dense", 0);
        for (int i = 0; i < period * 60 / 1000; i++) {
            int t = i * 1000 / 60;
            s.run(mc -> {
                    Preview.debugTime = t;
                    SelfTest.startCapture(String.format("ui-dense-%s-%04d", kind, t), 1);
                })
                .until("ui: dense frame written", 40, mc -> !SelfTest.capturing());
        }
    }
}
