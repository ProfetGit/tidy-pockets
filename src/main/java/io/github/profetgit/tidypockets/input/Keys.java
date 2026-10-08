package io.github.profetgit.tidypockets.input;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.TidyPockets;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class Keys {
    //? if >=1.21.9 {
    public static final KeyMapping.Category CATEGORY = category(Identifier.fromNamespaceAndPath(TidyPockets.MOD_ID, "main"));

    /** Controlify's entrypoint can load this class before the loader entry has run, so there may be no platform yet. */
    private static KeyMapping.Category category(Identifier id) {
        if (TidyPockets.platform() != null) return TidyPockets.platform().keyCategory(id);
        //? neoforge {
        /*return new KeyMapping.Category(id);
        *///?} else {
        return KeyMapping.Category.register(id);
        //?}
    }
    //?} else {
    /*public static final String CATEGORY = "key.categories.tidypockets";
    *///?}

    public static final KeyMapping SORT = new KeyMapping("key.tidypockets.sort",
        InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE, CATEGORY);
    public static final KeyMapping LOCK = new KeyMapping("key.tidypockets.lock", InputConstants.KEY_LALT, CATEGORY);
    public static final KeyMapping PALETTE = new KeyMapping("key.tidypockets.palette", InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping SEARCH = new KeyMapping("key.tidypockets.search", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping DEPOSIT = new KeyMapping("key.tidypockets.deposit", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping RESTOCK = new KeyMapping("key.tidypockets.restock", InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static final List<KeyMapping> ALL = List.of(SORT, LOCK, PALETTE, SEARCH, DEPOSIT, RESTOCK);

    private Keys() {}
}
