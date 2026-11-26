"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var base64 = _interopRequireWildcard(require("base64-js"));
var _reactNative = require("react-native");
var _Logger = _interopRequireDefault(require("./Logger"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
var cache;
function _interopRequireWildcard(e, t) { if (!t && e && e.__esModule) return e; var r, c, a = Object.defineProperty, n = { __proto__: null, default: e }; if (Object(e) !== e) return n; if (cache || "function" != typeof WeakMap || (cache = new WeakMap()), cache) { if (cache.has(e)) return cache.get(e); cache.set(e, n); } for (c in e) "default" !== c && {}.hasOwnProperty.call(e, c) && ((r = a && Object.getOwnPropertyDescriptor(e, c)) && (r.get || r.set) ? a(n, c, r) : n[c] = e[c]); return n; }
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
const {
  LKRNWebRTCModule: WebRTCModule
} = _reactNative.NativeModules;
const log = new _Logger.default('pc');
class RTCDataPacketCryptor {
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
exports.default = RTCDataPacketCryptor;
//# sourceMappingURL=RTCDataPacketCryptor.js.map