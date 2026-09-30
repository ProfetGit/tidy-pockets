package io.github.profetgit.tidypockets.palette;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.anim.HudAnims;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * The random palette: the hotbar slots marked with a die. While one of them is selected, placing a block rolls which
 * marked slot it comes from. Marks are kept per world or server, like slot locks.
 */
public final class Palette {
    private static final class WorldPalette {
        TreeSet<Integer> slots = new TreeSet<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, WorldPalette> all;

    private Palette() {}

    private static Path file() {
        return TidyPockets.platform().configDir().resolve("tidypockets-palette.json");
    }

    private static WorldPalette current() {
        if (all == null) {
            all = new HashMap<>();
            try {
                if (Files.isRegularFile(file())) {
                    Map<String, WorldPalette> m = GSON.fromJson(Files.readString(file()), new TypeToken<Map<String, WorldPalette>>() {}.getType());
                    if (m != null) all.putAll(m);
                }
            } catch (Exception e) {
                TidyPockets.LOG.warn("Could not read the random palette: {}", e.toString());
            }
        }
        return all.computeIfAbsent(SlotLocks.worldKey(), k -> new WorldPalette());
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(all));
        } catch (Exception e) {
            TidyPockets.LOG.warn("Could not save the random palette: {}", e.toString());
        }
    }

    public static boolean has(int hotbarSlot) {
        return current().slots.contains(hotbarSlot);
    }

    public static boolean canJoin(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    /** Hotbar slots (0-8) that are marked. */
    public static List<Integer> slots() {
        return List.copyOf(current().slots);
    }

    /** Marked slots that hold a block right now. */
    public static int usable(LocalPlayer p) {
        int n = 0;
        for (int i : current().slots) if (canJoin(p.getInventory().getItem(i))) n++;
        return n;
    }

    /** True while the selected slot is marked, so the next placement rolls. */
    public static boolean armed(LocalPlayer p) {
        return p != null && TidyConfig.get().randomEnabled && has(p.getInventory().getSelectedSlot());
    }

    /** Marks or unmarks a hotbar slot. Returns false when the slot can't join (empty, or not a block). */
    public static boolean toggle(int hotbarSlot, ItemStack stack) {
        WorldPalette w = current();
        if (w.slots.remove(hotbarSlot)) {
            save();
            return true;
        }
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.SELECTION_SIZE || !canJoin(stack)) return false;
        w.slots.add(hotbarSlot);
        save();
        return true;
    }

    public static void clear() {
        if (current().slots.isEmpty()) return;
        current().slots.clear();
        save();
    }

    /** The palette key was pressed in the world (or over a hotbar slot in a screen). Shift clears the whole palette. */
    public static void press(LocalPlayer p, int hotbarSlot, boolean clearAll) {
        Minecraft mc = Minecraft.getInstance();
        if (clearAll) {
            boolean had = !current().slots.isEmpty();
            clear();
            click(mc, 0.8f);
            Compat.overlay(p, Component.translatable(had ? "tidypockets.palette.cleared" : "tidypockets.palette.empty"));
            return;
        }
        ItemStack stack = p.getInventory().getItem(hotbarSlot);
        boolean was = has(hotbarSlot);
        if (!toggle(hotbarSlot, stack)) {
            click(mc, 0.6f);
            HudAnims.shake(hotbarSlot);
            Compat.overlay(p, Component.translatable("tidypockets.palette.blocksOnly"));
            return;
        }
        click(mc, was ? 1.0f : 1.4f);
        HudAnims.pop(hotbarSlot);
        int n = usable(p);
        Compat.overlay(p, Component.translatable(n == 0 ? "tidypockets.palette.empty"
            : n == 1 ? "tidypockets.palette.one" : "tidypockets.palette.count", n));
    }

    private static void click(Minecraft mc, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.4f));
    }

    /** Keeps the marks honest: a slot that now holds something that isn't a block leaves the palette. */
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (mc.gui.screen() == null) {
            while (Keys.PALETTE.consumeClick()) {
                if (TidyConfig.get().randomEnabled) press(p, p.getInventory().getSelectedSlot(), Compat.shiftDown());
            }
        } else {
            while (Keys.PALETTE.consumeClick()) { /* the screen decides what the key means */ }
        }
        WorldPalette w = current();
        boolean changed = false;
        for (Integer i : List.copyOf(w.slots)) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && !canJoin(s)) {
                w.slots.remove(i);
                changed = true;
            }
        }
        if (changed) save();
    }

    private static final Identifier DICE = Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "dice");
    private static final Identifier DICE_DIM = Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "dice_dim");

    /** Draws the die in the top-left corner of a marked hotbar slot at (x, y): lit while a palette slot is selected. */
    public static void drawPip(GuiGraphicsExtractor g, int x, int y, int hotbarSlot, ItemStack stack) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !TidyConfig.get().randomEnabled || !has(hotbarSlot) || !canJoin(stack)) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, armed(p) ? DICE : DICE_DIM, x - 1, y - 1, 7, 7);
    }

    /** Hotbar version of {@link #drawPip}: finds the slot from the stack the HUD is drawing (only your own hotbar). */
    public static void drawHudPip(GuiGraphicsExtractor g, int x, int y, Player shown, ItemStack stack) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || shown != p) return;
        int slot = hotbarIndexOf(p, stack);
        if (slot >= 0) drawPip(g, x, y, slot, stack);
    }

    /** The hotbar slot (0-8) holding exactly this stack object, or -1. */
    public static int hotbarIndexOf(LocalPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return -1;
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) if (p.getInventory().getItem(i) == stack) return i;
        return -1;
    }

    /** Test hook: forget what was read from disk. */
    public static void reload() {
        all = null;
    }
}
