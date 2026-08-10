function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }
import { NativeModules, Platform } from 'react-native';
import { addListener, removeListener } from './EventEmitter';
const {
  WebRTCModule
} = NativeModules;

/**
 * Raw native event payload. Every engine event carries a `requestId` that must
 * be echoed back to the matching resolve call so the native side can drop a
 * response from a round that already timed out. This id is internal and is not
 * surfaced to app-registered handlers.
 */

// Empty object for events with no data

/**
 * Handler function that must return a number (0 for success, non-zero for error)
 */

/**
 * Event emitter for RTCAudioDeviceModule delegate callbacks.
 * iOS/macOS only.
 */
class AudioDeviceModuleEventEmitter {
  constructor() {
    _defineProperty(this, "engineCreatedHandler", null);
    _defineProperty(this, "willEnableEngineHandler", null);
    _defineProperty(this, "willStartEngineHandler", null);
    _defineProperty(this, "didStopEngineHandler", null);
    _defineProperty(this, "didDisableEngineHandler", null);
    _defineProperty(this, "willReleaseEngineHandler", null);
    _defineProperty(this, "listenersSetUp", false);
  }
  setupListeners() {
    if (Platform.OS !== 'android' && WebRTCModule) {
      // Reconcile on every call so a recreated native observer (which defaults
      // every hook to active) is re-synced to the current handler state even
      // when JS listeners are already registered. addListener appends without
      // de-duping, so the registration below must run only once.
      if (this.listenersSetUp) {
        this.reconcileActiveFlags();
        return;
      }
      this.listenersSetUp = true;

      // Setup handlers for blocking delegate methods
      addListener(this, 'audioDeviceModuleEngineCreated', async event => {
        const {
          requestId
        } = event;
        let result = 0;
        if (this.engineCreatedHandler) {
          try {
            await this.engineCreatedHandler();
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveEngineCreated(requestId, result);
      });
      addListener(this, 'audioDeviceModuleEngineWillEnable', async event => {
        const {
          requestId,
          isPlayoutEnabled,
          isRecordingEnabled
        } = event;
        let result = 0;
        if (this.willEnableEngineHandler) {
          try {
            await this.willEnableEngineHandler({
              isPlayoutEnabled,
              isRecordingEnabled
            });
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveWillEnableEngine(requestId, result);
      });
      addListener(this, 'audioDeviceModuleEngineWillStart', async event => {
        const {
          requestId,
          isPlayoutEnabled,
          isRecordingEnabled
        } = event;
        let result = 0;
        if (this.willStartEngineHandler) {
          try {
            await this.willStartEngineHandler({
              isPlayoutEnabled,
              isRecordingEnabled
            });
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveWillStartEngine(requestId, result);
      });
      addListener(this, 'audioDeviceModuleEngineDidStop', async event => {
        const {
          requestId,
          isPlayoutEnabled,
          isRecordingEnabled
        } = event;
        let result = 0;
        if (this.didStopEngineHandler) {
          try {
            await this.didStopEngineHandler({
              isPlayoutEnabled,
              isRecordingEnabled
            });
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveDidStopEngine(requestId, result);
      });
      addListener(this, 'audioDeviceModuleEngineDidDisable', async event => {
        const {
          requestId,
          isPlayoutEnabled,
          isRecordingEnabled
        } = event;
        let result = 0;
        if (this.didDisableEngineHandler) {
          try {
            await this.didDisableEngineHandler({
              isPlayoutEnabled,
              isRecordingEnabled
            });
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveDidDisableEngine(requestId, result);
      });
      addListener(this, 'audioDeviceModuleEngineWillRelease', async event => {
        const {
          requestId
        } = event;
        let result = 0;
        if (this.willReleaseEngineHandler) {
          try {
            await this.willReleaseEngineHandler();
          } catch (error) {
            // If error is a number, use it as the error code, otherwise use -1
            result = typeof error === 'number' ? error : -1;
          }
        }
        WebRTCModule.audioDeviceModuleResolveWillReleaseEngine(requestId, result);
      });
      this.reconcileActiveFlags();
    }
  }

  /**
   * Subscribe to speech activity events (started/ended)
   */
  addSpeechActivityListener(listener) {
    addListener(listener, 'audioDeviceModuleSpeechActivity', listener);
  }

  /**
   * Remove a previously registered speech activity listener
   */
  removeSpeechActivityListener(listener) {
    removeListener(listener);
  }

  /**
   * Subscribe to devices updated event (input/output devices changed)
   */
  addDevicesUpdatedListener(listener) {
    addListener(listener, 'audioDeviceModuleDevicesUpdated', listener);
  }

  /**
   * Remove a previously registered devices updated listener
   */
  removeDevicesUpdatedListener(listener) {
    removeListener(listener);
  }

  /**
   * Push a handler's active state to native. The native active-flag setters are
   * iOS/macOS only, so this is gated like setupListeners() to avoid a TypeError
   * on Android, where these methods do not exist.
   */
  pushHandlerActive(method, isActive) {
    if (Platform.OS !== 'android' && WebRTCModule) {
      WebRTCModule[method](isActive);
    }
  }

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
  applyHandlerActive(method, wasActive, isActive, assign) {
    if (isActive && !wasActive) {
      this.pushHandlerActive(method, true);
    }
    assign();
    if (!isActive && wasActive) {
      this.pushHandlerActive(method, false);
    }
  }

  /**
   * Push the current handler state for every hook to native. Native defaults
   * each hook to active, so this re-syncs a fresh or recreated observer to the
   * handlers registered now rather than relying on a set/clear transition that
   * may have already happened.
   */
  reconcileActiveFlags() {
    const activeFlags = [['audioDeviceModuleSetEngineCreatedActive', this.engineCreatedHandler !== null], ['audioDeviceModuleSetWillEnableEngineActive', this.willEnableEngineHandler !== null], ['audioDeviceModuleSetWillStartEngineActive', this.willStartEngineHandler !== null], ['audioDeviceModuleSetDidStopEngineActive', this.didStopEngineHandler !== null], ['audioDeviceModuleSetDidDisableEngineActive', this.didDisableEngineHandler !== null], ['audioDeviceModuleSetWillReleaseEngineActive', this.willReleaseEngineHandler !== null]];
    for (const [method, isActive] of activeFlags) {
      this.pushHandlerActive(method, isActive);
    }
  }

  /**
   * Set handler for engine created delegate - MUST return 0 for success or error code
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setEngineCreatedHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetEngineCreatedActive', this.engineCreatedHandler !== null, handler !== null, () => {
      this.engineCreatedHandler = handler;
    });
  }

  /**
   * Set handler for will enable engine delegate - MUST return 0 for success or error code
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setWillEnableEngineHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetWillEnableEngineActive', this.willEnableEngineHandler !== null, handler !== null, () => {
      this.willEnableEngineHandler = handler;
    });
  }

  /**
   * Set handler for will start engine delegate - MUST return 0 for success or error code
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setWillStartEngineHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetWillStartEngineActive', this.willStartEngineHandler !== null, handler !== null, () => {
      this.willStartEngineHandler = handler;
    });
  }

  /**
   * Set handler for did stop engine delegate - MUST return 0 for success or error code
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setDidStopEngineHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetDidStopEngineActive', this.didStopEngineHandler !== null, handler !== null, () => {
      this.didStopEngineHandler = handler;
    });
  }

  /**
   * Set handler for did disable engine delegate - MUST return 0 for success or error code
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setDidDisableEngineHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetDidDisableEngineActive', this.didDisableEngineHandler !== null, handler !== null, () => {
      this.didDisableEngineHandler = handler;
    });
  }

  /**
   * Set handler for will release engine delegate
   * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
   */
  setWillReleaseEngineHandler(handler) {
    this.applyHandlerActive('audioDeviceModuleSetWillReleaseEngineActive', this.willReleaseEngineHandler !== null, handler !== null, () => {
      this.willReleaseEngineHandler = handler;
    });
  }
}
export const audioDeviceModuleEvents = new AudioDeviceModuleEventEmitter();
//# sourceMappingURL=AudioDeviceModuleEvents.js.map