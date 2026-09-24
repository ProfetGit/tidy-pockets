package io.github.profetgit.tidypockets.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The sorted layout: identical stacks merged, full stacks first, keys in {@link ItemKey#ORDER}; locked slots untouched. */
public final class SortPlanner {
    private SortPlanner() {}

    public static Stack[] target(Stack[] slots, boolean[] locked) {
        Map<ItemKey, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < slots.length; i++) {
            if (!locked[i] && slots[i] != null) totals.merge(slots[i].key(), slots[i].count(), Integer::sum);
        }
        List<ItemKey> keys = new ArrayList<>(totals.keySet());
        keys.sort(ItemKey.ORDER);
        List<Stack> stacks = new ArrayList<>();
        for (ItemKey k : keys) {
            int left = totals.get(k);
            while (left > 0) {
                int n = Math.min(left, k.maxStack);
                stacks.add(new Stack(k, n));
                left -= n;
            }
        }
        Stack[] out = new Stack[slots.length];
        int next = 0;
        for (int i = 0; i < slots.length; i++) {
            if (locked[i]) out[i] = slots[i];
            else if (next < stacks.size()) out[i] = stacks.get(next++);
        }
        if (next < stacks.size()) throw new IllegalStateException("sorted layout does not fit");
        return out;
    }
}
