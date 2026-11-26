"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = exports.RTCKeyDerivationAlgorithm = exports.RTCFrameCryptorAlgorithm = void 0;
var base64 = _interopRequireWildcard(require("base64-js"));
var _reactNative = require("react-native");
var _RTCFrameCryptor = _interopRequireDefault(require("./RTCFrameCryptor"));
var _RTCKeyProvider = _interopRequireDefault(require("./RTCKeyProvider"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
var cache;
function _interopRequireWildcard(e, t) { if (!t && e && e.__esModule) return e; var r, c, a = Object.defineProperty, n = { __proto__: null, default: e }; if (Object(e) !== e) return n; if (cache || "function" != typeof WeakMap || (cache = new WeakMap()), cache) { if (cache.has(e)) return cache.get(e); cache.set(e, n); } for (c in e) "default" !== c && {}.hasOwnProperty.call(e, c) && ((r = a && Object.getOwnPropertyDescriptor(e, c)) && (r.get || r.set) ? a(n, c, r) : n[c] = e[c]); return n; }
const {
  LKRNWebRTCModule: WebRTCModule
} = _reactNative.NativeModules;
let RTCFrameCryptorAlgorithm = exports.RTCFrameCryptorAlgorithm = void 0;
(function (RTCFrameCryptorAlgorithm) {
  RTCFrameCryptorAlgorithm[RTCFrameCryptorAlgorithm["kAesGcm"] = 0] = "kAesGcm";
})(RTCFrameCryptorAlgorithm || (exports.RTCFrameCryptorAlgorithm = RTCFrameCryptorAlgorithm = {}));
let RTCKeyDerivationAlgorithm = exports.RTCKeyDerivationAlgorithm = void 0;
(function (RTCKeyDerivationAlgorithm) {
  RTCKeyDerivationAlgorithm[RTCKeyDerivationAlgorithm["PBKDF2"] = 0] = "PBKDF2";
  RTCKeyDerivationAlgorithm[RTCKeyDerivationAlgorithm["HKDF"] = 1] = "HKDF";
})(RTCKeyDerivationAlgorithm || (exports.RTCKeyDerivationAlgorithm = RTCKeyDerivationAlgorithm = {}));
class RTCFrameCryptorFactory {
  static createFrameCryptorForRtpSender(participantId, sender, algorithm, keyProvider) {
    const params = {
      'peerConnectionId': sender._peerConnectionId,
      'rtpSenderId': sender._id,
      participantId,
      'keyProviderId': keyProvider._id,
      'type': 'sender',
      'algorithm': algorithm
    };
    const result = WebRTCModule.frameCryptorFactoryCreateFrameCryptor(params);
    if (!result) {
      throw new Error('Error when creating frame cryptor for sender');
    }
    return new _RTCFrameCryptor.default(result, participantId);
  }
  static createFrameCryptorForRtpReceiver(participantId, receiver, algorithm, keyProvider) {
    const params = {
      'peerConnectionId': receiver._peerConnectionId,
      'rtpReceiverId': receiver._id,
      participantId,
      'keyProviderId': keyProvider._id,
      'type': 'receiver',
      'algorithm': algorithm
    };
    const result = WebRTCModule.frameCryptorFactoryCreateFrameCryptor(params);
    if (!result) {
      throw new Error('Error when creating frame cryptor for receiver');
    }
    return new _RTCFrameCryptor.default(result, participantId);
  }
  static createDefaultKeyProvider(options) {
    var _options$failureToler, _options$keyRingSize, _options$discardFrame, _options$keyDerivatio;
    const params = {
      'sharedKey': options.sharedKey,
      'ratchetWindowSize': options.ratchetWindowSize,
      'failureTolerance': (_options$failureToler = options.failureTolerance) !== null && _options$failureToler !== void 0 ? _options$failureToler : -1,
      'keyRingSize': (_options$keyRingSize = options.keyRingSize) !== null && _options$keyRingSize !== void 0 ? _options$keyRingSize : 16,
      'discardFrameWhenCryptorNotReady': (_options$discardFrame = options.discardFrameWhenCryptorNotReady) !== null && _options$discardFrame !== void 0 ? _options$discardFrame : false,
      'keyDerivationAlgorithm': (_options$keyDerivatio = options.keyDerivationAlgorithm) !== null && _options$keyDerivatio !== void 0 ? _options$keyDerivatio : 0
    };
    if (typeof options.ratchetSalt === 'string') {
      params['ratchetSalt'] = options.ratchetSalt;
      params['ratchetSaltIsBase64'] = false;
    } else {
      const bytes = options.ratchetSalt;
      params['ratchetSalt'] = base64.fromByteArray(bytes);
      params['ratchetSaltIsBase64'] = true;
    }
    if (options.uncryptedMagicBytes) {
      params['uncryptedMagicBytes'] = base64.fromByteArray(options.uncryptedMagicBytes);
    }
    const result = WebRTCModule.frameCryptorFactoryCreateKeyProvider(params);
    if (!result) {
      throw new Error('Error when creating key provider!');
    }
    return new _RTCKeyProvider.default(result);
  }
}
exports.default = RTCFrameCryptorFactory;
//# sourceMappingURL=RTCFrameCryptorFactory.js.map