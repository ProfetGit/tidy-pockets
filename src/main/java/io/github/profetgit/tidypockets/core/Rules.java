package io.github.profetgit.tidypockets.core;

import java.util.List;

/** Small pure decisions shared by refill and tool protection. */
public final class Rules {
    private Rules() {}

    /** True when doing an action costing {@code cost} durability would leave the tool at or below {@code margin}. */
    public static boolean wouldBreak(int maxDamage, int damage, int cost, int margin) {
        int remaining = maxDamage - damage;
        return remaining - Math.max(1, cost) <= margin;
    }

    /** A refill or replacement candidate from the player's inventory. */
    public record Candidate(int invIndex, boolean exact, boolean sameEnchants, int remaining, int count) {}

    /**
     * Best candidate, or -1. Tools prefer same enchantments, then most durability left; other items prefer an exact
     * match (when {@code exactOnly}, only exact matches count), then the biggest stack. Main inventory wins ties.
     */
    public static int pick(List<Candidate> cands, boolean tool, boolean exactOnly, int minRemaining) {
        Candidate best = null;
        for (Candidate c : cands) {
            if (tool && c.remaining <= minRemaining) continue;
            if (!tool && exactOnly && !c.exact) continue;
            if (best == null || better(c, best, tool)) best = c;
        }
        return best == null ? -1 : best.invIndex;
    }

    private static boolean better(Candidate a, Candidate b, boolean tool) {
        if (tool) {
            if (a.sameEnchants != b.sameEnchants) return a.sameEnchants;
            if (a.remaining != b.remaining) return a.remaining > b.remaining;
        } else {
            if (a.exact != b.exact) return a.exact;
            if (a.count != b.count) return a.count > b.count;
        }
        return isMain(a.invIndex) && !isMain(b.invIndex);
    }

    private static boolean isMain(int invIndex) {
        return invIndex >= 9 && invIndex < 36;
    }
}
