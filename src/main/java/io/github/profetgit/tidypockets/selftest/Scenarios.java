package io.github.profetgit.tidypockets.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.core.ClickPlanner;
import io.github.profetgit.tidypockets.core.Stack;
import io.github.profetgit.tidypockets.inv.Creative;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.inv.StackKeys;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.palette.Palette;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import io.github.profetgit.tidypockets.mixin.CreativeScreenInvoker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class Scenarios {
    private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
    private static final int MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;

    private static Stack[] expected;
    private static Map<String, Integer> totalsBefore;
    private static List<ItemStack> serverView;
    private static List<ItemStack> lockedBefore;

    private Scenarios() {}

    static Script build() {
        Script s = new Script();
        createWorld(s);
        sortChest(s, "sort-chest", 11, false, 0.7);
        sortChest(s, "sort-double-chest", 12, true, 0.8);
        sortChest(s, "sort-full-chest-bundles", 13, false, 1.0);
        sortPlayer(s);
        if (io.github.profetgit.tidypockets.TidyPockets.platform().isModLoaded("controlify")) sortChest(s, "controlify-sort-chest", 14, false, 0.8, true);
        refill(s);
        protect(s);
        palette(s);
        mouse(s);
        tools(s);
        locks(s);
        anims(s);
        pictures(s);
        creative(s);
        return s;
    }

    /** With other inventory and recipe mods installed: sorting still works, overlaps are switched off, overlays stay put. */
    static Script buildCompat() {
        Script s = new Script();
        createWorld(s);
        var platform = io.github.profetgit.tidypockets.TidyPockets.platform();
        s.check("compat: mouse shortcuts yield to Mouse Tweaks",
                mc -> io.github.profetgit.tidypockets.config.Conflicts.mouse == platform.isModLoaded("mousetweaks") ? null : "flag wrong")
            .check("compat: item flight yields to Smooth Swapping",
                mc -> io.github.profetgit.tidypockets.config.Conflicts.fly == platform.isModLoaded("smoothswapping") ? null : "flag wrong")
            .waitTicks(100);
        sortChest(s, "compat-sort", 11, false, 0.7);
        s.capture("compat-pop", 20);
        openChest(s, new BlockPos(-2, 0, 2), c -> c.setItem(0, new ItemStack(Items.TORCH, 8)));
        s.waitTicks(10).run(mc -> mc.player.closeContainer()).waitTicks(5);
        return s;
    }

    /** Against a real dedicated server over a socket: no integrated-server shortcuts, only commands and clicks. */
    static Script buildRemote() {
        Script s = new Script();
        remoteSort(s, "remote-sort", new BlockPos(2, 0, 0), false);
        remoteSort(s, "remote-sort-safe-mode", new BlockPos(-2, 0, 0), true);
        return s;
    }

    private static final String[] REMOTE_ITEMS = {"minecraft:dirt", "minecraft:stone", "minecraft:torch", "minecraft:bread",
        "minecraft:oak_log", "minecraft:bundle", "minecraft:diamond_pickaxe[damage=100]", "minecraft:ender_pearl", "minecraft:cobblestone"};

    private static void remoteSort(Script s, String name, BlockPos offset, boolean safe) {
        BlockPos[] pos = new BlockPos[1];
        s.run(mc -> {
                pos[0] = mc.player.blockPosition().offset(offset);
                BlockPos p = pos[0];
                String at = p.getX() + " " + p.getY() + " " + p.getZ();
                mc.player.connection.sendCommand("setblock " + at + " minecraft:chest replace");
                RandomSource r = RandomSource.create(offset.getX() + 99);
                for (int i = 0; i < 27; i++) {
                    if (r.nextFloat() < 0.3f) continue;
                    String item = REMOTE_ITEMS[r.nextInt(REMOTE_ITEMS.length)];
                    int max = item.contains("bundle") || item.contains("pickaxe") ? 1 : item.contains("pearl") ? 16 : 64;
                    mc.player.connection.sendCommand("item replace block " + at + " container." + i + " with " + item + " " + (1 + r.nextInt(max)));
                }
            })
            .waitTicks(20)
            .run(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos[0]), Direction.UP, pos[0], false)))
            .until(name + " chest", 200, mc -> mc.gui.screen() instanceof ContainerScreen && !chestSlots(mc).isEmpty())
            .waitTicks(10)
            .run(mc -> {
                TidyConfig.get().safeMode = safe;
                List<Slot> slots = chestSlots(mc);
                expected = ClickPlanner.plan(StackKeys.read(slots), new boolean[slots.size()]).target();
                totalsBefore = totals(slots);
                contentPacketsReset();
            })
            .run(mc -> click(mc, chestSlots(mc).get(4), MIDDLE))
            .waitTicks(safe ? 40 : 20)
            .check(name + " layout", mc -> Arrays.equals(StackKeys.read(chestSlots(mc)), expected) ? null : "layout differs from plan")
            .check(name + " no resync", mc -> SelfTest.contentPackets == 0 ? null : SelfTest.contentPackets + " full resyncs")
            .run(mc -> {
                TidyConfig.get().safeMode = false;
                mc.player.closeContainer();
            })
            .waitTicks(10)
            .run(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos[0]), Direction.UP, pos[0], false)))
            .until(name + " reopened", 200, mc -> mc.gui.screen() instanceof ContainerScreen && !chestSlots(mc).isEmpty())
            .waitTicks(10)
            .check(name + " server kept the sorted layout", mc -> Arrays.equals(StackKeys.read(chestSlots(mc)), expected)
                && totals(chestSlots(mc)).equals(totalsBefore) ? null : "server copy differs after reopening")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(10);
    }

    private static void createWorld(Script s) {
        s.run(mc -> io.github.profetgit.tidypockets.Compat.createWorld(mc, "tptest-" + System.currentTimeMillis()))
            .until("world to load", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null)
            .waitTicks(60);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static List<ItemStack> pool() {
        List<ItemStack> p = new ArrayList<>();
        p.add(new ItemStack(Items.STONE, 64));
        p.add(new ItemStack(Items.COBBLESTONE, 64));
        p.add(new ItemStack(Items.DIRT, 64));
        p.add(new ItemStack(Items.OAK_LOG, 64));
        p.add(new ItemStack(Items.TORCH, 64));
        p.add(new ItemStack(Items.REDSTONE, 64));
        p.add(new ItemStack(Items.BREAD, 64));
        p.add(new ItemStack(Items.ENDER_PEARL, 16));
        p.add(new ItemStack(Items.EGG, 16));
        p.add(new ItemStack(Items.BUNDLE, 1));
        p.add(new ItemStack(Items.IRON_SWORD, 1));
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE, 1);
        pick.set(DataComponents.DAMAGE, 100);
        p.add(pick);
        ItemStack named = new ItemStack(Items.COBBLESTONE, 64);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special Cobble"));
        p.add(named);
        p.add(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
        p.add(PotionContents.createItemStack(Items.POTION, Potions.HEALING));
        return p;
    }

    private static void fill(Container c, int from, int to, long seed, double fill, boolean bundleHeavy) {
        RandomSource r = RandomSource.create(seed);
        List<ItemStack> pool = pool();
        for (int i = from; i < to; i++) {
            if (r.nextDouble() > fill) {
                c.setItem(i, ItemStack.EMPTY);
                continue;
            }
            ItemStack t = bundleHeavy && r.nextInt(4) == 0 ? new ItemStack(Items.BUNDLE) : pool.get(r.nextInt(pool.size()));
            c.setItem(i, t.copyWithCount(1 + r.nextInt(t.getMaxStackSize())));
        }
    }

    private static void click(Minecraft mc, Slot slot, int button) {
        AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) scr;
        double x = a.tidypockets$left() + slot.x + 8, y = a.tidypockets$top() + slot.y + 8;
        io.github.profetgit.tidypockets.input.MouseEvt e = new io.github.profetgit.tidypockets.input.MouseEvt(x, y, button, 0);
        io.github.profetgit.tidypockets.Compat.mouseClicked(scr, e, false);
        io.github.profetgit.tidypockets.Compat.mouseReleased(scr, e);
    }

    private static Map<String, Integer> totals(List<Slot> slots) {
        Map<String, Integer> m = new HashMap<>();
        for (Slot s : slots) {
            ItemStack st = s.getItem();
            if (!st.isEmpty()) m.merge(st.getItem() + "" + st.getComponentsPatch(), st.getCount(), Integer::sum);
        }
        return m;
    }

    private static String compareServer(List<Slot> clientSlots, List<ItemStack> server) {
        if (server == null || server.size() != clientSlots.size()) return "server view has " + (server == null ? 0 : server.size()) + " slots";
        for (int i = 0; i < server.size(); i++) {
            if (!ItemStack.matches(server.get(i), clientSlots.get(i).getItem())) {
                return "slot " + i + ": client " + clientSlots.get(i).getItem() + " server " + server.get(i);
            }
        }
        return null;
    }

    private static List<Slot> chestSlots(Minecraft mc) {
        AbstractContainerMenu menu = mc.player.containerMenu;
        return Inv.containerSlots(menu, mc.player);
    }

    private static String pad() {
        return io.github.profetgit.tidypockets.compat.ControlifyCompat.status();
    }

    /** Moves the cursor with the d-pad (Controlify snaps it slot to slot) until it rests on a slot matching {@code target}. */
    private static java.util.function.Predicate<Minecraft> padGoTo(java.util.function.Predicate<Slot> target) {
        int[] n = {0};
        return mc -> {
            if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c)) return false;
            Slot h = ((io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor) c).tidypockets$hoveredSlot();
            if (h != null && target.test(h)) return true;
            if (n[0]++ % 8 != 0) return false;
            Slot goal = null;
            for (Slot sl : c.getMenu().slots) if (target.test(sl)) { goal = sl; break; }
            if (goal == null) throw new IllegalStateException("no slot matches the pad target");
            int dx = h == null ? 0 : goal.x - h.x, dy = h == null ? 1 : goal.y - h.y;
            String hat = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "hat 1 0" : "hat -1 0") : (dy > 0 ? "hat 0 -1" : "hat 0 1");
            SelfTest.pad(hat);
            SelfTest.pad("wait 100");
            SelfTest.pad("hat 0 0");
            return false;
        };
    }

    static Script buildPad() {
        Script s = new Script();
        createWorld(s);
        s.until("controller connected", 200, mc -> io.github.profetgit.tidypockets.compat.ControlifyCompat.controllerReady())
            .run(mc -> {
                SelfTest.record("pad controller connected", true, pad());
                java.util.List<String> ids = io.github.profetgit.tidypockets.compat.ControlifyCompat.bindingIds();
                SelfTest.record("controlify lists only our 7 bindings, no automatic duplicates", ids.size() == 7, ids.toString());
                for (var key : io.github.profetgit.tidypockets.input.Keys.ALL) {
                    if (key == io.github.profetgit.tidypockets.input.Keys.SORT || key == io.github.profetgit.tidypockets.input.Keys.LOCK
                        || key == io.github.profetgit.tidypockets.input.Keys.PALETTE || key == io.github.profetgit.tidypockets.input.Keys.SEARCH
                        || key == io.github.profetgit.tidypockets.input.Keys.DEPOSIT || key == io.github.profetgit.tidypockets.input.Keys.RESTOCK) {
                        java.util.List<String> c = io.github.profetgit.tidypockets.compat.ControlifyCompat.correlated(key);
                        SelfTest.record("key " + key.getName() + " maps to one binding", c.size() == 1 && c.getFirst().startsWith("tidypockets:"), c.toString());
                    }
                }
                for (String[] b : new String[][] {{"lock", "right_shoulder"}, {"palette", "left_shoulder"}, {"search", "guide"},
                    {"deposit", "start"}, {"restock", "left_stick"}}) {
                    boolean ok = io.github.profetgit.tidypockets.compat.ControlifyCompat.bindForTest(b[0], "controlify:button/" + b[1]);
                    SelfTest.record("pad bound " + b[0], ok, b[1]);
                }
            });
        padSortChest(s);
        padLockAndPalette(s);
        padTools(s);
        return s;
    }

    /** The default bind: a right-stick click sorts the container under the cursor. */
    private static void padSortChest(Script s) {
        String name = "pad-sort-chest";
        openChest(s, new BlockPos(2, 0, 2), c -> fill(c, 0, c.getContainerSize(), 14, 0.8, false));
        s.waitTicks(20)
            .capture(name + "-clip", 480)
            .waitTicks(30)
            .run(mc -> {
                List<Slot> slots = chestSlots(mc);
                expected = ClickPlanner.plan(StackKeys.read(slots), new boolean[slots.size()]).target();
                totalsBefore = totals(slots);
                SelfTest.pad("axis lx 0.6");
            })
            .waitTicks(10)
            .run(mc -> SelfTest.pad("axis lx 0"))
            .until("pad cursor on a chest slot", 400, padGoTo(slot -> slot.container != Minecraft.getInstance().player.getInventory() && slot.index == 4))
            .waitTicks(30)
            .run(mc -> SelfTest.pad("tap rs 150"))
            .waitTicks(75)
            .check(name + " layout", mc -> {
                Stack[] after = StackKeys.read(chestSlots(mc));
                return Arrays.equals(after, expected) ? null : "layout differs from plan: " + pad();
            })
            .check(name + " items conserved", mc -> totals(chestSlots(mc)).equals(totalsBefore) ? null : "totals changed")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(10);
    }

    /** Lock and Palette bound to shoulder buttons: pressed over a player slot. */
    private static void padLockAndPalette(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(12, new ItemStack(Items.EGG, 7));
            inv.setItem(0, new ItemStack(Items.DIRT, 20));
            inv.setItem(1, new ItemStack(Items.STONE, 20));
        });
        openChest(s, new BlockPos(-2, 0, 2), c -> c.setItem(0, new ItemStack(Items.TORCH, 8)));
        s.waitTicks(10)
            .capture("pad-lock-clip", 600)
            .waitTicks(30)
            .run(mc -> SelfTest.pad("axis lx 0.6"))
            .waitTicks(10)
            .run(mc -> SelfTest.pad("axis lx 0"))
            .until("pad cursor on the egg", 600, padGoTo(sl -> sl.getItem().is(Items.EGG)))
            .waitTicks(30)
            .run(mc -> SelfTest.pad("tap rb 150"))
            .waitTicks(40)
            .check("pad lock locks the slot under the cursor", mc -> SlotLocks.isLocked(12) ? null : "slot 12 not locked: " + pad())
            .run(mc -> SelfTest.pad("tap rb 150"))
            .waitTicks(40)
            .check("pad lock unlocks it again", mc -> !SlotLocks.isLocked(12) ? null : "still locked: " + pad())
            .until("pad cursor on the dirt", 600, padGoTo(sl -> sl.getItem().is(Items.DIRT)))
            .waitTicks(30)
            .run(mc -> SelfTest.pad("tap lb 150"))
            .waitTicks(60)
            .check("pad palette adds the hotbar block", mc -> io.github.profetgit.tidypockets.palette.Palette.has(0) ? null
                : "palette " + io.github.profetgit.tidypockets.palette.Palette.slots() + " " + pad())
            .run(mc -> {
                io.github.profetgit.tidypockets.palette.Palette.clear();
                mc.player.closeContainer();
            })
            .waitTicks(10);
    }

    /** Search, Deposit and Restock on bound buttons. */
    private static void padTools(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(10, new ItemStack(Items.STONE, 20));
            inv.setItem(11, new ItemStack(Items.DIRT, 5));
            inv.setItem(0, new ItemStack(Items.TORCH, 10));
        });
        openChest(s, new BlockPos(-2, 0, -2), c -> {
            c.setItem(0, new ItemStack(Items.STONE, 1));
            c.setItem(1, new ItemStack(Items.DIRT, 1));
            c.setItem(6, new ItemStack(Items.TORCH, 64));
        });
        s.run(mc -> SelfTest.pad("tap start 150"))
            .waitTicks(10)
            .check("pad deposit moves the matching stacks", mc -> {
                Inventory inv = mc.player.getInventory();
                return inv.getItem(10).isEmpty() && inv.getItem(11).isEmpty() ? null : dump(mc) + " " + pad();
            })
            .run(mc -> SelfTest.pad("tap ls 150"))
            .waitTicks(10)
            .check("pad restock tops the hotbar up", mc -> mc.player.getInventory().getItem(0).getCount() == 64 ? null : dump(mc) + " " + pad())
            .run(mc -> SelfTest.pad("tap guide 150"))
            .waitTicks(10)
            .check("pad search opens the search box", mc -> io.github.profetgit.tidypockets.tools.ContainerTools.searchOpen(
                (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.gui.screen()) ? null : "search closed: " + pad())
            .run(mc -> mc.player.closeContainer())
            .waitTicks(10);
    }

    private static void sortChest(Script s, String name, long seed, boolean dbl, double fill) {
        sortChest(s, name, seed, dbl, fill, false);
    }

    /** {@code viaController}: the sort comes from Controlify's Sort binding instead of a middle click. */
    private static void sortChest(Script s, String name, long seed, boolean dbl, double fill, boolean viaController) {
        BlockPos[] pos = new BlockPos[1];
        s.server(server -> {
                ServerPlayer p = player(server);
                ServerLevel level = server.overworld();
                BlockPos base = p.blockPosition().offset(2, 0, (int) (seed % 5) * 2 - 4);
                pos[0] = base;
                BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
                if (dbl) {
                    level.setBlock(base, left.setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
                    level.setBlock(base.east(), left.setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
                } else {
                    level.setBlock(base, left, 3);
                }
                BlockState st = level.getBlockState(base);
                Container c = ChestBlock.getContainer((ChestBlock) st.getBlock(), st, level, base, true);
                fill(c, 0, c.getContainerSize(), seed, fill, name.contains("bundles"));
                p.openMenu(st.getMenuProvider(level, base));
            })
            .until(name + " screen", 200, mc -> mc.gui.screen() instanceof ContainerScreen && !chestSlots(mc).isEmpty())
            .waitTicks(10)
            .run(mc -> {
                List<Slot> slots = chestSlots(mc);
                expected = ClickPlanner.plan(StackKeys.read(slots), new boolean[slots.size()]).target();
                totalsBefore = totals(slots);
                contentPacketsReset();
            })
            .waitTicks(viaController ? 30 : 0)
            .capture(viaController ? name + "-before" : name, viaController ? 2 : 24)
            .waitTicks(viaController ? 4 : 0)
            .run(mc -> {
                if (viaController) {
                    boolean ok = io.github.profetgit.tidypockets.compat.ControlifyCompat.press("sort",
                        (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.gui.screen(), chestSlots(mc).get(4));
                    SelfTest.record("controlify sort binding acted", ok, "");
                } else click(mc, chestSlots(mc).get(4), MIDDLE);
            })
            .waitTicks(10)
            .capture(name + "-after", 2)
            .server(server -> {
                BlockState st = server.overworld().getBlockState(pos[0]);
                Container c = ChestBlock.getContainer((ChestBlock) st.getBlock(), st, server.overworld(), pos[0], true);
                List<ItemStack> view = new ArrayList<>();
                for (int i = 0; i < c.getContainerSize(); i++) view.add(c.getItem(i).copy());
                serverView = view;
            })
            .check(name + " layout", mc -> {
                Stack[] after = StackKeys.read(chestSlots(mc));
                return Arrays.equals(after, expected) ? null : "layout differs from plan: " + Arrays.toString(after);
            })
            .check(name + " items conserved", mc -> totals(chestSlots(mc)).equals(totalsBefore) ? null : "totals changed")
            .check(name + " server agrees", mc -> compareServer(chestSlots(mc), serverView))
            .check(name + " no resync", mc -> SelfTest.contentPackets == 0 ? null : SelfTest.contentPackets + " full resyncs")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(10);
    }

    private static void contentPacketsReset() {
        SelfTest.contentPackets = 0;
    }

    private static List<Slot> mainSlots(Minecraft mc) {
        return Inv.playerSlots(mc.player.containerMenu, mc.player, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE);
    }

    private static List<Slot> hotbar(Minecraft mc) {
        return Inv.playerSlots(mc.player.containerMenu, mc.player, 0, Inventory.SELECTION_SIZE);
    }

    private static void sortPlayer(Script s) {
        int[] lockedMain = {12, 20};
        s.server(server -> {
                ServerPlayer p = player(server);
                Inventory inv = p.getInventory();
                inv.clearContent();
                fill(inv, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE, 21, 0.75, false);
                inv.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
                inv.setItem(12, new ItemStack(Items.EGG, 7));
                inv.setItem(20, new ItemStack(Items.REDSTONE, 9));
                inv.setItem(1, new ItemStack(Items.TORCH, 30));
                inv.setItem(2, new ItemStack(Items.BREAD, 5));
            })
            .waitTicks(10)
            .run(mc -> {
                for (int i : lockedMain) if (!SlotLocks.isLocked(i)) SlotLocks.toggle(i, mc.player.getInventory().getItem(i));
                if (!SlotLocks.isLocked(0)) SlotLocks.toggle(0, mc.player.getInventory().getItem(0));
            })
            .server(server -> {
                Inventory inv = player(server).getInventory();
                inv.setItem(30, inv.getItem(0).copy());
                inv.setItem(0, ItemStack.EMPTY);
            })
            .waitTicks(10)
            .run(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)))
            .until("inventory screen", 100, mc -> mc.gui.screen() instanceof InventoryScreen)
            .waitTicks(5)
            .run(mc -> {
                lockedBefore = new ArrayList<>();
                for (int i : lockedMain) lockedBefore.add(mc.player.getInventory().getItem(i).copy());
                for (int i = 1; i < Inventory.SELECTION_SIZE; i++) lockedBefore.add(mc.player.getInventory().getItem(i).copy());
                contentPacketsReset();
            })
            .capture("sort-player", 24)
            .run(mc -> click(mc, mainSlots(mc).get(5), MIDDLE))
            .waitTicks(10)
            .check("sort-player locked slots kept", mc -> {
                List<ItemStack> now = new ArrayList<>();
                for (int i : lockedMain) now.add(mc.player.getInventory().getItem(i));
                for (int i = 1; i < Inventory.SELECTION_SIZE; i++) now.add(mc.player.getInventory().getItem(i));
                for (int i = 0; i < now.size(); i++) {
                    if (!ItemStack.matches(now.get(i), lockedBefore.get(i))) return "slot changed: " + lockedBefore.get(i) + " -> " + now.get(i);
                }
                return null;
            })
            .check("sort-player remembered hotbar item pulled back",
                mc -> mc.player.getInventory().getItem(0).is(Items.DIAMOND_PICKAXE) ? null : "hotbar 0 = " + mc.player.getInventory().getItem(0))
            .check("sort-player main sorted", mc -> {
                List<Slot> main = mainSlots(mc);
                boolean[] locked = new boolean[main.size()];
                for (int i = 0; i < locked.length; i++) locked[i] = SlotLocks.isLocked(main.get(i).getContainerSlot());
                Stack[] now = StackKeys.read(main);
                return ClickPlanner.plan(now, locked).clicks().isEmpty() ? null : "main inventory is not in sorted order";
            })
            .check("sort-player no resync", mc -> SelfTest.contentPackets == 0 ? null : SelfTest.contentPackets + " full resyncs")
            .run(mc -> {
                for (int i : lockedMain) SlotLocks.toggle(i, ItemStack.EMPTY);
                SlotLocks.toggle(0, ItemStack.EMPTY);
                mc.player.closeContainer();
            })
            .waitTicks(5);
    }

    private static BlockPos ground;

    private static void select(Script s, int slot) {
        s.run(mc -> mc.player.getInventory().setSelectedSlot(slot)).waitTicks(3);
    }

    private static String inv(Minecraft mc, int i, net.minecraft.world.item.Item item, int count) {
        ItemStack st = mc.player.getInventory().getItem(i);
        if (item == null) return st.isEmpty() ? null : "slot " + i + " should be empty, has " + st;
        return st.is(item) && (count < 0 || st.getCount() == count) ? null : "slot " + i + " = " + st + ", want " + item + " x" + count;
    }

    private static void resetInventory(Script s, java.util.function.Consumer<Inventory> setup) {
        s.server(server -> {
            ServerPlayer p = player(server);
            p.getInventory().clearContent();
            p.containerMenu.setCarried(ItemStack.EMPTY);
            Container grid = p.inventoryMenu.getCraftSlots();
            for (int i = 0; i < grid.getContainerSize(); i++) grid.setItem(i, ItemStack.EMPTY);
            setup.accept(p.getInventory());
            ground = p.blockPosition().below();
            ServerLevel level = server.overworld();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    level.setBlock(ground.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(ground.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(ground.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // clearing the area breaks chests left by earlier scenarios; their contents land by the player's feet
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(ground).inflate(16))) item.discard();
        }).waitTicks(10);
    }

    private static BlockHitResult topOf(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
    }

    private static void markSelected(Script s) {
        s.run(mc -> net.minecraft.client.KeyMapping.click(((io.github.profetgit.tidypockets.mixin.KeyMappingAccessor) io.github.profetgit.tidypockets.input.Keys.PALETTE).tidypockets$key()))
            .waitTicks(3);
    }

    private static final BlockPos[] chestAt = new BlockPos[1];

    /** Share of neighbouring cell pairs (x, z and up) in the 7x3x3 test grid that hold the same block. */
    private static String sameNeighbours(Minecraft mc, double[] out) {
        int same = 0, pairs = 0, placed = 0;
        java.util.Map<Block, Integer> kinds = new java.util.HashMap<>();
        for (int layer = 1; layer <= 3; layer++) {
            for (int dz = 1; dz <= 3; dz++) {
                for (int dx = -3; dx <= 3; dx++) {
                    Block b = mc.level.getBlockState(ground.offset(dx, layer, dz)).getBlock();
                    if (b == Blocks.AIR) continue;
                    placed++;
                    kinds.merge(b, 1, Integer::sum);
                    int[][] next = {{1, 0, 0}, {0, 0, 1}, {0, 1, 0}};
                    for (int[] d : next) {
                        int x = dx + d[0], y = layer + d[1], z = dz + d[2];
                        if (x > 3 || z > 3 || y > 3) continue;
                        pairs++;
                        if (mc.level.getBlockState(ground.offset(x, y, z)).getBlock() == b) same++;
                    }
                }
            }
        }
        out[0] = pairs == 0 ? 1 : (double) same / pairs;
        out[1] = placed;
        out[2] = kinds.size();
        for (int n : kinds.values()) out[3] = Math.max(out[3], n);
        return null;
    }

    private static void clearGrid(Script s) {
        s.server(server -> {
            ServerLevel level = server.overworld();
            for (int layer = 1; layer <= 3; layer++) {
                for (int dz = 1; dz <= 3; dz++) {
                    for (int dx = -3; dx <= 3; dx++) level.setBlock(ground.offset(dx, layer, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }).waitTicks(8);
    }

    private static final StringBuilder gridLog = new StringBuilder();

    private static void placeGrid(Minecraft mc) {
        gridLog.setLength(0);
        for (int layer = 0; layer < 3; layer++) {
            for (int dz = 1; dz <= 3; dz++) {
                for (int dx = -3; dx <= 3; dx++) {
                    if (dx == 0) continue; // the player stands there
                    var r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(ground.offset(dx, layer, dz)));
                    if (!(r instanceof net.minecraft.world.InteractionResult.Success)) {
                        gridLog.append(" [").append(layer).append(',').append(dx).append(',').append(dz).append(' ').append(r).append(']');
                    }
                }
            }
        }
    }

    private static void palette(Script s) {
        double[] spread = new double[4], plain = new double[4];
        int[] serverSel = {-1};
        resetInventory(s, inv -> {
            inv.setItem(0, new ItemStack(Items.STONE, 64));
            inv.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
            inv.setItem(2, new ItemStack(Items.DIRT, 64));
            inv.setItem(3, new ItemStack(Items.IRON_SWORD));
            inv.setItem(5, new ItemStack(Items.OAK_PLANKS, 64));
        });
        s.check("palette: which blocks are interactive", mc -> {
            Object[][] cases = {{Blocks.CHEST, true}, {Blocks.OAK_DOOR, true}, {Blocks.OAK_BUTTON, true}, {Blocks.CRAFTING_TABLE, true},
                {Blocks.LEVER, true}, {Blocks.HOPPER, true}, {Blocks.OAK_TRAPDOOR, true}, {Blocks.ENCHANTING_TABLE, true},
                {Blocks.STONE, false}, {Blocks.OAK_STAIRS, false}, {Blocks.OAK_FENCE, false}, {Blocks.OAK_LOG, false},
                {Blocks.GRASS_BLOCK, false}, {Blocks.GLASS, false}, {Blocks.OAK_LEAVES, false}, {Blocks.OAK_SLAB, false}};
            for (Object[] c : cases) {
                boolean got = io.github.profetgit.tidypockets.palette.RandomPlace.interactive(((Block) c[0]).defaultBlockState());
                if (got != (Boolean) c[1]) return c[0] + " interactive=" + got;
            }
            return null;
        });
        for (int slot : new int[] {0, 1, 2, 3}) {
            select(s, slot);
            markSelected(s);
        }
        s.check("palette: the key marks block slots", mc -> Palette.has(0) && Palette.has(1) && Palette.has(2) ? null : "marks " + Palette.slots())
            .check("palette: a sword can't join", mc -> !Palette.has(3) ? null : "sword is in the palette")
            .run(mc -> Palette.reload())
            .check("palette: marks are saved", mc -> Palette.has(0) && Palette.has(1) && Palette.has(2) && !Palette.has(3) ? null : "after reload " + Palette.slots())
            .run(mc -> mc.player.getInventory().setSelectedSlot(0))
            .waitTicks(4)
            .capture("palette-hud", 4)
            .waitTicks(6);
        select(s, 0);
        s.run(mc -> {
                TidyConfig.get().randomSpread = true;
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = new java.util.Random(7)::nextDouble;
                placeGrid(mc);
            })
            .waitTicks(25)
            .run(mc -> sameNeighbours(mc, spread))
            .server(server -> serverSel[0] = player(server).getInventory().getSelectedSlot())
            .check("palette: every click placed a block", mc -> spread[1] == 54 ? null : "placed " + (int) spread[1] + " of 54, refused:" + gridLog)
            .check("palette: all three blocks show up", mc -> spread[2] == 3 && spread[3] < 40 ? null : "kinds " + (int) spread[2] + " most " + (int) spread[3])
            .check("palette: avoid-clumps keeps like blocks apart", mc -> spread[0] < 0.2 ? null : String.format("%.2f of neighbours alike", spread[0]))
            .check("palette: server holds the rolled slot", mc -> serverSel[0] == mc.player.getInventory().getSelectedSlot() ? null
                : "client " + mc.player.getInventory().getSelectedSlot() + " server " + serverSel[0])
            .check("palette: placing used the stacks", mc -> {
                int left = mc.player.getInventory().getItem(0).getCount() + mc.player.getInventory().getItem(1).getCount() + mc.player.getInventory().getItem(2).getCount();
                return left == 192 - 54 ? null : "left " + left;
            });
        clearGrid(s);
        select(s, 0);
        s.run(mc -> {
                TidyConfig.get().randomSpread = false;
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = new java.util.Random(7)::nextDouble;
                placeGrid(mc);
            })
            .waitTicks(25)
            .run(mc -> sameNeighbours(mc, plain))
            .check("palette: plain random is patchier than avoid-clumps", mc -> plain[0] > spread[0] + 0.1
                ? null : String.format("plain %.2f vs spread %.2f", plain[0], spread[0]));
        clearGrid(s);
        select(s, 0);
        float[] heights = new float[6];
        boolean[] shownNow = {false};
        s.run(mc -> {
                TidyConfig.get().randomSpread = false;
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = () -> 0.9;
                mc.player.setXRot(60f);
            })
            .waitTicks(8)
            .run(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(ground.offset(1, 0, 2))))
            .waitTicks(2)
            .run(mc -> shownNow[0] = HandProbe.shown(mc) == mc.player.getMainHandItem());
        for (int i = 0; i < heights.length; i++) {
            int k = i;
            s.run(mc -> heights[k] = HandProbe.height(mc)).waitTicks(2);
        }
        s.check("palette: the hand shows the rolled block at once", mc -> shownNow[0] ? null : "still showing " + HandProbe.shown(mc))
            .check("palette: the hand is back up after the placing bounce", mc -> heights[3] > 0.95f ? null : "heights " + java.util.Arrays.toString(heights));
        clearGrid(s);
        select(s, 2);
        s.server(server -> {
                chestAt[0] = ground.offset(2, 1, 2);
                server.overworld().setBlock(chestAt[0], Blocks.CHEST.defaultBlockState(), 3);
            })
            .waitTicks(8)
            .run(mc -> {
                TidyConfig.get().randomSpread = true;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(chestAt[0]));
            })
            .until("chest to open", 100, mc -> mc.gui.screen() instanceof ContainerScreen)
            .check("palette: a chest click opens it and keeps the hand", mc -> mc.player.getInventory().getSelectedSlot() == 2 ? null
                : "selected slot became " + mc.player.getInventory().getSelectedSlot())
            .run(mc -> mc.player.closeContainer())
            .waitTicks(5)
            .server(server -> server.overworld().setBlock(chestAt[0], Blocks.AIR.defaultBlockState(), 3))
            .waitTicks(5);

        // a slot that runs out is refilled, wherever the roll came from
        resetInventory(s, inv -> {
            inv.setItem(0, new ItemStack(Items.STONE, 1));
            inv.setItem(1, new ItemStack(Items.COBBLESTONE, 1));
            inv.setItem(20, new ItemStack(Items.STONE, 40));
            inv.setItem(21, new ItemStack(Items.COBBLESTONE, 40));
        });
        s.run(mc -> {
            Palette.clear();
            mc.player.getInventory().setSelectedSlot(0);
            Palette.toggle(0, mc.player.getInventory().getItem(0));
            Palette.toggle(1, mc.player.getInventory().getItem(1));
            TidyConfig.get().randomSpread = false;
        }).waitTicks(3);
        s.run(mc -> {
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = () -> 0.9;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(ground.offset(2, 0, 2)));
            })
            .waitTicks(10)
            .check("palette: the rolled slot was refilled", mc -> {
                String a = inv(mc, 1, Items.COBBLESTONE, 40);
                return a != null ? a : inv(mc, 21, null, 0);
            })
            .check("palette: the roll left the selection on that slot", mc -> mc.player.getInventory().getSelectedSlot() == 1 ? null
                : "selected " + mc.player.getInventory().getSelectedSlot())
            .run(mc -> {
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = () -> 0.1;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(ground.offset(1, 0, 2)));
            })
            .waitTicks(10)
            .check("palette: refill works from the other slot too", mc -> {
                String a = inv(mc, 0, Items.STONE, 40);
                return a != null ? a : inv(mc, 20, null, 0);
            })
            .server(Scenarios::snapshotPlayer)
            .check("palette: server agrees after refills", Scenarios::serverMatches);

        // a slot that turns into a non-block leaves the palette by itself
        s.server(server -> {
                player(server).getInventory().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
                player(server).getInventory().setItem(5, new ItemStack(Items.OAK_PLANKS, 64));
            })
            .waitTicks(6)
            .check("palette: a slot holding a sword drops out", mc -> !Palette.has(1) && Palette.has(0) ? null : "marks " + Palette.slots());

        // the key over a hotbar slot in the inventory screen, and Shift to clear
        s.run(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)))
            .until("inventory", 100, mc -> mc.gui.screen() instanceof InventoryScreen)
            .waitTicks(5)
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.screen.ScreenInput.keyPressed(scr, playerSlot(mc, 5),
                    new io.github.profetgit.tidypockets.input.KeyEvt(InputConstants.KEY_R, 0, 0));
                io.github.profetgit.tidypockets.screen.ScreenInput.keyPressed(scr, playerSlot(mc, 20),
                    new io.github.profetgit.tidypockets.input.KeyEvt(InputConstants.KEY_R, 0, 0));
            })
            .check("palette: R over a hotbar block in the inventory adds it", mc -> Palette.has(5) ? null : "marks " + Palette.slots())
            .check("palette: R over a main-inventory slot does nothing", mc -> !Palette.has(20) ? null : "slot 20 marked")
            .waitTicks(4)
            .capture("palette-inventory", 2)
            .waitTicks(4)
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.Compat.forceShift = true;
                io.github.profetgit.tidypockets.screen.ScreenInput.keyPressed(scr, playerSlot(mc, 5),
                    new io.github.profetgit.tidypockets.input.KeyEvt(InputConstants.KEY_R, 0, io.github.profetgit.tidypockets.Compat.MOD_SHIFT));
                io.github.profetgit.tidypockets.Compat.forceShift = false;
            })
            .check("palette: Shift+R clears the palette", mc -> Palette.slots().isEmpty() ? null : "marks " + Palette.slots())
            .run(mc -> mc.player.closeContainer())
            .waitTicks(3)
            .run(mc -> mc.gui.setScreen(new io.github.profetgit.tidypockets.config.TidyConfigScreen(null)))
            .waitTicks(10)
            .capture("palette-config", 2)
            .waitTicks(4)
            .run(mc -> {
                mc.gui.screen().onClose();
            })
            .waitTicks(3)
            .run(mc -> {
                io.github.profetgit.tidypockets.palette.RandomPlace.rng = null;
                TidyConfig.get().randomSpread = true;
                mc.player.closeContainer();
            })
            .waitTicks(3);
    }

    private static void refill(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
            inv.setItem(20, new ItemStack(Items.COBBLESTONE, 40));
            inv.setItem(1, new ItemStack(Items.WATER_BUCKET));
            inv.setItem(21, new ItemStack(Items.WATER_BUCKET));
        });
        select(s, 0);
        s.check("refill setup applied", mc -> inv(mc, 0, Items.COBBLESTONE, 1));
        s.run(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, topOf(ground.offset(0, 0, 2))))
            .waitTicks(10)
            .check("refill block", mc -> {
                String a = inv(mc, 0, Items.COBBLESTONE, 40);
                return a != null ? a : inv(mc, 20, null, 0);
            })
            .server(Scenarios::snapshotPlayer)
            .check("refill block server agrees", Scenarios::serverMatches);
        select(s, 1);
        s.run(mc -> {
                mc.player.setXRot(70f);
                mc.player.setYRot(0f);
            })
            .waitTicks(3)
            .run(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND))
            .waitTicks(10)
            .check("refill bucket", mc -> {
                String a = inv(mc, 1, Items.WATER_BUCKET, 1);
                return a != null ? a : inv(mc, 21, Items.BUCKET, 1);
            })
            .server(Scenarios::snapshotPlayer)
            .check("refill bucket server agrees", Scenarios::serverMatches);
    }

    private static List<ItemStack> serverPlayerView;

    private static void snapshotPlayer(MinecraftServer server) {
        Inventory inv = player(server).getInventory();
        List<ItemStack> v = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) v.add(inv.getItem(i).copy());
        serverPlayerView = v;
    }

    private static String serverMatches(Minecraft mc) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack c = mc.player.getInventory().getItem(i), sv = serverPlayerView.get(i);
            if (!ItemStack.matches(c, sv)) return "slot " + i + ": client " + c + " server " + sv;
        }
        return null;
    }

    private static ItemStack worn(net.minecraft.world.item.Item item) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.DAMAGE, s.getMaxDamage() - 1);
        return s;
    }

    private static void protect(Script s) {
        int[] pigId = {-1};
        resetInventory(s, inv -> {
            inv.setItem(2, worn(Items.IRON_SWORD));
            inv.setItem(22, new ItemStack(Items.IRON_SWORD));
            inv.setItem(3, worn(Items.DIAMOND_PICKAXE));
        });
        s.server(server -> {
            ServerPlayer p = player(server);
            Entity pig = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pig"))
                .create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            pig.snapTo(p.getX(), p.getY(), p.getZ() + 2, 0, 0);
            ((net.minecraft.world.entity.Mob) pig).setNoAi(true);
            server.overworld().addFreshEntity(pig);
            pigId[0] = pig.getId();
        }).waitTicks(20);
        select(s, 2);
        s.run(mc -> mc.gameMode.attack(mc.player, mc.level.getEntity(pigId[0])))
            .waitTicks(10)
            .check("protect swaps in a fresh sword", mc -> {
                ItemStack hand = mc.player.getInventory().getItem(2), old = mc.player.getInventory().getItem(22);
                if (!hand.is(Items.IRON_SWORD) || hand.getDamageValue() != 0) return "hand holds " + hand;
                return old.is(Items.IRON_SWORD) && old.getMaxDamage() - old.getDamageValue() == 1 ? null : "worn sword is now " + old;
            });
        select(s, 3);
        s.run(mc -> mc.gameMode.startDestroyBlock(ground.offset(1, 0, 2), Direction.UP))
            .waitTicks(10)
            .check("protect stows a worn pickaxe with no spare", mc -> {
                if (!mc.player.getInventory().getItem(3).isEmpty()) return "hand still holds " + mc.player.getInventory().getItem(3);
                for (int i = 9; i < 36; i++) {
                    ItemStack st = mc.player.getInventory().getItem(i);
                    if (st.is(Items.DIAMOND_PICKAXE)) return st.getMaxDamage() - st.getDamageValue() == 1 ? null : "pickaxe damaged to " + st;
                }
                return "pickaxe vanished";
            })
            .check("protect keeps the block intact", mc -> mc.level.getBlockState(ground.offset(1, 0, 2)).is(Blocks.STONE) ? null : "block was broken");
        s.server(server -> {
                var inv = player(server).getInventory();
                for (int i = 0; i < 36; i++) {
                    if (inv.getItem(i).is(Items.DIAMOND_PICKAXE)) {
                        ItemStack st = inv.getItem(i);
                        inv.setItem(i, ItemStack.EMPTY);
                        inv.setItem(3, st);
                        break;
                    }
                }
            }).waitTicks(5)
            .run(mc -> mc.gameMode.startDestroyBlock(ground.offset(1, 0, 2), Direction.UP))
            .waitTicks(10)
            .check("protect lets a deliberately re-equipped tool be used", mc -> {
                ItemStack st = mc.player.getInventory().getItem(3);
                return st.is(Items.DIAMOND_PICKAXE) && st.getMaxDamage() - st.getDamageValue() == 1 ? null : "hand holds " + st;
            });
        resetInventory(s, inv -> {
            for (int i = 9; i < 36; i++) inv.setItem(i, new ItemStack(Items.DIRT, 64));
            inv.setItem(4, worn(Items.DIAMOND_PICKAXE));
        });
        select(s, 4);
        s.run(mc -> mc.gameMode.startDestroyBlock(ground.offset(-1, 0, 2), Direction.UP))
            .waitTicks(10)
            .check("protect blocks the action when the inventory is full", mc -> {
                ItemStack st = mc.player.getInventory().getItem(4);
                return st.is(Items.DIAMOND_PICKAXE) && st.getMaxDamage() - st.getDamageValue() == 1 ? null : "hand holds " + st;
            });
    }

    private static void openChest(Script s, BlockPos offset, java.util.function.Consumer<Container> setup) {
        s.server(server -> {
                ServerPlayer p = player(server);
                ServerLevel level = server.overworld();
                BlockPos at = p.blockPosition().offset(offset);
                level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
                BlockState st = level.getBlockState(at);
                Container c = ChestBlock.getContainer((ChestBlock) st.getBlock(), st, level, at, true);
                for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, ItemStack.EMPTY);
                setup.accept(c);
                p.openMenu(st.getMenuProvider(level, at));
            })
            .until("chest screen", 200, mc -> mc.gui.screen() instanceof ContainerScreen && !chestSlots(mc).isEmpty())
            .waitTicks(8);
    }

    private static Slot playerSlot(Minecraft mc, int invIndex) {
        for (Slot sl : mc.player.containerMenu.slots) {
            if (sl.container == mc.player.getInventory() && Inv.index(sl) == invIndex) return sl;
        }
        throw new IllegalStateException("no slot for inventory index " + invIndex);
    }

    private static void scroll(Minecraft mc, Slot slot, double amount, boolean shift) {
        AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) scr;
        io.github.profetgit.tidypockets.Compat.forceShift = shift;
        io.github.profetgit.tidypockets.Compat.mouseScrolled(scr, a.tidypockets$left() + slot.x + 8, a.tidypockets$top() + slot.y + 8, 0, amount);
        io.github.profetgit.tidypockets.Compat.forceShift = false;
    }

    private static io.github.profetgit.tidypockets.input.MouseEvt at(Minecraft mc, Slot slot, int button, int mods) {
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) mc.gui.screen();
        return new io.github.profetgit.tidypockets.input.MouseEvt(a.tidypockets$left() + slot.x + 8, a.tidypockets$top() + slot.y + 8, button, mods);
    }

    private static String dump(Minecraft mc) {
        StringBuilder b = new StringBuilder("chest[");
        for (Slot sl : chestSlots(mc)) if (sl.hasItem()) b.append(sl.getContainerSlot()).append('=').append(sl.getItem()).append(' ');
        b.append("] player[");
        for (int i = 0; i < 36; i++) if (!mc.player.getInventory().getItem(i).isEmpty()) b.append(i).append('=').append(mc.player.getInventory().getItem(i)).append(' ');
        return b.append("] cursor=").append(mc.player.containerMenu.getCarried()).toString();
    }

    private static int count(List<Slot> slots, net.minecraft.world.item.Item item) {
        int n = 0;
        for (Slot sl : slots) if (sl.getItem().is(item)) n += sl.getItem().getCount();
        return n;
    }

    private static void mouse(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(10, new ItemStack(Items.STONE, 20));
            inv.setItem(11, new ItemStack(Items.OAK_LOG, 5));
            inv.setItem(12, new ItemStack(Items.TORCH, 7));
            inv.setItem(13, new ItemStack(Items.BREAD, 3));
        });
        openChest(s, new BlockPos(-2, 0, 0), c -> {
            c.setItem(0, new ItemStack(Items.DIRT, 10));
            c.setItem(1, new ItemStack(Items.DIRT, 10));
            c.setItem(2, new ItemStack(Items.DIRT, 10));
        });
        s.capture("fly-wheel", 12)
            .run(mc -> scroll(mc, playerSlot(mc, 10), -1, false))
            .waitTicks(4)
            .check("wheel down moves one item", mc -> count(chestSlots(mc), Items.STONE) == 1
                && mc.player.getInventory().getItem(10).getCount() == 19 ? null : dump(mc))
            .run(mc -> scroll(mc, playerSlot(mc, 10), 1, false))
            .waitTicks(4)
            .check("wheel up pulls it back", mc -> count(chestSlots(mc), Items.STONE) == 0
                && mc.player.getInventory().getItem(10).getCount() == 20 ? null : dump(mc))
            .run(mc -> scroll(mc, playerSlot(mc, 10), -1, true))
            .waitTicks(4)
            .check("shift+wheel moves the stack", mc -> count(chestSlots(mc), Items.STONE) == 20 ? null : dump(mc))
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                int shift = io.github.profetgit.tidypockets.Compat.MOD_SHIFT;
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, at(mc, playerSlot(mc, 11), LEFT, shift), false);
                io.github.profetgit.tidypockets.Compat.mouseDragged(scr, at(mc, playerSlot(mc, 12), LEFT, shift), 0, 0);
                io.github.profetgit.tidypockets.Compat.mouseDragged(scr, at(mc, playerSlot(mc, 13), LEFT, shift), 0, 0);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, at(mc, playerSlot(mc, 13), LEFT, shift));
            })
            .waitTicks(4)
            .check("shift-drag moves every slot passed", mc -> count(chestSlots(mc), Items.OAK_LOG) == 5
                && count(chestSlots(mc), Items.TORCH) == 7 && count(chestSlots(mc), Items.BREAD) == 3 ? null
                : dump(mc))
            .run(mc -> click(mc, chestSlots(mc).get(0), LEFT))
            .waitTicks(4)
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                List<Slot> c = chestSlots(mc);
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, at(mc, c.get(1), LEFT, 0), false);
                io.github.profetgit.tidypockets.Compat.mouseDragged(scr, at(mc, c.get(2), LEFT, 0), 0, 0);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, at(mc, c.get(2), LEFT, 0));
            })
            .waitTicks(4)
            .check("drag collects matching stacks onto the cursor", mc -> {
                ItemStack carried = mc.player.containerMenu.getCarried();
                return carried.is(Items.DIRT) && carried.getCount() == 30 && count(chestSlots(mc), Items.DIRT) == 0 ? null
                    : "cursor " + carried + ", chest dirt " + count(chestSlots(mc), Items.DIRT);
            })
            .run(mc -> click(mc, chestSlots(mc).get(0), LEFT))
            .waitTicks(4)
            .check("mouse moves keep server in sync", mc -> {
                List<Slot> c = chestSlots(mc);
                return c.get(0).getItem().is(Items.DIRT) && c.get(0).getItem().getCount() == 30 ? null : "slot 0 = " + c.get(0).getItem();
            })
            .run(mc -> mc.player.closeContainer())
            .waitTicks(5);
    }

    private static void pressButton(Minecraft mc, String name) {
        AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
        for (var child : scr.children()) {
            if (child instanceof net.minecraft.client.gui.components.ImageButton b
                && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("tidypockets.button." + name)) {
                io.github.profetgit.tidypockets.input.MouseEvt e = new io.github.profetgit.tidypockets.input.MouseEvt(b.getX() + 5, b.getY() + 5, LEFT, 0);
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, e, false);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, e);
                return;
            }
        }
        throw new IllegalStateException("no " + name + " button on " + scr.getClass().getSimpleName());
    }

    private static void tools(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(10, new ItemStack(Items.STONE, 20));
            inv.setItem(11, new ItemStack(Items.DIRT, 5));
            inv.setItem(12, new ItemStack(Items.TORCH, 10));
            inv.setItem(13, new ItemStack(Items.BREAD, 3));
            inv.setItem(0, new ItemStack(Items.STONE, 30));
            inv.setItem(1, new ItemStack(Items.TORCH, 10));
        });
        openChest(s, new BlockPos(-2, 0, -2), c -> {
            c.setItem(0, new ItemStack(Items.STONE, 1));
            c.setItem(1, new ItemStack(Items.DIRT, 1));
            c.setItem(6, new ItemStack(Items.TORCH, 64));
        });
        s.capture("buttons", 3)
            .waitTicks(2)
            .run(mc -> pressButton(mc, "deposit"))
            .waitTicks(5)
            .check("deposit moves matching main-inventory stacks", mc -> {
                Inventory inv = mc.player.getInventory();
                if (!inv.getItem(10).isEmpty() || !inv.getItem(11).isEmpty() || !inv.getItem(12).isEmpty()) return dump(mc);
                if (inv.getItem(13).getCount() != 3) return "bread moved: " + dump(mc);
                return inv.getItem(0).getCount() == 30 && inv.getItem(1).getCount() == 10 ? null : "hotbar touched: " + dump(mc);
            })
            .run(mc -> pressButton(mc, "restock"))
            .waitTicks(5)
            .check("restock tops up partial stacks", mc -> {
                Inventory inv = mc.player.getInventory();
                return inv.getItem(0).getCount() == 51 && inv.getItem(1).getCount() == 64 && inv.getItem(13).getCount() == 3
                    ? null : dump(mc);
            })
            .check("tools keep items conserved", mc -> count(chestSlots(mc), Items.TORCH) + mc.player.getInventory().getItem(1).getCount() == 84
                ? null : dump(mc))
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.Compat.keyPressed(scr, new io.github.profetgit.tidypockets.input.KeyEvt(InputConstants.KEY_F, 0, io.github.profetgit.tidypockets.Compat.MOD_CONTROL));
                if (scr.getFocused() instanceof net.minecraft.client.gui.components.EditBox box) box.setValue("torch");
            })
            .waitTicks(3)
            .capture("search", 3)
            .waitTicks(3)
            .check("ctrl+f opens search", mc -> "torch".equals(io.github.profetgit.tidypockets.tools.ContainerTools.query(
                (AbstractContainerScreen<?>) mc.gui.screen())) ? null : "search query not set")
            .run(mc -> io.github.profetgit.tidypockets.Compat.keyPressed(((AbstractContainerScreen<?>) mc.gui.screen()), 
                new io.github.profetgit.tidypockets.input.KeyEvt(InputConstants.KEY_E, 0, 0)))
            .waitTicks(2)
            .check("typing in search does not close the screen", mc -> mc.gui.screen() instanceof ContainerScreen ? null : "screen closed")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(5);
    }

    private static double[] amounts = new double[2];

    private static net.minecraft.client.gui.components.AbstractScrollArea scrollArea(Minecraft mc) {
        for (var c : mc.gui.screen().children()) {
            if (c instanceof net.minecraft.client.gui.components.AbstractScrollArea a) return a;
        }
        throw new IllegalStateException("no scroll area");
    }

    private static void anims(Script s) {
        resetInventory(s, inv -> {
            for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(Items.STONE, i + 1));
            inv.setItem(20, new ItemStack(Items.DIAMOND_HELMET));
        });
        s.capture("pop-chest", 16);
        openChest(s, new BlockPos(2, 0, 2), c -> {
            c.setItem(3, new ItemStack(Items.TORCH, 12));
            c.setItem(13, new ItemStack(Items.BREAD, 5));
        });
        s.run(mc -> mc.player.closeContainer()).waitTicks(5);
        select(s, 0);
        s.capture("hotbar-glide", 10)
            .run(mc -> mc.player.getInventory().setSelectedSlot(6))
            .waitTicks(8)
            .capture("inventory-pop", 14)
            .run(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)))
            .waitTicks(20)
            .capture("player-hop", 16)
            .server(server -> {
                Inventory inv = player(server).getInventory();
                player(server).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, inv.getItem(20).copy());
                inv.setItem(20, ItemStack.EMPTY);
            })
            .waitTicks(20)
            .check("equipping with the inventory open keeps working", mc -> mc.player.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) ? null : "helmet not equipped")
            .run(mc -> {
                mc.player.closeContainer();
                mc.gui.setScreen(new io.github.profetgit.tidypockets.config.TidyConfigScreen(null));
            })
            .waitTicks(10)
            .capture("config", 2)
            .waitTicks(3)
            .check("config screen opens", mc -> mc.gui.screen() instanceof io.github.profetgit.tidypockets.config.TidyConfigScreen ? null
                : "screen is " + mc.gui.screen())
            .capture("smooth-scroll", 14)
            .run(mc -> {
                var area = scrollArea(mc);
                io.github.profetgit.tidypockets.Compat.mouseScrolled(area, area.getX() + 10, area.getY() + 10, 0, -3);
            })
            .waitTicks(1)
            .run(mc -> amounts[0] = scrollArea(mc).scrollAmount())
            .waitTicks(10)
            .run(mc -> amounts[1] = scrollArea(mc).scrollAmount())
            .check("list scrolls smoothly", mc -> amounts[1] > 0 && amounts[0] < amounts[1] ? null
                : "scroll amounts " + amounts[0] + " then " + amounts[1])
            .run(mc -> mc.gui.setScreen(null))
            .waitTicks(5);
    }

    /** 3D pictures (enchanting book, smithing armour stand) are drawn outside the GUI pose but must pop with the panel. */
    private static void pictures(Script s) {
        picturePop(s, "pop-enchanting", Blocks.ENCHANTING_TABLE, EnchantmentScreen.class, new BlockPos(2, 0, -2));
        picturePop(s, "pop-smithing", Blocks.SMITHING_TABLE, SmithingScreen.class, new BlockPos(-2, 0, -2));
    }

    private static void picturePop(Script s, String name, Block block, Class<?> screen, BlockPos offset) {
        int[] posed = new int[2];
        s.run(mc -> io.github.profetgit.tidypockets.anim.ScreenPop.posedPictures = 0)
            .capture(name, 30)
            .server(server -> {
                ServerPlayer p = player(server);
                ServerLevel level = server.overworld();
                BlockPos at = p.blockPosition().offset(offset);
                level.setBlock(at, block.defaultBlockState(), 3);
                p.openMenu(level.getBlockState(at).getMenuProvider(level, at));
            })
            .until(name + " screen", 200, mc -> screen.isInstance(mc.gui.screen()))
            .waitTicks(8)
            .run(mc -> posed[0] = io.github.profetgit.tidypockets.anim.ScreenPop.posedPictures)
            .waitTicks(10)
            .run(mc -> posed[1] = io.github.profetgit.tidypockets.anim.ScreenPop.posedPictures)
            .check(name + ": the 3D picture pops with the panel", mc -> posed[0] > 0 || !io.github.profetgit.tidypockets.Compat.HAS_PIP ? null : "no picture took the pop pose")
            .check(name + ": after the pop the picture keeps vanilla's pose", mc -> posed[1] == posed[0] ? null
                : (posed[1] - posed[0]) + " pictures posed after the pop ended")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(5);
    }

    private static void locks(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
            inv.setItem(12, new ItemStack(Items.DIRT, 32));
            inv.setItem(13, new ItemStack(Items.DIRT, 5));
            inv.setItem(14, new ItemStack(Items.BREAD, 4));
        });
        s.run(mc -> {
                SlotLocks.toggle(0, mc.player.getInventory().getItem(0));
                SlotLocks.toggle(12, mc.player.getInventory().getItem(12));
                SlotLocks.toggle(14, mc.player.getInventory().getItem(14));
                mc.gui.setScreen(new InventoryScreen(mc.player));
            })
            .until("inventory", 100, mc -> mc.gui.screen() instanceof InventoryScreen)
            .waitTicks(5)
            .run(mc -> click(mc, playerSlot(mc, 12), LEFT))
            .waitTicks(3)
            .check("lock: clicking a locked stack doesn't pick it up", mc -> mc.player.containerMenu.getCarried().isEmpty()
                && mc.player.getInventory().getItem(12).getCount() == 32 ? null : "cursor " + mc.player.containerMenu.getCarried())
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.input.MouseEvt e = at(mc, playerSlot(mc, 0), LEFT, io.github.profetgit.tidypockets.Compat.MOD_SHIFT);
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, e, false);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, e);
            })
            .waitTicks(3)
            .check("lock: shift-click leaves a locked item in place", mc -> mc.player.getInventory().getItem(0).is(Items.DIAMOND_SWORD)
                ? null : "hotbar 0 = " + mc.player.getInventory().getItem(0))
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                int shift = io.github.profetgit.tidypockets.Compat.MOD_SHIFT;
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, at(mc, playerSlot(mc, 15), LEFT, shift), false);
                io.github.profetgit.tidypockets.Compat.mouseDragged(scr, at(mc, playerSlot(mc, 14), LEFT, shift), 0, 0);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, at(mc, playerSlot(mc, 14), LEFT, shift));
            })
            .waitTicks(3)
            .check("lock: shift-drag skips a locked item", mc -> mc.player.getInventory().getItem(14).is(Items.BREAD)
                ? null : "slot 14 = " + mc.player.getInventory().getItem(14))
            .run(mc -> click(mc, playerSlot(mc, 13), LEFT))
            .waitTicks(2)
            .run(mc -> click(mc, playerSlot(mc, 12), LEFT))
            .waitTicks(3)
            .check("lock: matching items can still be added", mc -> mc.player.getInventory().getItem(12).getCount() == 37
                && mc.player.containerMenu.getCarried().isEmpty() ? null : "slot 12 = " + mc.player.getInventory().getItem(12))
            .run(mc -> mc.player.closeContainer())
            .waitTicks(3);
        select(s, 0);
        s.run(mc -> io.github.profetgit.tidypockets.Compat.dropHeld())
            .waitTicks(5)
            .check("lock: Q doesn't drop a locked item", mc -> mc.player.getInventory().getItem(0).is(Items.DIAMOND_SWORD)
                ? null : "hotbar 0 = " + mc.player.getInventory().getItem(0))
            .server(server -> {
                Inventory inv = player(server).getInventory();
                inv.setItem(12, ItemStack.EMPTY);
                inv.setItem(0, ItemStack.EMPTY);
            })
            .waitTicks(25)
            .check("lock: a lock goes away with its item", mc -> !SlotLocks.isLocked(12) ? null : "slot 12 still locked")
            .check("lock: a hotbar lock keeps its remembered item", mc -> SlotLocks.isLocked(0) ? null : "hotbar 0 unlocked")
            .run(mc -> {
                SlotLocks.toggle(0, ItemStack.EMPTY);
                if (SlotLocks.isLocked(14)) SlotLocks.toggle(14, ItemStack.EMPTY);
            })
            .waitTicks(3);
    }

    private static void creative(Script s) {
        s.server(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a"))
            .waitTicks(10)
            .run(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                mc.player, mc.player.connection.enabledFeatures(), false)))
            .waitTicks(20)
            .capture("creative-scroll", 14)
            .run(mc -> io.github.profetgit.tidypockets.Compat.mouseScrolled(mc.gui.screen(), 213, 100, 0, -1))
            .waitTicks(10)
            .check("creative screen scrolls", mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                ? null : "screen closed")
            .run(mc -> mc.player.closeContainer())
            .waitTicks(5);
        creativeInventoryTab(s);
        creativeItemTabs(s);
        s.server(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode survival @a"))
            .waitTicks(5);
    }

    private static void openCreative(Script s, CreativeModeTab.Type type) {
        s.run(mc -> mc.gui.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false)))
            .until("creative screen", 100, mc -> mc.gui.screen() instanceof CreativeModeInventoryScreen)
            .waitTicks(5)
            .run(mc -> {
                CreativeModeTab tab = type == CreativeModeTab.Type.CATEGORY ? CreativeModeTabs.getDefaultTab()
                    : CreativeModeTabs.allTabs().stream().filter(t -> t.getType() == type).findFirst().orElseThrow();
                ((CreativeScreenInvoker) mc.gui.screen()).tidypockets$selectTab(tab);
            })
            .waitTicks(10);
    }

    private static List<Slot> creativeMenu(Minecraft mc) {
        return ((AbstractContainerScreen<?>) mc.gui.screen()).getMenu().slots;
    }

    private static void shiftDrag(Minecraft mc, Slot... path) {
        AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
        int shift = io.github.profetgit.tidypockets.Compat.MOD_SHIFT;
        io.github.profetgit.tidypockets.Compat.mouseClicked(scr, at(mc, path[0], LEFT, shift), false);
        for (int i = 1; i < path.length; i++) io.github.profetgit.tidypockets.Compat.mouseDragged(scr, at(mc, path[i], LEFT, shift), 0, 0);
        io.github.profetgit.tidypockets.Compat.mouseReleased(scr, at(mc, path[path.length - 1], LEFT, shift));
    }

    /** The "Survival Inventory" tab works like the survival inventory; the trash can spares locked slots. */
    private static void creativeInventoryTab(Script s) {
        resetInventory(s, inv -> {
            inv.setItem(9, new ItemStack(Items.STONE, 20));
            inv.setItem(10, new ItemStack(Items.OAK_LOG, 5));
            inv.setItem(11, new ItemStack(Items.TORCH, 7));
            inv.setItem(12, new ItemStack(Items.BREAD, 3));
            inv.setItem(20, new ItemStack(Items.DIRT, 32));
            inv.setItem(25, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(33, new ItemStack(Items.COBBLESTONE, 5));
        });
        openCreative(s, CreativeModeTab.Type.INVENTORY);
        s.check("creative inv: the inventory tab is open", mc -> Creative.tab(mc.gui.screen()) == Creative.Tab.INVENTORY ? null : "tab " + Creative.tab(mc.gui.screen()))
            .run(mc -> shiftDrag(mc, playerSlot(mc, 10), playerSlot(mc, 11), playerSlot(mc, 12)))
            .waitTicks(4)
            .check("creative inv: shift-drag moves every slot passed", mc -> count(hotbar(mc), Items.OAK_LOG) == 5
                && count(hotbar(mc), Items.TORCH) == 7 && count(hotbar(mc), Items.BREAD) == 3 && mc.player.getInventory().getItem(11).isEmpty()
                ? null : "hotbar " + hotbar(mc).stream().map(Slot::getItem).toList())
            .run(mc -> scroll(mc, playerSlot(mc, 9), -1, false))
            .waitTicks(4)
            .check("creative inv: wheel moves one item to the hotbar", mc -> count(hotbar(mc), Items.STONE) == 1
                && mc.player.getInventory().getItem(9).getCount() == 19 ? null : "slot 9 = " + mc.player.getInventory().getItem(9))
            .run(mc -> click(mc, playerSlot(mc, 35), MIDDLE))
            .waitTicks(4)
            .check("creative inv: middle-click sorts", mc -> {
                List<Slot> main = mainSlots(mc);
                boolean[] locked = new boolean[main.size()];
                for (int i = 0; i < locked.length; i++) locked[i] = SlotLocks.isLocked(Inv.index(main.get(i)));
                return count(main, Items.COBBLESTONE) == 15 && ClickPlanner.plan(StackKeys.read(main), locked).clicks().isEmpty()
                    ? null : "main not sorted: " + main.stream().map(Slot::getItem).filter(i -> !i.isEmpty()).toList();
            })
            .server(server -> player(server).getInventory().setItem(34, new ItemStack(Items.COBBLESTONE, 7)))
            .waitTicks(5)
            .run(mc -> {
                AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) mc.gui.screen();
                io.github.profetgit.tidypockets.input.MouseEvt e = new io.github.profetgit.tidypockets.input.MouseEvt(a.tidypockets$left() + 12, a.tidypockets$top() + 12, MIDDLE, 0);
                io.github.profetgit.tidypockets.Compat.mouseClicked(mc.gui.screen(), e, false);
                io.github.profetgit.tidypockets.Compat.mouseReleased(mc.gui.screen(), e);
            })
            .waitTicks(4)
            .check("creative inv: middle-click on the background sorts", mc -> {
                List<Slot> cobble = mainSlots(mc).stream().filter(sl -> sl.getItem().is(Items.COBBLESTONE)).toList();
                return cobble.size() == 1 && cobble.get(0).getItem().getCount() == 22 ? null : "cobblestone stacks " + cobble.stream().map(Slot::getItem).toList();
            })
            .server(Scenarios::snapshotPlayer)
            .check("creative inv: server in sync", Scenarios::serverMatches)
            .capture("creative-trash", 14)
            .run(mc -> {
                int dirt = -1;
                for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) if (mc.player.getInventory().getItem(i).is(Items.DIRT)) dirt = i;
                SlotLocks.toggle(dirt, mc.player.getInventory().getItem(dirt));
                List<Slot> slots = creativeMenu(mc);
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.input.MouseEvt e = at(mc, slots.get(slots.size() - 1), LEFT, io.github.profetgit.tidypockets.Compat.MOD_SHIFT);
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, e, false);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, e);
            })
            .waitTicks(4)
            .check("creative inv: trash-can clear keeps the locked stack", mc -> {
                int items = 0, dirt = 0;
                for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                    ItemStack st = mc.player.getInventory().getItem(i);
                    if (!st.isEmpty()) items++;
                    if (st.is(Items.DIRT)) dirt += st.getCount();
                }
                return items == 1 && dirt == 32 ? null : items + " stacks left, dirt " + dirt;
            })
            .server(Scenarios::snapshotPlayer)
            .check("creative inv: server in sync after the clear", Scenarios::serverMatches)
            .run(mc -> {
                for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) if (SlotLocks.isLocked(i)) SlotLocks.toggle(i, ItemStack.EMPTY);
                mc.player.closeContainer();
            })
            .waitTicks(5);
    }

    /** Item tabs: shift-drag over the grid fills the inventory; the hotbar row below it is never dragged over. */
    private static void creativeItemTabs(Script s) {
        ItemStack[] grid = new ItemStack[4];
        resetInventory(s, inv -> inv.setItem(0, new ItemStack(Items.DIAMOND_SWORD)));
        s.run(mc -> SlotLocks.toggle(0, mc.player.getInventory().getItem(0)));
        openCreative(s, CreativeModeTab.Type.CATEGORY);
        s.capture("creative-grab", 12)
            .run(mc -> {
                for (int i = 0; i < grid.length; i++) grid[i] = creativeMenu(mc).get(i).getItem().copy();
                shiftDrag(mc, creativeMenu(mc).get(0), creativeMenu(mc).get(1), creativeMenu(mc).get(2));
            })
            .waitTicks(4)
            .check("creative items: shift-drag puts a full stack of each item in the hotbar", mc -> {
                for (int i = 0; i < 3; i++) {
                    ItemStack st = mc.player.getInventory().getItem(i + 1);
                    if (!ItemStack.isSameItemSameComponents(st, grid[i]) || st.getCount() != st.getMaxStackSize()) return "hotbar " + (i + 1) + " = " + st + ", want " + grid[i];
                }
                return mc.player.containerMenu.getCarried().isEmpty() ? null : "cursor " + mc.player.containerMenu.getCarried();
            })
            .server(Scenarios::snapshotPlayer)
            .check("creative items: server in sync", Scenarios::serverMatches)
            .run(mc -> shiftDrag(mc, creativeMenu(mc).get(3), playerSlot(mc, 0), playerSlot(mc, 1)))
            .waitTicks(4)
            .check("creative items: a grid shift-drag leaves the hotbar row alone", mc ->
                mc.player.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && ItemStack.isSameItemSameComponents(mc.player.getInventory().getItem(1), grid[0])
                    ? null : "hotbar 0 = " + mc.player.getInventory().getItem(0) + ", 1 = " + mc.player.getInventory().getItem(1))
            .run(mc -> mc.player.containerMenu.setCarried(ItemStack.EMPTY))
            .capture("creative-delete", 12)
            .run(mc -> shiftDrag(mc, playerSlot(mc, 0), playerSlot(mc, 1), playerSlot(mc, 2), creativeMenu(mc).get(3), playerSlot(mc, 3)))
            .waitTicks(4)
            .check("creative items: a hotbar shift-drag deletes every unlocked item passed", mc -> {
                Inventory inv = mc.player.getInventory();
                for (int i = 1; i <= 3; i++) if (!inv.getItem(i).isEmpty()) return "hotbar " + i + " = " + inv.getItem(i);
                for (int i = 4; i < Inventory.INVENTORY_SIZE; i++) if (!inv.getItem(i).isEmpty()) return "grid item grabbed into slot " + i + ": " + inv.getItem(i);
                return inv.getItem(0).is(Items.DIAMOND_SWORD) && mc.player.containerMenu.getCarried().isEmpty() ? null
                    : "hotbar 0 = " + inv.getItem(0) + ", cursor " + mc.player.containerMenu.getCarried();
            })
            .server(Scenarios::snapshotPlayer)
            .check("creative items: server in sync after deleting", Scenarios::serverMatches)
            .run(mc -> {
                AbstractContainerScreen<?> scr = (AbstractContainerScreen<?>) mc.gui.screen();
                io.github.profetgit.tidypockets.input.MouseEvt e = at(mc, playerSlot(mc, 0), LEFT, io.github.profetgit.tidypockets.Compat.MOD_SHIFT);
                io.github.profetgit.tidypockets.Compat.mouseClicked(scr, e, false);
                io.github.profetgit.tidypockets.Compat.mouseReleased(scr, e);
            })
            .waitTicks(4)
            .check("creative items: shift-click can't delete a locked hotbar item", mc -> mc.player.getInventory().getItem(0).is(Items.DIAMOND_SWORD)
                ? null : "hotbar 0 = " + mc.player.getInventory().getItem(0))
            .run(mc -> {
                SlotLocks.toggle(0, ItemStack.EMPTY);
                mc.player.closeContainer();
            })
            .waitTicks(5);
    }
}
