package io.github.profetgit.tidypockets.input;

import io.github.profetgit.tidypockets.Compat;

/** A key press as the mod's input code sees it: the same on every Minecraft version (1.21.9 changed the vanilla event). */
public record KeyEvt(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return (modifiers & Compat.MOD_SHIFT) != 0;
    }

    public boolean hasControlDown() {
        return (modifiers & Compat.MOD_CONTROL) != 0;
    }
}
