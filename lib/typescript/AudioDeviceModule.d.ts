export declare enum AudioEngineMuteMode {
    Unknown = -1,
    VoiceProcessing = 0,
    RestartEngine = 1,
    InputMixer = 2
}
export interface AudioEngineAvailability {
    isInputAvailable: boolean;
    isOutputAvailable: boolean;
}
export declare const AudioEngineAvailability: {
    readonly default: {
        readonly isInputAvailable: true;
        readonly isOutputAvailable: true;
    };
    readonly none: {
        readonly isInputAvailable: false;
        readonly isOutputAvailable: false;
    };
};
/**
 * Accepted values mirror the native observer's friendly-name maps. Unknown
 * values never reach the session: categories fall back to playAndRecord and
 * modes to default, and the literal unions below reject them at compile time.
 */
export declare type AutomaticAppleAudioCategory = 'ambient' | 'soloAmbient' | 'playback' | 'record' | 'playAndRecord' | 'multiRoute';
export declare type AutomaticAppleAudioMode = 'default' | 'voiceChat' | 'videoChat' | 'gameChat' | 'videoRecording' | 'measurement' | 'moviePlayback' | 'spokenAudio' | 'voicePrompt';
export declare type AutomaticAppleAudioCategoryOption = 'mixWithOthers' | 'duckOthers' | 'allowBluetooth' | 'allowBluetoothA2DP' | 'allowAirPlay' | 'defaultToSpeaker' | 'interruptSpokenAudioAndMixWithOthers';
/**
 * Apple audio session configuration for a single engine state. Matches the
 * AppleAudioConfiguration shape used by AudioSession.setAppleAudioConfiguration.
 */
export interface AutomaticAppleAudioConfiguration {
    audioCategory?: AutomaticAppleAudioCategory;
    audioMode?: AutomaticAppleAudioMode;
    audioCategoryOptions?: AutomaticAppleAudioCategoryOption[];
}
/**
 * Native default audio-session policy. When set, the native observer configures
 * the AVAudioSession in willEnable/didDisable without a JS round trip.
 * - `recording` is applied while recording is enabled,
 * - `playout` is applied while only playout is enabled,
 * - `deactivateOnStop` determines whether the session is deactivated when
 *   neither recording nor playout is enabled.
 */
export interface AutomaticAudioSessionConfiguration {
    recording: AutomaticAppleAudioConfiguration;
    playout: AutomaticAppleAudioConfiguration;
    deactivateOnStop: boolean;
}
/**
 * Audio Device Module API for controlling audio devices and settings.
 * iOS/macOS only - will throw on Android.
 */
export declare class AudioDeviceModule {
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
    static setAutomaticAudioSessionConfiguration(config: AutomaticAudioSessionConfiguration | null): void;
    /**
     * Start audio playback
     */
    static startPlayout(): Promise<void>;
    /**
     * Stop audio playback
     */
    static stopPlayout(): Promise<void>;
    /**
     * Start audio recording
     */
    static startRecording(): Promise<void>;
    /**
     * Stop audio recording
     */
    static stopRecording(): Promise<void>;
    /**
     * Initialize and start local audio recording (calls initAndStartRecording)
     */
    static startLocalRecording(): Promise<void>;
    /**
     * Stop local audio recording
     */
    static stopLocalRecording(): Promise<void>;
    /**
     * Mute or unmute the microphone
     */
    static setMicrophoneMuted(muted: boolean): Promise<void>;
    /**
     * Check if microphone is currently muted
     */
    static isMicrophoneMuted(): boolean;
    /**
     * Enable or disable voice processing (requires engine restart)
     */
    static setVoiceProcessingEnabled(enabled: boolean): Promise<void>;
    /**
     * Check if voice processing is enabled
     */
    static isVoiceProcessingEnabled(): boolean;
    /**
     * Temporarily bypass voice processing without restarting the engine
     */
    static setVoiceProcessingBypassed(bypassed: boolean): void;
    /**
     * Check if voice processing is currently bypassed
     */
    static isVoiceProcessingBypassed(): boolean;
    /**
     * Enable or disable Automatic Gain Control (AGC)
     */
    static setVoiceProcessingAGCEnabled(enabled: boolean): void;
    /**
     * Check if AGC is enabled
     */
    static isVoiceProcessingAGCEnabled(): boolean;
    /**
     * Check if audio is currently playing
     */
    static isPlaying(): boolean;
    /**
     * Check if audio is currently recording
     */
    static isRecording(): boolean;
    /**
     * Check if the audio engine is running
     */
    static isEngineRunning(): boolean;
    /**
     * Set the microphone mute mode
     */
    static setMuteMode(mode: AudioEngineMuteMode): Promise<void>;
    /**
     * Get the current mute mode
     */
    static getMuteMode(): AudioEngineMuteMode;
    /**
     * Enable or disable advanced audio ducking
     */
    static setAdvancedDuckingEnabled(enabled: boolean): void;
    /**
     * Check if advanced ducking is enabled
     */
    static isAdvancedDuckingEnabled(): boolean;
    /**
     * Set the audio ducking level (0-100)
     */
    static setDuckingLevel(level: number): void;
    /**
     * Get the current ducking level
     */
    static getDuckingLevel(): number;
    /**
     * Check if recording always prepared mode is enabled
     */
    static isRecordingAlwaysPreparedMode(): boolean;
    /**
     * Enable or disable recording always prepared mode
     */
    static setRecordingAlwaysPreparedMode(enabled: boolean): Promise<void>;
    /**
     * Get the current engine availability (input/output availability)
     */
    static getEngineAvailability(): AudioEngineAvailability;
    /**
     * Set the engine availability (input/output availability)
     */
    static setEngineAvailability(availability: AudioEngineAvailability): Promise<void>;
}
