"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _reactNative = require("react-native");
var _RTCDataPacketCryptor = _interopRequireDefault(require("./RTCDataPacketCryptor"));
function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }
const {
  WebRTCModule
} = _reactNative.NativeModules;
class RTCDataPacketCryptorFactory {
  static async createDataPacketCryptor(algorithm, keyProvider) {
    const params = {
      'algorithm': algorithm,
      'keyProviderId': keyProvider._id
    };
    const result = await WebRTCModule.dataPacketCryptorFactoryCreateDataPacketCryptor(params);
    if (!result) {
      throw new Error('Error when creating data packet cryptor for sender');
    }
    return new _RTCDataPacketCryptor.default(result.dataPacketCryptorId);
  }
}
exports.default = RTCDataPacketCryptorFactory;
//# sourceMappingURL=RTCDataPacketCryptorFactory.js.map