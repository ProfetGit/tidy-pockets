package io.github.profetgit.tidypockets.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ClickPlannerTest {
    private static ItemKey key(String id, int max, boolean special, long order) {
        return new ItemKey(id, id.hashCode(), max, special, order, id);
    }

    private static final List<ItemKey> KEYS = List.of(
        key("stone", 64, false, 1), key("dirt", 64, false, 2), key("pearl", 16, false, 3),
        key("sword", 1, false, 4), key("bundle_a", 1, true, 5), key("bundle_b", 1, true, 6),
        key("egg", 16, false, 7), key("torch", 64, false, 8), key("potion_x", 1, false, 9),
        key("apple", 64, false, 10));

    private static Stack[] random(Random r, int n, double fill) {
        Stack[] s = new Stack[n];
        for (int i = 0; i < n; i++) {
            if (r.nextDouble() > fill) continue;
            ItemKey k = KEYS.get(r.nextInt(KEYS.size()));
            s[i] = new Stack(k, 1 + r.nextInt(k.maxStack));
        }
        return s;
    }

    private static Map<ItemKey, Integer> totals(Stack[] s) {
        Map<ItemKey, Integer> m = new HashMap<>();
        for (Stack x : s) if (x != null) m.merge(x.key(), x.count(), Integer::sum);
        return m;
    }

    @Test
    void randomInventories() {
        Random r = new Random(1234);
        int worst = 0;
        for (int run = 0; run < 5000; run++) {
            int n = switch (run % 4) { case 0 -> 27; case 1 -> 54; case 2 -> 9; default -> 5; };
            Stack[] slots = random(r, n, r.nextDouble());
            boolean[] locked = new boolean[n];
            if (run % 3 == 0) for (int i = 0; i < n; i++) locked[i] = r.nextDouble() < 0.15;

            ClickPlanner.Plan plan = ClickPlanner.plan(slots, locked);
            Stack[] result = ClickModel.run(slots, plan.clicks());

            assertArrayEquals(plan.target(), result, "run " + run);
            assertEquals(totals(slots), totals(result), "items conserved, run " + run);
            for (int i = 0; i < n; i++) if (locked[i]) assertEquals(slots[i], result[i], "locked slot moved");
            for (int c : plan.clicks()) assertTrue(!locked[c], "clicked a locked slot");
            assertTrue(plan.clicks().size() <= 4 * n + 3 * KEYS.size(), "too many clicks: " + plan.clicks().size());
            worst = Math.max(worst, plan.clicks().size() / n);

            boolean sawEmpty = false;
            for (int i = 0; i < n; i++) {
                if (locked[i]) continue;
                if (result[i] == null) sawEmpty = true;
                else assertTrue(!sawEmpty || isPinnedSpecial(slots, i), "gap before stack, run " + run);
            }
            assertEquals(0, ClickPlanner.plan(result, locked).clicks().size(), "sorted input needs no clicks");
        }
        assertTrue(worst <= 3, "worst clicks per slot " + worst);
    }

    private static boolean isPinnedSpecial(Stack[] before, int i) {
        return before[i] != null && before[i].key().special;
    }

    @Test
    void identicalStacksNeverSwapThroughEachOther() {
        ItemKey stone = KEYS.get(0);
        Stack[] slots = {new Stack(stone, 20), new Stack(stone, 64), new Stack(stone, 64)};
        ClickPlanner.Plan plan = ClickPlanner.plan(slots, new boolean[3]);
        assertArrayEquals(new Stack[] {new Stack(stone, 64), new Stack(stone, 64), new Stack(stone, 20)}, plan.target());
    }

    @Test
    void fullRegionWithBundlesKeepsBundlesInPlace() {
        ItemKey dirt = KEYS.get(1), stone = KEYS.get(0), bundle = KEYS.get(4);
        Stack[] slots = {new Stack(dirt, 64), new Stack(bundle, 1), new Stack(stone, 64)};
        ClickPlanner.Plan plan = ClickPlanner.plan(slots, new boolean[3]);
        assertEquals(new Stack(bundle, 1), plan.target()[1]);
        assertEquals(new Stack(stone, 64), plan.target()[0]);
    }

    @Test
    void bundlesMoveThroughAnEmptySlot() {
        ItemKey dirt = KEYS.get(1), stone = KEYS.get(0), bundle = KEYS.get(4);
        Stack[] slots = {new Stack(bundle, 1), new Stack(dirt, 64), new Stack(stone, 64), null};
        ClickPlanner.Plan plan = ClickPlanner.plan(slots, new boolean[4]);
        assertArrayEquals(new Stack[] {new Stack(stone, 64), new Stack(dirt, 64), new Stack(bundle, 1), null}, plan.target());
    }
}
