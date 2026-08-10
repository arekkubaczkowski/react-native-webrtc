export interface RTCEncryptedPacket {
    payload: Uint8Array;
    iv: Uint8Array;
    keyIndex: number;
}
export default class RTCDataPacketCryptor {
    _id: string;
    constructor(dataPacketCryptorId: string);
    encrypt(participantId: string, keyIndex: number, data: Uint8Array): Promise<RTCEncryptedPacket | null>;
    decrypt(participantId: string, packet: RTCEncryptedPacket): Promise<Uint8Array | null>;
    dispose(): Promise<void>;
}
