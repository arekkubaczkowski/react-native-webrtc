package com.oney.WebRTCModule;

/**
 * Geometry for re-framing a {@code SurfaceViewRenderer} by changing what the GL drawer samples
 * instead of resizing the surface. Free of Android types so it can be checked on a plain JVM.
 *
 * Coordinates:
 * - "window": the part of the displayed (rotated, mirrored-as-shown) frame visible inside the
 *   renderer bounds, as fractions of the frame, y pointing down.
 * - "output": GL texture coordinates of the drawn quad, [0, 1], y pointing up. EglRenderer's
 *   drawMatrix, then the frame rotation and the buffer transform, map output to texture.
 */
final class FaceReframeGeometry {
    private static final float IDENTITY_EPSILON = 1e-5f;

    private FaceReframeGeometry() {}

    /** Renderer bounds in container pixels plus the frame window those bounds show. */
    static final class Placement {
        final int left;
        final int top;
        final int right;
        final int bottom;
        final boolean hasWindow;
        final float windowX;
        final float windowY;
        final float windowWidth;
        final float windowHeight;

        Placement(int left, int top, int right, int bottom, boolean hasWindow,
                  float windowX, float windowY, float windowWidth, float windowHeight) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.hasWindow = hasWindow;
            this.windowX = windowX;
            this.windowY = windowY;
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
        }

        boolean sameBounds(Placement other) {
            return other != null
                    && left == other.left
                    && top == other.top
                    && right == other.right
                    && bottom == other.bottom;
        }

        boolean sameWindow(Placement other) {
            return other != null
                    && hasWindow == other.hasWindow
                    && windowX == other.windowX
                    && windowY == other.windowY
                    && windowWidth == other.windowWidth
                    && windowHeight == other.windowHeight;
        }
    }

    /**
     * Same integer bounds as the SurfaceView branch of {@code layoutWithCustomTransform} has always
     * produced, so the hard clip to the container is unchanged. The window is the slice of the
     * scaled, translated video rectangle that falls inside those bounds.
     */
    static Placement place(int containerWidth, int containerHeight, int baseWidth, int baseHeight,
                           float scale, float translateX, float translateY) {
        int scaledWidth = (int) (baseWidth * scale);
        int scaledHeight = (int) (baseHeight * scale);
        int centerX = containerWidth / 2;
        int centerY = containerHeight / 2;
        int offsetX = (int) (containerWidth * translateX);
        int offsetY = (int) (containerHeight * translateY);
        int rawLeft = centerX - scaledWidth / 2 + offsetX;
        int rawTop = centerY - scaledHeight / 2 + offsetY;
        int left = Math.max(0, rawLeft);
        int top = Math.max(0, rawTop);
        int right = Math.min(containerWidth, rawLeft + scaledWidth);
        int bottom = Math.min(containerHeight, rawTop + scaledHeight);

        if (scaledWidth <= 0 || scaledHeight <= 0 || right <= left || bottom <= top) {
            return new Placement(left, top, right, bottom, false, 0f, 0f, 1f, 1f);
        }
        return new Placement(left, top, right, bottom, true,
                (left - rawLeft) / (float) scaledWidth,
                (top - rawTop) / (float) scaledHeight,
                (right - left) / (float) scaledWidth,
                (bottom - top) / (float) scaledHeight);
    }

    /** One crop change. Immutable, so the render thread reads a consistent snapshot. */
    static final class Transition {
        final float fromX;
        final float fromY;
        final float fromWidth;
        final float fromHeight;
        final float toX;
        final float toY;
        final float toWidth;
        final float toHeight;
        final long startNs;
        final long durationNs;
        /** Aspect EglRenderer uses for its centered crop: renderer width / height as float. */
        final float boundsAspect;

        Transition(float fromX, float fromY, float fromWidth, float fromHeight,
                   float toX, float toY, float toWidth, float toHeight,
                   long startNs, long durationNs, float boundsAspect) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromWidth = fromWidth;
            this.fromHeight = fromHeight;
            this.toX = toX;
            this.toY = toY;
            this.toWidth = toWidth;
            this.toHeight = toHeight;
            this.startNs = startNs;
            this.durationNs = durationNs;
            this.boundsAspect = boundsAspect;
        }

        static Transition snap(Placement placement, float boundsAspect) {
            return new Transition(
                    placement.windowX, placement.windowY, placement.windowWidth, placement.windowHeight,
                    placement.windowX, placement.windowY, placement.windowWidth, placement.windowHeight,
                    0L, 0L, boundsAspect);
        }
    }

    /**
     * Writes the window presented at {@code nowNs} into {@code out} (x, y, width, height) and
     * returns whether the transition has settled. Ease-out cubic.
     */
    static boolean windowAt(Transition transition, long nowNs, float[] out) {
        long elapsed = nowNs - transition.startNs;
        if (transition.durationNs <= 0L || elapsed >= transition.durationNs) {
            out[0] = transition.toX;
            out[1] = transition.toY;
            out[2] = transition.toWidth;
            out[3] = transition.toHeight;
            return true;
        }
        float progress = elapsed <= 0L ? 0f : elapsed / (float) transition.durationNs;
        float remaining = 1f - progress;
        float eased = 1f - remaining * remaining * remaining;
        out[0] = transition.fromX + (transition.toX - transition.fromX) * eased;
        out[1] = transition.fromY + (transition.toY - transition.fromY) * eased;
        out[2] = transition.fromWidth + (transition.toWidth - transition.fromWidth) * eased;
        out[3] = transition.fromHeight + (transition.toHeight - transition.fromHeight) * eased;
        return false;
    }

    /**
     * Computes the output-space affine crop {scaleX, scaleY, offsetX, offsetY} that makes the
     * drawer show {@code window} instead of EglRenderer's centered crop. EglRenderer has already
     * folded that centered crop (scale sx, sy about 0.5) into the matrix the drawer receives, so
     * the window is expressed relative to it rather than applied on top of it.
     *
     * Returns false when nothing should change: invalid input, or a crop equal to identity.
     */
    static boolean cropFor(float[] window, float frameAspect, float boundsAspect, float[] out) {
        if (!(frameAspect > 0f) || !(boundsAspect > 0f)) {
            return false;
        }
        float sx;
        float sy;
        if (frameAspect > boundsAspect) {
            sx = boundsAspect / frameAspect;
            sy = 1f;
        } else {
            sx = 1f;
            sy = frameAspect / boundsAspect;
        }
        float x0 = Math.max(0f, window[0]);
        float y0 = Math.max(0f, window[1]);
        float x1 = Math.min(1f, window[0] + window[2]);
        float y1 = Math.min(1f, window[1] + window[3]);
        if (!(x1 > x0) || !(y1 > y0)) {
            return false;
        }
        float bottomUp = 1f - y1;
        float scaleX = (x1 - x0) / sx;
        float scaleY = (y1 - y0) / sy;
        float offsetX = 0.5f + (x0 - 0.5f) / sx;
        float offsetY = 0.5f + (bottomUp - 0.5f) / sy;
        if (Math.abs(scaleX - 1f) < IDENTITY_EPSILON
                && Math.abs(scaleY - 1f) < IDENTITY_EPSILON
                && Math.abs(offsetX) < IDENTITY_EPSILON
                && Math.abs(offsetY) < IDENTITY_EPSILON) {
            return false;
        }
        out[0] = scaleX;
        out[1] = scaleY;
        out[2] = offsetX;
        out[3] = offsetY;
        return true;
    }

    /**
     * out = texMatrix * crop, both column-major 4x4, where crop is the affine
     * [scaleX 0 0 offsetX; 0 scaleY 0 offsetY; 0 0 1 0; 0 0 0 1]. The SDK's per-frame
     * texMatrix stays the left factor, so rotation, mirroring and the buffer transform apply
     * after the crop exactly as before.
     */
    static void compose(float[] texMatrix, float[] crop, float[] out) {
        float scaleX = crop[0];
        float scaleY = crop[1];
        float offsetX = crop[2];
        float offsetY = crop[3];
        for (int row = 0; row < 4; row++) {
            float c0 = texMatrix[row];
            float c1 = texMatrix[4 + row];
            out[row] = c0 * scaleX;
            out[4 + row] = c1 * scaleY;
            out[8 + row] = texMatrix[8 + row];
            out[12 + row] = c0 * offsetX + c1 * offsetY + texMatrix[12 + row];
        }
    }
}
