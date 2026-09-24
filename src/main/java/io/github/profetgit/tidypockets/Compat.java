package io.github.profetgit.tidypockets;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The few calls that differ between 26.2 and 26.3. */
public final class Compat {
    private Compat() {}

    public static boolean isKeyDown(int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} else {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        *///?}
    }

    /** Q in the world: drop the held item (the call moved from the player to the game mode in 26.3). */
    public static void dropHeld() {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.3 {
        mc.gameMode.dropItem(mc.player, false);
        //?} else {
        /*mc.player.drop(false);
        *///?}
    }

    /** Visual arm swing for the local player. */
    public static void swing() {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.3 {
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
        //?} else {
        /*mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        *///?}
    }

    public static boolean shiftDown() {
        return isKeyDown(InputConstants.KEY_LSHIFT) || isKeyDown(InputConstants.KEY_RSHIFT) || forceShift;
    }

    /** Self-test only: pretend shift is held. */
    public static boolean forceShift;

    /** Self-test only: pretend this key is held. */
    public static KeyMapping forceHeld;

    /** Whether a keyboard-bound mapping is held right now; works while a screen is open, unlike {@link KeyMapping#isDown}. */
    public static boolean isHeld(KeyMapping mapping) {
        if (forceHeld == mapping) return true;
        InputConstants.Key key = ((KeyMappingAccessor) mapping).tidypockets$key();
        if (key.getType() == InputConstants.Type.MOUSE || key.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        return isKeyDown(key.getValue());
    }
}
