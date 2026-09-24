package io.github.profetgit.tidypockets.core;

import java.util.Comparator;
import java.util.Objects;

/**
 * Identity of a stack for sorting: two stacks with equal keys merge in vanilla. {@code special} marks items that
 * override left-click stacking (bundles), which the click planner must never click onto an occupied slot.
 */
public final class ItemKey {
    public static final Comparator<ItemKey> ORDER =
        Comparator.comparingLong((ItemKey k) -> k.order).thenComparing(k -> k.tiebreak);

    private final Object identity;
    private final int hash;
    public final int maxStack;
    public final boolean special;
    public final long order;
    public final String tiebreak;

    public ItemKey(Object identity, int hash, int maxStack, boolean special, long order, String tiebreak) {
        this.identity = identity;
        this.hash = hash;
        this.maxStack = Math.max(1, maxStack);
        this.special = special;
        this.order = order;
        this.tiebreak = tiebreak;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ItemKey k && hash == k.hash && Objects.equals(identity, k.identity);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return tiebreak;
    }
}
