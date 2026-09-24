package io.github.profetgit.tidypockets.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns a region's contents into the shortest practical list of left clicks (slot indices) that produces
 * {@link SortPlanner#target}. Three phases:
 * <ol>
 *   <li>merge partial stacks of the same item, so the stack multiset matches the target;</li>
 *   <li>permute by item: cycles through the cursor, or, for cycles holding a bundle, hole moves through an empty
 *       slot, so a carried stack only ever lands on an empty slot or a different item;</li>
 *   <li>move each item's partial stack into its target slot.</li>
 * </ol>
 * The result is replayed through {@link ClickModel}; a plan that doesn't reproduce the target is never returned.
 */
public final class ClickPlanner {
    public record Plan(Stack[] target, List<Integer> clicks) {}

    private final Stack[] work;
    private final boolean[] locked;
    private final List<Integer> clicks = new ArrayList<>();
    private final ClickModel model;

    private ClickPlanner(Stack[] slots, boolean[] locked) {
        this.work = slots.clone();
        this.locked = locked;
        this.model = new ClickModel(slots);
    }

    public static Plan plan(Stack[] slots, boolean[] locked) {
        Plan p = tryPlan(slots, locked);
        if (p != null) return p;
        boolean[] pinned = locked.clone();
        for (int i = 0; i < slots.length; i++) if (slots[i] != null && slots[i].key().special) pinned[i] = true;
        p = tryPlan(slots, pinned);
        if (p == null) throw new IllegalStateException("no valid plan");
        return p;
    }

    private static Plan tryPlan(Stack[] slots, boolean[] locked) {
        Stack[] target = SortPlanner.target(slots, locked);
        ClickPlanner cp = new ClickPlanner(slots, locked);
        if (!cp.merge() || !cp.permute(target)) return null;
        cp.fixPartials(target);
        Stack[] result = ClickModel.run(slots, cp.clicks);
        if (!Arrays.equals(result, target)) throw new IllegalStateException("plan does not reach target");
        return new Plan(target, List.copyOf(cp.clicks));
    }

    private void click(int i) {
        model.click(i);
        clicks.add(i);
    }

    private boolean merge() {
        Map<ItemKey, List<Integer>> partials = new LinkedHashMap<>();
        for (int i = 0; i < work.length; i++) {
            Stack s = work[i];
            if (!locked[i] && s != null && s.count() < s.key().maxStack) {
                partials.computeIfAbsent(s.key(), k -> new ArrayList<>()).add(i);
            }
        }
        for (List<Integer> p : partials.values()) {
            int d = 0, s = p.size() - 1;
            while (d < s) {
                int src = p.get(s);
                Stack carried = work[src];
                work[src] = null;
                click(src);
                while (carried != null && d < s) {
                    int dst = p.get(d);
                    int put = Math.min(carried.count(), work[dst].space());
                    work[dst] = work[dst].withCount(work[dst].count() + put);
                    carried = carried.withCount(carried.count() - put);
                    click(dst);
                    if (work[dst].space() == 0) d++;
                }
                if (carried != null) {
                    work[src] = carried;
                    click(src);
                } else {
                    s--;
                }
            }
        }
        return true;
    }

    private boolean permute(Stack[] target) {
        int n = work.length;
        int[] from = new int[n];
        Arrays.fill(from, -1);
        boolean[] used = new boolean[n];
        for (int t = 0; t < n; t++) {
            if (!locked[t] && Objects.equals(keyOf(work[t]), keyOf(target[t]))) {
                from[t] = t;
                used[t] = true;
            }
        }
        Map<ItemKey, List<Integer>> pool = new HashMap<>();
        List<Integer> emptyPool = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            if (locked[s] || used[s]) continue;
            if (work[s] == null) emptyPool.add(s);
            else pool.computeIfAbsent(work[s].key(), k -> new ArrayList<>()).add(s);
        }
        for (int t = 0; t < n; t++) {
            if (locked[t] || from[t] >= 0) continue;
            List<Integer> src = target[t] == null ? emptyPool : pool.get(target[t].key());
            if (src == null || src.isEmpty()) throw new IllegalStateException("no source for slot " + t);
            from[t] = src.remove(src.size() - 1);
        }
        int[] to = new int[n];
        Arrays.fill(to, -1);
        for (int t = 0; t < n; t++) if (from[t] >= 0) to[from[t]] = t;

        boolean[] done = new boolean[n];
        List<List<Integer>> cycles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (locked[i] || done[i] || to[i] == i) continue;
            List<Integer> cyc = new ArrayList<>();
            for (int j = i; !done[j]; j = to[j]) {
                done[j] = true;
                cyc.add(j);
            }
            cycles.add(cyc);
        }
        // Plain cycles first, then bundle cycles that contain an empty slot; after those every empty slot is final,
        // so the remaining bundle cycles can borrow one as a buffer.
        List<List<Integer>> needBuffer = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (List<Integer> cyc : cycles) {
                boolean special = cyc.stream().anyMatch(i -> work[i] != null && work[i].key().special);
                if (special != (pass == 1)) continue;
                int empty = -1;
                for (int k = 0; k < cyc.size(); k++) if (work[cyc.get(k)] == null) empty = k;
                if (special && empty < 0) {
                    needBuffer.add(cyc);
                    continue;
                }
                if (empty >= 0) Collections.rotate(cyc, cyc.size() - 1 - empty);
                if (special) holeMoves(cyc, -1);
                else cursorCycle(cyc);
            }
        }
        if (needBuffer.isEmpty()) return true;
        int buffer = -1;
        for (int i = 0; i < n && buffer < 0; i++) if (!locked[i] && work[i] == null) buffer = i;
        if (buffer < 0) return false;
        for (List<Integer> cyc : needBuffer) holeMoves(cyc, buffer);
        return true;
    }

    /** Content of cyc[i] goes to cyc[i+1]; the last element's content goes to cyc[0]. */
    private void cursorCycle(List<Integer> cyc) {
        Stack carried = null;
        for (int k = 0; k < cyc.size(); k++) {
            int i = cyc.get(k);
            if (carried == null && work[i] == null) continue;
            Stack here = work[i];
            work[i] = carried;
            carried = here;
            click(i);
        }
        if (carried != null) {
            int first = cyc.get(0);
            work[first] = carried;
            click(first);
        }
    }

    /** Only ever places onto empty slots, so bundles never swallow anything. {@code buffer} is -1 when the cycle already ends in an empty slot. */
    private void holeMoves(List<Integer> cyc, int buffer) {
        int k = cyc.size();
        if (buffer >= 0) move(cyc.get(k - 1), buffer);
        for (int j = k - 2; j >= 0; j--) move(cyc.get(j), cyc.get(j + 1));
        if (buffer >= 0) move(buffer, cyc.get(0));
    }

    private void move(int src, int dst) {
        if (work[src] == null) return;
        work[dst] = work[src];
        work[src] = null;
        click(src);
        click(dst);
    }

    private void fixPartials(Stack[] target) {
        Map<ItemKey, Integer> have = new HashMap<>(), want = new HashMap<>();
        for (int i = 0; i < work.length; i++) {
            if (locked[i]) continue;
            if (work[i] != null && work[i].count() < work[i].key().maxStack) have.put(work[i].key(), i);
            if (target[i] != null && target[i].count() < target[i].key().maxStack) want.put(target[i].key(), i);
        }
        for (Map.Entry<ItemKey, Integer> e : want.entrySet()) {
            int x = have.getOrDefault(e.getKey(), -1), y = e.getValue();
            if (x < 0 || x == y) continue;
            Stack full = work[y], part = work[x];
            work[x] = full;
            work[y] = part;
            click(y);
            click(x);
            click(y);
        }
    }

    private static ItemKey keyOf(Stack s) {
        return s == null ? null : s.key();
    }
}
