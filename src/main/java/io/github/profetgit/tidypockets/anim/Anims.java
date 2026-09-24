package io.github.profetgit.tidypockets.anim;

import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Entry points the gameplay code calls to start animations. */
public final class Anims {
    private Anims() {}

    public static void screenOpened(net.minecraft.client.gui.screens.Screen s) {
        ScreenPop.opened(s);
        PlayerHop.reset();
    }

    public static void sorted(List<Slot> slots, boolean[] moved) {
        SortPop.start(slots, moved);
    }

    /** Snapshot the menu before a group of clicks; {@link #endMove} turns what moved into flying items. */
    public static void beginMove(AbstractContainerMenu menu) {
        FlyAnims.begin(menu);
    }

    public static void endMove(AbstractContainerMenu menu) {
        FlyAnims.end(menu);
    }

    /** A hotbar slot (0-8) or the offhand (40) was just refilled. */
    public static void refilled(int invIndex) {
        HudAnims.pop(invIndex);
    }

    /** Tool protection kicked in for a hotbar slot or the offhand. */
    public static void toolWarn(int invIndex) {
        HudAnims.shake(invIndex);
    }
}
