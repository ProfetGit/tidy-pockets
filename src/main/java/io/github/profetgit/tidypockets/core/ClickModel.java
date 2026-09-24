package io.github.profetgit.tidypockets.core;

import java.util.List;

/**
 * Pure re-implementation of vanilla's left-click PICKUP on plain container slots, used to prove a click plan
 * before anything is sent. Clicking a carried stack onto an occupied slot, or onto a slot holding a special item,
 * is where vanilla would insert into a bundle instead; the model rejects those so a bad plan never ships.
 */
public final class ClickModel {
    private final Stack[] slots;
    private Stack carried;

    public ClickModel(Stack[] slots) {
        this.slots = slots.clone();
    }

    public static Stack[] run(Stack[] slots, List<Integer> clicks) {
        ClickModel m = new ClickModel(slots);
        for (int c : clicks) m.click(c);
        if (m.carried != null) throw new IllegalStateException("plan ends holding " + m.carried);
        return m.slots;
    }

    public void click(int i) {
        Stack s = slots[i];
        if (carried == null) {
            if (s == null) throw new IllegalStateException("empty click on " + i);
            carried = s;
            slots[i] = null;
            return;
        }
        if (s == null) {
            int put = Math.min(carried.count(), carried.key().maxStack);
            slots[i] = carried.withCount(put);
            carried = carried.withCount(carried.count() - put);
            return;
        }
        if (carried.key().special || s.key().special) {
            throw new IllegalStateException("special item stacked on " + i + ": " + carried + " onto " + s);
        }
        if (carried.key().equals(s.key())) {
            int put = Math.min(carried.count(), s.space());
            if (put == 0) throw new IllegalStateException("no-op merge on " + i);
            slots[i] = s.withCount(s.count() + put);
            carried = carried.withCount(carried.count() - put);
            return;
        }
        slots[i] = carried;
        carried = s;
    }
}
