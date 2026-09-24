package io.github.profetgit.tidypockets.platform;

import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public interface Platform {
    String loader();

    String minecraftVersion();

    Path configDir();

    boolean isModLoaded(String modId);

    /** NeoForge registers categories through its event, so it only constructs one here. */
    default KeyMapping.Category keyCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }
}
