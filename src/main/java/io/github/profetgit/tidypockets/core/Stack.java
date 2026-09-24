package io.github.profetgit.tidypockets.core;

public record Stack(ItemKey key, int count) {
    public Stack {
        if (count <= 0) throw new IllegalArgumentException("empty stack " + key);
    }

    public Stack withCount(int n) {
        return n <= 0 ? null : new Stack(key, n);
    }

    public int space() {
        return key.maxStack - count;
    }
}
