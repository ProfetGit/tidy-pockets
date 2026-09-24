package io.github.profetgit.tidypockets.inv;

import io.github.profetgit.tidypockets.config.TidyConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Sends left clicks through vanilla's normal click path, which applies each one to the open menu at once and then
 * tells the server. By default the whole list goes out in the same frame; safe mode drips it out per tick. Clicks in
 * the creative inventory always go at once: they reach the server as creative slot updates, not clicks.
 */
public final class ClickSender {
    private record Pending(int containerId, int slot, int button, ContainerInput input) {}

    private static final Deque<Pending> QUEUE = new ArrayDeque<>();

    private ClickSender() {}

    public static void clicks(AbstractContainerMenu menu, List<Slot> slots, List<Integer> regionIndices) {
        for (int i : regionIndices) send(menu, slots.get(i), 0, ContainerInput.PICKUP);
    }

    public static void send(AbstractContainerMenu menu, Slot slot, int button, ContainerInput input) {
        if (Creative.isMenu(menu)) {
            log(menu.slots.indexOf(slot), button, input, "creative");
            Creative.click(menu, slot, button, input);
            return;
        }
        Pending p = new Pending(menu.containerId, slot.index, button, input);
        if (TidyConfig.get().safeMode || !QUEUE.isEmpty()) QUEUE.add(p);
        else fire(p);
    }

    public static boolean busy() {
        return !QUEUE.isEmpty();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            QUEUE.clear();
            return;
        }
        for (int n = 0; n < TidyConfig.get().safeModeClicksPerTick && !QUEUE.isEmpty(); n++) {
            Pending p = QUEUE.poll();
            if (p.containerId != mc.player.containerMenu.containerId) {
                QUEUE.clear();
                return;
            }
            fire(p);
        }
    }

    private static void fire(Pending p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        log(p.slot, p.button, p.input, "menu " + p.containerId);
        mc.gameMode.handleContainerInput(p.containerId, p.slot, p.button, p.input, mc.player);
    }

    private static void log(int slot, int button, ContainerInput input, String menu) {
        if (io.github.profetgit.tidypockets.selftest.SelfTest.active()) {
            io.github.profetgit.tidypockets.TidyPockets.LOG.info("[click] {} slot {} button {} {} from {}", menu, slot, button, input,
                StackWalker.getInstance().walk(f -> f.dropWhile(x -> x.getClassName().equals(ClickSender.class.getName())).limit(3).map(x -> x.getClassName().replaceAll(".*\\.", "") + "." + x.getMethodName()).toList()));
        }
    }
}
