package com.oney.WebRTCModule.videoEffects;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * SPIKE: runs MediaPipe selfie segmentation through a TFLite interpreter whose
 * GPU delegate is FORCED to the OpenGL ES backend (not OpenCL).
 *
 * Why: on Imagination PowerVR (Pixel 10 / Tensor G5) the OpenCL inference path
 * the TSVB SDK uses produces no / garbage output (black screen). TSVB bakes the
 * backend choice into a native subgraph we cannot flip. Here we run our own
 * interpreter where forcing the GL backend is a one-liner, to answer a single
 * question on-device: does the GL backend yield a valid mask on this GPU?
 *
 * It reports mask statistics only — no compositing.
 *
 * Must be constructed and used on the SAME thread (GL delegate context affinity).
 */
public class SelfieSegmenterGl {
    private static final String TAG = "PowerVrSegSpike";
    private static final String MODEL_ASSET = "selfie_segmenter.tflite";

    private Interpreter interpreter;
    private GpuDelegate delegate;
    private int inW = 256, inH = 256, inC = 3;
    private int outLen = 256 * 256;
    private ByteBuffer inputBuffer;
    private ByteBuffer outputBuffer;
    private boolean ready = false;

    public SelfieSegmenterGl(Context context) {
        try {
            ByteBuffer model = loadAsset(context, MODEL_ASSET);

            GpuDelegateFactory.Options opts = new GpuDelegateFactory.Options();
            // The whole point of the spike: force OpenGL ES, never OpenCL.
            opts.setForceBackend(GpuDelegateFactory.Options.GpuBackend.OPENGL);
            delegate = new GpuDelegate(opts);

            Interpreter.Options io = new Interpreter.Options();
            io.addDelegate(delegate);
            interpreter = new Interpreter(model, io);

            int[] inShape = interpreter.getInputTensor(0).shape(); // [1,H,W,C]
            inH = inShape[1];
            inW = inShape[2];
            inC = inShape[3];
            int[] outShape = interpreter.getOutputTensor(0).shape();
            outLen = 1;
            for (int d : outShape) {
                outLen *= d;
            }

            inputBuffer = ByteBuffer.allocateDirect(inW * inH * inC * 4).order(ByteOrder.nativeOrder());
            outputBuffer = ByteBuffer.allocateDirect(outLen * 4).order(ByteOrder.nativeOrder());

            ready = true;
            Log.i(TAG, "Segmenter ready (GL-forced). input=" + inW + "x" + inH + "x" + inC + " outLen=" + outLen);
        } catch (Throwable t) {
            Log.e(TAG, "Segmenter init FAILED (GL delegate could not be created?) — " + t, t);
        }
    }

    public boolean isReady() {
        return ready;
    }

    public int inputWidth() {
        return inW;
    }

    public int inputHeight() {
        return inH;
    }

    /**
     * Runs inference on a model-sized ARGB frame (length inW*inH) and logs mask stats.
     * A valid mask (GL works) => non-zero coverage, max near 1, zero NaN.
     * An invalid mask (GL is not a fix) => all zero or NaN.
     */
    public void runAndLog(int[] argb) {
        if (!ready) {
            return;
        }
        try {
            inputBuffer.rewind();
            int px = inW * inH;
            for (int i = 0; i < px; i++) {
                int p = argb[i];
                inputBuffer.putFloat(((p >> 16) & 0xFF) / 255f); // R
                inputBuffer.putFloat(((p >> 8) & 0xFF) / 255f);  // G
                inputBuffer.putFloat((p & 0xFF) / 255f);         // B
            }
            inputBuffer.rewind();
            outputBuffer.rewind();

            long t0 = System.nanoTime();
            interpreter.run(inputBuffer, outputBuffer);
            long ms = (System.nanoTime() - t0) / 1_000_000;

            outputBuffer.rewind();
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, sum = 0;
            int nan = 0, fg = 0;
            for (int i = 0; i < outLen; i++) {
                float v = outputBuffer.getFloat();
                if (Float.isNaN(v)) {
                    nan++;
                    continue;
                }
                if (v < min) min = v;
                if (v > max) max = v;
                sum += v;
                if (v > 0.5f) fg++;
            }
            float mean = sum / Math.max(1, outLen - nan);
            float coverage = 100f * fg / outLen;
            Log.i(TAG, String.format(
                    "MASK GL-forced: min=%.3f max=%.3f mean=%.3f coverage=%.1f%% nan=%d run=%dms",
                    min, max, mean, coverage, nan, ms));
            boolean valid = nan == 0 && max > 0.5f && coverage > 0.5f && coverage < 99.5f;
            Log.i(TAG, valid
                    ? ">>> RESULT: GL BACKEND WORKS on this GPU (valid silhouette mask) <<<"
                    : ">>> RESULT: GL mask INVALID (zero/NaN/uniform) — GL is likely NOT the fix <<<");
        } catch (Throwable t) {
            Log.e(TAG, "Inference FAILED — " + t, t);
        }
    }

    public void close() {
        try {
            if (interpreter != null) {
                interpreter.close();
            }
            if (delegate != null) {
                delegate.close();
            }
        } catch (Throwable ignored) {
        }
        ready = false;
    }

    private static ByteBuffer loadAsset(Context ctx, String name) throws Exception {
        InputStream is = ctx.getAssets().open(name);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(1024, is.available()));
            byte[] chunk = new byte[16 * 1024];
            int n;
            while ((n = is.read(chunk)) > 0) {
                bos.write(chunk, 0, n);
            }
            byte[] bytes = bos.toByteArray();
            ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
            bb.put(bytes);
            bb.rewind();
            return bb;
        } finally {
            is.close();
        }
    }
}
