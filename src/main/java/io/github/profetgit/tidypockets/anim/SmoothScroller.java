package io.github.profetgit.tidypockets.anim;

/** Implemented by scroll areas (through a mixin) so rendering can advance their eased scroll position. */
public interface SmoothScroller {
    void tidypockets$step();
}
