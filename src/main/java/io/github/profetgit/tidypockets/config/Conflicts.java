package io.github.profetgit.tidypockets.config;

import io.github.profetgit.tidypockets.TidyPockets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Other mods that already do one of our jobs. Their overlapping feature of ours stays off while they are installed,
 * so the two never fight over the same click or animation; the config values themselves are left alone.
 */
public final class Conflicts {
    public static boolean mouse, fly, scroll, screenPop, sorting;

    private Conflicts() {}

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
