"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var base64 = _interopRequireWildcard(require("base64-js"));
var _reactNative = require("react-native");
var _Logger = _interopRequireDefault(require("./Logger"));
function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }
function _getRequireWildcardCache(nodeInterop) { if (typeof WeakMap !== "function") return null; var cacheBabelInterop = new WeakMap(); var cacheNodeInterop = new WeakMap(); return (_getRequireWildcardCache = function (nodeInterop) { return nodeInterop ? cacheNodeInterop : cacheBabelInterop; })(nodeInterop); }
function _interopRequireWildcard(obj, nodeInterop) { if (!nodeInterop && obj && obj.__esModule) { return obj; } if (obj === null || typeof obj !== "object" && typeof obj !== "function") { return { default: obj }; } var cache = _getRequireWildcardCache(nodeInterop); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (key !== "default" && Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj.default = obj; if (cache) { cache.set(obj, newObj); } return newObj; }
function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }
const {
  WebRTCModule
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