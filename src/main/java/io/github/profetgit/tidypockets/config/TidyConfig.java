package io.github.profetgit.tidypockets.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.profetgit.tidypockets.TidyPockets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Every option, with its default. Saved as config/tidypockets.json; unknown or missing keys fall back to defaults. */
public final class TidyConfig {
    public enum SortOrder { CREATIVE, NAME, ID }

    public enum Protect { ALL, ENCHANTED, OFF }

    // Sorting
    public boolean sortEnabled = true;
    public boolean sortHotbar = false;
    public SortOrder sortOrder = SortOrder.CREATIVE;
    public boolean safeMode = false;
    public int safeModeClicksPerTick = 8;
    public boolean sortSound = true;

    // Refill
    public boolean refillEnabled = true;
    public boolean refillExactMatch = true;
    public boolean refillOffhand = true;

    // Tool protection
    public Protect protect = Protect.ALL;
    public int protectMargin = 0;

    // Mouse
    public boolean wheelMove = true;
    public boolean wheelInvert = false;
    public boolean shiftDrag = true;
    public boolean collectDrag = true;

    // Container tools
    public boolean containerButtons = true;
    public boolean search = true;

    // Animation
    public double animSpeed = 1.0;
    public boolean animSort = true;
    public boolean animFly = true;
    public boolean animScreenPop = true;
    public boolean animAllScreens = false;
    public boolean animHotbar = true;
    public boolean animCountBump = true;
    public boolean animPlayerHop = true;
    public boolean smoothScroll = true;
    public boolean blur = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TidyConfig instance = new TidyConfig();

    public static TidyConfig get() {
        return instance;
    }

    private static Path file() {
        return TidyPockets.platform().configDir().resolve("tidypockets.json");
    }

    public static void load() {
        Path f = file();
        if (Files.isRegularFile(f)) {
            try {
                TidyConfig c = GSON.fromJson(Files.readString(f), TidyConfig.class);
                if (c != null) instance = c.sanitized();
            } catch (Exception e) {
                TidyPockets.LOG.warn("Could not read {}, using defaults: {}", f, e.toString());
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(instance));
        } catch (IOException e) {
            TidyPockets.LOG.warn("Could not save config: {}", e.toString());
        }
    }

    private TidyConfig sanitized() {
        TidyConfig d = new TidyConfig();
        if (sortOrder == null) sortOrder = d.sortOrder;
        if (protect == null) protect = d.protect;
        animSpeed = Math.clamp(animSpeed, 0.0, 3.0);
        safeModeClicksPerTick = Math.clamp(safeModeClicksPerTick, 1, 64);
        protectMargin = Math.clamp(protectMargin, 0, 20);
        return this;
    }
}
