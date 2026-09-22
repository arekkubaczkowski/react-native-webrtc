# WebRTC
-keep class livekit.org.webrtc.** { *; }

# The native library resolves JniInit by name from C++ and nothing in Java
# references it, so R8 strips it and jni_zero aborts the process on the first
# PeerConnectionFactory.initialize. io.github.webrtc-sdk:android-prefixed ships
# this package under livekit.org too, so an org.jni_zero rule matches nothing.
-keep class livekit.org.jni_zero.** { *; }

# tsvb-react-native resolves these by name through Class.forName, inside a
# try/catch that only logs — R8 renaming them disables Android video effects
# with no error.
-keep class com.oney.WebRTCModule.videoEffects.CapturerProvider { *; }
-keep class com.oney.WebRTCModule.videoEffects.CapturerFactoryInterface { *; }
