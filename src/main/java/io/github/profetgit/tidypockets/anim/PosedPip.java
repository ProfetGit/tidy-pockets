package io.github.profetgit.tidypockets.anim;

import org.joml.Matrix3x2fc;

/**
 * A picture-in-picture render state (3D book, entity, skin, banner in a GUI) that can take a pose.
 * Vanilla renders these into their own texture and blits it ignoring the GUI pose, so they would sit still while the
 * rest of the screen pops. {@code mixin.PictureInPictureStateMixin} implements this.
 */
public interface PosedPip {
    void tidypockets$pose(Matrix3x2fc pose);
}
