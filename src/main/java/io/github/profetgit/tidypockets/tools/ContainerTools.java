package io.github.profetgit.tidypockets.tools;

import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.TidyPockets;
import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.inv.ClickSender;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import io.github.profetgit.tidypockets.mixin.ScreenInvoker;
import io.github.profetgit.tidypockets.sort.SortAction;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
//? if >=1.20.2 {
import net.minecraft.client.gui.components.WidgetSprites;
//?}
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Sort / deposit matching / restock buttons and the search box on storage screens. */
public final class ContainerTools {
    private static EditBox search;
    private static AbstractContainerScreen<?> searchScreen;

    private ContainerTools() {}

    //? if >=1.20.2 {
    private static WidgetSprites sprites(String name) {
        return new WidgetSprites(Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, name),
            Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, name + "_highlighted"));
    }
    //?}

    public static void init(AbstractContainerScreen<?> screen) {
        search = null;
        searchScreen = null;
        LocalPlayer p = Minecraft.getInstance().player;
        TidyConfig cfg = TidyConfig.get();
        if (p == null || !Inv.isStorage(screen.getMenu())) return;
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
        ScreenInvoker inv = (ScreenInvoker) screen;
        int x = a.tidypockets$left() + a.tidypockets$width() - 7 - 11, y = a.tidypockets$top() + 4;
        if (cfg.containerButtons) {
            button(inv, x, y, "sort", () -> SortAction.trySort(screen, null));
            button(inv, x - 12, y, "deposit", () -> deposit(screen));
            button(inv, x - 24, y, "restock", () -> restock(screen));
            x -= 36;
        }
        if (cfg.search) {
            button(inv, x, y, "search", () -> toggleSearch(screen));
            EditBox box = new EditBox(Minecraft.getInstance().font, a.tidypockets$left() + 2, a.tidypockets$top() - 14, 110, 12,
                Component.translatable("tidypockets.search"));
            box.setHint(Component.translatable("tidypockets.search.hint"));
            box.setMaxLength(40);
            box.setVisible(false);
            search = addWidget(inv, box);
            searchScreen = screen;
        }
    }

    /** Screen.addRenderableWidget through the widget lists. */
    private static <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable
        & net.minecraft.client.gui.narration.NarratableEntry> T addWidget(ScreenInvoker screen, T widget) {
        screen.tidypockets$renderables().add(widget);
        screen.tidypockets$children().add(widget);
        screen.tidypockets$narratables().add(widget);
        return widget;
    }

    private static void button(ScreenInvoker screen, int x, int y, String name, Runnable action) {
        //? if >=1.20.2 {
        ImageButton b = new ImageButton(x, y, 11, 11, sprites(name), btn -> action.run(),
            Component.translatable("tidypockets.button." + name));
        //?} else {
        /*net.minecraft.resources.ResourceLocation lo = new net.minecraft.resources.ResourceLocation(TidyPockets.MOD_ID, "textures/gui/sprites/" + name + ".png");
        net.minecraft.resources.ResourceLocation hi = new net.minecraft.resources.ResourceLocation(TidyPockets.MOD_ID, "textures/gui/sprites/" + name + "_highlighted.png");
        ImageButton b = new ImageButton(x, y, 11, 11, 0, 0, 11, lo, 11, 11, btn -> action.run(),
            Component.translatable("tidypockets.button." + name)) {
            @Override
            public void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                g.blit(isHoveredOrFocused() ? hi : lo, getX(), getY(), 0, 0, 11, 11, 11, 11);
            }
        };
        *///?}
        if (!io.github.profetgit.tidypockets.selftest.SelfTest.active()) b.setTooltip(Tooltip.create(Component.translatable("tidypockets.button." + name)));
        addWidget(screen, b);
    }

    // ---- search ----

    public static boolean searchOpen(AbstractContainerScreen<?> screen) {
        return search != null && searchScreen == screen && search.visible;
    }

    public static void toggleSearch(AbstractContainerScreen<?> screen) {
        if (search == null || searchScreen != screen) return;
        boolean show = !search.visible;
        search.setVisible(show);
        if (show) {
            screen.setFocused(search);
        } else {
            search.setValue("");
            screen.setFocused(null);
        }
    }

    /** Null when no search is active, else the lower-case query. */
    public static String query(AbstractContainerScreen<?> screen) {
        if (!searchOpen(screen) || search.getValue().isBlank()) return null;
        return search.getValue().toLowerCase().strip();
    }

    public static boolean matches(ItemStack s, String q) {
        if (s.isEmpty()) return false;
        return s.getHoverName().getString().toLowerCase().contains(q)
            || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains(q.replace(' ', '_'));
    }

    // ---- actions ----

    /**
     * Shift-clicks every unlocked main-inventory stack whose item is already somewhere in the container; with Shift held,
     * every unlocked main-inventory stack.
     */
    public static void deposit(AbstractContainerScreen<?> screen) {
        LocalPlayer p = Minecraft.getInstance().player;
        AbstractContainerMenu menu = screen.getMenu();
        if (p == null || !menu.getCarried().isEmpty() || ClickSender.busy()) return;
        List<Slot> container = Inv.containerSlots(menu, p);
        boolean all = Compat.shiftDown();
        Anims.beginMove(menu);
        for (Slot s : Inv.playerSlots(menu, p, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE)) {
            if (!s.hasItem() || SlotLocks.isLocked(s.getContainerSlot())) continue;
            boolean present = all;
            for (Slot c : container) if (ItemStack.isSameItemSameComponents(c.getItem(), s.getItem())) present = true;
            if (!present) continue;
            ClickSender.send(menu, s, 0, ContainerInput.QUICK_MOVE);
        }
        Anims.endMove(menu);
    }

    /**
     * Tops up partial stacks in the hotbar, then the main inventory, from matching stacks in the container; with Shift
     * held, shift-clicks everything out of the container.
     */
    public static void restock(AbstractContainerScreen<?> screen) {
        LocalPlayer p = Minecraft.getInstance().player;
        AbstractContainerMenu menu = screen.getMenu();
        if (p == null || !menu.getCarried().isEmpty() || ClickSender.busy()) return;
        List<Slot> container = Inv.containerSlots(menu, p);
        List<Slot> mine = Inv.playerSlots(menu, p, 0, Inventory.INVENTORY_SIZE);
        Anims.beginMove(menu);
        if (Compat.shiftDown()) {
            for (Slot c : container) if (c.hasItem()) ClickSender.send(menu, c, 0, ContainerInput.QUICK_MOVE);
            Anims.endMove(menu);
            return;
        }
        for (Slot s : mine) {
            ItemStack have = s.getItem();
            if (have.isEmpty() || SlotLocks.isLocked(s.getContainerSlot())) continue;
            for (Slot c : container) {
                int room = s.getMaxStackSize(have) - have.getCount();
                if (room <= 0) break;
                ItemStack from = c.getItem();
                if (!ItemStack.isSameItemSameComponents(from, have)) continue;
                int n = from.getCount();
                ClickSender.send(menu, c, 0, ContainerInput.PICKUP);
                ClickSender.send(menu, s, 0, ContainerInput.PICKUP);
                if (n > room) ClickSender.send(menu, c, 0, ContainerInput.PICKUP);
                have = s.getItem();
            }
        }
        Anims.endMove(menu);
    }
}
