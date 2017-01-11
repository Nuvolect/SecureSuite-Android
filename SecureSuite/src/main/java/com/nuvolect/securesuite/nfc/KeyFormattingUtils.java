package com.nuvolect.securesuite.nfc;//

import java.nio.ByteBuffer;

//TODO create class description
//
public class KeyFormattingUtils {

    public static long getKeyIdFromFingerprint(byte[] fingerprint) {
        ByteBuffer buf = ByteBuffer.wrap(fingerprint);
        // skip first 12 bytes of the fingerprint
        buf.position(12);
        // the last eight bytes are the key id (big endian, which is default order in ByteBuffer)
        return buf.getLong();
    }
}
