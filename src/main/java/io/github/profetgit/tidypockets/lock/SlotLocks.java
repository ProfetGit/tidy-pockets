package io.github.profetgit.tidypockets.lock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.profetgit.tidypockets.TidyPockets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Locked player-inventory slots (by inventory index), kept per world or server. A locked hotbar slot also remembers
 * the item it held when it was locked.
 */
public final class SlotLocks {
    private static final class WorldLocks {
        TreeSet<Integer> locked = new TreeSet<>();
        TreeMap<Integer, String> remember = new TreeMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, WorldLocks> all;
    private static String currentKey;

    private SlotLocks() {}

    private static Path file() {
        return TidyPockets.platform().configDir().resolve("tidypockets-locks.json");
    }

    private static WorldLocks current() {
        if (all == null) {
            all = new HashMap<>();
            try {
                if (Files.isRegularFile(file())) {
                    Map<String, WorldLocks> m = GSON.fromJson(Files.readString(file()),
                        new TypeToken<Map<String, WorldLocks>>() {}.getType());
                    if (m != null) all.putAll(m);
                }
            } catch (Exception e) {
                TidyPockets.LOG.warn("Could not read slot locks: {}", e.toString());
            }
        }
        currentKey = worldKey();
        return all.computeIfAbsent(currentKey, k -> new WorldLocks());
    }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) return "world:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip;
        return "unknown";
    }

    public static boolean isLocked(int invIndex) {
        return current().locked.contains(invIndex);
    }

    /** Locks a slot holding an item, or unlocks a locked one. Empty unlocked slots can't be locked. */
    public static void toggle(int invIndex, ItemStack held) {
        WorldLocks w = current();
        if (!w.locked.contains(invIndex) && held.isEmpty()) return;
        if (!w.locked.remove(invIndex)) {
            w.locked.add(invIndex);
            if (invIndex < Inventory.SELECTION_SIZE && !held.isEmpty()) {
                w.remember.put(invIndex, BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            }
        } else {
            w.remember.remove(invIndex);
        }
        save();
    }

    private static final java.util.Set<Integer> SEEN_FULL = new java.util.HashSet<>();
    private static final java.util.Map<Integer, Integer> EMPTY_TICKS = new java.util.HashMap<>();
    private static String seenFor;

    /**
     * A lock goes away once its item has left the slot for good (used up, crafted away, cleared by a command), except
     * on hotbar slots that remember an item: those stay reserved so a sort can put the item back. Slots count as "left"
     * only after they were seen holding something this session, so locks survive the empty inventory while joining.
     */
    public static void tick(net.minecraft.world.entity.player.Player p) {
        if (p == null) {
            SEEN_FULL.clear();
            EMPTY_TICKS.clear();
            seenFor = null;
            return;
        }
        WorldLocks w = current();
        if (!currentKey.equals(seenFor)) {
            SEEN_FULL.clear();
            EMPTY_TICKS.clear();
            seenFor = currentKey;
        }
        boolean changed = false;
        for (Integer i : java.util.List.copyOf(w.locked)) {
            if (!p.getInventory().getItem(i).isEmpty()) {
                SEEN_FULL.add(i);
                EMPTY_TICKS.remove(i);
                continue;
            }
            if (!SEEN_FULL.contains(i) || w.remember.containsKey(i)) continue;
            if (EMPTY_TICKS.merge(i, 1, Integer::sum) >= 10) {
                w.locked.remove(i);
                EMPTY_TICKS.remove(i);
                SEEN_FULL.remove(i);
                changed = true;
            }
        }
        if (changed) save();
    }

    /** The item a locked hotbar slot should hold, or null. */
    public static Item remembered(int invIndex) {
        String id = current().remember.get(invIndex);
        if (id == null) return null;
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(all));
        } catch (Exception e) {
            TidyPockets.LOG.warn("Could not save slot locks: {}", e.toString());
        }
    }
}
