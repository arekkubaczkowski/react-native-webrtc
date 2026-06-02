package com.oney.WebRTCModule.videoEffects;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;

/**
 * SPIKE processor. Passes every frame through UNCHANGED (no compositing, so it can
 * never cause a black screen), but every N-th frame runs GL-forced selfie
 * segmentation (see {@link SelfieSegmenterGl}) and logs the resulting mask stats.
 *
 * The segmenter runs on its OWN HandlerThread (not the WebRTC capture/GL thread) so
 * the TFLite GL delegate gets a clean EGL environment — otherwise a context clash
 * with WebRTC's GL context could fail the delegate and produce a false negative.
 *
 * Activate from JS: localCameraTrack._setVideoEffects(['powervr-gl-seg-spike']).
 * Read logcat tag "PowerVrSegSpike" on the device for the verdict.
 */
public class PowerVrSegmentationProcessor implements VideoFrameProcessor {
    private static final String TAG = "PowerVrSegSpike";
    public static final String NAME = "powervr-gl-seg-spike";
    private static final int EVERY_N_FRAMES = 30;
    private static final int SAMPLE = 256; // pre-downsample target; segmenter re-checks model size

    private final Context appContext;
    private final HandlerThread segThread;
    private final Handler segHandler;

    private SelfieSegmenterGl segmenter; // created on segThread (GL affinity)
    private boolean initTried = false;
    private boolean firstFrameLogged = false;
    private volatile boolean busy = false;
    private int frameCount = 0;

    public PowerVrSegmentationProcessor(Context context) {
        this.appContext = context.getApplicationContext();
        this.segThread = new HandlerThread("powervr-seg");
        this.segThread.start();
        this.segHandler = new Handler(segThread.getLooper());
    }

    /** Registers this processor so JS setVideoEffects(["powervr-gl-seg-spike"]) can activate it. */
    public static void register(Context context) {
        final Context app = context.getApplicationContext();
        ProcessorProvider.addProcessor(NAME, () -> new PowerVrSegmentationProcessor(app));
        Log.i(TAG, "Registered processor: " + NAME);
    }

    @Override
    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper) {
        try {
            if (!firstFrameLogged) {
                firstFrameLogged = true;
                Log.i(TAG, "first camera frame received — sampling segmentation every "
                        + EVERY_N_FRAMES + " frames");
            }
            if (!busy && (frameCount++ % EVERY_N_FRAMES == 0)) {
                // Extract pixels synchronously (frame is only valid during this call),
                // then hand the copy to the segmentation thread.
                final int[] argb = downsampleToArgb(frame, SAMPLE, SAMPLE);
                busy = true;
                segHandler.post(() -> {
                    try {
                        if (!initTried) {
                            initTried = true;
                            segmenter = new SelfieSegmenterGl(appContext); // on segThread
                        }
                        if (segmenter != null && segmenter.isReady()) {
                            segmenter.runAndLog(resizeArgb(argb, SAMPLE, SAMPLE,
                                    segmenter.inputWidth(), segmenter.inputHeight()));
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "segmentation run failed — " + t, t);
                    } finally {
                        busy = false;
                    }
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "frame sample failed — " + t, t);
            busy = false;
        }

        // Passthrough. VideoEffectProcessor releases BOTH the input and the returned
        // frame; returning the input means retaining once to balance the extra release.
        frame.retain();
        return frame;
    }

    /** Downsample a VideoFrame straight to dstW x dstH ARGB (BT.601 full-range YUV->RGB). */
    private static int[] downsampleToArgb(VideoFrame frame, int dstW, int dstH) {
        VideoFrame.I420Buffer i420 = frame.getBuffer().toI420();
        try {
            int w = i420.getWidth();
            int h = i420.getHeight();
            ByteBuffer Y = i420.getDataY();
            ByteBuffer U = i420.getDataU();
            ByteBuffer V = i420.getDataV();
            int sY = i420.getStrideY();
            int sU = i420.getStrideU();
            int sV = i420.getStrideV();

            int[] out = new int[dstW * dstH];
            for (int dy = 0; dy < dstH; dy++) {
                int sy = dy * h / dstH;
                int uvRow = sy / 2;
                for (int dx = 0; dx < dstW; dx++) {
                    int sx = dx * w / dstW;
                    int yv = Y.get(sy * sY + sx) & 0xFF;
                    int uv = (U.get(uvRow * sU + (sx / 2)) & 0xFF) - 128;
                    int vv = (V.get(uvRow * sV + (sx / 2)) & 0xFF) - 128;
                    int r = clamp((int) (yv + 1.370705f * vv));
                    int g = clamp((int) (yv - 0.337633f * uv - 0.698001f * vv));
                    int b = clamp((int) (yv + 1.732446f * uv));
                    out[dy * dstW + dx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
            return out;
        } finally {
            i420.release();
        }
    }

    /** Nearest-neighbour resize between two ARGB grids (handles model size != SAMPLE). */
    private static int[] resizeArgb(int[] src, int sw, int sh, int dw, int dh) {
        if (sw == dw && sh == dh) {
            return src;
        }
        int[] out = new int[dw * dh];
        for (int dy = 0; dy < dh; dy++) {
            int sy = dy * sh / dh;
            for (int dx = 0; dx < dw; dx++) {
                out[dy * dw + dx] = src[sy * sw + (dx * sw / dw)];
            }
        }
        return out;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
