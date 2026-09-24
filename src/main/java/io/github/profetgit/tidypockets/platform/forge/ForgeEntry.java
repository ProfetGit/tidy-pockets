package io.github.profetgit.tidypockets.platform.forge;

//? forge {
/*import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.config.TidyConfigScreen;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.platform.Platform;
import java.nio.file.Path;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(TidyPockets.MOD_ID)
public final class ForgeEntry implements Platform {
    public ForgeEntry(FMLJavaModLoadingContext context) {
        TidyPockets.init(this);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new TidyConfigScreen(parent)));
        RegisterKeyMappingsEvent.BUS.addListener(e -> Keys.ALL.forEach(e::register));
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public String minecraftVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().name();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }
}
*///?}
