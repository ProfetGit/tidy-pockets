package io.github.profetgit.tidypockets.inv;

import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.core.ItemKey;
import io.github.profetgit.tidypockets.core.Stack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Converts real stacks into the pure {@link ItemKey}/{@link Stack} model, ordered like the creative inventory. */
public final class StackKeys {
    private static Object builtFor;
    private static final Map<Item, Integer> ITEM_ORDER = new HashMap<>();
    private static final Map<Item, List<ItemStack>> VARIANTS = new HashMap<>();

    private StackKeys() {}

    /** Identity with vanilla's merge rule: same item and same components. */
    private record Ident(ItemStack stack) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Ident i && ItemStack.isSameItemSameComponents(stack, i.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }

    public static Stack[] read(List<Slot> slots) {
        ensureOrder();
        Stack[] out = new Stack[slots.size()];
        for (int i = 0; i < out.length; i++) {
            ItemStack s = slots.get(i).getItem();
            if (!s.isEmpty()) out[i] = new Stack(key(s, slots.get(i)), s.getCount());
        }
        return out;
    }

    public static ItemKey key(ItemStack s, Slot slot) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        String tiebreak = id + s.getComponentsPatch();
        long order = switch (TidyConfig.get().sortOrder) {
            case CREATIVE -> creativeOrder(s);
            case NAME -> 0;
            case ID -> 0;
        };
        if (TidyConfig.get().sortOrder == TidyConfig.SortOrder.NAME) tiebreak = s.getHoverName().getString().toLowerCase() + "|" + tiebreak;
        int max = slot == null ? s.getMaxStackSize() : Math.min(s.getMaxStackSize(), slot.getMaxStackSize(s));
        return new ItemKey(new Ident(s.copyWithCount(1)), ItemStack.hashItemAndComponents(s), max,
            s.is(ItemTags.BUNDLES), order, tiebreak);
    }

    private static long creativeOrder(ItemStack s) {
        Integer base = ITEM_ORDER.get(s.getItem());
        if (base == null) return (1L << 40) + BuiltInRegistries.ITEM.getId(s.getItem());
        List<ItemStack> vars = VARIANTS.get(s.getItem());
        int v = 4095;
        for (int i = 0; i < vars.size() && i < 4095; i++) {
            if (ItemStack.isSameItemSameComponents(vars.get(i), s)) {
                v = i;
                break;
            }
        }
        return (long) base * 4096 + v;
    }

    private static void ensureOrder() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null || builtFor == conn.registryAccess()) return;
        builtFor = conn.registryAccess();
        CreativeModeTabs.tryRebuildTabContents(conn.enabledFeatures(), false, conn.registryAccess());
        ITEM_ORDER.clear();
        VARIANTS.clear();
        int next = 0;
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            for (ItemStack s : tab.getDisplayItems()) {
                if (ITEM_ORDER.putIfAbsent(s.getItem(), next) == null) next++;
                VARIANTS.computeIfAbsent(s.getItem(), k -> new java.util.ArrayList<>()).add(s);
            }
        }
    }
}
