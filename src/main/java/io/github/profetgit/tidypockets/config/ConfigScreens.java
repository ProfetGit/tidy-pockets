package io.github.profetgit.tidypockets.config;

import net.minecraft.client.gui.screens.Screen;

/** The one place that decides which settings screen a loader's "config" button opens. */
public final class ConfigScreens {
    private ConfigScreens() {}

    public static Screen create(Screen parent) {
        //? if >=26.3 {
        return io.github.profetgit.profetsettings.ProfetSettings.screen(parent, io.github.profetgit.tidypockets.TidyPockets.MOD_ID);
        //?} else {
        /*return new LegacyConfigScreen(parent);
        *///?}
    }

    public static boolean isConfigScreen(Screen screen) {
        //? if >=26.3 {
        return io.github.profetgit.profetsettings.ProfetSettings.isScreen(screen);
        //?} else {
        /*return screen instanceof LegacyConfigScreen;
        *///?}
    }
}
