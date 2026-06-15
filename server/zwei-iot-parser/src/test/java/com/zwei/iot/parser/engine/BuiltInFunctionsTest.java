package com.zwei.iot.parser.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BuiltInFunctions")
class BuiltInFunctionsTest {

    private final BuiltInFunctions fns = new BuiltInFunctions();

    @Nested
    @DisplayName("hexDecode")
    class HexDecode {
        @Test
        @DisplayName("should decode uppercase hex string to bytes")
        void decodeUppercase() {
            byte[] result = fns.hexDecode("414243");
            assertThat(result).containsExactly(0x41, 0x42, 0x43);
        }

        @Test
        @DisplayName("should decode empty string to empty array")
        void emptyString() {
            byte[] result = fns.hexDecode("");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("readFloat")
    class ReadFloat {
        @Test
        @DisplayName("should read big-endian float")
        void readFloatBE() {
            // 25.5 = 0x41CC0000
            byte[] data = {0x41, (byte) 0xCC, 0x00, 0x00};
            float result = fns.readFloat(data, 0);
            assertThat(result).isCloseTo(25.5f, org.assertj.core.data.Offset.offset(0.01f));
        }
    }

    @Nested
    @DisplayName("readDouble")
    class ReadDouble {
        @Test
        @DisplayName("should read big-endian double")
        void readDoubleBE() {
            // 25.5 = 0x4039800000000000
            byte[] data = {0x40, 0x39, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00};
            double result = fns.readDouble(data, 0);
            assertThat(result).isCloseTo(25.5, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("readUInt16")
    class ReadUInt16 {
        @Test
        @DisplayName("should read big-endian uint16")
        void readUint16BE() {
            byte[] data = {0x01, 0x02};
            int result = fns.readUInt16(data, 0);
            assertThat(result).isEqualTo(258);  // 1*256 + 2
        }
    }

    @Nested
    @DisplayName("readUInt8")
    class ReadUInt8 {
        @Test
        @DisplayName("should read unsigned byte")
        void readByte() {
            byte[] data = {(byte) 0xFF};
            int result = fns.readUInt8(data, 0);
            assertThat(result).isEqualTo(255);  // unsigned interpretation
        }
    }

    @Nested
    @DisplayName("readAscii")
    class ReadAscii {
        @Test
        @DisplayName("should read fixed-length ASCII string")
        void readAsciiString() {
            byte[] data = "HELLO   ".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            String result = fns.readAscii(data, 0, 8);
            assertThat(result).isEqualTo("HELLO   ");
        }
    }

    @Nested
    @DisplayName("Bounds checking")
    class BoundsChecking {
        @Test
        @DisplayName("should throw on out-of-bounds offset")
        void outOfBounds() {
            byte[] data = {0x00, 0x01};
            assertThatThrownBy(() -> fns.readFloat(data, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bounds violation");
        }

        @Test
        @DisplayName("should throw on null data")
        void nullData() {
            assertThatThrownBy(() -> fns.readUInt8(null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data array is null");
        }
    }

    @Nested
    @DisplayName("toDouble")
    class ToDouble {
        @Test
        @DisplayName("should convert Number to Double")
        void convertNumber() {
            assertThat(fns.toDouble(42)).isEqualTo(42.0);
        }

        @Test
        @DisplayName("should convert numeric string")
        void convertString() {
            assertThat(fns.toDouble("42.5")).isEqualTo(42.5);
        }

        @Test
        @DisplayName("should return null for non-numeric")
        void nonNumeric() {
            assertThat(fns.toDouble("abc")).isNull();
        }

        @Test
        @DisplayName("should return null for null")
        void nullInput() {
            assertThat(fns.toDouble(null)).isNull();
        }
    }

    @Nested
    @DisplayName("sha256")
    class Sha256 {
        @Test
        @DisplayName("should produce 64-char hex string")
        void producesHash() {
            String hash = fns.sha256("test".getBytes());
            assertThat(hash).hasSize(64);
            assertThat(hash).matches("^[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("should be deterministic")
        void deterministic() {
            byte[] input = "hello".getBytes();
            assertThat(fns.sha256(input)).isEqualTo(fns.sha256(input));
        }
    }
}
