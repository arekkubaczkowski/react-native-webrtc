"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AudioEngineMuteMode = exports.AudioEngineAvailability = exports.AudioDeviceModule = void 0;
var _reactNative = require("react-native");
const {
  WebRTCModule
} = _reactNative.NativeModules;
let AudioEngineMuteMode;
exports.AudioEngineMuteMode = AudioEngineMuteMode;
(function (AudioEngineMuteMode) {
  AudioEngineMuteMode[AudioEngineMuteMode["Unknown"] = -1] = "Unknown";
  AudioEngineMuteMode[AudioEngineMuteMode["VoiceProcessing"] = 0] = "VoiceProcessing";
  AudioEngineMuteMode[AudioEngineMuteMode["RestartEngine"] = 1] = "RestartEngine";
  AudioEngineMuteMode[AudioEngineMuteMode["InputMixer"] = 2] = "InputMixer";
})(AudioEngineMuteMode || (exports.AudioEngineMuteMode = AudioEngineMuteMode = {}));
const AudioEngineAvailability = {
  default: {
    isInputAvailable: true,
    isOutputAvailable: true
  },
  none: {
    isInputAvailable: false,
    isOutputAvailable: false
  }
};

/**
 * Accepted values mirror the native observer's friendly-name maps. Unknown
 * values never reach the session: categories fall back to playAndRecord and
 * modes to default, and the literal unions below reject them at compile time.
 */

/**
 * Apple audio session configuration for a single engine state. Matches the
 * AppleAudioConfiguration shape used by AudioSession.setAppleAudioConfiguration.
 */

/**
 * Native default audio-session policy. When set, the native observer configures
 * the AVAudioSession in willEnable/didDisable without a JS round trip.
 * - `recording` is applied while recording is enabled,
 * - `playout` is applied while only playout is enabled,
 * - `deactivateOnStop` determines whether the session is deactivated when
 *   neither recording nor playout is enabled.
 */
exports.AudioEngineAvailability = AudioEngineAvailability;
/**
 * Audio Device Module API for controlling audio devices and settings.
 * iOS/macOS only - will throw on Android.
 */
class AudioDeviceModule {
  /**
   * Push (or clear with null) the native default audio-session configuration
   * policy. When set, the native observer configures the session itself in
   * willEnable/didDisable, removing the JS round trip from the default path.
   * iOS only (including tvOS), a no-op elsewhere. The macOS build excludes
   * the audio device module natives, so this must not reach the bridge there.
   *
   * Handler precedence is per hook:
   * while a policy is set, custom willEnable/didDisable handlers must be
   * registered or cleared as a pair, otherwise one regime can activate the
   * session while the other never releases it.
   *
   * Clearing a `deactivateOnStop: false` policy while the engine is already
   * stopped (playout and recording both disabled) keeps the session activation
   * held until the next engine cycle, because no callback fires in between.
   * For an immediate release, stop under `deactivateOnStop: true` before
   * clearing.
   */
  static setAutomaticAudioSessionConfiguration(config) {
    if (_reactNative.Platform.OS !== 'ios' || !WebRTCModule) {
      return;
    }
    WebRTCModule.audioDeviceModuleSetAutomaticAudioSessionConfiguration(config);
  }

  /**
   * Start audio playback
   */
  static async startPlayout() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStartPlayout();
  }

  /**
   * Stop audio playback
   */
  static async stopPlayout() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStopPlayout();
  }

  /**
   * Start audio recording
   */
  static async startRecording() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStartRecording();
  }

  /**
   * Stop audio recording
   */
  static async stopRecording() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStopRecording();
  }

  /**
   * Initialize and start local audio recording (calls initAndStartRecording)
   */
  static async startLocalRecording() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStartLocalRecording();
  }

  /**
   * Stop local audio recording
   */
  static async stopLocalRecording() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleStopLocalRecording();
  }

  /**
   * Mute or unmute the microphone
   */
  static async setMicrophoneMuted(muted) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetMicrophoneMuted(muted);
  }

  /**
   * Check if microphone is currently muted
   */
  static isMicrophoneMuted() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsMicrophoneMuted();
  }

  /**
   * Enable or disable voice processing (requires engine restart)
   */
  static async setVoiceProcessingEnabled(enabled) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetVoiceProcessingEnabled(enabled);
  }

  /**
   * Check if voice processing is enabled
   */
  static isVoiceProcessingEnabled() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsVoiceProcessingEnabled();
  }

  /**
   * Temporarily bypass voice processing without restarting the engine
   */
  static setVoiceProcessingBypassed(bypassed) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    WebRTCModule.audioDeviceModuleSetVoiceProcessingBypassed(bypassed);
  }

  /**
   * Check if voice processing is currently bypassed
   */
  static isVoiceProcessingBypassed() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsVoiceProcessingBypassed();
  }

  /**
   * Enable or disable Automatic Gain Control (AGC)
   */
  static setVoiceProcessingAGCEnabled(enabled) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetVoiceProcessingAGCEnabled(enabled);
  }

  /**
   * Check if AGC is enabled
   */
  static isVoiceProcessingAGCEnabled() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsVoiceProcessingAGCEnabled();
  }

  /**
   * Check if audio is currently playing
   */
  static isPlaying() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsPlaying();
  }

  /**
   * Check if audio is currently recording
   */
  static isRecording() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsRecording();
  }

  /**
   * Check if the audio engine is running
   */
  static isEngineRunning() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsEngineRunning();
  }

  /**
   * Set the microphone mute mode
   */
  static async setMuteMode(mode) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetMuteMode(mode);
  }

  /**
   * Get the current mute mode
   */
  static getMuteMode() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleGetMuteMode();
  }

  /**
   * Enable or disable advanced audio ducking
   */
  static setAdvancedDuckingEnabled(enabled) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetAdvancedDuckingEnabled(enabled);
  }

  /**
   * Check if advanced ducking is enabled
   */
  static isAdvancedDuckingEnabled() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsAdvancedDuckingEnabled();
  }

  /**
   * Set the audio ducking level (0-100)
   */
  static setDuckingLevel(level) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetDuckingLevel(level);
  }

  /**
   * Get the current ducking level
   */
  static getDuckingLevel() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleGetDuckingLevel();
  }

  /**
   * Check if recording always prepared mode is enabled
   */
  static isRecordingAlwaysPreparedMode() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleIsRecordingAlwaysPreparedMode();
  }

  /**
   * Enable or disable recording always prepared mode
   */
  static async setRecordingAlwaysPreparedMode(enabled) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetRecordingAlwaysPreparedMode(enabled);
  }

  /**
   * Get the current engine availability (input/output availability)
   */
  static getEngineAvailability() {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleGetEngineAvailability();
  }

  /**
   * Set the engine availability (input/output availability)
   */
  static async setEngineAvailability(availability) {
    if (_reactNative.Platform.OS === 'android') {
      throw new Error('AudioDeviceModule is only available on iOS/macOS');
    }
    return WebRTCModule.audioDeviceModuleSetEngineAvailability(availability);
  }
}
exports.AudioDeviceModule = AudioDeviceModule;
//# sourceMappingURL=AudioDeviceModule.js.map