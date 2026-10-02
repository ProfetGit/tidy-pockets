package io.github.profetgit.tidypockets.anim;

import io.github.profetgit.tidypockets.config.TidyConfig;

/** Time and easing. All durations are in milliseconds at speed 1 and scale with the animation speed option. */
public final class Ease {
    private Ease() {}

    private static double clock = System.nanoTime() / 1_000_000.0;

    /** Animation time in ms: the sum of the frames' real-time deltas, so a recorder that steps frames at a fixed
     * rate (lockstep capture) sees the animations at their real speed. In play it follows the wall clock. */
    public static double now() {
        return clock;
    }

    public static void frame(float realtimeDeltaTicks) {
        clock += realtimeDeltaTicks * 50.0;
    }

    public static boolean enabled() {
        return TidyConfig.get().animSpeed > 0;
    }

    /** Progress 0..1 of an animation that started at {@code start} and lasts {@code ms} at speed 1. */
    public static double progress(double start, double ms) {
        double speed = TidyConfig.get().animSpeed;
        if (speed <= 0) return 1;
        return Math.clamp((now() - start) * speed / ms, 0, 1);
    }

    public static double scaled(double ms) {
        double speed = TidyConfig.get().animSpeed;
        return speed <= 0 ? 0 : ms / speed;
    }

    public static double outCubic(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    public static double inCubic(double t) {
        return t * t * t;
    }

    /** Overshoots to about 1.1 before settling, for pops. */
    public static double outBack(double t) {
        double c1 = 1.9, c3 = c1 + 1;
        double u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }
}
