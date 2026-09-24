package io.github.profetgit.tidypockets.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.sort.SortAction;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Mouse and key handling added to every container screen. Returning true swallows the event. */
public final class ScreenInput {
    private ScreenInput() {}

    public static boolean mouseClicked(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return false;
        if (Keys.SORT.matchesMouse(e)) {
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

    public static boolean keyPressed(AbstractContainerScreen<?> screen, Slot hovered, KeyEvent e) {
        Creative.Tab tab = Creative.tab(screen);
        if (tab != Creative.Tab.NONE) return tab == Creative.Tab.INVENTORY && Keys.SORT.matches(e) && SortAction.trySort(screen, hovered);
        if (ContainerTools.searchOpen(screen)) {
            if (e.key() == InputConstants.KEY_ESCAPE) {
                ContainerTools.toggleSearch(screen);
                return true;
            }
            if (screen.getFocused() instanceof EditBox box) {
                box.keyPressed(e);
                return true;
            }
        }
        if ((e.hasControlDown() && e.key() == InputConstants.KEY_F) || Keys.SEARCH.matches(e)) {
            ContainerTools.toggleSearch(screen);
            return true;
        }
        if (Keys.SORT.matches(e)) return SortAction.trySort(screen, hovered);
        if (Keys.DEPOSIT.matches(e)) {
            ContainerTools.deposit(screen);
            return true;
        }
        if (Keys.RESTOCK.matches(e)) {
            ContainerTools.restock(screen);
            return true;
        }
        return false;
    }
}
