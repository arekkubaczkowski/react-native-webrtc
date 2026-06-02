package com.oney.WebRTCModule.videoEffects;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * SPIKE: runs a segmentation model on the SAME frame through three TFLite backends and
 * compares them:
 *   - CPU                       -> ground-truth output (known good; upstream: "CPU works")
 *   - GPU delegate, OpenCL      -> the backend TSVB uses; expected to break on PowerVR DXT
 *   - GPU delegate, OpenGL ES   -> the candidate fix
 *
 * One device run confirms both cause and fix:
 *   - CPU valid + CL diverges/NaN + GL ~= CPU  => CL is the bug, forcing GL is the fix
 *   - GL diverges/NaN                          => GL also broken (CL<->GL interop) — not the fix
 *   - CL ~= CPU                                => bug not reproduced (driver/path differs)
 *
 * Model: deeplabv3_257_mv_gpu — the canonical TFLite GPU-delegate segmentation model, which
 * uses ONLY built-in ops (the MediaPipe selfie model needs the custom op
 * Convolution2DTransposeBias, absent from the stock org.tensorflow:tensorflow-lite runtime).
 * Input sub_7 [1,257,257,3] f32 (normalized [-1,1]); output ResizeBilinear_3 [1,257,257,21]
 * f32 logits. The verdict only compares backends against CPU, so it is independent of the
 * model's output semantics and scale.
 *
 * Must be constructed and used on the SAME thread (GPU delegate context affinity).
 */
public class SelfieSegmenterGl {
    private static final String TAG = "PowerVrSegSpike";
    private static final String MODEL_ASSET = "deeplabv3_257_mv_gpu.tflite";
    private static final float MATCH_FRACTION = 0.05f;  // mean abs diff vs CPU, as a fraction of CPU's range
    private static final float FLAT_EPS = 1e-3f;        // output range below this is "flat" (zero/constant)

    private Interpreter cpu, cl, gl;
    private GpuDelegate clDelegate, glDelegate;
    private boolean cpuReady, clReady, glReady;

    private int inW = 257, inH = 257, inC = 3;
    private int outLen = 257 * 257 * 21;
    private ByteBuffer inputBuffer, cpuOut, clOut, glOut;

    public SelfieSegmenterGl(Context context) {
        // Self-contained device context + an init marker: if this line appears but
        // "Segmenter ready" never follows, construction hung (likely a delegate).
        String soc = Build.VERSION.SDK_INT >= 31
                ? Build.SOC_MANUFACTURER + "/" + Build.SOC_MODEL
                : "n/a";
        Log.i(TAG, "Initializing segmenter on " + Build.MANUFACTURER + " " + Build.MODEL
                + " (hardware=" + Build.HARDWARE + ", soc=" + soc + ", api=" + Build.VERSION.SDK_INT
                + ") — backends: CPU + OpenCL + OpenGL(forced)");

        byte[] model;
        try {
            model = loadAssetBytes(context, MODEL_ASSET);
        } catch (Throwable t) {
            Log.e(TAG, "Model load FAILED — " + t, t);
            return;
        }

        // CPU reference (ground truth).
        try {
            cpu = new Interpreter(toDirectBuffer(model), new Interpreter.Options());
            int[] in = cpu.getInputTensor(0).shape();
            inH = in[1];
            inW = in[2];
            inC = in[3];
            int[] out = cpu.getOutputTensor(0).shape();
            outLen = 1;
            for (int d : out) {
                outLen *= d;
            }
            cpuReady = true;
        } catch (Throwable t) {
            Log.e(TAG, "CPU interpreter init FAILED — " + t, t);
        }

        clDelegate = makeDelegate(GpuDelegateFactory.Options.GpuBackend.OPENCL, "OpenCL");
        if (clDelegate != null) {
            try {
                Interpreter.Options io = new Interpreter.Options();
                io.addDelegate(clDelegate);
                Log.i(TAG, "applying OpenCL delegate to interpreter...");
                cl = new Interpreter(toDirectBuffer(model), io);
                clReady = true;
            } catch (Throwable t) {
                Log.e(TAG, "OpenCL interpreter init FAILED — " + t, t);
            }
        }

        glDelegate = makeDelegate(GpuDelegateFactory.Options.GpuBackend.OPENGL, "OpenGL");
        if (glDelegate != null) {
            try {
                Interpreter.Options io = new Interpreter.Options();
                io.addDelegate(glDelegate);
                Log.i(TAG, "applying OpenGL delegate to interpreter...");
                gl = new Interpreter(toDirectBuffer(model), io);
                glReady = true;
            } catch (Throwable t) {
                Log.e(TAG, "OpenGL interpreter init FAILED — " + t, t);
            }
        }

        if (cpuReady || clReady || glReady) {
            inputBuffer = ByteBuffer.allocateDirect(inW * inH * inC * 4).order(ByteOrder.nativeOrder());
            cpuOut = ByteBuffer.allocateDirect(outLen * 4).order(ByteOrder.nativeOrder());
            clOut = ByteBuffer.allocateDirect(outLen * 4).order(ByteOrder.nativeOrder());
            glOut = ByteBuffer.allocateDirect(outLen * 4).order(ByteOrder.nativeOrder());
        }
        Log.i(TAG, "Segmenter ready: cpu=" + cpuReady + " openCL=" + clReady + " openGL=" + glReady
                + " input=" + inW + "x" + inH + "x" + inC + " outLen=" + outLen);
    }

    private static GpuDelegate makeDelegate(GpuDelegateFactory.Options.GpuBackend backend, String label) {
        Log.i(TAG, "creating " + label + " delegate (forced)...");
        try {
            GpuDelegateFactory.Options opts = new GpuDelegateFactory.Options();
            opts.setForceBackend(backend);
            return new GpuDelegate(opts);
        } catch (Throwable t) {
            Log.e(TAG, label + " delegate could not be created on this GPU — " + t, t);
            return null;
        }
    }

    public boolean isReady() {
        return cpuReady || clReady || glReady;
    }

    public int inputWidth() {
        return inW;
    }

    public int inputHeight() {
        return inH;
    }

    /** Runs every available backend on a model-sized ARGB frame and logs stats + verdict. */
    public void runAndLog(int[] argb) {
        if (!isReady()) {
            return;
        }
        try {
            writeInput(argb);

            float[] cpuOutArr = run(cpu, cpuReady, cpuOut, "CPU");
            float[] clOutArr = run(cl, clReady, clOut, "OpenCL");
            float[] glOutArr = run(gl, glReady, glOut, "OpenGL");

            if (cpuOutArr == null) {
                Log.i(TAG, ">>> RESULT: no CPU reference available — cannot compare <<<");
                return;
            }
            Stats cpuStats = stats(cpuOutArr);
            if (cpuStats.range() < FLAT_EPS) {
                Log.i(TAG, String.format(
                        ">>> RESULT: CPU output is flat (range=%.4f) — inconclusive, point the camera at a real scene <<<",
                        cpuStats.range()));
                return;
            }

            String clVerdict = backendVerdict(clOutArr, clReady, cpuOutArr, cpuStats);
            String glVerdict = backendVerdict(glOutArr, glReady, cpuOutArr, cpuStats);
            Log.i(TAG, "OpenCL vs CPU: " + clVerdict);
            Log.i(TAG, "OpenGL vs CPU: " + glVerdict);

            boolean glWorks = glVerdict.startsWith("MATCH");
            boolean clBroken = glReady && (!clReady || clVerdict.startsWith("BROKEN") || clVerdict.startsWith("DIFFERS"));

            if (glWorks && clBroken) {
                Log.i(TAG, ">>> RESULT: CONFIRMED — OpenCL is broken on this GPU, OpenGL matches CPU. Forcing the GL backend is the fix. <<<");
            } else if (glWorks) {
                Log.i(TAG, ">>> RESULT: OpenGL works (matches CPU). OpenCL did NOT reproduce the bug here — backend/driver path differs. <<<");
            } else if (glReady) {
                Log.i(TAG, ">>> RESULT: OpenGL also broken on this GPU (NaN/diverges) — bug is in CL<->GL interop, forcing GL is NOT a fix. <<<");
            } else {
                Log.i(TAG, ">>> RESULT: OpenGL delegate unavailable on this GPU — cannot force GL here. <<<");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Inference FAILED — " + t, t);
        }
    }

    private float[] run(Interpreter interp, boolean ready, ByteBuffer out, String label) {
        if (!ready || interp == null) {
            return null;
        }
        try {
            inputBuffer.rewind();
            out.rewind();
            long t0 = System.nanoTime();
            interp.run(inputBuffer, out);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            float[] arr = toFloats(out);
            Stats s = stats(arr);
            Log.i(TAG, String.format("%-7s: min=%.3f max=%.3f mean=%.3f range=%.3f nan=%d run=%dms",
                    label, s.min, s.max, s.mean, s.range(), s.nan, ms));
            return arr;
        } catch (Throwable t) {
            Log.e(TAG, label + " inference FAILED — " + t, t);
            return null;
        }
    }

    /** MATCH / DIFFERS / BROKEN / UNAVAILABLE for a GPU backend vs the CPU reference. */
    private String backendVerdict(float[] mask, boolean ready, float[] cpuArr, Stats cpuStats) {
        if (!ready || mask == null) {
            return "UNAVAILABLE";
        }
        Stats s = stats(mask);
        if (s.nan > 0) {
            return String.format("BROKEN (NaN: nan=%d)", s.nan);
        }
        if (s.range() < FLAT_EPS) {
            return String.format("BROKEN (flat output range=%.4f while CPU range=%.3f)", s.range(), cpuStats.range());
        }
        float nd = meanAbsDiff(mask, cpuArr) / Math.max(cpuStats.range(), FLAT_EPS);
        if (nd <= MATCH_FRACTION) {
            return String.format("MATCH (normDiff=%.3f)", nd);
        }
        return String.format("DIFFERS (normDiff=%.3f)", nd);
    }

    private void writeInput(int[] argb) {
        inputBuffer.rewind();
        int px = inW * inH;
        for (int i = 0; i < px; i++) {
            int p = argb[i];
            inputBuffer.putFloat((((p >> 16) & 0xFF) / 127.5f) - 1f); // R, normalized [-1,1]
            inputBuffer.putFloat((((p >> 8) & 0xFF) / 127.5f) - 1f);  // G
            inputBuffer.putFloat(((p & 0xFF) / 127.5f) - 1f);         // B
        }
    }

    private float[] toFloats(ByteBuffer buf) {
        buf.rewind();
        float[] out = new float[outLen];
        for (int i = 0; i < outLen; i++) {
            out[i] = buf.getFloat();
        }
        return out;
    }

    private static class Stats {
        float min, max, mean;
        int nan;

        float range() {
            return max - min;
        }
    }

    private Stats stats(float[] v) {
        Stats s = new Stats();
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, sum = 0;
        int nan = 0;
        for (float x : v) {
            if (Float.isNaN(x)) {
                nan++;
                continue;
            }
            if (x < min) {
                min = x;
            }
            if (x > max) {
                max = x;
            }
            sum += x;
        }
        s.min = (nan == v.length) ? 0 : min;
        s.max = (nan == v.length) ? 0 : max;
        s.mean = sum / Math.max(1, v.length - nan);
        s.nan = nan;
        return s;
    }

    private float meanAbsDiff(float[] a, float[] b) {
        double sum = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            float x = a[i], y = b[i];
            if (Float.isNaN(x) || Float.isNaN(y)) {
                continue; // NaN handled by the broken-check; don't let it dominate the average
            }
            sum += Math.abs(x - y);
        }
        return (float) (sum / Math.max(1, n));
    }

    public void close() {
        for (Interpreter i : new Interpreter[] {cpu, cl, gl}) {
            try {
                if (i != null) {
                    i.close();
                }
            } catch (Throwable ignored) {
            }
        }
        for (GpuDelegate d : new GpuDelegate[] {clDelegate, glDelegate}) {
            try {
                if (d != null) {
                    d.close();
                }
            } catch (Throwable ignored) {
            }
        }
        cpuReady = clReady = glReady = false;
    }

    private static byte[] loadAssetBytes(Context ctx, String name) throws Exception {
        InputStream is = ctx.getAssets().open(name);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(1024, is.available()));
            byte[] chunk = new byte[16 * 1024];
            int n;
            while ((n = is.read(chunk)) > 0) {
                bos.write(chunk, 0, n);
            }
            return bos.toByteArray();
        } finally {
            is.close();
        }
    }

    /** TFLite needs a direct ByteBuffer that outlives the interpreter; one per interpreter. */
    private static ByteBuffer toDirectBuffer(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        bb.put(bytes);
        bb.rewind();
        return bb;
    }
}
