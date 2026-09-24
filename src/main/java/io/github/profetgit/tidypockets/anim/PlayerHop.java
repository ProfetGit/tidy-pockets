package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** The player figure in the inventory hops when armour or the offhand item changes. */
public final class PlayerHop {
    private static final EquipmentSlot[] WATCHED = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
        EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
    static final double HOP_MS = 180;
    private static List<ItemStack> last;
    private static int lastEntity = -1;
    private static double start = -1e9;

    private PlayerHop() {}

    /** Pixels to lift the figure this frame. */
    public static float offset(LivingEntity e) {
        List<ItemStack> now = new ArrayList<>();
        for (EquipmentSlot s : WATCHED) now.add(e.getItemBySlot(s));
        boolean changed = false;
        if (last != null && e.getId() == lastEntity) {
            for (int i = 0; i < now.size(); i++) if (!ItemStack.matches(now.get(i), last.get(i))) changed = true;
        }
        if (changed && TidyConfig.get().animPlayerHop && Ease.enabled()) start = Ease.now();
        last = new ArrayList<>();
        for (ItemStack s : now) last.add(s.copy());
        lastEntity = e.getId();
        double t = Ease.progress(start, HOP_MS);
        return t >= 1 ? 0 : (float) (4 * Math.sin(Math.PI * t));
    }

    public static void reset() {
        last = null;
    }
}
