package com.oney.WebRTCModule;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import livekit.org.webrtc.GlShader;
import livekit.org.webrtc.GlUtil;
import livekit.org.webrtc.RendererCommon;

/** Uses the pinned GlGenericDrawer sampling equations with a mask in output pixels. */
final class MaskedGlDrawer implements RendererCommon.GlDrawer {
    private static final String VERTEX =
            "attribute vec4 in_pos;\n"
            + "attribute vec4 in_tc;\n"
            + "uniform mat4 tex_mat;\n"
            + "varying mediump vec2 tc;\n"
            + "void main() { gl_Position = in_pos; tc = (tex_mat * in_tc).xy; }\n";
    private static final String PRECISION =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
            + "precision highp float;\n"
            + "#else\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying mediump vec2 tc;\n";
    private static final String YUV_SAMPLE =
            "uniform sampler2D y_tex, u_tex, v_tex;\n"
            + "vec3 videoColor() {\n"
            + "  float y = texture2D(y_tex, tc).r * 1.16438;\n"
            + "  float u = texture2D(u_tex, tc).r;\n"
            + "  float v = texture2D(v_tex, tc).r;\n"
            + "  return vec3(y + 1.59603 * v - 0.874202,\n"
            + "              y - 0.391762 * u - 0.812968 * v + 0.531668,\n"
            + "              y + 2.01723 * u - 1.08563);\n"
            + "}\n";
    private static final String MASK =
            "uniform vec4 viewport;\n"
            + "uniform float radius;\n"
            + "void main() {\n"
            + "  vec2 halfSize = viewport.zw * 0.5;\n"
            + "  vec2 pixel = gl_FragCoord.xy - viewport.xy;\n"
            + "  vec2 q = abs(pixel - halfSize) - (halfSize - vec2(radius));\n"
            + "  float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;\n"
            + "  float alpha = 1.0 - smoothstep(-0.5, 0.5, distance);\n"
            + "  gl_FragColor = vec4(clamp(videoColor(), 0.0, 1.0) * alpha, alpha);\n"
            + "}\n";

    private enum Input {
        OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
        RGB(GLES20.GL_TEXTURE_2D),
        YUV(GLES20.GL_TEXTURE_2D);

        final int target;

        Input(int target) {
            this.target = target;
        }
    }

    private final FloatBuffer vertices = GlUtil.createFloatBuffer(
            new float[] {-1, -1, 1, -1, -1, 1, 1, 1});
    private final FloatBuffer texCoords = GlUtil.createFloatBuffer(
            new float[] {0, 0, 1, 0, 0, 1, 1, 1});
    private final float radiusPx;
    private GlShader shader;
    private Input shaderInput;
    private int matrixLocation;
    private int viewportLocation;
    private int radiusLocation;

    MaskedGlDrawer(float radiusPx) {
        this.radiusPx = radiusPx;
    }

    @Override
    public void drawOes(int textureId, float[] matrix, int frameWidth, int frameHeight,
                        int x, int y, int width, int height) {
        drawTexture(Input.OES, textureId, matrix, x, y, width, height);
    }

    @Override
    public void drawRgb(int textureId, float[] matrix, int frameWidth, int frameHeight,
                        int x, int y, int width, int height) {
        drawTexture(Input.RGB, textureId, matrix, x, y, width, height);
    }

    private void drawTexture(Input input, int textureId, float[] matrix,
                             int x, int y, int width, int height) {
        prepare(input, matrix, x, y, width, height);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(input.target, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(input.target, 0);
    }

    @Override
    public void drawYuv(int[] textures, float[] matrix, int frameWidth, int frameHeight,
                        int x, int y, int width, int height) {
        prepare(Input.YUV, matrix, x, y, width, height);
        for (int plane = 0; plane < 3; plane++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + plane);
            GLES20.glBindTexture(Input.YUV.target, textures[plane]);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        for (int plane = 0; plane < 3; plane++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + plane);
            GLES20.glBindTexture(Input.YUV.target, 0);
        }
    }

    private void prepare(Input input, float[] matrix, int x, int y, int width, int height) {
        if (input != shaderInput) {
            release();
            shader = new GlShader(VERTEX, fragmentSource(input));
            shaderInput = input;
            shader.useProgram();
            if (input == Input.YUV) {
                GLES20.glUniform1i(shader.getUniformLocation("y_tex"), 0);
                GLES20.glUniform1i(shader.getUniformLocation("u_tex"), 1);
                GLES20.glUniform1i(shader.getUniformLocation("v_tex"), 2);
            } else {
                GLES20.glUniform1i(shader.getUniformLocation("tex"), 0);
            }
            shader.setVertexAttribArray("in_pos", 2, vertices);
            shader.setVertexAttribArray("in_tc", 2, texCoords);
            matrixLocation = shader.getUniformLocation("tex_mat");
            viewportLocation = shader.getUniformLocation("viewport");
            radiusLocation = shader.getUniformLocation("radius");
        } else {
            shader.useProgram();
        }
        GLES20.glUniformMatrix4fv(matrixLocation, 1, false, matrix, 0);
        GLES20.glUniform4f(viewportLocation, x, y, width, height);
        GLES20.glUniform1f(radiusLocation, Math.min(radiusPx, Math.min(width, height) * 0.5f));
        GLES20.glViewport(x, y, width, height);
    }

    private static String fragmentSource(Input input) {
        if (input == Input.YUV) {
            return PRECISION + YUV_SAMPLE + MASK;
        }
        String header = input == Input.OES ? "#extension GL_OES_EGL_image_external : require\n" : "";
        String sampler = input == Input.OES ? "samplerExternalOES" : "sampler2D";
        return header + PRECISION + "uniform " + sampler + " tex;\n"
                + "vec3 videoColor() { return texture2D(tex, tc).rgb; }\n" + MASK;
    }

    @Override
    public void release() {
        if (shader != null) {
            shader.release();
            shader = null;
        }
        shaderInput = null;
    }
}
