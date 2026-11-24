function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
import * as base64 from 'base64-js';
import { NativeModules } from 'react-native';
const {
  LKRNWebRTCModule: WebRTCModule
} = NativeModules;
export let FrameCryptorState;
(function (FrameCryptorState) {
  FrameCryptorState[FrameCryptorState["FrameCryptorStateNew"] = 0] = "FrameCryptorStateNew";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateOk"] = 1] = "FrameCryptorStateOk";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateEncryptionFailed"] = 2] = "FrameCryptorStateEncryptionFailed";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateDecryptionFailed"] = 3] = "FrameCryptorStateDecryptionFailed";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateMissingKey"] = 4] = "FrameCryptorStateMissingKey";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateKeyRatcheted"] = 5] = "FrameCryptorStateKeyRatcheted";
  FrameCryptorState[FrameCryptorState["FrameCryptorStateInternalError"] = 6] = "FrameCryptorStateInternalError";
})(FrameCryptorState || (FrameCryptorState = {}));
export default class RTCKeyProvider {
  constructor(keyProviderId) {
    _defineProperty(this, "_id", void 0);
    this._id = keyProviderId;
  }
  async setSharedKey(key, keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      keyIndex
    };
    if (typeof key === 'string') {
      params['key'] = key;
      params['keyIsBase64'] = false;
    } else {
      params['key'] = base64.fromByteArray(key);
      params['keyIsBase64'] = true;
    }
    return WebRTCModule.keyProviderSetSharedKey(params).then(data => data['result']);
  }
  async ratchetSharedKey(keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      keyIndex
    };
    return WebRTCModule.keyProviderRatchetSharedKey(params).then(data => base64.toByteArray(data['result']));
  }
  async exportSharedKey(keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      keyIndex
    };
    return WebRTCModule.keyProviderExportSharedKey(params).then(data => base64.toByteArray(data['result']));
  }
  async setKey(participantId, key, keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      participantId,
      keyIndex
    };
    if (typeof key === 'string') {
      params['key'] = key;
      params['keyIsBase64'] = false;
    } else {
      params['key'] = base64.fromByteArray(key);
      params['keyIsBase64'] = true;
    }
    return WebRTCModule.keyProviderSetKey(params).then(data => data['result']);
  }
  async ratchetKey(participantId, keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      participantId,
      keyIndex
    };
    return WebRTCModule.keyProviderRatchetKey(params).then(data => base64.toByteArray(data['result']));
  }
  async exportKey(participantId, keyIndex = 0) {
    const params = {
      keyProviderId: this._id,
      participantId,
      keyIndex
    };
    return WebRTCModule.keyProviderExportKey(params).then(data => base64.toByteArray(data['result']));
  }
  async setSifTrailer(trailer) {
    const params = {
      keyProviderId: this._id,
      'sifTrailer': base64.fromByteArray(trailer)
    };
    return WebRTCModule.keyProviderSetSifTrailer(params);
  }
  async dispose() {
    const params = {
      keyProviderId: this._id
    };
    return WebRTCModule.keyProviderDispose(params);
  }
}
//# sourceMappingURL=RTCKeyProvider.js.map