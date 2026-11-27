package io.github.profetgit.tidypockets.ui.settings;

import io.github.profetgit.profetsettings.Gfx;
import net.minecraft.resources.Identifier;

// Vanilla's own widgets as the previews draw them. Their art moved between versions: GUI sprites came with 1.20.2 (before it
// widgets.png and plain fills), the slot highlight and the shield slot sprites with 1.21.2.
final class VanillaArt {
    private VanillaArt() {}

    //? if >=1.20.2 {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button"),
        HOTBAR = Identifier.withDefaultNamespace("hud/hotbar"), SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection"),
        SCROLLER = Identifier.withDefaultNamespace("widget/scroller"), TRACK = Identifier.withDefaultNamespace("widget/scroller_background"),
        FIELD = Identifier.withDefaultNamespace("widget/text_field_highlighted"),
        LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    //?} else {
    /*private static final Identifier WIDGETS = Identifier.withDefaultNamespace("textures/gui/widgets.png"),
        LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/options_background.png");
    *///?}

    //? if >=1.21.2 {
    private static final Identifier HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back"),
        HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front"),
        SHIELD = Identifier.withDefaultNamespace("container/slot/shield");
    //?} else {
    /*private static final Identifier SHIELD = Identifier.withDefaultNamespace("textures/item/empty_armor_slot_shield.png");
    *///?}

    // a 20 px tall button
    static void button(Gfx g, int x, int y, int w) {
        //? if >=1.20.2 {
        g.sprite(BUTTON, x, y, w, 20);
        //?} else {
        /*g.blit(WIDGETS, x, y, 0, 66, w / 2, 20, 256, 256);
        g.blit(WIDGETS, x + w / 2, y, 200 - (w - w / 2), 66, w - w / 2, 20, 256, 256);
        *///?}
    }

    static void hotbar(Gfx g, int x, int y, int w) {
        //? if >=1.20.2 {
        g.sprite(HOTBAR, x, y, w, 22);
        //?} else {
        /*g.blit(WIDGETS, x, y, 0, 0, w, 22, 256, 256);
        *///?}
    }

    static void selection(Gfx g, int x, int y) {
        //? if >=1.20.2 {
        g.sprite(SELECTION, x, y, 24, 23);
        //?} else {
        /*g.blit(WIDGETS, x, y, 0, 22, 24, 23, 256, 256);
        *///?}
    }

    // the highlight of the slot whose item is at (x, y): one sprite behind the item, one over it (before 1.21.2 one fill over it)
    static void highlight(Gfx g, int x, int y, boolean front) {
        //? if >=1.21.2 {
        g.sprite(front ? HIGHLIGHT_FRONT : HIGHLIGHT_BACK, x - 4, y - 4, 24, 24);
        //?} else {
        /*if (front) g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        *///?}
    }

    static void shield(Gfx g, int x, int y) {
        //? if >=1.21.2 {
        g.sprite(SHIELD, x, y, 16, 16);
        //?} else {
        /*g.blit(SHIELD, x, y, 0, 0, 16, 16, 16, 16);
        *///?}
    }

    // the dark options list background, scrolled to v
    static void listBackground(Gfx g, int x, int y, int w, int h, int v) {
        //? if >=1.20.2 {
        g.blit(LIST_BACKGROUND, x, y, 0, v, w, h, 32, 32);
        //?} else {
        /*g.blit(LIST_BACKGROUND, x, y, 0, v, w, h, 32, 32, 0xFF404040);
        *///?}
    }

    static void scrollTrack(Gfx g, int x, int y, int h) {
        //? if >=1.20.2 {
        g.sprite(TRACK, x, y, 6, h);
        //?} else {
        /*g.fill(x, y, x + 6, y + h, 0xFF000000);
        *///?}
    }

    static void scrollThumb(Gfx g, int x, int y, int h) {
        //? if >=1.20.2 {
        g.sprite(SCROLLER, x, y, 6, h);
        //?} else {
        /*g.fill(x, y, x + 6, y + h, 0xFF808080);
        g.fill(x, y, x + 5, y + h - 1, 0xFFC0C0C0);
        *///?}
    }

    static void field(Gfx g, int x, int y, int w, int h) {
        //? if >=1.20.2 {
        g.sprite(FIELD, x, y, w, h);
        //?} else {
        /*g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
        *///?}
    }
}
