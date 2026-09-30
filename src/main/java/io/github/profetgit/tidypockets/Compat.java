package io.github.profetgit.tidypockets;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The few calls that differ between 26.2 and 26.3. */
public final class Compat {
    private Compat() {}

    /** GLFW modifier bits (InputConstants only has them since 1.21.9). */
    public static final int MOD_SHIFT = 1, MOD_CONTROL = 2;

    /** Whether the GUI draws 3D pictures through render states that can take a pose (before 1.21.6 they draw in place). */
    //? if >=1.21.6 {
    public static final boolean HAS_PIP = true;
    //?} else {
    /*public static final boolean HAS_PIP = false;
    *///?}

    public static boolean isKeyDown(int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} else if >=1.21.9 {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        *///?} else {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
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

    /** The small hand bounce after placing a block. */
    public static void itemUsed(net.minecraft.world.InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.3 {
        mc.player.itemUsed(hand);
        //?} else {
        /*mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
        *///?}
    }

    public static boolean shiftDown() {
        return isKeyDown(InputConstants.KEY_LSHIFT) || isKeyDown(InputConstants.KEY_RSHIFT) || pretendedShift();
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

    /** A message over the hotbar. */
    public static void overlay(net.minecraft.client.player.LocalPlayer player, net.minecraft.network.chat.Component message) {
        //? if >=26.2 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    // ---- self-test helpers ----

    public static com.mojang.blaze3d.pipeline.RenderTarget mainTarget(Minecraft mc) {
        //? if >=26.2 {
        return mc.gameRenderer.mainRenderTarget();
        //?} else {
        /*return mc.getMainRenderTarget();
        *///?}
    }

    /** The raw RGBA bytes of a screenshot. */
    public static java.nio.ByteBuffer pixelBytes(com.mojang.blaze3d.platform.NativeImage img) {
        //? if >=26.2 {
        return img.getPixelBytes();
        //?}
        //? if >=1.21.2 <26.2 {
        /*int[] abgr = img.getPixelsABGR();
        java.nio.ByteBuffer out = java.nio.ByteBuffer.allocate(abgr.length * 4);
        for (int p : abgr) out.put((byte) p).put((byte) (p >> 8)).put((byte) (p >> 16)).put((byte) (p >> 24));
        out.flip();
        return out;
        *///?}
        //? if <1.21.2 {
        /*java.nio.ByteBuffer out = java.nio.ByteBuffer.allocate(img.getWidth() * img.getHeight() * 4);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getPixelRGBA(x, y);
                out.put((byte) p).put((byte) (p >> 8)).put((byte) (p >> 16)).put((byte) (p >> 24));
            }
        }
        out.flip();
        return out;
        *///?}
    }

    public static void clearToasts(Minecraft mc) {
        //? if >=26.2 {
        mc.gui.toastManager().clear();
        //?}
        //? if >=1.21.2 <26.2 {
        /*mc.getToastManager().clear();
        *///?}
        //? if <1.21.2 {
        /*mc.getToasts().clear();
        *///?}
    }

    public static void createWorld(Minecraft mc, String name) {
        //? if >=26.2 {
        mc.createWorldOpenFlows().createFreshLevel(name,
            new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.SURVIVAL,
                new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false),
                true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
            new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
            net.minecraft.world.level.levelgen.presets.WorldPresets::createTestWorldDimensions, mc.gui.screen());
        //?}
        //? if >=1.21.2 <26.2 {
        /*mc.createWorldOpenFlows().createFreshLevel(name,
            new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.SURVIVAL, false, net.minecraft.world.Difficulty.PEACEFUL, true,
                gameRules(),
                net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
            new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
            net.minecraft.world.level.levelgen.presets.WorldPresets::createFlatWorldDimensions, mc.screen);
        *///?}
        //? if <1.21.2 {
        /*mc.createWorldOpenFlows().createFreshLevel(name,
            new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.SURVIVAL, false, net.minecraft.world.Difficulty.PEACEFUL, true,
                gameRules(),
                net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
            new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
            access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
        *///?}
    }

    // ---- input events (1.21.9 replaced the loose arguments with event objects) ----

    public static boolean matches(KeyMapping mapping, io.github.profetgit.tidypockets.input.KeyEvt e) {
        //? if >=1.21.9 {
        return mapping.matches(new net.minecraft.client.input.KeyEvent(e.key(), e.scancode(), e.modifiers()));
        //?} else {
        /*return mapping.matches(e.key(), e.scancode());
        *///?}
    }

    public static boolean matchesMouse(KeyMapping mapping, io.github.profetgit.tidypockets.input.MouseEvt e) {
        //? if >=1.21.9 {
        return mapping.matchesMouse(new net.minecraft.client.input.MouseButtonEvent(e.x(), e.y(), new net.minecraft.client.input.MouseButtonInfo(e.button(), e.modifiers())));
        //?} else {
        /*return mapping.matchesMouse(e.button());
        *///?}
    }

    /** Sends a press through the screen's own method (self-test). */
    public static boolean mouseClicked(net.minecraft.client.gui.components.events.GuiEventListener screen, io.github.profetgit.tidypockets.input.MouseEvt e, boolean doubleClick) {
        //? if >=1.21.9 {
        return screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(e.x(), e.y(), new net.minecraft.client.input.MouseButtonInfo(e.button(), e.modifiers())), doubleClick);
        //?} else {
        /*sentModifiers = e.modifiers();
        try {
            return screen.mouseClicked(e.x(), e.y(), e.button());
        } finally {
            sentModifiers = -1;
        }
        *///?}
    }

    public static boolean mouseReleased(net.minecraft.client.gui.components.events.GuiEventListener screen, io.github.profetgit.tidypockets.input.MouseEvt e) {
        //? if >=1.21.9 {
        return screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(e.x(), e.y(), new net.minecraft.client.input.MouseButtonInfo(e.button(), e.modifiers())));
        //?} else {
        /*sentModifiers = e.modifiers();
        try {
            return screen.mouseReleased(e.x(), e.y(), e.button());
        } finally {
            sentModifiers = -1;
        }
        *///?}
    }

    public static boolean mouseDragged(net.minecraft.client.gui.components.events.GuiEventListener screen, io.github.profetgit.tidypockets.input.MouseEvt e, double dx, double dy) {
        //? if >=1.21.9 {
        return screen.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(e.x(), e.y(), new net.minecraft.client.input.MouseButtonInfo(e.button(), e.modifiers())), dx, dy);
        //?} else {
        /*sentModifiers = e.modifiers();
        try {
            return screen.mouseDragged(e.x(), e.y(), e.button(), dx, dy);
        } finally {
            sentModifiers = -1;
        }
        *///?}
    }

    public static boolean keyPressed(net.minecraft.client.gui.components.events.GuiEventListener screen, io.github.profetgit.tidypockets.input.KeyEvt e) {
        //? if >=1.21.9 {
        return screen.keyPressed(new net.minecraft.client.input.KeyEvent(e.key(), e.scancode(), e.modifiers()));
        //?} else {
        /*return screen.keyPressed(e.key(), e.scancode(), e.modifiers());
        *///?}
    }

    /** Self-test: the modifiers of the event being sent (before 1.21.9 the screen methods carry none). */
    private static int sentModifiers = -1;

    /** The modifier keys held right now, as GLFW modifier bits (shift 1, control 2, alt 4). */
    public static int modifiers() {
        if (sentModifiers >= 0) return sentModifiers;
        return (shiftDown() ? 1 : 0) | (isKeyDown(InputConstants.KEY_LCONTROL) || isKeyDown(InputConstants.KEY_RCONTROL) ? 2 : 0)
            | (isKeyDown(InputConstants.KEY_LALT) || isKeyDown(InputConstants.KEY_RALT) ? 4 : 0);
    }

    //? if >=1.21.9 {
    public static io.github.profetgit.tidypockets.input.KeyEvt keyEvt(net.minecraft.client.input.KeyEvent e) {
        //? if >=26.3 {
        return new io.github.profetgit.tidypockets.input.KeyEvt(e.key(), e.keycode(), e.modifiers());
        //?} else {
        /*return new io.github.profetgit.tidypockets.input.KeyEvt(e.key(), e.scancode(), e.modifiers());
        *///?}
    }

    public static net.minecraft.client.input.KeyEvent vanilla(io.github.profetgit.tidypockets.input.KeyEvt e) {
        return new net.minecraft.client.input.KeyEvent(e.key(), e.scancode(), e.modifiers());
    }
    //?}

    //? if >=1.21.11 {
    /*private static net.minecraft.world.level.gamerules.GameRules gameRules() {
        return new net.minecraft.world.level.gamerules.GameRules(net.minecraft.world.level.WorldDataConfiguration.DEFAULT.enabledFeatures());
    }
    *///?} else if >=1.21.2 {
    /*private static net.minecraft.world.level.GameRules gameRules() {
        return new net.minecraft.world.level.GameRules(net.minecraft.world.level.WorldDataConfiguration.DEFAULT.enabledFeatures());
    }
    *///?} else if >=1.20.5 {
    /*private static net.minecraft.world.level.GameRules gameRules() {
        return new net.minecraft.world.level.GameRules();
    }
    *///?}

    /** Self-test: Shift is pretended to be held (forced, or carried by the event being sent). */
    public static boolean pretendedShift() {
        return forceShift || (sentModifiers >= 0 && (sentModifiers & 1) != 0);
    }

    /** Durability an attack with this stack costs. */
    public static int attackCost(net.minecraft.world.item.ItemStack stack) {
        //? if >=1.21.5 {
        net.minecraft.world.item.component.Weapon w = stack.get(net.minecraft.core.component.DataComponents.WEAPON);
        return w == null ? 1 : w.itemDamagePerAttack();
        //?} else {
        /*return stack.getItem() instanceof net.minecraft.world.item.DiggerItem ? 2 : 1;
        *///?}
    }

    /** Pushes the GUI pose with an identity matrix (parts that must not scale with a pop). */
    public static void pushIdentity(net.minecraft.client.gui.GuiGraphicsExtractor g) {
        //? if >=1.21.6 {
        g.pose().pushMatrix().identity();
        //?} else {
        /*g.pose().pushPose();
        g.pose().setIdentity();
        *///?}
    }

    public static void screenshot(com.mojang.blaze3d.pipeline.RenderTarget target, java.util.function.Consumer<com.mojang.blaze3d.platform.NativeImage> then) {
        //? if >=1.21.5 {
        net.minecraft.client.Screenshot.takeScreenshot(target, then);
        //?} else {
        /*then.accept(net.minecraft.client.Screenshot.takeScreenshot(target));
        *///?}
    }

    /** The wheel through the screen's own method (self-test; before 1.21.2 the container hook lives in the mouse handler). */
    public static boolean mouseScrolled(net.minecraft.client.gui.components.events.GuiEventListener screen, double x, double y, double sx, double sy) {
        //? if >=1.21.2 {
        return screen.mouseScrolled(x, y, sx, sy);
        //?} else {
        /*boolean handled = screen.mouseScrolled(x, y, sx, sy);
        if (!handled && screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c) {
            handled = io.github.profetgit.tidypockets.mouse.MouseTweaks.scrolledAt(c, x, y, sy);
        }
        return handled;
        *///?}
    }
}
