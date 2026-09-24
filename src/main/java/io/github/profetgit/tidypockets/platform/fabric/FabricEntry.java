package io.github.profetgit.tidypockets.platform.fabric;

//? fabric {
import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.platform.Platform;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntry implements ClientModInitializer, Platform {
    @Override
    public void onInitializeClient() {
        TidyPockets.init(this);
        Keys.ALL.forEach(KeyMappingHelper::registerKeyMapping);
    }

    @Override
    public String loader() {
        return "fabric";
    }

    @Override
    public String minecraftVersion() {
        return FabricLoader.getInstance().getRawGameVersion();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
//?}
