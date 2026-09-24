package io.github.profetgit.tidypockets.protect;

import io.github.profetgit.tidypockets.anim.Anims;
import io.github.profetgit.tidypockets.config.TidyConfig;
import io.github.profetgit.tidypockets.core.Rules;
import io.github.profetgit.tidypockets.inv.PlayerInv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;

/**
 * Stops a tool from breaking. Called before each action that costs durability; when the action would break the
 * held tool it is cancelled and the tool is swapped for a fresh copy, or moved out of the hand, or (inventory full)
 * kept but blocked. The player keeps holding the button, so the next tick continues with the new tool.
 */
public final class ToolProtect {
    public enum Cost { BLOCK, ATTACK, USE }

    private static long lastWarn;

    private ToolProtect() {}

    /** Returns true if the action must be cancelled. */
    public static boolean check(InteractionHand hand, Cost kind) {
        TidyConfig cfg = TidyConfig.get();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (cfg.protect == TidyConfig.Protect.OFF || p == null || p.hasInfiniteMaterials() || mc.gui.screen() != null) return false;
        ItemStack s = p.getItemInHand(hand);
        if (!s.isDamageableItem()) return false;
        if (cfg.protect == TidyConfig.Protect.ENCHANTED && !s.isEnchanted()) return false;
        int cost = cost(s, kind);
        if (!Rules.wouldBreak(s.getMaxDamage(), s.getDamageValue(), cost, cfg.protectMargin)) return false;

        Inventory inv = p.getInventory();
        int slot = hand == InteractionHand.MAIN_HAND ? inv.getSelectedSlot() : Inventory.SLOT_OFFHAND;
        int button = hand == InteractionHand.MAIN_HAND ? slot : PlayerInv.OFFHAND_BUTTON;
        int fresh = Rules.pick(PlayerInv.candidates(p, s, slot), true, false, cost + cfg.protectMargin);
        String what;
        if (fresh >= 0) {
            PlayerInv.swapInto(p, fresh, button);
            what = "tidypockets.protect.swapped";
        } else {
            int empty = PlayerInv.firstEmptyMain(p);
            if (empty >= 0) {
                PlayerInv.swapInto(p, empty, button);
                what = "tidypockets.protect.stowed";
            } else {
                what = "tidypockets.protect.blocked";
            }
        }
        warn(mc, p, s, slot, what);
        return true;
    }

    private static int cost(ItemStack s, Cost kind) {
        return switch (kind) {
            case BLOCK -> {
                Tool t = s.get(DataComponents.TOOL);
                yield t == null ? 1 : t.damagePerBlock();
            }
            case ATTACK -> {
                Weapon w = s.get(DataComponents.WEAPON);
                yield w == null ? 1 : w.itemDamagePerAttack();
            }
            case USE -> 1;
        };
    }

    private static void warn(Minecraft mc, LocalPlayer p, ItemStack s, int slot, String key) {
        Anims.toolWarn(slot);
        long now = System.currentTimeMillis();
        if (now - lastWarn < 1500) return;
        lastWarn = now;
        p.sendOverlayMessage(Component.translatable(key, s.getHoverName(), s.getMaxDamage() - s.getDamageValue()));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK.value(), 1.8f, 0.35f));
    }
}
