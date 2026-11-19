package com.oney.WebRTCModule;

import livekit.org.webrtc.GlRectDrawer;
import livekit.org.webrtc.RendererCommon;

/**
 * Wraps {@link GlRectDrawer} and re-frames each frame by composing a crop into the texture
 * matrix EglRenderer hands it. Work happens only inside draws EglRenderer already makes for
 * incoming frames: there is no timer, no copy and no allocation per frame. A transition
 * advances only when a video frame arrives; once settled the crop is computed once and reused.
 */
final class FaceReframeGlDrawer implements RendererCommon.GlDrawer {
    private final GlRectDrawer delegate = new GlRectDrawer();

    private volatile FaceReframeGeometry.Transition transition;
    private volatile float frameAspect;

    private final float[] window = new float[4];
    private final float[] crop = new float[4];
    private final float[] composed = new float[16];
    private FaceReframeGeometry.Transition cachedTransition;
    private float cachedFrameAspect;
    private boolean cachedSettled;
    private boolean cachedApplies;

    /** Any thread. {@code null} draws exactly what GlRectDrawer would. */
    void setTransition(FaceReframeGeometry.Transition transition) {
        this.transition = transition;
    }

    /** Any thread; called before EglRenderer draws the first frame of a new size or rotation. */
    void setFrameAspect(float frameAspect) {
        this.frameAspect = frameAspect;
    }

    @Override
    public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        delegate.drawOes(oesTextureId, reframe(texMatrix), frameWidth, frameHeight,
                viewportX, viewportY, viewportWidth, viewportHeight);
    }

    @Override
    public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        delegate.drawRgb(textureId, reframe(texMatrix), frameWidth, frameHeight,
                viewportX, viewportY, viewportWidth, viewportHeight);
    }

    @Override
    public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        delegate.drawYuv(yuvTextures, reframe(texMatrix), frameWidth, frameHeight,
                viewportX, viewportY, viewportWidth, viewportHeight);
    }

    @Override
    public void release() {
        delegate.release();
    }

    /** Render thread only. */
    private float[] reframe(float[] texMatrix) {
        FaceReframeGeometry.Transition current = transition;
        float aspect = frameAspect;
        if (current == null) {
            return texMatrix;
        }
        if (!cachedSettled || current != cachedTransition || aspect != cachedFrameAspect) {
            cachedSettled = FaceReframeGeometry.windowAt(current, System.nanoTime(), window);
            cachedApplies = FaceReframeGeometry.cropFor(window, aspect, current.boundsAspect, crop);
            cachedTransition = current;
            cachedFrameAspect = aspect;
        }
        if (!cachedApplies) {
            return texMatrix;
        }
        FaceReframeGeometry.compose(texMatrix, crop, composed);
        return composed;
    }
}
