package io.github.profetgit.tidypockets.platform.forge;

import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.config.ConfigScreens;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.platform.Platform;
import java.nio.file.Path;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(TidyPockets.MOD_ID)
public final class ForgeEntry implements Platform {
    @SuppressWarnings("removal")
    public ForgeEntry() {
        TidyPockets.init(this);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ConfigScreens.create(parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterKeyMappingsEvent e) -> Keys.ALL.forEach(e::register));
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public String minecraftVersion() {
        return FMLLoader.versionInfo().mcVersion();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
