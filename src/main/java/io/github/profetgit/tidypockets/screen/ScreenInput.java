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
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return false;
        if (Compat.matchesMouse(Keys.SORT, e)) {
            if (Creative.tab(screen) == Creative.Tab.ITEMS) return false;
            if (p.hasInfiniteMaterials() && slot != null && slot.hasItem()) return false;
            if (!screen.getMenu().getCarried().isEmpty()) return false;
            return SortAction.trySort(screen, slot);
        }
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT && slot != null && Inv.isPlayerSlot(slot, p)
            && Compat.isHeld(Keys.LOCK) && screen.getMenu().getCarried().isEmpty()) {
            int i = Inv.index(slot);
            if (i < Inventory.INVENTORY_SIZE || i == Inventory.SLOT_OFFHAND) {
                SlotLocks.toggle(i, slot.getItem());
                boolean on = SlotLocks.isLocked(i);
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), on ? 1.4f : 1.0f, 0.4f));
                return true;
            }
        }
        return false;
    }

    public static boolean keyPressed(AbstractContainerScreen<?> screen, Slot hovered, KeyEvt e) {
        Creative.Tab tab = Creative.tab(screen);
        LocalPlayer player = Minecraft.getInstance().player;
        if (Compat.matches(Keys.PALETTE, e) && TidyConfig.get().randomEnabled && tab != Creative.Tab.ITEMS && player != null && hovered != null
            && !(screen.getFocused() instanceof EditBox) && !ContainerTools.searchOpen(screen)
            && Inv.isPlayerSlot(hovered, player) && Inv.index(hovered) < Inventory.SELECTION_SIZE) {
            Palette.press(player, Inv.index(hovered), Compat.shiftDown());
            return true;
        }
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
