package com.zwei.iot.parser.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class BuiltInFunctions {

    private static final Logger log = LoggerFactory.getLogger(BuiltInFunctions.class);

    /** hex string → byte[] */
    public byte[] hexDecode(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /** big-endian float (4 bytes at offset) */
    public float readFloat(byte[] data, int offset) {
        checkBounds(data, offset, 4);
        int bits = ((data[offset] & 0xFF) << 24)
                 | ((data[offset + 1] & 0xFF) << 16)
                 | ((data[offset + 2] & 0xFF) << 8)
                 | (data[offset + 3] & 0xFF);
        return Float.intBitsToFloat(bits);
    }

    /** big-endian double (8 bytes at offset) */
    public double readDouble(byte[] data, int offset) {
        checkBounds(data, offset, 8);
        long bits = ((long)(data[offset] & 0xFF) << 56)
                  | ((long)(data[offset + 1] & 0xFF) << 48)
                  | ((long)(data[offset + 2] & 0xFF) << 40)
                  | ((long)(data[offset + 3] & 0xFF) << 32)
                  | ((long)(data[offset + 4] & 0xFF) << 24)
                  | ((long)(data[offset + 5] & 0xFF) << 16)
                  | ((long)(data[offset + 6] & 0xFF) << 8)
                  | (data[offset + 7] & 0xFF);
        return Double.longBitsToDouble(bits);
    }

    /** big-endian uint16 (2 bytes at offset) */
    public int readUInt16(byte[] data, int offset) {
        checkBounds(data, offset, 2);
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /** big-endian int16 (signed, 2 bytes at offset) */
    public short readInt16(byte[] data, int offset) {
        checkBounds(data, offset, 2);
        return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
    }

    /** uint8 (1 byte at offset) */
    public int readUInt8(byte[] data, int offset) {
        checkBounds(data, offset, 1);
        return data[offset] & 0xFF;
    }

    /** ASCII string (fixed-length, length bytes starting at offset) */
    public String readAscii(byte[] data, int offset, int length) {
        checkBounds(data, offset, length);
        return new String(data, offset, length, StandardCharsets.US_ASCII);
    }

    /** BCD-encoded timestamp (7 bytes: YY MM DD HH MM SS) → epoch millis */
    public long readBcdTimestamp(byte[] data, int offset) {
        checkBounds(data, offset, 7);
        try {
            int year = bcdToInt(data[offset]) * 100 + bcdToInt(data[offset + 1]);
            int month = bcdToInt(data[offset + 2]);
            int day = bcdToInt(data[offset + 3]);
            int hour = bcdToInt(data[offset + 4]);
            int min = bcdToInt(data[offset + 5]);
            int sec = bcdToInt(data[offset + 6]);
            LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, min, sec);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("BCD timestamp parse failed", e);
            return System.currentTimeMillis();
        }
    }

    private void checkBounds(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("data array is null");
        }
        if (offset < 0 || offset + length > data.length) {
            throw new IllegalArgumentException(
                "Bounds violation: data.length=" + data.length + ", offset=" + offset + ", need=" + length);
        }
    }

    private int bcdToInt(byte b) {
        return ((b >> 4) & 0x0F) * 10 + (b & 0x0F);
    }

    /** Current epoch millis */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /** SHA-256 hex digest of byte array */
    public String sha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Object → Double, safe conversion. Returns null on failure. */
    public Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Object → Integer, safe conversion. Returns defaultVal on failure. */
    public Integer toInt(Object v, int defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
