package io.github.profetgit.tidypockets.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.palette.Palette;
import io.github.profetgit.tidypockets.sort.SortAction;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import io.github.profetgit.tidypockets.input.KeyEvt;
import io.github.profetgit.tidypockets.input.MouseEvt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Mouse and key handling added to every container screen. Returning true swallows the event. */
public final class ScreenInput {
    private ScreenInput() {}

    public static boolean mouseClicked(AbstractContainerScreen<?> screen, Slot slot, MouseEvt e) {
        if (Minecraft.getInstance().player == null) return false;
        if (Compat.matchesMouse(Keys.SORT, e)) return sort(screen, slot);
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT && Compat.isHeld(Keys.LOCK) && toggleLock(screen, slot)) return true;
        return false;
    }

    public static boolean toggleLock(AbstractContainerScreen<?> screen, Slot slot) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || slot == null || !Inv.isPlayerSlot(slot, p) || !screen.getMenu().getCarried().isEmpty()) return false;
        int i = Inv.index(slot);
        if (i >= Inventory.INVENTORY_SIZE && i != Inventory.SLOT_OFFHAND) return false;
        SlotLocks.toggle(i, slot.getItem());
        boolean on = SlotLocks.isLocked(i);
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), on ? 1.4f : 1.0f, 0.4f));
        return true;
    }

    /** The palette key's action on a hovered hotbar slot. */
    public static boolean palette(AbstractContainerScreen<?> screen, Slot hovered, boolean clear) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!TidyConfig.get().randomEnabled || Creative.tab(screen) == Creative.Tab.ITEMS || player == null || hovered == null
            || screen.getFocused() instanceof EditBox || ContainerTools.searchOpen(screen)
            || !Inv.isPlayerSlot(hovered, player) || Inv.index(hovered) >= Inventory.SELECTION_SIZE) return false;
        Palette.press(player, Inv.index(hovered), clear);
        return true;
    }

    /** What the sort key does in a screen, for input that is not a mouse click. */
    public static boolean sort(AbstractContainerScreen<?> screen, Slot hovered) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || Creative.tab(screen) == Creative.Tab.ITEMS) return false;
        if (p.hasInfiniteMaterials() && hovered != null && hovered.hasItem()) return false;
        if (!screen.getMenu().getCarried().isEmpty()) return false;
        return SortAction.trySort(screen, hovered);
    }

    /** Search, deposit and restock only exist on plain container screens. */
    public static boolean tool(AbstractContainerScreen<?> screen, Runnable action) {
        if (Creative.tab(screen) != Creative.Tab.NONE) return false;
        action.run();
        return true;
    }

    public static boolean keyPressed(AbstractContainerScreen<?> screen, Slot hovered, KeyEvt e) {
        Creative.Tab tab = Creative.tab(screen);
        if (Compat.matches(Keys.PALETTE, e) && palette(screen, hovered, Compat.shiftDown())) return true;
        if (tab != Creative.Tab.NONE) return tab == Creative.Tab.INVENTORY && Compat.matches(Keys.SORT, e) && SortAction.trySort(screen, hovered);
        if (ContainerTools.searchOpen(screen)) {
            if (e.key() == InputConstants.KEY_ESCAPE) {
                ContainerTools.toggleSearch(screen);
                return true;
            }
            if (screen.getFocused() instanceof EditBox box) {
                Compat.keyPressed(box, e);
                return true;
            }
        }
        if ((e.hasControlDown() && e.key() == InputConstants.KEY_F) || Compat.matches(Keys.SEARCH, e)) {
            ContainerTools.toggleSearch(screen);
            return true;
        }
        if (Compat.matches(Keys.SORT, e)) return SortAction.trySort(screen, hovered);
        if (Compat.matches(Keys.DEPOSIT, e)) {
            ContainerTools.deposit(screen);
            return true;
        }
        if (Compat.matches(Keys.RESTOCK, e)) {
            ContainerTools.restock(screen);
            return true;
        }
        return false;
    }
}
