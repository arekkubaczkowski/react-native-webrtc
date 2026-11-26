function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
import * as base64 from 'base64-js';
import { NativeModules } from 'react-native';
import Logger from './Logger';
const {
  LKRNWebRTCModule: WebRTCModule
} = NativeModules;
const log = new Logger('pc');
export default class RTCDataPacketCryptor {
  constructor(dataPacketCryptorId) {
    _defineProperty(this, "_id", void 0);
    this._id = dataPacketCryptorId;
  }
  async encrypt(participantId, keyIndex, data) {
    const params = {
      dataPacketCryptorId: this._id,
      participantId,
      keyIndex,
      data: base64.fromByteArray(data)
    };
    const result = await WebRTCModule.dataPacketCryptorEncrypt(params);
    if (!result) {
      log.info('encrypt: result null');
      return null;
    }
    if (result.payload === undefined) {
      log.info('encrypt: payload null');
      return null;
    }
    if (result.iv === undefined) {
      log.info('encrypt: iv null');
      return null;
    }
    if (result.keyIndex === undefined) {
      log.info('encrypt: keyIndex null');
      return null;
    }
    return {
      payload: base64.toByteArray(result.payload),
      iv: base64.toByteArray(result.iv),
      keyIndex: result.keyIndex
    };
  }
  async decrypt(participantId, packet) {
    const params = {
      dataPacketCryptorId: this._id,
      participantId,
      payload: base64.fromByteArray(packet.payload),
      iv: base64.fromByteArray(packet.iv),
      keyIndex: packet.keyIndex
    };
    const result = await WebRTCModule.dataPacketCryptorDecrypt(params);
    if (!result) {
      log.info('decrypt: result null');
      return null;
    }
    return base64.toByteArray(result.data);
  }
  async dispose() {
    const params = {
      dataPacketCryptorId: this._id
    };
    await WebRTCModule.dataPacketCryptorDispose(params);
  }
}
//# sourceMappingURL=RTCDataPacketCryptor.js.map