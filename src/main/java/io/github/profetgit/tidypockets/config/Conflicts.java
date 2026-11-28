package io.github.profetgit.tidypockets.config;

import io.github.profetgit.tidypockets.TidyPockets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Other mods that already do one of our jobs. Their overlapping feature of ours stays off while they are installed,
 * so the two never fight over the same click or animation; the config values themselves are left alone.
 */
public final class Conflicts {
    public static boolean mouse, fly, scroll, screenPop, sorting;
    /**
     * A resource pack replaced the shader that draws GUI textures. Such packs (Immersive Interfaces) pick and place
     * panel art by exact screen position: the screen pop must not move vertices before the shader sees them, and the
     * settings previews draw the game's own art.
     */
    public static boolean guiShaders;
    private static final String[] GUI_SHADERS = {"position_tex.json", "position_tex.vsh", "position_tex_color.json",
        "position_tex_color.vsh"};

    private Conflicts() {}

    /** Packs can change at any time, so this runs for every newly opened screen. */
    public static void checkGuiShaders() {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        String pack = Stream.of(GUI_SHADERS)
            .map(file -> resources.getResource(Identifier.withDefaultNamespace("shaders/core/" + file)))
            .flatMap(Optional::stream)
            .map(Resource::sourcePackId)
            // the player's and the server's packs only: the game and mods ship their own copies
            .filter(id -> id.startsWith("file/") || id.startsWith("server"))
            .findFirst().orElse(null);
        if (pack != null && !guiShaders) {
            TidyPockets.LOG.info("Resource pack {} replaces the GUI shaders", pack);
        }
        guiShaders = pack != null;
    }

    public static void detect() {
        List<String> notes = new ArrayList<>();
        mouse = any(notes, "mouse shortcuts", "mousetweaks", "mousewheelie", "moremousetweaks");
        fly = any(notes, "item flight", "smoothswapping", "smooth_swapping", "smooth-swapping");
        scroll = any(notes, "smooth scrolling", "smoothscroll", "smooth_scroll", "smoothscrollingrefurbished");
        screenPop = any(notes, "screen pop", "smoothgui", "smooth_gui", "animated_gui", "animatedgui");
        sorting = any(notes, "sorting and refill", "inventoryprofilesnext");
        if (!notes.isEmpty()) TidyPockets.LOG.info("Tidy Pockets turned off to avoid doubling up: {}", String.join("; ", notes));
    }

    private static boolean any(List<String> notes, String feature, String... ids) {
        String found = Stream.of(ids).filter(TidyPockets.platform()::isModLoaded).findFirst().orElse(null);
        if (found != null) notes.add(feature + " (" + found + " is installed)");
        return found != null;
    }
}
