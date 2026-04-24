package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.RendererEvents;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * A TextureView-based video renderer for WebRTC.
 * Unlike SurfaceViewRenderer, TextureView renders in the normal View hierarchy
 * and supports borderRadius, overflow:hidden, alpha, and other standard View operations.
 *
 * Uses EglRenderer internally for OpenGL rendering to the TextureView's SurfaceTexture.
 */
public class TextureViewRenderer extends TextureView
        implements TextureView.SurfaceTextureListener, VideoSink {

    private static final String TAG = "TextureViewRenderer";

    private final EglRenderer eglRenderer;
    // All fields below are written from the UI thread (init/release/SurfaceTextureListener
    // callbacks) and read from the EglRenderer thread (onFrame). volatile gives the necessary
    // cross-thread visibility without taking a lock during heavy GL calls.
    private volatile RendererEvents rendererEvents;
    private volatile boolean isInitialized = false;
    private volatile boolean isFirstFrameRendered = false;
    private volatile int rotatedFrameWidth;
    private volatile int rotatedFrameHeight;
    private volatile int frameRotation;

    public TextureViewRenderer(Context context) {
        super(context);
        eglRenderer = new EglRenderer(TAG);
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    public void init(EglBase.Context sharedContext, RendererEvents rendererEvents) {
        init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new org.webrtc.GlRectDrawer());
    }

    public void init(EglBase.Context sharedContext, RendererEvents rendererEvents,
                     int[] configAttributes, RendererCommon.GlDrawer drawer) {
        this.rendererEvents = rendererEvents;
        eglRenderer.init(sharedContext, configAttributes, drawer);
        isInitialized = true;
        isFirstFrameRendered = false;

        // If the SurfaceTexture is already available, create the EGL surface now
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            eglRenderer.createEglSurface(surfaceTexture);
        }
    }

    public void release() {
        if (isInitialized) {
            // Flip flag FIRST so any in-flight onFrame on the EglRenderer thread that passes
            // its `isInitialized` snapshot still operates on a renderer that's about to be
            // released — EglRenderer's internal lock serializes that final frame safely.
            isInitialized = false;
            rendererEvents = null;
            eglRenderer.release();
        }
    }

    public void setMirror(boolean mirror) {
        eglRenderer.setMirror(mirror);
    }

    public void setScalingType(ScalingType scalingType) {
        // Scaling is handled by WebRTCView.onLayout which sizes the TextureView
        // appropriately for cover/contain. EglRenderer stretches to fill, which
        // is correct since onLayout already computed the right bounds. External
        // callers expecting per-renderer scaling control will get a no-op — log
        // so the silent disagreement is at least visible in logcat.
        Log.d(TAG, "setScalingType(" + scalingType + ") is a no-op — scaling handled by host layout");
    }

    public void clearImage() {
        eglRenderer.clearImage();
    }

    // VideoSink implementation
    @Override
    public void onFrame(VideoFrame videoFrame) {
        // Snapshot once — release() can flip isInitialized=false from the UI thread mid-call;
        // if we passed that snapshot, the EglRenderer is still operating on its own thread and
        // will accept the frame (it has its own internal lock for the actual GL ops).
        if (!isInitialized) {
            return;
        }
        eglRenderer.onFrame(videoFrame);

        // Capture snapshot so a concurrent release() that nulls rendererEvents can't NPE us
        // between the null-check and the call.
        RendererEvents events = rendererEvents;

        if (!isFirstFrameRendered) {
            isFirstFrameRendered = true;
            if (events != null) {
                events.onFirstFrameRendered();
            }
        }

        // Check for resolution changes
        int rotation = videoFrame.getRotation();
        int width = (rotation % 180 == 0)
                ? videoFrame.getRotatedWidth()
                : videoFrame.getRotatedHeight();
        int height = (rotation % 180 == 0)
                ? videoFrame.getRotatedHeight()
                : videoFrame.getRotatedWidth();

        if (width != rotatedFrameWidth || height != rotatedFrameHeight || rotation != frameRotation) {
            rotatedFrameWidth = width;
            rotatedFrameHeight = height;
            frameRotation = rotation;
            if (events != null) {
                events.onFrameResolutionChanged(
                        videoFrame.getBuffer().getWidth(),
                        videoFrame.getBuffer().getHeight(),
                        rotation);
            }
        }
    }

    // TextureView.SurfaceTextureListener implementation
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (isInitialized) {
            eglRenderer.createEglSurface(surfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        // EglRenderer handles size changes through the layout mechanism
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (isInitialized) {
            // Return false = we take ownership of the SurfaceTexture and release it
            // asynchronously after EGL is done. This avoids blocking the UI thread.
            eglRenderer.releaseEglSurface(() -> surfaceTexture.release());
            return false;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // No-op
    }
}
