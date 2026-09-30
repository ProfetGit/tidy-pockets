package io.github.profetgit.tidypockets.input;

/** A key press as the mod's input code sees it: the same on every Minecraft version (1.21.9 changed the vanilla event). */
public record KeyEvt(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return (modifiers & 1) != 0;
    }

    public boolean hasControlDown() {
        return (modifiers & 2) != 0;
    }
}
