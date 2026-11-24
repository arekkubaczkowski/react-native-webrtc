export declare type SpeechActivityEvent = 'started' | 'ended';
export interface SpeechActivityEventData {
    event: SpeechActivityEvent;
}
export interface EngineStateEventData {
    isPlayoutEnabled: boolean;
    isRecordingEnabled: boolean;
}
/**
 * Engine state for the willEnable/didDisable pair, which additionally reports
 * whether Apple Voice Processing I/O is running. Use it to pick a session mode
 * that matches the processing implementation - the voiceChat/videoChat modes
 * engage iOS's call-tuned speaker gain, which only VPIO compensates for.
 *
 * On willEnable this is the resolved state the engine is transitioning to, and
 * is not yet readable from the module. On didDisable it is the state the last
 * willEnable reported, because the input path is already torn down by then and
 * the live value would always read false.
 */
export interface EngineProcessingStateEventData extends EngineStateEventData {
    isVoiceProcessingEnabled: boolean;
}
export declare type AudioDeviceModuleEventType = 'speechActivity' | 'devicesUpdated';
export declare type AudioDeviceModuleEventData = SpeechActivityEventData | EngineStateEventData | EngineProcessingStateEventData | Record<string, never>;
export declare type AudioDeviceModuleEventListener = (data: AudioDeviceModuleEventData) => void;
/**
 * Handler function that must return a number (0 for success, non-zero for error)
 */
export declare type AudioEngineEventNoParamsHandler = () => Promise<void>;
export declare type AudioEngineEventHandler = (params: EngineStateEventData) => Promise<void>;
/**
 * Handler for the willEnable/didDisable hooks, which also report the Apple
 * Voice Processing I/O state. Assignable from an {@link AudioEngineEventHandler},
 * so existing handlers keep working.
 */
export declare type AudioEngineProcessingEventHandler = (params: EngineProcessingStateEventData) => Promise<void>;
/**
 * Event emitter for RTCAudioDeviceModule delegate callbacks.
 * iOS/macOS only.
 */
declare class AudioDeviceModuleEventEmitter {
    private engineCreatedHandler;
    private willEnableEngineHandler;
    private willStartEngineHandler;
    private didStopEngineHandler;
    private didDisableEngineHandler;
    private willReleaseEngineHandler;
    private listenersSetUp;
    setupListeners(): void;
    /**
     * Subscribe to speech activity events (started/ended)
     */
    addSpeechActivityListener(listener: (data: SpeechActivityEventData) => void): void;
    /**
     * Remove a previously registered speech activity listener
     */
    removeSpeechActivityListener(listener: (data: SpeechActivityEventData) => void): void;
    /**
     * Subscribe to devices updated event (input/output devices changed)
     */
    addDevicesUpdatedListener(listener: () => void): void;
    /**
     * Remove a previously registered devices updated listener
     */
    removeDevicesUpdatedListener(listener: () => void): void;
    /**
     * Push a handler's active state to native. The native active-flag setters are
     * iOS/macOS only, so this is gated like setupListeners() to avoid a TypeError
     * on Android, where these methods do not exist.
     */
    private pushHandlerActive;
    /**
     * Apply a handler change while keeping the native active flag ordered so a
     * delegate callback racing registration can never skip a handler that exists.
     *
     * On activation (null to non-null) the flag is pushed active *before* the
     * handler is published, so the worst case is a JS round trip whose listener
     * sees the not-yet-published handler and resolves 0, never a skipped veto. On
     * deactivation the handler is cleared first, then the flag pushed inactive, so
     * the same safe ordering holds in reverse. No push happens when active state
     * is unchanged.
     */
    private applyHandlerActive;
    /**
     * Push the current handler state for every hook to native. Native defaults
     * each hook to active, so this re-syncs a fresh or recreated observer to the
     * handlers registered now rather than relying on a set/clear transition that
     * may have already happened.
     */
    private reconcileActiveFlags;
    /**
     * Set handler for engine created delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setEngineCreatedHandler(handler: AudioEngineEventNoParamsHandler | null): void;
    /**
     * Set handler for will enable engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillEnableEngineHandler(handler: AudioEngineProcessingEventHandler | null): void;
    /**
     * Set handler for will start engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillStartEngineHandler(handler: AudioEngineEventHandler | null): void;
    /**
     * Set handler for did stop engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setDidStopEngineHandler(handler: AudioEngineEventHandler | null): void;
    /**
     * Set handler for did disable engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setDidDisableEngineHandler(handler: AudioEngineProcessingEventHandler | null): void;
    /**
     * Set handler for will release engine delegate
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillReleaseEngineHandler(handler: AudioEngineEventNoParamsHandler | null): void;
}
export declare const audioDeviceModuleEvents: AudioDeviceModuleEventEmitter;
export {};
