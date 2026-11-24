import RTCDataPacketCryptor from './RTCDataPacketCryptor';
import RTCFrameCryptorAlgorithm from './RTCFrameCryptorFactory';
import RTCKeyProvider from './RTCKeyProvider';
export default class RTCDataPacketCryptorFactory {
    static createDataPacketCryptor(algorithm: RTCFrameCryptorAlgorithm, keyProvider: RTCKeyProvider): Promise<RTCDataPacketCryptor>;
}
