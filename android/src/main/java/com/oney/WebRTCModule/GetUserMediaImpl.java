package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.util.Consumer;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.oney.WebRTCModule.videoEffects.ProcessorProvider;
import com.oney.WebRTCModule.videoEffects.VideoEffectProcessor;
import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;
import com.effectssdk.tsvb.EffectsSDKStatus;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The implementation of {@code getUserMedia} extracted into a separate file in
 * order to reduce complexity and to (somewhat) separate concerns.
 */
class GetUserMediaImpl {
    /**
     * The {@link Log} tag with which {@code GetUserMediaImpl} is to log.
     */
    private static final String TAG = WebRTCModule.TAG;

    private static final int PERMISSION_REQUEST_CODE = (int) (Math.random() * Short.MAX_VALUE);
    
    private static EffectsSDKCameraCapturer globalEffectsSDKCapturer = null;

    private CameraEnumerator cameraEnumerator;
    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link MediaStreamTrack}s created by {@code GetUserMediaImpl} mapped by
     * track ID.
     */
    private final Map<String, TrackPrivate> tracks = new HashMap<>();

    private final WebRTCModule webRTCModule;

    private Promise displayMediaPromise;
    private Intent mediaProjectionPermissionResultData;

    GetUserMediaImpl(WebRTCModule webRTCModule, ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;

        reactContext.addActivityEventListener(new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                super.onActivityResult(activity, requestCode, resultCode, data);
                if (requestCode == PERMISSION_REQUEST_CODE) {
                    if (resultCode != Activity.RESULT_OK) {
                        displayMediaPromise.reject("DOMException", "NotAllowedError");
                        displayMediaPromise = null;
                        return;
                    }

                    mediaProjectionPermissionResultData = data;

                    ThreadUtils.runOnExecutor(() -> {
                        MediaProjectionService.launch(activity);
                        createScreenStream();
                    });
                }
            }
        });
    }

    private AudioTrack createAudioTrack(ReadableMap constraints) {
        ReadableMap audioConstraintsMap = constraints.getMap("audio");

        Log.d(TAG, "getUserMedia(audio): " + audioConstraintsMap);

        String id = UUID.randomUUID().toString();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        MediaConstraints peerConstraints = webRTCModule.constraintsForOptions(audioConstraintsMap);

        // Convert given constraints into the internal webrtc media constraints.
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("googAutoGainControl",
                audioConstraintsMap.hasKey("autoGainControl")
                        ? ReactBridgeUtil.getMapStrValue(audioConstraintsMap, "autoGainControl")
                        : "true"));
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("googNoiseSuppression",
                audioConstraintsMap.hasKey("noiseSuppression")
                        ? ReactBridgeUtil.getMapStrValue(audioConstraintsMap, "noiseSuppression")
                        : "true"));
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("googEchoCancellation",
                audioConstraintsMap.hasKey("echoCancellation")
                        ? ReactBridgeUtil.getMapStrValue(audioConstraintsMap, "echoCancellation")
                        : "true"));
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("googHighpassFilter",
                audioConstraintsMap.hasKey("highpassFilter")
                        ? ReactBridgeUtil.getMapStrValue(audioConstraintsMap, "highpassFilter")
                        : "true"));

        // PeerConnectionFactory.createAudioSource will throw an error when mandatory constraints contain nulls.
        // so, let's check for nulls
        checkMandatoryConstraints(peerConstraints);

        AudioSource audioSource = pcFactory.createAudioSource(peerConstraints);
        AudioTrack track = pcFactory.createAudioTrack(id, audioSource);

        // surfaceTextureHelper is initialized for videoTrack only, so its null here.
        tracks.put(id, new TrackPrivate(track, audioSource, /* videoCapturer */ null, /* surfaceTextureHelper */ null));

        return track;
    }

    private void checkMandatoryConstraints(MediaConstraints peerConstraints) {
        ArrayList<MediaConstraints.KeyValuePair> valid = new ArrayList<>(peerConstraints.mandatory.size());

        for (MediaConstraints.KeyValuePair constraint : peerConstraints.mandatory) {
            if (constraint.getValue() != null) {
                valid.add(constraint);
            } else {
                Log.d(TAG, String.format("constraint %s is null, ignoring it", constraint.getKey()));
            }
        }

        peerConstraints.mandatory.clear();
        peerConstraints.mandatory.addAll(valid);
    }

    private boolean getEffectsSDKConstraint(ReadableMap videoConstraints) {
        try {
            if (videoConstraints != null && videoConstraints.hasKey("effectsSdkRequired")) {
                return videoConstraints.getBoolean("effectsSdkRequired");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking EffectsSDK constraint", e);
        }
        return false;
    }

    private VideoCapturer createEffectsSDKVideoCapturer(String cameraName, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        try {
            return new EffectsSDKCameraCapturer(cameraName, eventsHandler, getCameraEnumerator());
        } catch (Exception e) {
            Log.e(TAG, "Failed to create EffectsSDKVideoCapturer", e);
            return null;
        }
    }

    public EffectsSDKStatus initializeEffectsSdk(String trackId, String customerId, String url) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            return capturer.initializeEffectsSdk(customerId, url);
        }
        return EffectsSDKStatus.INACTIVE;
    }

    public boolean isInitialized(String trackId) {
        return globalEffectsSDKCapturer != null;
    }
    
    private EffectsSDKCameraCapturer getEffectsSdkVideoCapturer(String trackId) {
        return globalEffectsSDKCapturer;
    }
    
    private String getCameraNameFromConstraints(ReadableMap videoConstraints) {
        String[] deviceNames = getCameraEnumerator().getDeviceNames();
        
        // Try deviceId first
        if (videoConstraints.hasKey("deviceId")) {
            String requestedDeviceId = videoConstraints.getString("deviceId");
            for (String name : deviceNames) {
                if (name.equals(requestedDeviceId)) {
                    return name;
                }
            }
        }
        
        // Try facingMode
        if (videoConstraints.hasKey("facingMode")) {
            String requestedFacingMode = videoConstraints.getString("facingMode");
            boolean wantFrontCamera = "user".equals(requestedFacingMode);
            for (String name : deviceNames) {
                if (getCameraEnumerator().isFrontFacing(name) == wantFrontCamera) {
                    return name;
                }
            }
        }
        
        // Fallback to front camera
        for (String name : deviceNames) {
            if (getCameraEnumerator().isFrontFacing(name)) {
                return name;
            }
        }
        
        // Final fallback to first available camera
        return deviceNames.length > 0 ? deviceNames[0] : null;
    }
    

    private CameraEnumerator getCameraEnumerator() {
        if (cameraEnumerator == null) {
            if (Camera2Enumerator.isSupported(reactContext)) {
                Log.d(TAG, "Creating camera enumerator using the Camera2 API");
                cameraEnumerator = new Camera2Enumerator(reactContext);
            } else {
                Log.d(TAG, "Creating camera enumerator using the Camera1 API");
                cameraEnumerator = new Camera1Enumerator(false);
            }
        }

        return cameraEnumerator;
    }

    ReadableArray enumerateDevices() {
        WritableArray array = Arguments.createArray();
        String[] devices = getCameraEnumerator().getDeviceNames();

        for (int i = 0; i < devices.length; ++i) {
            String deviceName = devices[i];
            boolean isFrontFacing;
            try {
                // This can throw an exception when using the Camera 1 API.
                isFrontFacing = getCameraEnumerator().isFrontFacing(deviceName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check the facing mode of camera");
                continue;
            }
            WritableMap params = Arguments.createMap();
            params.putString("facing", isFrontFacing ? "front" : "environment");
            params.putString("deviceId", "" + i);
            params.putString("groupId", "");
            params.putString("label", deviceName);
            params.putString("kind", "videoinput");
            array.pushMap(params);
        }

        WritableMap audio = Arguments.createMap();
        audio.putString("deviceId", "audio-1");
        audio.putString("groupId", "");
        audio.putString("label", "Audio");
        audio.putString("kind", "audioinput");
        array.pushMap(audio);

        return array;
    }

    MediaStreamTrack getTrack(String id) {
        TrackPrivate private_ = tracks.get(id);

        return private_ == null ? null : private_.track;
    }

    /**
     * Implements {@code getUserMedia}. Note that at this point constraints have
     * been normalized and permissions have been granted. The constraints only
     * contain keys for which permissions have already been granted, that is,
     * if audio permission was not granted, there will be no "audio" key in
     * the constraints map.
     */
    void getUserMedia(final ReadableMap constraints, final Callback successCallback, final Callback errorCallback) {
        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;

        if (constraints.hasKey("audio")) {
            audioTrack = createAudioTrack(constraints);
        }

        if (constraints.hasKey("video")) {
            ReadableMap videoConstraintsMap = constraints.getMap("video");

            CameraCaptureController videoCaptureController = new CameraCaptureController(
                    reactContext.getCurrentActivity(), getCameraEnumerator(), videoConstraintsMap);
            
            boolean effectsSdkRequired = getEffectsSDKConstraint(videoConstraintsMap);
            boolean hasGlobalEffectsSDK = globalEffectsSDKCapturer != null;
            
            if (effectsSdkRequired || hasGlobalEffectsSDK) {
                VideoCapturer effectsSDKCapturer = null;
                
                if (hasGlobalEffectsSDK) {
                    Log.d(TAG, "Reusing global EffectsSDK capturer");
                    effectsSDKCapturer = globalEffectsSDKCapturer;
                } else if (effectsSdkRequired) {
                    String cameraName = getCameraNameFromConstraints(videoConstraintsMap);
                    
                    if (cameraName != null) {
                        Log.d(TAG, "Creating new global EffectsSDK capturer for camera: " + cameraName);
                        
                        CameraVideoCapturer.CameraEventsHandler cameraEventsHandler = new CameraVideoCapturer.CameraEventsHandler() {
                                @Override
                                public void onCameraError(String errorDescription) {
                                    Log.e(TAG, "EffectsSDK camera error: " + errorDescription);
                                }
                                @Override
                                public void onCameraDisconnected() {
                                    Log.w(TAG, "EffectsSDK camera disconnected");
                                }
                                @Override
                                public void onCameraFreezed(String errorDescription) {
                                    Log.w(TAG, "EffectsSDK camera freezed: " + errorDescription);
                                }
                                @Override
                                public void onCameraOpening(String cameraName) {
                                    Log.d(TAG, "EffectsSDK camera opening: " + cameraName);
                                }
                                @Override
                                public void onFirstFrameAvailable() {
                                    Log.d(TAG, "EffectsSDK first frame available");
                                }
                                @Override
                                public void onCameraClosed() {
                                    Log.d(TAG, "EffectsSDK camera closed");
                                }
                            };
                            
                        effectsSDKCapturer = createEffectsSDKVideoCapturer(cameraName, cameraEventsHandler);
                        
                        if (effectsSDKCapturer instanceof EffectsSDKCameraCapturer) {
                            globalEffectsSDKCapturer = (EffectsSDKCameraCapturer) effectsSDKCapturer;
                            Log.d(TAG, "Stored new EffectsSDK capturer globally");
                        }
                    } else {
                        Log.w(TAG, "No camera found for EffectsSDK capturer");
                    }
                }
                
                if (effectsSDKCapturer != null) {
                    videoCaptureController.videoCapturer = effectsSDKCapturer;
                }
            }
            
            videoTrack = createVideoTrack(videoCaptureController);
        }

        if (audioTrack == null && videoTrack == null) {
            // Fail with DOMException with name AbortError as per:
            // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
            errorCallback.invoke("DOMException", "AbortError");
            return;
        }

        createStream(new MediaStreamTrack[] {audioTrack, videoTrack}, (streamId, tracksInfo) -> {
            WritableArray tracksInfoWritableArray = Arguments.createArray();

            for (WritableMap trackInfo : tracksInfo) {
                tracksInfoWritableArray.pushMap(trackInfo);
            }

            successCallback.invoke(streamId, tracksInfoWritableArray);
        });
    }

    void mediaStreamTrackSetEnabled(String trackId, final boolean enabled) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController != null) {
            if (enabled) {
                track.videoCaptureController.startCapture();
            } else {
                track.videoCaptureController.stopCapture();
            }
        }
    }

    void switchCamera(String trackId) {
        // Handle EffectsSDK capturer with direct pipeline control
        if (globalEffectsSDKCapturer != null) {
            CameraEnumerator cameraEnumerator = getCameraEnumerator();
            String[] deviceNames = cameraEnumerator.getDeviceNames();
            String currentDevice = globalEffectsSDKCapturer.getCurrentDevice();
            
            Log.d(TAG, "EffectsSDK switchCamera - current device: " + currentDevice + ", available devices: " + java.util.Arrays.toString(deviceNames));
            
            // Determine current camera facing mode once
            boolean currentIsFrontFacing;
            try {
                currentIsFrontFacing = cameraEnumerator.isFrontFacing(currentDevice);
                Log.d(TAG, "Current camera is " + (currentIsFrontFacing ? "front" : "back") + " facing");
            } catch (Exception e) {
                Log.e(TAG, "Failed to determine current camera facing mode for device: " + currentDevice, e);
                return;
            }
            
            for (String deviceName : deviceNames) {
                try {
                    boolean deviceIsFrontFacing = cameraEnumerator.isFrontFacing(deviceName);
                    
                    if (deviceIsFrontFacing != currentIsFrontFacing) {
                        globalEffectsSDKCapturer.switchCameraDevice(deviceName, new CameraVideoCapturer.CameraSwitchHandler() {
                            @Override
                            public void onCameraSwitchDone(boolean isFrontCamera) {
                                Log.d(TAG, "EffectsSDK camera switch successful for track " + trackId + 
                                          ", now using: " + (isFrontCamera ? "front" : "back") + " device: " + deviceName);
                            }
                            
                            @Override
                            public void onCameraSwitchError(String error) {
                                Log.e(TAG, "EffectsSDK camera switch failed for track " + trackId + ": " + error);
                            }
                        });
                        return;
                    } else {
                        Log.d(TAG, "Skipping device " + deviceName + " - same facing mode as current (" + (deviceIsFrontFacing ? "front" : "back") + ")");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to check facing mode for device " + deviceName + ": " + e.getMessage());
                }
            }
            
            Log.w(TAG, "No opposite camera found for EffectsSDK switch, current: " + currentDevice + " (" + (currentIsFrontFacing ? "front" : "back") + ")");
            return;
        }
        
        TrackPrivate trackPrivate = tracks.get(trackId);
        if (trackPrivate == null || !(trackPrivate.videoCaptureController instanceof CameraCaptureController)) {
            Log.w(TAG, "No suitable video capturer found for track: " + trackId);
            return;
        }
        
        CameraCaptureController controller = (CameraCaptureController) trackPrivate.videoCaptureController;
        VideoCapturer videoCapturer = controller.videoCapturer;
        
        if (!(videoCapturer instanceof CameraVideoCapturer)) {
            Log.w(TAG, "Video capturer is not a camera capturer for track: " + trackId);
            return;
        }
        
        switchCameraForStandardCapturer((CameraVideoCapturer) videoCapturer, trackId);
    }
    
    private void switchCameraForStandardCapturer(CameraVideoCapturer videoCapturer, String trackId) {
        CameraEnumerator cameraEnumerator = getCameraEnumerator();
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        
        for (String deviceName : deviceNames) {
            try {
                videoCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(boolean isFrontCamera) {
                        Log.d(TAG, "Standard camera switch successful for track " + trackId + 
                                  ", now using: " + (isFrontCamera ? "front" : "back"));
                    }
                    
                    @Override
                    public void onCameraSwitchError(String error) {
                        Log.e(TAG, "Standard camera switch failed for track " + trackId + ": " + error);
                    }
                }, deviceName);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to switch to device " + deviceName + " for track " + trackId + ": " + e.getMessage());
            }
        }
        
        Log.w(TAG, "No suitable camera found to switch to for track: " + trackId);
    }

    void disposeTrack(String id) {
        TrackPrivate track = tracks.remove(id);
        if (track != null) {
            if (track.videoCaptureController != null) {
                try {
                    track.videoCaptureController.stopCapture();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to stop video capturer for track " + id + ": " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Track disposed, global EffectsSDK capturer preserved: " + id);
            
            track.dispose();
        }
    }

    void applyConstraints(String trackId, ReadableMap constraints, Promise promise) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController instanceof AbstractVideoCaptureController) {
            AbstractVideoCaptureController captureController =
                    (AbstractVideoCaptureController) track.videoCaptureController;
            captureController.applyConstraints(constraints, new Consumer<Exception>() {
                public void accept(Exception e) {
                    if (e != null) {
                        promise.reject(e);
                        return;
                    }

                    promise.resolve(captureController.getSettings());
                }
            });
        } else {
            promise.reject(new Exception("Camera track not found!"));
        }
    }

    void getDisplayMedia(Promise promise) {
        if (this.displayMediaPromise != null) {
            promise.reject(new RuntimeException("Another operation is pending."));
            return;
        }

        Activity currentActivity = this.reactContext.getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(new RuntimeException("No current Activity."));
            return;
        }

        this.displayMediaPromise = promise;

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) currentActivity.getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);

        if (mediaProjectionManager != null) {
            UiThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentActivity.startActivityForResult(
                            mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_REQUEST_CODE);
                }
            });

        } else {
            promise.reject(new RuntimeException("MediaProjectionManager is null."));
        }
    }

    private void createScreenStream() {
        VideoTrack track = createScreenTrack();

        if (track == null) {
            displayMediaPromise.reject(new RuntimeException("ScreenTrack is null."));
        } else {
            createStream(new MediaStreamTrack[] {track}, (streamId, tracksInfo) -> {
                WritableMap data = Arguments.createMap();

                data.putString("streamId", streamId);

                if (tracksInfo.size() == 0) {
                    displayMediaPromise.reject(new RuntimeException("No ScreenTrackInfo found."));
                } else {
                    data.putMap("track", tracksInfo.get(0));
                    displayMediaPromise.resolve(data);
                }
            });
        }

        // Cleanup
        mediaProjectionPermissionResultData = null;
        displayMediaPromise = null;
    }

    void createStream(MediaStreamTrack[] tracks, BiConsumer<String, ArrayList<WritableMap>> successCallback) {
        String streamId = UUID.randomUUID().toString();
        MediaStream mediaStream = webRTCModule.mFactory.createLocalMediaStream(streamId);

        ArrayList<WritableMap> tracksInfo = new ArrayList<>();

        for (MediaStreamTrack track : tracks) {
            if (track == null) {
                continue;
            }

            if (track instanceof AudioTrack) {
                mediaStream.addTrack((AudioTrack) track);
            } else {
                mediaStream.addTrack((VideoTrack) track);
            }

            WritableMap trackInfo = Arguments.createMap();
            String trackId = track.id();

            trackInfo.putBoolean("enabled", track.enabled());
            trackInfo.putString("id", trackId);
            trackInfo.putString("kind", track.kind());
            trackInfo.putString("readyState", "live");
            trackInfo.putBoolean("remote", false);

            if (track instanceof VideoTrack) {
                TrackPrivate tp = this.tracks.get(trackId);
                AbstractVideoCaptureController vcc = tp.videoCaptureController;
                trackInfo.putMap("settings", vcc.getSettings());
            }

            if (track instanceof AudioTrack) {
                WritableMap settings = Arguments.createMap();
                settings.putString("deviceId", "audio-1");
                settings.putString("groupId", "");
                trackInfo.putMap("settings", settings);
            }

            tracksInfo.add(trackInfo);
        }

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModule.localStreams.put(streamId, mediaStream);

        successCallback.accept(streamId, tracksInfo);
    }

    private VideoTrack createScreenTrack() {
        DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics(reactContext.getCurrentActivity());
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        ScreenCaptureController screenCaptureController = new ScreenCaptureController(
                reactContext.getCurrentActivity(), width, height, mediaProjectionPermissionResultData);
        return createVideoTrack(screenCaptureController);
    }

    VideoTrack createVideoTrack(AbstractVideoCaptureController videoCaptureController) {
        videoCaptureController.initializeVideoCapturer();

        VideoCapturer videoCapturer = videoCaptureController.videoCapturer;
        if (videoCapturer == null) {
            return null;
        }
        

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext);

        if (surfaceTextureHelper == null) {
            Log.d(TAG, "Error creating SurfaceTextureHelper");
            return null;
        }

        String id = UUID.randomUUID().toString();
        
        TrackCapturerEventsEmitter eventsEmitter = new TrackCapturerEventsEmitter(webRTCModule, id);
        videoCaptureController.setCapturerEventsListener(eventsEmitter);

        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, reactContext, videoSource.getCapturerObserver());

        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        tracks.put(id, new TrackPrivate(track, videoSource, videoCaptureController, surfaceTextureHelper));

        videoCaptureController.startCapture();

        return track;
    }

    /**
     * Set video effects to the TrackPrivate corresponding to the trackId with the help of VideoEffectProcessor
     * corresponding to the names.
     * @param trackId TrackPrivate id
     * @param names VideoEffectProcessor names
     */
    void setVideoEffects(String trackId, ReadableArray names) {
        TrackPrivate track = tracks.get(trackId);

        if (track != null && track.videoCaptureController instanceof CameraCaptureController) {
            VideoSource videoSource = (VideoSource) track.mediaSource;
            SurfaceTextureHelper surfaceTextureHelper = track.surfaceTextureHelper;

            if (names != null) {
                List<VideoFrameProcessor> processors =
                        names.toArrayList()
                                .stream()
                                .filter(name -> name instanceof String)
                                .map(name -> {
                                    VideoFrameProcessor videoFrameProcessor =
                                            ProcessorProvider.getProcessor((String) name);
                                    if (videoFrameProcessor == null) {
                                        Log.e(TAG, "no videoFrameProcessor associated with this name: " + name);
                                    }
                                    return videoFrameProcessor;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                VideoEffectProcessor videoEffectProcessor = new VideoEffectProcessor(processors, surfaceTextureHelper);
                videoSource.setVideoProcessor(videoEffectProcessor);

            } else {
                videoSource.setVideoProcessor(null);
            }
        }
    }


    /**
     * Application/library-specific private members of local
     * {@code MediaStreamTrack}s created by {@code GetUserMediaImpl}.
     */
    private static class TrackPrivate {
        /**
         * The {@code MediaSource} from which {@link #track} was created.
         */
        public final MediaSource mediaSource;

        public final MediaStreamTrack track;

        /**
         * The {@code VideoCapturer} from which {@link #mediaSource} was created
         * if {@link #track} is a {@link VideoTrack}.
         */
        public final AbstractVideoCaptureController videoCaptureController;

        private final SurfaceTextureHelper surfaceTextureHelper;

        /**
         * Whether this object has been disposed or not.
         */
        private boolean disposed;

        /**
         * Initializes a new {@code TrackPrivate} instance.
         *
         * @param track
         * @param mediaSource            the {@code MediaSource} from which the specified
         *                               {@code code} was created
         * @param videoCaptureController the {@code AbstractVideoCaptureController} from which the
         *                               specified {@code mediaSource} was created if the specified
         *                               {@code track} is a {@link VideoTrack}
         */
        public TrackPrivate(MediaStreamTrack track, MediaSource mediaSource,
                AbstractVideoCaptureController videoCaptureController, SurfaceTextureHelper surfaceTextureHelper) {
            this.track = track;
            this.mediaSource = mediaSource;
            this.videoCaptureController = videoCaptureController;
            this.surfaceTextureHelper = surfaceTextureHelper;
            this.disposed = false;
        }

        public void dispose() {
            if (!disposed) {
                if (videoCaptureController != null) {
                    if (videoCaptureController.stopCapture()) {
                        videoCaptureController.dispose();
                    }
                }

                /*
                 * As per webrtc library documentation - The caller still has ownership of {@code
                 * surfaceTextureHelper} and is responsible for making sure surfaceTextureHelper.dispose() is
                 * called. This also means that the caller can reuse the SurfaceTextureHelper to initialize a new
                 * VideoCapturer once the previous VideoCapturer has been disposed. */

                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.stopListening();
                    surfaceTextureHelper.dispose();
                }

                mediaSource.dispose();
                track.dispose();
                disposed = true;
            }
        }
    }

    // EffectsSDK methods
    public void setEffectsSdkPipelineMode(String trackId, String pipelineMode) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setPipelineMode(pipelineMode);
        }
    }
    
    public void setEffectsSdkBlurPower(String trackId, double blurPower) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setBlurPower((float) blurPower);
        }
    }
    
    public void enableEffectsSdkVideoStream(String trackId, boolean enabled) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.enableVideo(enabled);
        }
    }
    
    public void enableEffectsSdkBeautification(String trackId, boolean enableBeautification) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.enableBeautification(enableBeautification);
        }
    }
    
    public boolean isEffectsSdkBeautificationEnabled(String trackId) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            return capturer.isBeautificationEnabled();
        }
        return false;
    }
    
    public void setEffectsSdkBeautificationPower(String trackId, double beautificationPower) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setBeautificationPower(beautificationPower);
        }
    }
    
    public void setEffectsSdkZoomLevel(String trackId, double zoomLevel) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setZoomLevel(zoomLevel);
        }
    }
    
    public double getEffectsSdkZoomLevel(String trackId) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            return capturer.getZoomLevel();
        }
        return 0.0;
    }
    
    public void enableEffectsSdkSharpening(String trackId, boolean enableSharpening) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.enableSharpening(enableSharpening);
        }
    }
    
    public void setEffectsSdkSharpeningStrength(String trackId, double strength) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setSharpeningStrength(strength);
        }
    }
    
    public double getEffectsSdkSharpeningStrength(String trackId) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            return capturer.getSharpeningStrength();
        }
        return 0.0;
    }
    
    public void setEffectsSdkColorFilterStrength(String trackId, double strength) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setColorFilterStrength(strength);
        }
    }
    
    public void setEffectsSdkColorCorrectionMode(String trackId, String colorCorrectionMode) {
        EffectsSDKCameraCapturer capturer = getEffectsSdkVideoCapturer(trackId);
        if (capturer != null) {
            capturer.setColorCorrectionMode(colorCorrectionMode);
        }
    }

    public interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}
