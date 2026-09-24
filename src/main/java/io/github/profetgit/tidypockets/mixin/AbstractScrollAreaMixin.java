package io.github.profetgit.tidypockets.mixin;

import io.github.profetgit.tidypockets.anim.Ease;
import io.github.profetgit.tidypockets.anim.SmoothScroller;
import io.github.profetgit.tidypockets.config.Conflicts;
import io.github.profetgit.tidypockets.config.TidyConfig;
import net.minecraft.client.gui.components.AbstractScrollArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wheel scrolling sets a target; each frame the list eases toward it. Dragging the bar or code jumps as before. */
@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin implements SmoothScroller {
    @Shadow
    public abstract double scrollAmount();

    @Shadow
    public abstract void setScrollAmount(double amount);

    @Shadow
    public abstract int maxScrollAmount();

    @Shadow
    protected abstract double scrollRate();

    @Unique
    private double tidypockets$target = Double.NaN, tidypockets$set = Double.NaN, tidypockets$time;

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void tidypockets$scroll(double x, double y, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
        if (!TidyConfig.get().smoothScroll || Conflicts.scroll || !Ease.enabled() || !((AbstractScrollArea) (Object) this).visible) return;
        double base = Double.isNaN(tidypockets$target) ? scrollAmount() : tidypockets$target;
        tidypockets$target = Math.clamp(base - sy * scrollRate(), 0, maxScrollAmount());
        tidypockets$set = scrollAmount();
        tidypockets$time = Ease.now();
        cir.setReturnValue(true);
    }

    @Override
    public void tidypockets$step() {
        if (Double.isNaN(tidypockets$target)) return;
        double cur = scrollAmount();
        if (Math.abs(cur - tidypockets$set) > 0.5) {
            tidypockets$target = Double.NaN;
            return;
        }
        double now = Ease.now(), dt = now - tidypockets$time;
        tidypockets$time = now;
        double k = 1 - Math.exp(-dt * TidyConfig.get().animSpeed / 45.0);
        double next = cur + (tidypockets$target - cur) * k;
        if (Math.abs(tidypockets$target - next) < 0.3) {
            next = tidypockets$target;
            tidypockets$target = Double.NaN;
        }
        setScrollAmount(next);
        tidypockets$set = scrollAmount();
    }
}
