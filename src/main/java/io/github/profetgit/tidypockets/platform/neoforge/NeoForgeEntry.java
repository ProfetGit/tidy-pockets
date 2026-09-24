package io.github.profetgit.tidypockets.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.config.TidyConfigScreen;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.platform.Platform;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TidyPockets.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeEntry implements Platform {
    public NeoForgeEntry(IEventBus modBus, ModContainer container) {
        TidyPockets.init(this);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new TidyConfigScreen(parent));
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.registerCategory(Keys.CATEGORY);
            Keys.ALL.forEach(e::register);
        });
    }

    @Override
    public KeyMapping.Category keyCategory(Identifier id) {
        return new KeyMapping.Category(id);
    }

    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public String minecraftVersion() {
        return FMLLoader.getCurrent().getVersionInfo().mcVersion();
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
*///?}
