import { EventTarget, Event, defineEventAttribute } from 'event-target-shim/index';
import { NativeModules } from 'react-native';

import { MediaTrackConstraints } from './Constraints';
import { addListener, removeListener } from './EventEmitter';
import Logger from './Logger';
import { deepClone, normalizeConstraints } from './RTCUtil';

const log = new Logger('pc');
const { WebRTCModule } = NativeModules;

type MediaStreamTrackState = 'live' | 'ended';

export type MediaStreamTrackInfo = {
    id: string;
    kind: string;
    remote: boolean;
    constraints: object;
    enabled: boolean;
    settings: object;
    peerConnectionId: number;
    readyState: MediaStreamTrackState;
}

export type MediaTrackSettings = {
    width?: number;
    height?: number;
    frameRate?: number;
    facingMode?: string;
    deviceId?: string;
    groupId?: string;
}

type MediaStreamTrackEventMap = {
    ended: Event<'ended'>;
    mute: Event<'mute'>;
    unmute: Event<'unmute'>;
}

export default class MediaStreamTrack extends EventTarget<MediaStreamTrackEventMap> {
    _constraints: MediaTrackConstraints;
    _enabled: boolean;
    _settings: MediaTrackSettings;
    _muted: boolean;
    _peerConnectionId: number;
    _readyState: MediaStreamTrackState;

    readonly id: string;
    readonly kind: string;
    readonly label: string = '';
    readonly remote: boolean;

    constructor(info: MediaStreamTrackInfo) {
        super();

        this._constraints = info.constraints || {};
        this._enabled = info.enabled;
        this._settings = info.settings || {};
        this._muted = false;
        this._peerConnectionId = info.peerConnectionId;
        this._readyState = info.readyState;

        this.id = info.id;
        this.kind = info.kind;
        this.remote = info.remote;

        if (!this.remote) {
            this._registerEvents();
        }
    }

    get enabled(): boolean {
        return this._enabled;
    }

    set enabled(enabled: boolean) {
        if (enabled === this._enabled) {
            return;
        }

        this._enabled = Boolean(enabled);

        if (this._readyState === 'ended') {
            return;
        }

        WebRTCModule.mediaStreamTrackSetEnabled(this.remote ? this._peerConnectionId : -1, this.id, this._enabled);
    }

    get muted(): boolean {
        return this._muted;
    }

    get readyState(): string {
        return this._readyState;
    }

    stop(): void {
        this.enabled = false;
        this._readyState = 'ended';
    }

    /**
     * Private / custom API for switching the cameras on the fly, without the
     * need for adding / removing tracks or doing any SDP renegotiation.
     *
     * This is how the reference application (AppRTCMobile) implements camera
     * switching.
     *
     * @deprecated Use applyConstraints instead.
     */
    _switchCamera(): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        const constraints = deepClone(this._settings);

        delete constraints.deviceId;
        constraints.facingMode = this._settings.facingMode === 'user' ? 'environment' : 'user';

        this.applyConstraints(constraints);
    }

    _setVideoEffects(names: string[]) {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.mediaStreamTrackSetVideoEffects(this.id, names);
    }

    _setVideoEffect(name: string) {
        this._setVideoEffects([ name ]);
    }

    /**
     * Initialize EffectsSDK for this video track
     */
    async initializeEffectsSDK(customerId: string, url?: string): Promise<string> {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        return WebRTCModule.initializeEffectsSdk(this.id, customerId, url);
    }

    /**
     * Check if EffectsSDK is initialized for this video track
     */
    async isInitialized(): Promise<boolean> {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        return WebRTCModule.isInitialized(this.id);
    }

    /**
     * Set EffectsSDK pipeline mode
     */
    setEffectsSdkPipelineMode(pipelineMode: string): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkPipelineMode(this.id, pipelineMode);
    }

    /**
     * Set EffectsSDK blur power
     */
    setEffectsSdkBlurPower(blurPower: number): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkBlurPower(this.id, blurPower);
    }

    /**
     * Enable/disable EffectsSDK video stream
     */
    enableEffectsSdkVideoStream(enabled: boolean): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.enableEffectsSdkVideoStream(this.id, enabled);
    }

    /**
     * Enable/disable EffectsSDK beautification
     */
    enableEffectsSdkBeautification(enabled: boolean): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.enableEffectsSdkBeautification(this.id, enabled);
    }

    /**
     * Check if EffectsSDK beautification is enabled
     */
    async isEffectsSdkBeautificationEnabled(): Promise<boolean> {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        return WebRTCModule.isEffectsSdkBeautificationEnabled(this.id);
    }

    /**
     * Set EffectsSDK beautification power
     */
    setEffectsSdkBeautificationPower(power: number): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkBeautificationPower(this.id, power);
    }

    /**
     * Set EffectsSDK zoom level
     */
    setEffectsSdkZoomLevel(zoomLevel: number): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkZoomLevel(this.id, zoomLevel);
    }

    /**
     * Get EffectsSDK zoom level
     */
    async getEffectsSdkZoomLevel(): Promise<number> {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        return WebRTCModule.getEffectsSdkZoomLevel(this.id);
    }

    /**
     * Enable/disable EffectsSDK sharpening
     */
    enableEffectsSdkSharpening(enabled: boolean): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.enableEffectsSdkSharpening(this.id, enabled);
    }

    /**
     * Set EffectsSDK sharpening strength
     */
    setEffectsSdkSharpeningStrength(strength: number): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkSharpeningStrength(this.id, strength);
    }

    /**
     * Get EffectsSDK sharpening strength
     */
    async getEffectsSdkSharpeningStrength(): Promise<number> {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        return WebRTCModule.getEffectsSdkSharpeningStrength(this.id);
    }

    /**
     * Set EffectsSDK color filter strength
     */
    setEffectsSdkColorFilterStrength(strength: number): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkColorFilterStrength(this.id, strength);
    }

    /**
     * Set EffectsSDK color correction mode
     */
    setEffectsSdkColorCorrectionMode(mode: string): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.setEffectsSdkColorCorrectionMode(this.id, mode);
    }

    /**
     * Internal function which is used to set the muted state on remote tracks and
     * emit the mute / unmute event.
     *
     * @param muted Whether the track should be marked as muted / unmuted.
     */
    _setMutedInternal(muted: boolean) {
        if (!this.remote) {
            throw new Error('Track is not remote!');
        }

        this._muted = muted;
        this.dispatchEvent(new Event(muted ? 'mute' : 'unmute'));
    }

    /**
     * Custom API for setting the volume on an individual audio track.
     *
     * @param volume a gain value in the range of 0-10. defaults to 1.0
     */
    _setVolume(volume: number) {
        if (this.kind !== 'audio') {
            throw new Error('Only implemented for audio tracks');
        }

        WebRTCModule.mediaStreamTrackSetVolume(this.remote ? this._peerConnectionId : -1, this.id, volume);
    }

    /**
     * Applies a new set of constraints to the track.
     *
     * @param constraints An object listing the constraints
     * to apply to the track's constrainable properties; any existing
     * constraints are replaced with the new values specified, and any
     * constrainable properties not included are restored to their default
     * constraints. If this parameter is omitted, all currently set custom
     * constraints are cleared.
     */
    async applyConstraints(constraints?: MediaTrackConstraints): Promise<void> {
        if (this.kind !== 'video') {
            log.info(`Only implemented for video tracks, ignoring applyConstraints for ${this.id}`);

            return;
        }

        const normalized = normalizeConstraints({ video: constraints ?? true });

        this._settings = await WebRTCModule.mediaStreamTrackApplyConstraints(this.id, normalized.video);
        this._constraints = constraints ?? {};
    }

    clone(): never {
        throw new Error('Not implemented.');
    }

    getCapabilities(): never {
        throw new Error('Not implemented.');
    }

    getConstraints() {
        return deepClone(this._constraints);
    }

    getSettings(): MediaTrackSettings {
        return deepClone(this._settings);
    }

    _registerEvents(): void {
        addListener(this, 'mediaStreamTrackEnded', (ev: any) => {
            if (ev.trackId !== this.id || this._readyState === 'ended') {
                return;
            }

            log.debug(`${this.id} mediaStreamTrackEnded`);
            this._readyState = 'ended';

            this.dispatchEvent(new Event('ended'));
        });
    }

    release(): void {
        if (this.remote) {
            return;
        }

        removeListener(this);
        WebRTCModule.mediaStreamTrackRelease(this.id);
    }
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = MediaStreamTrack.prototype;

defineEventAttribute(proto, 'ended');
defineEventAttribute(proto, 'mute');
defineEventAttribute(proto, 'unmute');
