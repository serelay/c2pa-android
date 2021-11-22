/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance;

// Alternatively on android could use: com.google.android.gms.common.util.Hex
public class Hex {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToStringUppercase(byte[] bytes) {
        // This version would be slow
//        StringBuilder sb = new StringBuilder(bytes.length * 2);
//        for(byte b: bytes)
//            sb.append(String.format("%02x", b));
//        return sb.toString();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] stringToBytes(String content) throws IllegalArgumentException {
        int length = content.length();
        if (length % 2 == 0) {
            byte[] bytes = new byte[(length / 2)];
            int i = 0;
            while (i < length) {
                int end = i + 2;
                bytes[i / 2] = (byte) Integer.parseInt(content.substring(i, end), 16);
                i = end;
            }
            return bytes;
        }
        throw new IllegalArgumentException("Hex string has an odd number of characters");
    }
}
