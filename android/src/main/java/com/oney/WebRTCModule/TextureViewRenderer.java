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
    // Written from UI thread, read from EglRenderer thread (onFrame) — volatile avoids locking.
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
        // Reset cached frame dims so the next first-frame fires onFrameResolutionChanged
        // even when the new stream has the same dims as the previous one. Without this, a
        // camera swap with matching dims (e.g. front→back→front, both 720x1280) silently
        // skips the resolution-change event and the host view keeps stale layout bounds.
        rotatedFrameWidth = 0;
        rotatedFrameHeight = 0;
        frameRotation = 0;

        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            // Pin producer buffer to view dims so EGL surface is created at the right aspect.
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                surfaceTexture.setDefaultBufferSize(w, h);
            }
            eglRenderer.createEglSurface(surfaceTexture);
        }
    }

    public void release() {
        if (isInitialized) {
            // Flip flag before tearing down — EglRenderer's internal lock handles in-flight frames.
            isInitialized = false;
            rendererEvents = null;
            eglRenderer.release();
        }
    }

    public void setMirror(boolean mirror) {
        eglRenderer.setMirror(mirror);
    }

    public void setScalingType(ScalingType scalingType) {
        // No-op: WebRTCView.onLayout sizes the TextureView to match frame aspect, so the
        // renderer's stretch-to-fill behavior produces correct output. Logged in case a
        // future change makes scaling control here actually meaningful.
        Log.d(TAG, "setScalingType(" + scalingType + ") is a no-op — scaling handled by host layout");
    }

    public void clearImage() {
        eglRenderer.clearImage();
    }

    // VideoSink implementation
    @Override
    public void onFrame(VideoFrame videoFrame) {
        if (!isInitialized) {
            return;
        }
        eglRenderer.onFrame(videoFrame);

        // Snapshot — concurrent release() can null rendererEvents between check and use.
        RendererEvents events = rendererEvents;

        if (!isFirstFrameRendered) {
            isFirstFrameRendered = true;
            if (events != null) {
                events.onFirstFrameRendered();
            }
        }

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
            surfaceTexture.setDefaultBufferSize(width, height);
            eglRenderer.createEglSurface(surfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (!isInitialized || width <= 0 || height <= 0) {
            return;
        }
        // TextureView does not auto-resize the SurfaceTexture buffer or the EGL viewport
        // on layout changes (unlike SurfaceView). Re-pin buffer + recreate EGL surface so
        // EglRenderer's per-frame eglBase.surfaceWidth/Height read returns new dims.
        surfaceTexture.setDefaultBufferSize(width, height);
        eglRenderer.releaseEglSurface(() -> {
            if (isInitialized) {
                eglRenderer.createEglSurface(surfaceTexture);
            }
        });
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (isInitialized) {
            // Return false = we own the SurfaceTexture, release async after EGL teardown.
            eglRenderer.releaseEglSurface(() -> surfaceTexture.release());
            return false;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
