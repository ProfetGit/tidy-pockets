package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.List;
import net.minecraft.world.inventory.Slot;

/** Sorted stacks poof out of their old slots and pop into their new ones in a quick diagonal wave. */
public final class SortPop {
    private static final double STEP_MS = 6, MAX_DELAY_MS = 80;

    private SortPop() {}

    public static void start(List<Slot> slots, boolean[] moved) {
        if (!TidyConfig.get().animSort || !Ease.enabled()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (Slot s : slots) {
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
        }
        for (int i = 0; i < slots.size(); i++) {
            if (!moved[i]) continue;
            Slot s = slots.get(i);
            int col = (s.x - minX) / 18, row = (s.y - minY) / 18;
            SlotAnims.pop(s, Math.min(MAX_DELAY_MS, (row + col) * STEP_MS), true);
        }
    }
}
