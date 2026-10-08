package io.github.profetgit.tidypockets.compat;

//? if (fabric || neoforge) && >=26.2 {
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.BindContext;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import io.github.profetgit.tidypockets.palette.Palette;
import net.minecraft.world.entity.player.Inventory;
import io.github.profetgit.tidypockets.screen.ScreenInput;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/** Controller bindings for container screens, registered through Controlify's entrypoint (a service file, loader-neutral). */
public final class ControlifyCompat implements ControlifyEntrypoint {
    private static InputBindingSupplier sort, lock, palette, clearPalette, search, deposit, restock;

    private static InputBindingSupplier bind(ControlifyBindApi api, String id) {
        return api.registerBinding(b -> b
            .id("tidypockets", id)
            .category(Component.translatable("controlify.binding.tidypockets.category"))
            .allowedContexts(BindContext.CONTAINER));
    }

    @Override
    public void onControlifyPreInit(PreInitContext ctx) {
        ControlifyBindApi api = ctx.bindings();
        sort = bind(api, "sort");
        lock = bind(api, "lock");
        palette = bind(api, "palette");
        clearPalette = bind(api, "clear_palette");
        search = bind(api, "search");
        deposit = bind(api, "deposit");
        restock = bind(api, "restock");
        ctx.contextualDomains().container().registerContributor(ControlifyCompat::contribute);
        io.github.profetgit.tidypockets.TidyPockets.LOG.info("Controlify bindings registered");
    }

    private static net.minecraft.resources.Identifier fact(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath("tidypockets", path);
    }

    /** Facts the guide rules in assets/controlify/contextual/guide/container.json test, so a hint only shows when its action would do something. */
    private static void contribute(dev.isxander.controlify.api.contextual.ContainerContext c,
                                   dev.isxander.controlify.api.contextual.ContextualStateSink sink) {
        //? if >=26.2 {
        var screen = Minecraft.getInstance().gui.screen();
        //?} else {
        /*var screen = Minecraft.getInstance().screen;
        *///?}
        var tab = Creative.tab(screen);
        var slot = c.hoveredSlot();
        var player = c.player();
        boolean holding = !c.holdingItem().isEmpty();
        boolean own = slot != null && Inv.isPlayerSlot(slot, player);
        int index = own ? Inv.index(slot) : -1;
        boolean lockable = own && !holding && (index < Inventory.INVENTORY_SIZE || index == Inventory.SLOT_OFFHAND);
        boolean hotbar = own && index >= 0 && index < Inventory.SELECTION_SIZE;
        sink.contributeFact(fact("can_sort"), screen instanceof AbstractContainerScreen<?> && tab != Creative.Tab.ITEMS && !holding);
        sink.contributeFact(fact("can_lock"), lockable);
        sink.contributeFact(fact("hovering_locked"), lockable && SlotLocks.isLocked(index));
        sink.contributeFact(fact("can_palette"), TidyConfig.get().randomEnabled && tab != Creative.Tab.ITEMS && hotbar && slot.hasItem());
        sink.contributeFact(fact("hovering_palette"), hotbar && Palette.has(index));
        sink.contributeFact(fact("palette_set"), TidyConfig.get().randomEnabled && !Palette.slots().isEmpty());
        sink.contributeFact(fact("container_tools"), screen instanceof AbstractContainerScreen<?> && tab == Creative.Tab.NONE);
    }

    @Override
    public void onControlifyInit(dev.isxander.controlify.api.entrypoint.InitContext ctx) {}

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {}

    public static void tick(Minecraft mc) {
        //? if >=26.2 {
        var current = mc.gui.screen();
        //?} else {
        /*var current = mc.screen;
        *///?}
        if (sort == null || !(current instanceof AbstractContainerScreen<?> screen)) return;
        var controller = ControlifyApi.get().getCurrentController().orElse(null);
        if (controller == null) return;
        var hovered = ((AbstractContainerScreenAccessor) screen).tidypockets$hoveredSlot();
        if (sort.on(controller).justPressed()) press("sort", screen, hovered);
        if (lock.on(controller).justPressed()) press("lock", screen, hovered);
        if (palette.on(controller).justPressed()) press("palette", screen, hovered);
        if (clearPalette.on(controller).justPressed()) press("clear_palette", screen, hovered);
        if (search.on(controller).justPressed()) press("search", screen, hovered);
        if (deposit.on(controller).justPressed()) press("deposit", screen, hovered);
        if (restock.on(controller).justPressed()) press("restock", screen, hovered);
    }

    /** Self-test diagnostics: the connected controller, the input mode and the slot under the cursor. */
    public static String status() {
        var api = ControlifyApi.get();
        var screen = Minecraft.getInstance().gui.screen();
        String slot = "-";
        if (screen instanceof AbstractContainerScreen<?> c) {
            var h = ((AbstractContainerScreenAccessor) c).tidypockets$hoveredSlot();
            slot = h == null ? "none" : String.valueOf(h.index);
        }
        return "controller=" + api.getCurrentController().map(c -> c.name()).orElse("none") + " mode=" + api.currentInputMode() + " hovered=" + slot;
    }

    public static boolean controllerReady() {
        return ControlifyApi.get().getCurrentController().isPresent();
    }

    /** Self-test: gives a binding a controller button (the real ones are unbound except Sort). */
    public static boolean bindForTest(String id, String button) {
        var controller = ControlifyApi.get().getCurrentController().orElse(null);
        InputBindingSupplier supplier = switch (id) {
            case "sort" -> sort;
            case "lock" -> lock;
            case "palette" -> palette;
            case "clear_palette" -> clearPalette;
            case "search" -> search;
            case "deposit" -> deposit;
            case "restock" -> restock;
            default -> null;
        };
        if (controller == null || supplier == null) return false;
        supplier.on(controller).setBoundInput(new dev.isxander.controlify.bindings.input.ButtonInput(
            net.minecraft.resources.Identifier.parse(button)));
        return true;
    }

    /** What a binding does; the self-test calls it directly because a headless client has no controller. */
    public static boolean press(String id, AbstractContainerScreen<?> screen, net.minecraft.world.inventory.Slot hovered) {
        return switch (id) {
            case "sort" -> ScreenInput.sort(screen, hovered);
            case "lock" -> ScreenInput.toggleLock(screen, hovered);
            case "palette" -> ScreenInput.palette(screen, hovered, false);
            case "clear_palette" -> ScreenInput.palette(screen, hovered, true);
            case "search" -> ScreenInput.tool(screen, () -> ContainerTools.toggleSearch(screen));
            case "deposit" -> ScreenInput.tool(screen, () -> ContainerTools.deposit(screen));
            case "restock" -> ScreenInput.tool(screen, () -> ContainerTools.restock(screen));
            default -> false;
        };
    }
}
//?} else {
/*public final class ControlifyCompat {
    private ControlifyCompat() {}

    public static void tick(net.minecraft.client.Minecraft mc) {}

    public static String status() {
        return "";
    }

    public static boolean controllerReady() {
        return false;
    }

    public static boolean bindForTest(String id, String button) {
        return false;
    }

    public static boolean press(String id, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
                                net.minecraft.world.inventory.Slot hovered) {
        return false;
    }
}
*///?}
