package com.oney.WebRTCModule.videoEffects;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A VideoFrameProcessor that can capture a single video frame on demand
 * and save it as a JPEG image to disk.
 *
 * Usage:
 *   1. Register via ProcessorProvider.addProcessor("frameCapture", FrameCaptureProcessor::new)
 *   2. Add to video effects chain via mediaStreamTrackSetVideoEffects
 *   3. Call captureNextFrame(outputPath) to capture the next frame
 *
 * Alternatively, use it standalone by calling captureFrame() from any VideoFrame source.
 */
public class FrameCaptureProcessor implements VideoFrameProcessor {
    private static final String TAG = "FrameCaptureProcessor";

    private final AtomicReference<CaptureRequest> pendingCapture = new AtomicReference<>(null);

    public interface CaptureCallback {
        void onCaptureComplete(String filePath);
        void onCaptureError(String error);
    }

    private static class CaptureRequest {
        final String outputPath;
        final int quality;
        final CaptureCallback callback;

        CaptureRequest(String outputPath, int quality, CaptureCallback callback) {
            this.outputPath = outputPath;
            this.quality = quality;
            this.callback = callback;
        }
    }

    /**
     * Request capturing the next video frame and saving it to the given path.
     *
     * @param outputPath Full file path where the JPEG will be saved
     * @param quality    JPEG quality (1-100)
     * @param callback   Callback for success/error notification
     */
    public void captureNextFrame(String outputPath, int quality, CaptureCallback callback) {
        CaptureRequest request = new CaptureRequest(outputPath, quality, callback);
        if (!pendingCapture.compareAndSet(null, request)) {
            callback.onCaptureError("A capture is already pending");
        }
    }

    /**
     * Convenience overload with default quality of 90.
     */
    public void captureNextFrame(String outputPath, CaptureCallback callback) {
        captureNextFrame(outputPath, 90, callback);
    }

    @Override
    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper) {
        CaptureRequest request = pendingCapture.getAndSet(null);
        if (request != null) {
            saveFrameToFile(frame, request);
        }
        // Pass through the frame unchanged - this processor is non-destructive
        return frame;
    }

    /**
     * Static utility: capture any VideoFrame to a file without needing the processor pipeline.
     * Can be called from any place that has access to a VideoFrame.
     */
    public static void captureFrame(VideoFrame frame, String outputPath, int quality, CaptureCallback callback) {
        try {
            Bitmap bitmap = videoFrameToBitmap(frame);
            saveBitmapToFile(bitmap, outputPath, quality);
            bitmap.recycle();
            callback.onCaptureComplete(outputPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture frame", e);
            callback.onCaptureError(e.getMessage());
        }
    }

    private void saveFrameToFile(VideoFrame frame, CaptureRequest request) {
        try {
            Bitmap bitmap = videoFrameToBitmap(frame);
            saveBitmapToFile(bitmap, request.outputPath, request.quality);
            bitmap.recycle();
            Log.d(TAG, "Frame captured to: " + request.outputPath);
            if (request.callback != null) {
                request.callback.onCaptureComplete(request.outputPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture frame", e);
            if (request.callback != null) {
                request.callback.onCaptureError(e.getMessage());
            }
        }
    }

    /**
     * Converts a WebRTC VideoFrame to an Android Bitmap.
     * Handles both texture-based and I420 buffer frames.
     */
    private static Bitmap videoFrameToBitmap(VideoFrame frame) {
        VideoFrame.I420Buffer i420Buffer = frame.getBuffer().toI420();

        int width = i420Buffer.getWidth();
        int height = i420Buffer.getHeight();

        // Convert I420 to ARGB
        ByteBuffer argbBuffer = ByteBuffer.allocateDirect(width * height * 4);

        // Use YuvHelper to convert I420 -> ARGB
        // We need to do I420 -> NV12 -> ARGB manually or use shader
        // Simpler approach: manual YUV to RGB conversion
        ByteBuffer yBuffer = i420Buffer.getDataY();
        ByteBuffer uBuffer = i420Buffer.getDataU();
        ByteBuffer vBuffer = i420Buffer.getDataV();
        int yStride = i420Buffer.getStrideY();
        int uStride = i420Buffer.getStrideU();
        int vStride = i420Buffer.getStrideV();

        int[] argbArray = new int[width * height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int yIndex = row * yStride + col;
                int uvRow = row / 2;
                int uvCol = col / 2;
                int uIndex = uvRow * uStride + uvCol;
                int vIndex = uvRow * vStride + uvCol;

                int y = yBuffer.get(yIndex) & 0xFF;
                int u = uBuffer.get(uIndex) & 0xFF;
                int v = vBuffer.get(vIndex) & 0xFF;

                // YUV to RGB conversion (BT.601)
                int r = (int) (y + 1.402 * (v - 128));
                int g = (int) (y - 0.344136 * (u - 128) - 0.714136 * (v - 128));
                int b = (int) (y + 1.772 * (u - 128));

                r = clamp(r, 0, 255);
                g = clamp(g, 0, 255);
                b = clamp(b, 0, 255);

                argbArray[row * width + col] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        i420Buffer.release();

        Bitmap bitmap = Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888);

        // Apply rotation if needed
        int rotation = frame.getRotation();
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            bitmap.recycle();
            return rotated;
        }

        return bitmap;
    }

    private static void saveBitmapToFile(Bitmap bitmap, String outputPath, int quality) throws IOException {
        File file = new File(outputPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            Bitmap.CompressFormat format;
            if (outputPath.toLowerCase().endsWith(".png")) {
                format = Bitmap.CompressFormat.PNG;
            } else {
                format = Bitmap.CompressFormat.JPEG;
            }
            bitmap.compress(format, quality, fos);
            fos.flush();
        }
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
