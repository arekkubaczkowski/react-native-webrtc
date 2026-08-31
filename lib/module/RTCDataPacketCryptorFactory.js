import { NativeModules } from 'react-native';
import RTCDataPacketCryptor from './RTCDataPacketCryptor';
const {
  WebRTCModule
} = NativeModules;
export default class RTCDataPacketCryptorFactory {
  static async createDataPacketCryptor(algorithm, keyProvider) {
    const params = {
      'algorithm': algorithm,
      'keyProviderId': keyProvider._id
    };
    const result = await WebRTCModule.dataPacketCryptorFactoryCreateDataPacketCryptor(params);
    if (!result) {
      throw new Error('Error when creating data packet cryptor for sender');
    }
    return new RTCDataPacketCryptor(result.dataPacketCryptorId);
  }
}
//# sourceMappingURL=RTCDataPacketCryptorFactory.js.map