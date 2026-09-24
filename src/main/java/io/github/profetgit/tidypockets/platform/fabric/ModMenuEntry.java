package io.github.profetgit.tidypockets.platform.fabric;

//? fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.profetgit.tidypockets.config.TidyConfigScreen;

public final class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TidyConfigScreen::new;
    }
}
//?}
