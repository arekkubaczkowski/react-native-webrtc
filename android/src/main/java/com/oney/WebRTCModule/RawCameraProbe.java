package com.oney.WebRTCModule;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import java.util.Map;

import livekit.org.webrtc.PeerConnection;
import livekit.org.webrtc.RTCStats;
import livekit.org.webrtc.RtpParameters;
import livekit.org.webrtc.RtpSender;
import livekit.org.webrtc.TextureBufferImpl;
import livekit.org.webrtc.VideoFrame;
import livekit.org.webrtc.VideoProcessor;
import livekit.org.webrtc.VideoSink;

final class RawCameraProbe {
    static final String TAG = "TapinRawTrace";
    private static final String CVO = "urn:3gpp:video-orientation";
    private static final long MODE_REFRESH_MS = 250;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static boolean enabled(String tag) {
        return Log.isLoggable(tag, Log.DEBUG);
    }

    static int publicationCeiling() {
        if (enabled("TapinRawPublish15")) {
            return 15;
        }
        return enabled("TapinRawPublish24") ? 24 : 0;
    }

    static String describe(VideoFrame frame) {
        VideoFrame.Buffer buffer = frame.getBuffer();
        String type = buffer instanceof VideoFrame.TextureBuffer
                ? ((VideoFrame.TextureBuffer) buffer).getType().name()
                : buffer.getClass().getSimpleName();
        return type + " " + buffer.getWidth() + "x" + buffer.getHeight()
                + " rot=" + frame.getRotation() + " ts=" + frame.getTimestampNs();
    }

    static VideoProcessor pipeline(VideoProcessor delegate) {
        boolean trace = enabled(TAG);
        boolean runtimeSwitch = enabled("TapinRawSurface");
        return trace || runtimeSwitch ? new Pipeline(delegate, trace, runtimeSwitch) : delegate;
    }

    static VideoSink postSource() {
        return new VideoSink() {
            private int frames;

            @Override
            public void onFrame(VideoFrame frame) {
                if (frames++ % 120 == 0) {
                    Log.i(TAG, "post-source " + describe(frame));
                }
            }
        };
    }

    private static final class Pipeline implements VideoProcessor {
        private final VideoProcessor delegate;
        private final boolean trace;
        private final boolean runtimeSwitch;
        private boolean rotate;
        private long nextModeRefreshMs;
        private String reportedMode;
        private VideoSink sink;
        private VideoFrame input;
        private boolean sample;
        private int frames;

        Pipeline(VideoProcessor delegate, boolean trace, boolean runtimeSwitch) {
            this.delegate = delegate;
            this.trace = trace;
            this.runtimeSwitch = runtimeSwitch;
        }

        @Override
        public void setSink(VideoSink sink) {
            this.sink = sink;
            if (delegate != null) {
                delegate.setSink(sink == null ? null : this::forward);
            }
        }

        @Override
        public void onCapturerStarted(boolean success) {
            if (delegate != null) {
                delegate.onCapturerStarted(success);
            }
        }

        @Override
        public void onCapturerStopped() {
            if (delegate != null) {
                delegate.onCapturerStopped();
            }
        }

        @Override
        public void onFrameCaptured(VideoFrame frame, FrameAdaptationParameters parameters) {
            sample = trace && frames++ % 120 == 0;
            if (sample) {
                Log.i(TAG, "capture " + describe(frame) + " crop=" + parameters.cropX + ","
                        + parameters.cropY + "," + parameters.cropWidth + "," + parameters.cropHeight
                        + " scale=" + parameters.scaleWidth + "x" + parameters.scaleHeight
                        + " drop=" + parameters.drop);
            }
            VideoFrame adapted = VideoProcessor.applyFrameAdaptationParameters(frame, parameters);
            if (adapted != null) {
                try {
                    onFrameCaptured(adapted);
                } finally {
                    adapted.release();
                }
            }
        }

        @Override
        public void onFrameCaptured(VideoFrame adapted) {
            input = adapted;
            try {
                if (delegate == null) {
                    forward(adapted);
                } else {
                    delegate.onFrameCaptured(adapted);
                }
            } finally {
                input = null;
            }
        }

        private void forward(VideoFrame frame) {
            VideoFrame.Buffer buffer = frame.getBuffer();
            boolean raw = input == frame;
            if (runtimeSwitch && raw) {
                long now = SystemClock.elapsedRealtime();
                if (now >= nextModeRefreshMs) {
                    rotate = enabled("TapinRawRotate");
                    nextModeRefreshMs = now + MODE_REFRESH_MS;
                }
            }
            int rotation = frame.getRotation();
            VideoFrame output = frame;
            if (rotate && raw && rotation != 0 && buffer instanceof TextureBufferImpl) {
                Matrix transform = new Matrix();
                transform.setRotate(rotation, 0.5f, 0.5f);
                TextureBufferImpl texture = ((TextureBufferImpl) buffer).applyTransformMatrix(
                        transform, frame.getRotatedWidth(), frame.getRotatedHeight());
                if (texture != null) {
                    output = new VideoFrame(texture, 0, frame.getTimestampNs());
                }
            }
            try {
                VideoFrame forwarded = output;
                if (sample && output.getBuffer() instanceof VideoFrame.TextureBuffer) {
                    VideoFrame.TextureBuffer texture = (VideoFrame.TextureBuffer) output.getBuffer();
                    forwarded = new VideoFrame(new TracedTexture(texture, output.getTimestampNs()),
                            output.getRotation(), output.getTimestampNs());
                }
                if (sample) {
                    Log.i(TAG, "native-source-input raw=" + raw + " matrixRotation=" + (output != frame)
                            + " " + describe(forwarded));
                }
                sink.onFrame(forwarded);
                if (runtimeSwitch && raw) {
                    String mode = rotate ? (output != frame ? "B-applied" : "B-bypassed") : "A";
                    if (!mode.equals(reportedMode)) {
                        reportedMode = mode;
                        Log.i(TAG, "runtime-mode variant=" + (rotate ? "B" : "A")
                                + " matrixRotation=" + (output != frame) + " ts=" + output.getTimestampNs());
                    }
                }
            } finally {
                if (output != frame) {
                    output.release();
                }
            }
        }
    }

    private static final class TracedTexture implements VideoFrame.TextureBuffer {
        private final VideoFrame.TextureBuffer texture;
        private final long timestampNs;

        TracedTexture(VideoFrame.TextureBuffer texture, long timestampNs) {
            this.texture = texture;
            this.timestampNs = timestampNs;
        }

        @Override public int getWidth() { return texture.getWidth(); }
        @Override public int getHeight() { return texture.getHeight(); }
        @Override public int getUnscaledWidth() { return texture.getUnscaledWidth(); }
        @Override public int getUnscaledHeight() { return texture.getUnscaledHeight(); }
        @Override public Type getType() { return texture.getType(); }
        @Override public int getTextureId() { return texture.getTextureId(); }
        @Override public Matrix getTransformMatrix() { return texture.getTransformMatrix(); }
        @Override public void retain() { texture.retain(); }
        @Override public void release() { texture.release(); }

        @Override
        public VideoFrame.Buffer cropAndScale(int x, int y, int w, int h, int outW, int outH) {
            VideoFrame.Buffer scaled = texture.cropAndScale(x, y, w, h, outW, outH);
            return scaled instanceof VideoFrame.TextureBuffer
                    ? new TracedTexture((VideoFrame.TextureBuffer) scaled, timestampNs) : scaled;
        }

        @Override
        public VideoFrame.TextureBuffer applyTransformMatrix(Matrix matrix, int width, int height) {
            VideoFrame.TextureBuffer transformed = texture.applyTransformMatrix(matrix, width, height);
            return transformed == null ? null : new TracedTexture(transformed, timestampNs);
        }

        @Override
        public VideoFrame.I420Buffer toI420() {
            long start = System.nanoTime();
            Trace.beginSection("TapinRaw.textureToI420");
            try {
                return texture.toI420();
            } finally {
                Trace.endSection();
                Log.i(TAG, "texture-to-i420 ts=" + timestampNs + " wallNs="
                        + (System.nanoTime() - start), new Throwable("conversion caller"));
            }
        }
    }

    static void sampleSender(WebRTCModule module, int pcId, String senderId) {
        MAIN.postDelayed(() -> ThreadUtils.runOnExecutor(() -> {
            PeerConnectionObserver observer = module.getPeerConnectionObserver(pcId);
            if (observer == null || observer.getPeerConnection() == null) {
                return;
            }
            PeerConnection pc = observer.getPeerConnection();
            if (pc.signalingState() == PeerConnection.SignalingState.CLOSED) {
                return;
            }
            RtpSender sender = observer.getSender(senderId);
            if (sender == null || sender.track() == null) {
                return;
            }
            RtpParameters parameters = sender.getParameters();
            int orientationId = -1;
            for (RtpParameters.HeaderExtension extension : parameters.getHeaderExtensions()) {
                if (CVO.equals(extension.getUri())) {
                    orientationId = extension.getId();
                }
            }
            final int cvo = orientationId;
            StringBuilder limits = new StringBuilder();
            for (RtpParameters.Encoding encoding : parameters.encodings) {
                limits.append(encoding.rid).append(':').append(encoding.maxFramerate)
                        .append('/').append(encoding.active).append(',');
            }
            final String senderLimits = limits.toString();
            pc.getStats(sender, report -> {
                for (RTCStats stats : report.getStatsMap().values()) {
                    Map<String, Object> members = stats.getMembers();
                    if ("outbound-rtp".equals(stats.getType()) && "video".equals(members.get("kind"))) {
                        Log.i(TAG, "outbound tUs=" + (long) stats.getTimestampUs()
                                + " id=" + stats.getId() + " cvoId=" + cvo + " limits=" + senderLimits
                                + " rid=" + members.get("rid") + " ssrc=" + members.get("ssrc")
                                + " framesEncoded=" + members.get("framesEncoded")
                                + " framesSent=" + members.get("framesSent")
                                + " width=" + members.get("frameWidth") + " height=" + members.get("frameHeight")
                                + " bytes=" + members.get("bytesSent")
                                + " encodeSeconds=" + members.get("totalEncodeTime")
                                + " quality=" + members.get("qualityLimitationReason")
                                + " packetDelaySeconds=" + members.get("totalPacketSendDelay"));
                    }
                }
            });
            sampleSender(module, pcId, senderId);
        }), 1000);
    }
}
