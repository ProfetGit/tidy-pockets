package io.github.profetgit.tidypockets.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.inv.Inv;
import io.github.profetgit.tidypockets.lock.SlotLocks;
import io.github.profetgit.tidypockets.mixin.AbstractContainerScreenAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Scripted scenes recorded for the Modrinth gallery and description. Run with dev/selftest/run.sh <ver> <loader> showcase. */
final class Showcase {
    private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT, MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;
    private static final int SHIFT = InputConstants.MOD_SHIFT;
    private static BlockPos origin, chest;

    private Showcase() {}

    static Script build() {
        Script s = new Script();
        s.run(mc -> mc.createWorldOpenFlows().createFreshLevel("showcase-" + System.currentTimeMillis(),
                new net.minecraft.world.level.LevelSettings("Tidy Pockets", net.minecraft.world.level.GameType.SURVIVAL,
                    new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false),
                    true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
                net.minecraft.world.level.levelgen.presets.WorldPresets::createTestWorldDimensions, mc.gui.screen()))
            .until("world", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null)
            .waitTicks(40)
            .server(Showcase::buildStudio)
            .waitTicks(60)
            .run(mc -> faceChest(mc));
        sort(s);
        open(s);
        moves(s);
        drag(s);
        locks(s);
        tools(s);
        refill(s);
        protect(s);
        hotbar(s);
        return s;
    }

    // ---- set ----

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void cmd(MinecraftServer server, String c) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }

    /** A small wooden room: bookshelf wall with lanterns, a chest ahead, a stone patch to mine and floor to build on. */
    private static void buildStudio(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel l = server.overworld();
        BlockPos o = p.blockPosition();
        origin = o;
        chest = o.offset(0, 0, -3);
        cmd(server, "time set 6000");
        cmd(server, "weather clear");
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -10; dz <= 6; dz++) {
                for (int dy = 0; dy <= 7; dy++) l.setBlock(o.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                boolean edge = Math.abs(dx) == 8 || dz == 6 || dz == -10;
                l.setBlock(o.offset(dx, -1, dz), (edge ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState(), 3);
            }
        }
        for (int dx = -8; dx <= 8; dx++) {
            boolean pillar = Math.abs(dx) == 8 || Math.abs(dx) == 4 || dx == 0;
            for (int dy = 0; dy <= 4; dy++) {
                BlockState b = pillar ? Blocks.SPRUCE_LOG.defaultBlockState()
                    : dy == 0 || dy == 3 ? Blocks.STONE_BRICKS.defaultBlockState()
                    : dy == 4 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState();
                l.setBlock(o.offset(dx, dy, -9), b, 3);
            }
        }
        for (int dx : new int[] {-6, -2, 2, 6}) {
            l.setBlock(o.offset(dx, 0, -8), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            l.setBlock(o.offset(dx, 1, -8), Blocks.LANTERN.defaultBlockState(), 3);
        }
        l.setBlock(o.offset(-1, 0, -8), Blocks.BARREL.defaultBlockState(), 3);
        l.setBlock(o.offset(1, 0, -8), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        l.setBlock(o.offset(-3, 0, -8), Blocks.POTTED_POPPY.defaultBlockState(), 3);
        l.setBlock(o.offset(3, 0, -8), Blocks.POTTED_BLUE_ORCHID.defaultBlockState(), 3);
        l.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
        for (int dx = 3; dx <= 5; dx++) {
            for (int dy = 0; dy <= 1; dy++) l.setBlock(o.offset(dx, dy, -3), Blocks.STONE.defaultBlockState(), 3);
        }
        cmd(server, String.format("tp %s %d.5 %d %d.5 180 30", p.getName().getString(), o.getX(), o.getY(), o.getZ()));
    }

    private static float[] lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 d = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[] {yaw, pitch};
    }

    private static void faceChest(Minecraft mc) {
        float[] yp = lookAt(mc, Vec3.atCenterOf(chest));
        mc.player.setYRot(yp[0]);
        mc.player.setXRot(yp[1]);
    }

    private static void setInventory(Script s, Consumer<Inventory> fill) {
        s.server(server -> {
            Inventory inv = player(server).getInventory();
            inv.clearContent();
            fill.accept(inv);
        }).waitTicks(10);
    }

    private static void setChest(Script s, Consumer<Container> fill) {
        s.server(server -> {
            ServerLevel l = server.overworld();
            BlockState st = l.getBlockState(chest);
            Container c = ChestBlock.getContainer((ChestBlock) st.getBlock(), st, l, chest, true);
            for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, ItemStack.EMPTY);
            fill.accept(c);
        });
    }

    private static void openChest(Script s) {
        s.run(Showcase::faceChest)
            .run(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(chest).add(0, 0, 0.5), Direction.SOUTH, chest, false)))
            .until("chest", 200, mc -> mc.gui.screen() instanceof ContainerScreen)
            .waitTicks(20);
    }

    private static void close(Script s) {
        s.run(mc -> {
            if (mc.gui.screen() instanceof ContainerScreen) mc.player.closeContainer();
            else mc.gui.setScreen(null);
        }).waitTicks(10);
    }

    private static void record(Script s, Director.Timeline t) {
        boolean[] started = {false};
        s.steps.add(mc -> {
            mc.gui.toastManager().clear();
            mc.gui.hud.getChat().clearMessages(false);
            if (!started[0]) {
                started[0] = true;
                Director.begin(mc, t);
            }
            return Director.finished(t);
        });
        s.waitTicks(10);
    }

    private static ItemStack st(Item i, int n) {
        return new ItemStack(i, n);
    }

    // ---- targets ----

    private static AbstractContainerScreenAccessor acc(Minecraft mc) {
        return (AbstractContainerScreenAccessor) mc.gui.screen();
    }

    private static Director.Target slot(java.util.function.Function<Minecraft, Slot> which) {
        return mc -> {
            Slot sl = which.apply(mc);
            return new float[] {acc(mc).tidypockets$left() + sl.x + 8, acc(mc).tidypockets$top() + sl.y + 8};
        };
    }

    private static Director.Target chestSlot(int i) {
        return slot(mc -> Inv.containerSlots(mc.player.containerMenu, mc.player).get(i));
    }

    private static Director.Target invSlot(int invIndex) {
        return slot(mc -> {
            for (Slot sl : mc.player.containerMenu.slots) {
                if (sl.container == mc.player.getInventory() && sl.getContainerSlot() == invIndex) return sl;
            }
            throw new IllegalStateException("no slot " + invIndex);
        });
    }

    private static Director.Target beside(float dx, float dy) {
        return mc -> new float[] {acc(mc).tidypockets$left() + acc(mc).tidypockets$width() + dx, acc(mc).tidypockets$top() + dy};
    }

    private static Director.Target button(String name) {
        return mc -> {
            for (var c : mc.gui.screen().children()) {
                if (c instanceof ImageButton b && b.getMessage().getContents() instanceof TranslatableContents t
                    && t.getKey().equals("tidypockets.button." + name)) return new float[] {b.getX() + 5.5f, b.getY() + 5.5f};
            }
            throw new IllegalStateException("no button " + name);
        };
    }

    /** The panel plus room for the search box above and the caption below, in GUI pixels. */
    private static int[] panelCrop(Minecraft mc) {
        AbstractContainerScreenAccessor a = acc(mc);
        int x = a.tidypockets$left() - 22, y = a.tidypockets$top() - 18;
        return new int[] {x, y, a.tidypockets$width() + 44, a.tidypockets$height() + 42};
    }

    private static int[] wideCrop(Minecraft mc) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        return new int[] {0, 0, w, h};
    }

    // ---- scenes ----

    private static void messyChest(Container c) {
        Item[] items = {Items.OAK_PLANKS, Items.STICK, Items.COBBLESTONE, Items.ANDESITE, Items.GRANITE, Items.DIORITE,
            Items.RAW_IRON, Items.COAL, Items.REDSTONE, Items.LAPIS_LAZULI, Items.OAK_LOG, Items.DIRT, Items.GRAVEL,
            Items.FLINT, Items.WHEAT_SEEDS, Items.BONE, Items.STRING, Items.COBBLESTONE, Items.DIRT, Items.OAK_PLANKS};
        int[] counts = {12, 7, 40, 23, 18, 9, 5, 31, 14, 8, 6, 51, 11, 3, 17, 4, 6, 24, 13, 20};
        int[] slots = {0, 2, 3, 5, 8, 9, 10, 12, 14, 15, 16, 18, 19, 20, 22, 23, 24, 25, 26, 7};
        for (int i = 0; i < items.length; i++) c.setItem(slots[i], st(items[i], counts[i]));
    }

    private static void messyInventory(Inventory inv) {
        inv.setItem(0, st(Items.DIAMOND_PICKAXE, 1));
        inv.setItem(1, st(Items.DIAMOND_SWORD, 1));
        inv.setItem(2, st(Items.TORCH, 23));
        inv.setItem(3, st(Items.BREAD, 9));
        inv.setItem(4, st(Items.COBBLESTONE, 64));
        Item[] items = {Items.COAL, Items.RAW_COPPER, Items.DIRT, Items.IRON_INGOT, Items.COBBLESTONE, Items.OAK_LOG, Items.RAW_IRON,
            Items.ROTTEN_FLESH, Items.DIRT, Items.COAL, Items.ARROW, Items.BONE, Items.GRAVEL, Items.OAK_LOG, Items.IRON_INGOT, Items.STRING};
        int[] counts = {9, 14, 22, 5, 37, 8, 11, 6, 40, 18, 12, 3, 7, 21, 4, 5};
        int[] slots = {9, 11, 12, 14, 17, 19, 20, 22, 24, 25, 27, 28, 30, 32, 33, 35};
        for (int i = 0; i < items.length; i++) inv.setItem(slots[i], st(items[i], counts[i]));
    }

    private static void sort(Script s) {
        setInventory(s, Showcase::messyInventory);
        setChest(s, Showcase::messyChest);
        openChest(s);
        record(s, new Director.Timeline("sort", "Middle-click sorts. Instantly.", 4400)
            .crop(Showcase::panelCrop)
            .move(0, 0, beside(12, 40))
            .move(150, 750, chestSlot(13))
            .click(950, MIDDLE, 0, "Middle click")
            .move(1900, 2450, invSlot(22))
            .click(2650, MIDDLE, 0, "Middle click"));
        close(s);
    }

    private static void open(Script s) {
        s.run(mc -> mc.gui.hud.getChat().clearMessages(false));
        record(s, new Director.Timeline("open", "Inventories pop open over a soft blur", 3900)
            .crop(Showcase::wideCrop).noCursor().captionTop().stride(2)
            .at(400, "E", mc -> mc.gui.setScreen(new InventoryScreen(mc.player)))
            .at(1600, null, mc -> mc.gui.setScreen(null))
            .at(2200, null, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(chest).add(0, 0, 0.5), Direction.SOUTH, chest, false)))
            .at(3500, null, mc -> mc.player.closeContainer()));
        s.waitTicks(10);
    }

    private static void moves(Script s) {
        setInventory(s, inv -> {
            inv.setItem(0, st(Items.DIAMOND_PICKAXE, 1));
            inv.setItem(10, st(Items.OAK_LOG, 32));
            inv.setItem(11, st(Items.COBBLESTONE, 64));
            inv.setItem(12, st(Items.RAW_IRON, 18));
            inv.setItem(15, st(Items.DIAMOND, 12));
        });
        setChest(s, c -> {
            c.setItem(0, st(Items.OAK_PLANKS, 20));
            c.setItem(1, st(Items.STICK, 9));
        });
        openChest(s);
        record(s, new Director.Timeline("moves", "Shift-click or scroll: items fly, no waiting", 4700)
            .crop(Showcase::panelCrop)
            .move(0, 0, beside(12, 120))
            .move(100, 500, invSlot(10))
            .click(600, LEFT, SHIFT, "Shift + click")
            .move(700, 950, invSlot(11))
            .click(1050, LEFT, SHIFT, "Shift + click")
            .move(1150, 1400, invSlot(12))
            .click(1500, LEFT, SHIFT, "Shift + click")
            .move(1800, 2200, invSlot(15))
            .scroll(2400, -1, "Scroll")
            .scroll(2650, -1, "Scroll")
            .scroll(2900, -1, "Scroll")
            .scroll(3400, 1, "Scroll back")
            .scroll(3650, 1, "Scroll back"));
        close(s);
    }

    private static void drag(Script s) {
        setInventory(s, inv -> {
            Item[] row = {Items.COBBLESTONE, Items.DIRT, Items.OAK_LOG, Items.COAL, Items.RAW_IRON, Items.GRAVEL, Items.ANDESITE,
                Items.FLINT, Items.BONE};
            for (int i = 0; i < 9; i++) inv.setItem(18 + i, st(row[i], 8 + i * 5));
            inv.setItem(0, st(Items.DIAMOND_PICKAXE, 1));
        });
        setChest(s, c -> {});
        openChest(s);
        record(s, new Director.Timeline("drag", "Hold Shift and drag to move a whole row", 2700)
            .crop(Showcase::panelCrop)
            .move(0, 0, beside(12, 100))
            .move(100, 600, invSlot(18))
            .press(750, LEFT, SHIFT, "Shift + drag")
            .move(800, 1700, invSlot(26))
            .release(1800));
        close(s);
    }

    private static void locks(Script s) {
        setInventory(s, inv -> {
            messyInventory(inv);
            inv.setItem(13, st(Items.DIAMOND, 16));
        });
        s.run(mc -> mc.gui.setScreen(new InventoryScreen(mc.player))).waitTicks(20);
        Consumer<Minecraft> altOn = mc -> Compat.forceHeld = Keys.LOCK, altOff = mc -> Compat.forceHeld = null;
        record(s, new Director.Timeline("locks", "Alt-click to lock. Locked items stay put.", 5200)
            .crop(Showcase::panelCrop)
            .move(0, 0, beside(12, 120))
            .move(100, 600, invSlot(0))
            .at(700, null, altOn).click(720, LEFT, 0, "Alt + click").at(740, null, altOff)
            .move(1000, 1500, invSlot(13))
            .at(1600, null, altOn).click(1620, LEFT, 0, "Alt + click").at(1640, null, altOff)
            .move(2000, 2400, invSlot(0))
            .click(2550, LEFT, SHIFT, "Locked!")
            .move(3000, 3400, invSlot(25))
            .click(3550, MIDDLE, 0, "Middle click"));
        s.run(mc -> {
            if (SlotLocks.isLocked(0)) SlotLocks.toggle(0, ItemStack.EMPTY);
            if (SlotLocks.isLocked(13)) SlotLocks.toggle(13, ItemStack.EMPTY);
        });
        close(s);
    }

    private static void tools(Script s) {
        setInventory(s, inv -> {
            inv.setItem(0, st(Items.DIAMOND_PICKAXE, 1));
            inv.setItem(2, st(Items.TORCH, 6));
            inv.setItem(9, st(Items.COBBLESTONE, 40));
            inv.setItem(12, st(Items.DIRT, 25));
            inv.setItem(16, st(Items.DIAMOND, 7));
            inv.setItem(20, st(Items.COBBLESTONE, 22));
            inv.setItem(23, st(Items.GRANITE, 14));
        });
        setChest(s, c -> {
            c.setItem(0, st(Items.COBBLESTONE, 10));
            c.setItem(1, st(Items.DIRT, 3));
            c.setItem(2, st(Items.GRANITE, 2));
            c.setItem(9, st(Items.TORCH, 64));
            c.setItem(10, st(Items.OAK_LOG, 30));
            c.setItem(11, st(Items.SPRUCE_LOG, 18));
            c.setItem(12, st(Items.BIRCH_LOG, 9));
            c.setItem(18, st(Items.OAK_PLANKS, 40));
        });
        openChest(s);
        record(s, new Director.Timeline("tools", "Deposit, restock and search in one click", 5600)
            .crop(Showcase::panelCrop)
            .move(0, 0, beside(12, 60))
            .move(150, 650, button("deposit"))
            .click(800, LEFT, 0, "Deposit matching")
            .move(1500, 1800, button("restock"))
            .click(1950, LEFT, 0, "Restock")
            .move(2500, 2900, chestSlot(22))
            .at(3100, "Ctrl + F", mc -> ((AbstractContainerScreen<?>) mc.gui.screen())
                .keyPressed(new KeyEvent(InputConstants.KEY_F, 0, InputConstants.MOD_CONTROL)))
            .at(3500, null, mc -> type(mc, "l"))
            .at(3650, null, mc -> type(mc, "o"))
            .at(3800, null, mc -> type(mc, "g")));
        close(s);
    }

    private static void type(Minecraft mc, String ch) {
        if (mc.gui.screen().getFocused() instanceof EditBox box) box.insertText(ch);
    }

    private static void refill(Script s) {
        setInventory(s, inv -> {
            inv.setItem(0, st(Items.COBBLESTONE, 4));
            inv.setItem(1, st(Items.DIAMOND_PICKAXE, 1));
            inv.setItem(2, st(Items.TORCH, 16));
            inv.setItem(20, st(Items.COBBLESTONE, 64));
        });
        s.run(mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            float[] yp = lookAt(mc, Vec3.atCenterOf(origin.offset(-3, -1, -2)).add(0, 0.8, 0));
            mc.player.setYRot(yp[0]);
            mc.player.setXRot(yp[1]);
        }).waitTicks(10);
        Director.Timeline t = new Director.Timeline("refill", "Run out of blocks? Your hotbar refills itself.", 5300)
            .crop(Showcase::wideCrop).noCursor().captionTop().stride(2);
        for (int i = 0; i < 7; i++) {
            int n = i;
            double at = 400 + i * 620;
            t.at(at, null, mc -> {
                BlockPos f = origin.offset(1 - n, -1, -2);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(f).add(0, 0.5, 0), Direction.UP, f, false));
                Compat.swing();
            });
        }
        record(s, t);
        s.run(Showcase::faceChest);
    }

    private static void protect(Script s) {
        s.server(server -> {
            for (int dx = -6; dx <= 2; dx++) server.overworld().setBlock(origin.offset(dx, 0, -2), Blocks.AIR.defaultBlockState(), 3);
        });
        setInventory(s, inv -> {
            ItemStack worn = st(Items.DIAMOND_PICKAXE, 1);
            worn.set(DataComponents.DAMAGE, worn.getMaxDamage() - 2);
            inv.setItem(0, st(Items.COBBLESTONE, 32));
            inv.setItem(1, worn);
            inv.setItem(2, st(Items.TORCH, 16));
            inv.setItem(22, st(Items.DIAMOND_PICKAXE, 1));
        });
        s.run(mc -> {
            mc.player.getInventory().setSelectedSlot(1);
            float[] yp = lookAt(mc, Vec3.atCenterOf(origin.offset(4, 0, -3)).add(0, 0.5, 0.5));
            mc.player.setYRot(yp[0]);
            mc.player.setXRot(yp[1]);
        }).waitTicks(10);
        List<BlockPos> targets = new ArrayList<>();
        for (int dy = 1; dy >= 0; dy--) for (int dx = 3; dx <= 5; dx++) targets.add(new BlockPos(0, dy, 0).offset(dx, 0, -3));
        int[] cur = {0};
        boolean[] started = {false};
        Director.Timeline t = new Director.Timeline("protect", "Tools stop before they break", 4600)
            .crop(Showcase::wideCrop).noCursor().captionTop().stride(2)
            .everyTick(mc -> {
                if (cur[0] >= targets.size()) return;
                BlockPos pos = origin.offset(targets.get(cur[0]));
                if (mc.level.getBlockState(pos).isAir()) {
                    cur[0]++;
                    started[0] = false;
                    return;
                }
                if (!started[0]) started[0] = mc.gameMode.startDestroyBlock(pos, Direction.SOUTH);
                else mc.gameMode.continueDestroyBlock(pos, Direction.SOUTH);
                Compat.swing();
            });
        record(s, t);
        s.run(mc -> mc.gameMode.stopDestroyBlock()).run(Showcase::faceChest);
    }

    private static void hotbar(Script s) {
        setInventory(s, inv -> {
            Item[] bar = {Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TORCH, Items.COBBLESTONE, Items.BREAD,
                Items.WATER_BUCKET, Items.OAK_LOG, Items.SHIELD};
            for (int i = 0; i < 9; i++) inv.setItem(i, st(bar[i], new ItemStack(bar[i]).getMaxStackSize() > 1 ? 32 : 1));
        });
        s.run(mc -> mc.player.getInventory().setSelectedSlot(0)).waitTicks(5);
        Director.Timeline t = new Director.Timeline("hotbar", "A hotbar that glides", 2600)
            .crop(mc -> {
                int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
                return new int[] {w / 2 - 100, h - 68, 200, 68};
            })
            .noCursor().captionTop();
        int[] order = {1, 2, 3, 4, 5, 6, 7, 8, 5, 2, 0};
        for (int i = 0; i < order.length; i++) {
            int slot = order[i];
            t.at(300 + i * 180, null, mc -> mc.player.getInventory().setSelectedSlot(slot));
        }
        record(s, t);
    }
}
