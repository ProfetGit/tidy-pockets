package io.github.profetgit.tidypockets;

import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.inv.ClickSender;
import io.github.profetgit.tidypockets.platform.Platform;
import io.github.profetgit.tidypockets.refill.Refill;
import io.github.profetgit.tidypockets.selftest.SelfTest;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TidyPockets {
    public static final String MOD_ID = "tidypockets";
    public static final Logger LOG = LoggerFactory.getLogger("Tidy Pockets");
    private static Platform platform;

    private TidyPockets() {}

    public static void init(Platform p) {
        platform = p;
        TidyConfig.load();
        Conflicts.detect();
        LOG.info("Tidy Pockets loading on {} (Minecraft {})", p.loader(), p.minecraftVersion());
    }

    public static Platform platform() {
        return platform;
    }

    public static void clientTick(Minecraft mc) {
        ClickSender.tick();
        Refill.tick(mc);
        io.github.profetgit.tidypockets.lock.SlotLocks.tick(mc.player);
        if (SelfTest.active()) SelfTest.onClientTick(mc);
    }
}
