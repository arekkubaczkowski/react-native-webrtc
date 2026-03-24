package com.oney.WebRTCModule.videoEffects;

import android.content.Context;

import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoCapturer;

/**
 * Factory interface for creating custom video capturers.
 * External modules (e.g., video effects SDKs) can register implementations
 * via {@link CapturerProvider} to provide custom capturers when
 * effectsSdkRequired constraint is set.
 */
public interface CapturerFactoryInterface {
    /**
     * Creates a custom video capturer.
     *
     * @param cameraName the camera device name
     * @param eventsHandler handler for camera events
     * @param enumerator camera enumerator for device discovery
     * @return a VideoCapturer instance, or null if creation fails
     */
    VideoCapturer createCapturer(
            String cameraName,
            CameraVideoCapturer.CameraEventsHandler eventsHandler,
            CameraEnumerator enumerator);
}
