package com.oney.WebRTCModule;

import java.util.Random;

/**
 * JVM check for FaceReframeGeometry against a replay of the SDK 144.7559.15 matrix chain
 * (EglRenderer.renderFrameOnRenderThread drawMatrix, VideoFrameDrawer.drawFrame renderMatrix,
 * drawTexture buffer transform, RendererCommon.convertMatrixFromAndroidGraphicsMatrix).
 */
public final class FaceReframeGeometryCheck {
    private static int checks;
    private static int failures;

    /** android.graphics.Matrix semantics: values row-major 3x3, preX means this = this * X. */
    static final class Affine {
        double[] v = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        Affine copy() { Affine a = new Affine(); a.v = v.clone(); return a; }
        void preConcat(Affine o) { v = mul(v, o.v); }
        void preTranslate(double x, double y) { v = mul(v, new double[]{1, 0, x, 0, 1, y, 0, 0, 1}); }
        void preScale(double x, double y) { v = mul(v, new double[]{x, 0, 0, 0, y, 0, 0, 0, 1}); }
        void preRotate(double deg) {
            double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
            v = mul(v, new double[]{c, -s, 0, s, c, 0, 0, 0, 1});
        }
        double[] map(double x, double y) {
            return new double[]{v[0] * x + v[1] * y + v[2], v[3] * x + v[4] * y + v[5]};
        }
        static double[] mul(double[] a, double[] b) {
            double[] o = new double[9];
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    for (int k = 0; k < 3; k++) o[r * 3 + c] += a[r * 3 + k] * b[k * 3 + c];
            return o;
        }
        float[] toGl() {
            return new float[]{(float) v[0], (float) v[3], 0, (float) v[6], (float) v[1], (float) v[4], 0, (float) v[7],
                    0, 0, 1, 0, (float) v[2], (float) v[5], 0, (float) v[8]};
        }
    }

    static Affine drawMatrix(float frameAspect, float layoutAspect, boolean mirror) {
        float sx, sy;
        if (frameAspect > layoutAspect) { sx = layoutAspect / frameAspect; sy = 1f; } else { sx = 1f; sy = frameAspect / layoutAspect; }
        Affine d = new Affine();
        d.preTranslate(0.5, 0.5);
        d.preScale(mirror ? -1 : 1, 1);
        d.preScale(sx, sy);
        d.preTranslate(-0.5, -0.5);
        return d;
    }

    static Affine frameChain(int rotation, boolean texture) {
        Affine r = new Affine();
        r.preTranslate(0.5, 0.5);
        if (!texture) r.preScale(1, -1);
        r.preRotate(rotation);
        r.preTranslate(-0.5, -0.5);
        return r;
    }

    static double[] glMap(float[] m, double x, double y) {
        return new double[]{m[0] * x + m[4] * y + m[12], m[1] * x + m[5] * y + m[13]};
    }

    static void check(boolean ok, String what) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL " + what); }
    }

    static boolean near(double a, double b, double eps) { return Math.abs(a - b) <= eps; }

    /** Copy of the pre-patch SurfaceView branch of layoutWithCustomTransform. */
    static int[] originalBounds(int width, int height, int baseX, int baseY, float customScale, float tx, float ty) {
        int scaledWidth = (int) (baseX * customScale);
        int scaledHeight = (int) (baseY * customScale);
        int centerX = width / 2, centerY = height / 2;
        int offsetX = (int) (width * tx), offsetY = (int) (height * ty);
        int rawL = centerX - scaledWidth / 2 + offsetX;
        int rawT = centerY - scaledHeight / 2 + offsetY;
        return new int[]{Math.max(0, rawL), Math.max(0, rawT), Math.min(width, rawL + scaledWidth), Math.min(height, rawT + scaledHeight)};
    }

    /** RendererCommon.getDisplaySize(SCALE_ASPECT_FIT, ...) */
    static int[] fitSize(float aspect, int w, int h) {
        return new int[]{Math.min(w, Math.round(h / 1f * aspect)), Math.min(h, Math.round(w / 1f / aspect))};
    }

    private static void verifyTileCrop(int width, int height, float frameAspect, float scale, float x, float y) {
        FaceReframeGeometry.Placement placement = FaceReframeGeometry.cover(width, height, frameAspect, scale, x, y);
        check(placement.left == 0 && placement.top == 0
                && placement.right == width && placement.bottom == height, "fixed tile viewport");
        float[] window = new float[] {placement.windowX, placement.windowY,
                placement.windowWidth, placement.windowHeight};
        check(window[0] >= 0f && window[0] + window[2] <= 1.000001f, "horizontal cover");
        check(window[1] >= 0f && window[1] + window[3] <= 1.000001f, "vertical cover");
        float layoutAspect = width / (float) height;
        near(frameAspect * window[2] / window[3], layoutAspect, "no aspect distortion");

        float[] crop = {1f, 1f, 0f, 0f};
        boolean applies = FaceReframeGeometry.cropFor(window, frameAspect, layoutAspect, crop);
        if (scale == 1f && x == 0f && y == 0f) {
            check(!applies, "centered minimum cover preserves the SDK matrix");
        }

        float sx = Math.min(1f, layoutAspect / frameAspect);
        float sy = Math.min(1f, frameAspect / layoutAspect);
        for (int mirror : new int[] {-1, 1}) {
            // SDK centered crop with horizontal mirror, then a rotated texture.
            float[] incoming = {0f, mirror * sx, 0f, 0f, -sy, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, (1f + sy) / 2f, (1f - mirror * sx) / 2f, 0f, 1f};
            float[] composed = new float[16];
            FaceReframeGeometry.compose(incoming, crop, composed);
            for (float point : new float[] {0f, 0.3f, 1f}) {
                float expectedU = window[1] + window[3] * (1f - point);
                float expectedV = 0.5f + mirror * (window[0] + window[2] * point - 0.5f);
                near(composed[0] * point + composed[4] * point + composed[12], expectedU, "rotated U");
                near(composed[1] * point + composed[5] * point + composed[13], expectedV, "mirrored V");
            }
        }
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.00002f, message + ": " + actual + " vs " + expected);
    }

    public static void main(String[] args) {
        for (int[] size : new int[][] {{56, 56}, {112, 112}, {166, 126}, {111, 168}}) {
            for (float aspect : new float[] {9f / 16f, 1f, 4f / 3f, 16f / 9f}) {
                verifyTileCrop(size[0], size[1], aspect, 1f, 0f, 0f);
                verifyTileCrop(size[0], size[1], aspect, 2.8f, 0.13f, -0.08f);
                verifyTileCrop(size[0], size[1], aspect, 2.8f, -2f, 2f);
                verifyTileCrop(size[0], size[1], aspect, Float.NaN, Float.NaN, Float.POSITIVE_INFINITY);
            }
        }

        Random random = new Random(42);
        int[][] frames = {{1280, 720}, {720, 1280}, {640, 480}, {1920, 1080}};
        int[][] containers = {{1080, 1920}, {540, 960}, {1080, 1080}, {720, 405}, {360, 500}};
        int[] rotations = {0, 90, 180, 270};
        // A typical SurfaceTexture transform: vertical flip.
        Affine oesBuffer = new Affine();
        oesBuffer.preTranslate(0, 1);
        oesBuffer.preScale(1, -1);

        for (int iteration = 0; iteration < 4000; iteration++) {
            int[] frame = frames[random.nextInt(frames.length)];
            int[] container = containers[random.nextInt(containers.length)];
            int rotation = rotations[random.nextInt(4)];
            boolean mirror = random.nextBoolean();
            boolean texture = random.nextBoolean();
            int rotW = rotation % 180 == 0 ? frame[0] : frame[1];
            int rotH = rotation % 180 == 0 ? frame[1] : frame[0];
            float frameAspect = rotation % 180 == 0 ? frame[0] / (float) frame[1] : frame[1] / (float) frame[0];
            int[] base = fitSize(frameAspect, container[0], container[1]);
            float scale = 0.6f + random.nextFloat() * 3f;
            float tx = (random.nextFloat() - 0.5f) * 0.8f;
            float ty = (random.nextFloat() - 0.5f) * 0.8f;
            boolean centeredCover = iteration % 5 == 0;
            if (centeredCover) {
                tx = 0;
                ty = 0;
                scale = Math.max(container[0] / (float) base[0], container[1] / (float) base[1]);
            }

            FaceReframeGeometry.Placement p = FaceReframeGeometry.place(container[0], container[1], base[0], base[1], scale, tx, ty);
            int[] orig = originalBounds(container[0], container[1], base[0], base[1], scale, tx, ty);
            check(p.left == orig[0] && p.top == orig[1] && p.right == orig[2] && p.bottom == orig[3], "bounds identical to pre-patch layout #" + iteration);
            if (!p.hasWindow) continue;
            check(p.windowX >= 0 && p.windowY >= 0 && p.windowX + p.windowWidth <= 1.0001f && p.windowY + p.windowHeight <= 1.0001f, "window inside frame #" + iteration);

            int bw = p.right - p.left, bh = p.bottom - p.top;
            float layoutAspect = bw / (float) bh;
            float[] window = {p.windowX, p.windowY, p.windowWidth, p.windowHeight};
            float[] crop = new float[4];
            boolean applies = FaceReframeGeometry.cropFor(window, frameAspect, layoutAspect, crop);
            if (!applies) { crop = new float[]{1, 1, 0, 0}; }

            Affine d = drawMatrix(frameAspect, layoutAspect, mirror);
            Affine chain = frameChain(rotation, texture);
            Affine render = chain.copy();
            render.preConcat(d);
            Affine sdk;
            if (texture) { sdk = oesBuffer.copy(); sdk.preConcat(render); } else { sdk = render; }
            float[] texMatrix = sdk.toGl();
            float[] composed = new float[16];
            FaceReframeGeometry.compose(texMatrix, crop, composed);

            int scaledW = (int) (base[0] * scale), scaledH = (int) (base[1] * scale);
            int rawL = container[0] / 2 - scaledW / 2 + (int) (container[0] * tx);
            int rawT = container[1] / 2 - scaledH / 2 + (int) (container[1] * ty);
            double[][] samples = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {0.5, 0.5}, {0.25, 0.8}};
            double[] firstFrameUp = null, firstScreen = null;
            for (double[] s : samples) {
                double px = s[0] * bw, py = s[1] * bh; // screen px within bounds, y down
                double qx = (p.left + px - rawL) / scaledW;
                double qyDown = (p.top + py - rawT) / scaledH;
                double qyUp = 1 - qyDown;
                double fx = 0.5 + (mirror ? -1 : 1) * (qx - 0.5);
                double fy = qyUp;
                Affine expectedChain = chain.copy();
                Affine expected;
                if (texture) { expected = oesBuffer.copy(); expected.preConcat(expectedChain); } else { expected = expectedChain; }
                double[] want = expected.map(fx, fy);
                double[] got = glMap(composed, s[0], 1 - s[1]);
                check(near(want[0], got[0], 2e-4) && near(want[1], got[1], 2e-4),
                        String.format("sample maps to expected texel #%d rot=%d mirror=%b tex=%b want=(%.5f,%.5f) got=(%.5f,%.5f)",
                                iteration, rotation, mirror, texture, want[0], want[1], got[0], got[1]));
                if (firstFrameUp == null) { firstFrameUp = new double[]{qx * rotW, qyDown * rotH}; firstScreen = new double[]{px, py}; }
                else if (s[0] != 0 && s[1] != 0) {
                    double kx = (qx * rotW - firstFrameUp[0]) / (px - firstScreen[0]);
                    double ky = (qyDown * rotH - firstFrameUp[1]) / (py - firstScreen[1]);
                    check(near(kx, ky, Math.max(kx, ky) * 0.01), "no aspect distortion #" + iteration + " kx=" + kx + " ky=" + ky);
                }
            }
            if (centeredCover) {
                double maxDelta = 0;
                for (int k = 0; k < 16; k++) maxDelta = Math.max(maxDelta, Math.abs(composed[k] - texMatrix[k]));
                double sx = frameAspect > layoutAspect ? layoutAspect / frameAspect : 1;
                double sy = frameAspect > layoutAspect ? 1 : frameAspect / layoutAspect;
                double tolerance = 2.0 / Math.min(scaledW * sx, scaledH * sy) + 2.0 / Math.min(bw, bh);
                check(maxDelta < tolerance * 4, "centered cover target equals SDK centered crop within rounding #" + iteration + " delta=" + maxDelta);
            }
        }

        FaceReframeGeometry.Transition t = new FaceReframeGeometry.Transition(0.1f, 0.2f, 0.5f, 0.6f, 0.3f, 0.1f, 0.4f, 0.5f, 1_000L, 280_000_000L, 0.5625f);
        float[] w = new float[4];
        float previous = 0.1f;
        boolean settled = false;
        for (long now = 0; now <= 400_000_000L; now += 8_333_333L) {
            settled = FaceReframeGeometry.windowAt(t, now, w);
            check(w[0] >= previous - 1e-6f && w[0] <= 0.3f + 1e-6f, "monotonic toward target at " + now);
            previous = w[0];
            if (now >= 280_001_000L) check(settled && w[0] == 0.3f && w[3] == 0.5f, "settles exactly on target");
        }
        check(settled, "settled at end");
        check(FaceReframeGeometry.windowAt(FaceReframeGeometry.Transition.snap(
                FaceReframeGeometry.place(1080, 1920, 1080, 608, 3.16f, 0.1f, 0f), 0.5625f), 0L, w), "snap settles immediately");
        float[] identityCrop = new float[4];
        check(!FaceReframeGeometry.cropFor(new float[]{0.2f, 0f, 0.6f, 1f}, 1f, 0.6f, identityCrop), "centered SDK crop is identity -> passthrough");

        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) System.exit(1);
    }
}
