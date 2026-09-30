package io.github.profetgit.tidypockets.core;

import java.util.List;

/** The dice behind the random palette: which palette slot to place from next. Pure, so it can be tested without a game. */
public final class Dice {
    /** How much each identical neighbour cuts a block's chance: one face neighbour leaves 30 %. */
    static final double CLUMP = 0.3;
    static final double EDGE_SHARE = 0.5, REPEAT_SHARE = 0.5;

    private Dice() {}

    /**
     * One palette slot that can be placed right now. {@code sameFaces} and {@code sameEdges} count the blocks of the
     * same kind among the 6 face and 12 edge neighbours of the target cell; {@code repeat} is set for the slot that
     * placed last.
     */
    public record Option(int slot, int sameFaces, int sameEdges, boolean repeat) {}

    /** Chance weight of an option. Without {@code spread} every slot counts the same. */
    public static double weight(Option o, boolean spread) {
        if (!spread) return 1;
        double score = o.sameFaces + EDGE_SHARE * o.sameEdges + (o.repeat ? REPEAT_SHARE : 0);
        return Math.pow(CLUMP, score);
    }

    /** The slot to place from; {@code u} is a uniform number in [0, 1). Returns -1 for no options. */
    public static int pick(List<Option> options, boolean spread, double u) {
        if (options.isEmpty()) return -1;
        double total = 0;
        for (Option o : options) total += weight(o, spread);
        double at = Math.clamp(u, 0, 1) * total;
        for (Option o : options) {
            at -= weight(o, spread);
            if (at < 0) return o.slot;
        }
        return options.get(options.size() - 1).slot;
    }
}
