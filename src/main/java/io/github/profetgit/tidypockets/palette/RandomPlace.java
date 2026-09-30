package io.github.profetgit.tidypockets.palette;

import io.github.profetgit.tidypockets.Compat;
import io.github.profetgit.tidypockets.anim.HudAnims;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.core.Dice;
import io.github.profetgit.tidypockets.mixin.BlockItemInvoker;
import io.github.profetgit.tidypockets.refill.Refill;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rolls the die at the moment a block is about to be placed. With a palette slot selected, the selected hotbar slot is
 * switched to a random palette slot right before the game sends the click, exactly like pressing a number key.
 */
public final class RandomPlace {
    private static int ticks, rolledAt = -1000, lastSlot = -1;
    private static boolean bounce;

    /** Self-test only: replaces the dice roll with a fixed sequence. */
    public static DoubleSupplier rng;

    private RandomPlace() {}

    public static void tick() {
        ticks++;
    }

    /** True for a moment after a roll changed the held block, so the hand shows the new block at once. */
    public static boolean recentlyRolled() {
        return ticks - rolledAt <= 2;
    }

    /** True while the hand should stay raised after a roll: the swap cooldown lasts 5 ticks, so 6 covers it. */
    public static boolean handSteady() {
        return ticks - rolledAt <= 6;
    }

    /** Called at the start of every block click. Switches the selected slot when the palette says so. */
    public static void before(LocalPlayer p, InteractionHand hand, BlockHitResult hit) {
        bounce = false;
        TidyConfig cfg = TidyConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (hand != InteractionHand.MAIN_HAND || !cfg.randomEnabled || mc.level == null || mc.gui.screen() != null) return;
        Inventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        if (!Palette.has(sel)) return;
        if (!p.isSecondaryUseActive() && interactive(mc.level.getBlockState(hit.getBlockPos()))) return;

        List<Dice.Option> options = new ArrayList<>();
        for (int slot : Palette.slots()) {
            Dice.Option o = option(p, mc.level, hand, hit, slot);
            if (o != null) options.add(o);
        }
        if (options.isEmpty()) return;
        int pick = Dice.pick(options, cfg.randomSpread, rng != null ? rng.getAsDouble() : p.getRandom().nextDouble());
        if (pick < 0) return;
        lastSlot = pick;
        if (pick == sel) return;
        inv.setSelectedSlot(pick);
        rolledAt = ticks;
        bounce = true;
        Refill.retarget(pick);
        HudAnims.rolled(pick);
    }

    /** Called when the click is done. A rolled block still gets the little hand bounce vanilla gives a placement. */
    public static void after(InteractionHand hand, InteractionResult result) {
        if (bounce && result instanceof InteractionResult.Success) Compat.itemUsed(hand);
        bounce = false;
    }

    private static Dice.Option option(LocalPlayer p, Level level, InteractionHand hand, BlockHitResult hit, int slot) {
        ItemStack stack = p.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        BlockPlaceContext ctx = item.updatePlacementContext(new BlockPlaceContext(p, hand, stack, hit));
        if (ctx == null || !ctx.canPlace()) return null;
        BlockState state = ((BlockItemInvoker) item).tidypockets$placementState(ctx);
        if (state == null) return null;
        Block block = state.getBlock();
        BlockPos at = ctx.getClickedPos();
        int faces = 0, edges = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int axes = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (axes == 0 || axes == 3 || !level.getBlockState(at.offset(dx, dy, dz)).is(block)) continue;
                    if (axes == 1) faces++;
                    else edges++;
                }
            }
        }
        return new Dice.Option(slot, faces, edges, slot == lastSlot);
    }

    /**
     * Whether right-clicking this block does something of its own (chests, doors, buttons...) instead of placing. A
     * click like that must not swap the hand for nothing. Sneaking places anyway, as in vanilla.
     */
    public static boolean interactive(BlockState state) {
        return INTERACTIVE.get(state.getBlock().getClass());
    }

    private static final ClassValue<Boolean> INTERACTIVE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            // fences only react to leads
            if (FenceBlock.class.isAssignableFrom(type)) return false;
            // matched by signature, not name: the runtime names differ from the Mojang names on 1.21.x
            for (Class<?> c = type; c != null && c != BlockBehaviour.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.getReturnType() != InteractionResult.class) continue;
                    if (java.util.Arrays.equals(m.getParameterTypes(), new Class<?>[] {BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class})) return true;
                }
            }
            return false;
        }
    };
}
