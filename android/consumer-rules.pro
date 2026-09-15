# WebRTC
-keep class livekit.org.webrtc.** { *; }
-keep class org.jni_zero.** { *; }

# tsvb-react-native resolves these by name through Class.forName, inside a
# try/catch that only logs — R8 renaming them disables Android video effects
# with no error.
-keep class com.oney.WebRTCModule.videoEffects.CapturerProvider { *; }
-keep class com.oney.WebRTCModule.videoEffects.CapturerFactoryInterface { *; }
