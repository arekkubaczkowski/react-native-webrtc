package com.oney.WebRTCModule.webrtcutils;

import android.util.Log;

import org.webrtc.EncodedImage;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoDecoder;

/**
 * Wraps a Java-side {@link VideoDecoder} so that a runtime exception never escapes back into
 * libwebrtc.
 *
 * libwebrtc's JNI layer treats any pending Java exception as unrecoverable — HandleException() in
 * sdk/android/src/jni/jvm.cc is a bare RTC_CHECK(false), which aborts the process. AndroidVideoDecoder
 * reaches that path on its own: initDecode() calls SurfaceTextureHelper.create(), which returns null
 * once the device runs out of EGL contexts, and then dereferences it without a null check. Rendering
 * many simultaneous streams is enough to hit it, and the result is SIGABRT rather than a failed track.
 *
 * Returning an error status instead lets the caller drop that one stream and keep the call alive.
 * Only decoders that run in Java are wrapped — a decoder backed by a native implementation is reached
 * through createNative() and never dispatches through these methods.
 */
class CrashSafeVideoDecoder implements VideoDecoder {
    private static final String TAG = "CrashSafeVideoDecoder";

    private final VideoDecoder decoder;

    CrashSafeVideoDecoder(VideoDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public VideoCodecStatus initDecode(Settings settings, Callback decodeCallback) {
        try {
            return decoder.initDecode(settings, decodeCallback);
        } catch (RuntimeException e) {
            Log.e(TAG, "initDecode threw, reporting failure instead of aborting", e);
            return VideoCodecStatus.ERROR;
        }
    }

    @Override
    public VideoCodecStatus decode(EncodedImage frame, DecodeInfo info) {
        try {
            return decoder.decode(frame, info);
        } catch (RuntimeException e) {
            Log.e(TAG, "decode threw, reporting failure instead of aborting", e);
            return VideoCodecStatus.ERROR;
        }
    }

    @Override
    public VideoCodecStatus release() {
        try {
            return decoder.release();
        } catch (RuntimeException e) {
            Log.e(TAG, "release threw, reporting failure instead of aborting", e);
            return VideoCodecStatus.ERROR;
        }
    }

    @Override
    public String getImplementationName() {
        return decoder.getImplementationName();
    }
}
