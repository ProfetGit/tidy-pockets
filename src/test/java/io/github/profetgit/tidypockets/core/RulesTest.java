package io.github.profetgit.tidypockets.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RulesTest {
    @Test
    void wouldBreak() {
        assertTrue(Rules.wouldBreak(250, 249, 1, 0), "last point of durability");
        assertFalse(Rules.wouldBreak(250, 248, 1, 0), "two left, one use");
        assertTrue(Rules.wouldBreak(250, 248, 2, 0), "sword mining costs 2");
        assertTrue(Rules.wouldBreak(250, 245, 1, 5), "margin keeps five");
        assertFalse(Rules.wouldBreak(250, 0, 1, 0));
    }

    @Test
    void toolsPreferSameEnchantsThenDurability() {
        List<Rules.Candidate> c = List.of(
            new Rules.Candidate(12, false, false, 900, 1),
            new Rules.Candidate(20, false, true, 100, 1),
            new Rules.Candidate(3, false, true, 400, 1));
        assertEquals(3, Rules.pick(c, true, false, 0));
        assertEquals(-1, Rules.pick(List.of(new Rules.Candidate(9, true, true, 1, 1)), true, false, 1), "worn spare is skipped");
    }

    @Test
    void blocksPreferExactThenBiggestThenMainInventory() {
        List<Rules.Candidate> c = List.of(
            new Rules.Candidate(2, true, true, Integer.MAX_VALUE, 40),
            new Rules.Candidate(15, true, true, Integer.MAX_VALUE, 40),
            new Rules.Candidate(30, false, true, Integer.MAX_VALUE, 64));
        assertEquals(15, Rules.pick(c, false, false, 0));
        assertEquals(15, Rules.pick(c, false, true, 0));
        assertEquals(-1, Rules.pick(List.of(new Rules.Candidate(30, false, true, 0, 64)), false, true, 0));
    }
}
