package io.github.profetgit.tidypockets.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DiceTest {
    private static Dice.Option opt(int slot, int faces, int edges, boolean repeat) {
        return new Dice.Option(slot, faces, edges, repeat);
    }

    @Test
    void noOptionsNoPick() {
        assertEquals(-1, Dice.pick(List.of(), true, 0.5));
    }

    @Test
    void plainRandomIsEven() {
        List<Dice.Option> o = List.of(opt(0, 4, 0, true), opt(1, 0, 0, false), opt(2, 0, 0, false));
        int[] n = new int[3];
        Random r = new Random(1);
        for (int i = 0; i < 30000; i++) n[Dice.pick(o, false, r.nextDouble())]++;
        for (int c : n) assertTrue(Math.abs(c - 10000) < 500, "even split, got " + c);
    }

    @Test
    void spreadAvoidsNeighbours() {
        // slot 0 touches the target on two faces, slot 1 on none: slot 1 should win most rolls
        List<Dice.Option> o = List.of(opt(0, 2, 0, false), opt(1, 0, 0, false));
        int[] n = new int[2];
        Random r = new Random(2);
        for (int i = 0; i < 20000; i++) n[Dice.pick(o, true, r.nextDouble())]++;
        assertTrue(n[1] > n[0] * 8, "blocks not next to the spot are strongly preferred: " + n[0] + " vs " + n[1]);
        assertTrue(n[0] > 0, "but the neighbour block is still possible");
    }

    @Test
    void spreadWithNothingAroundIsEven() {
        List<Dice.Option> o = List.of(opt(3, 0, 0, false), opt(5, 0, 0, false), opt(7, 0, 0, false));
        int[] n = new int[9];
        Random r = new Random(3);
        for (int i = 0; i < 30000; i++) n[Dice.pick(o, true, r.nextDouble())]++;
        for (int s : new int[] {3, 5, 7}) assertTrue(Math.abs(n[s] - 10000) < 500, "slot " + s + " got " + n[s]);
    }

    @Test
    void repeatIsMildlyDiscouraged() {
        double repeat = Dice.weight(opt(0, 0, 0, true), true), fresh = Dice.weight(opt(1, 0, 0, false), true);
        assertTrue(repeat < fresh && repeat > 0.3 * fresh, "a repeat is less likely, not forbidden");
    }

    @Test
    void edgesCountLessThanFaces() {
        assertTrue(Dice.weight(opt(0, 0, 2, false), true) > Dice.weight(opt(0, 1, 0, false), true) - 1e-9);
        assertTrue(Dice.weight(opt(0, 0, 1, false), true) > Dice.weight(opt(0, 1, 0, false), true));
    }

    @Test
    void edgesOfRangeUStillPick() {
        List<Dice.Option> o = List.of(opt(2, 0, 0, false), opt(4, 0, 0, false));
        assertEquals(2, Dice.pick(o, true, 0));
        assertEquals(4, Dice.pick(o, true, 1.0));
        assertEquals(4, Dice.pick(o, true, 0.999999));
    }

    @Test
    void weightedByRepeatedSlots() {
        // the same block in two slots is twice as likely
        List<Dice.Option> o = List.of(opt(0, 0, 0, false), opt(1, 0, 0, false), opt(2, 0, 0, false));
        assertEquals(0, Dice.pick(o, false, 0.1));
        assertEquals(1, Dice.pick(o, false, 0.5));
        assertEquals(2, Dice.pick(o, false, 0.9));
    }
}
