package com.zwei.iot.parser.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Groovy 脚本内置函数库。
 *
 * <h3>注入机制</h3>
 * <p>本类的单例实例通过 Groovy {@link groovy.lang.Binding} 以 {@code builtin} 变量名注入到
 * 每个解析脚本的执行上下文中。脚本中直接调用 {@code builtin.hexDecode(...)} /
 * {@code builtin.readFloat(...)} 等方法，无需 import。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>所有方法必须无副作用——解析脚本可能并发执行，静态方法或实例方法的内部状态
 *       必须线程安全。</li>
 *   <li>不暴露任何 Java 标准库敏感能力（无文件 IO、无网络、无反射、无进程控制）。</li>
 *   <li>二进制读取原语均为 <b>大端序（big-endian / network byte order）</b>，
 *       这是国标协议及大多数工控协议的默认字节序。</li>
 * </ul>
 *
 * <h3>方法分类</h3>
 * <table>
 *   <caption>API 概览</caption>
 *   <tr><th>类别</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>二进制解码</td><td>{@link #hexDecode}</td><td>hex 字符串 → byte[]</td></tr>
 *   <tr><td>二进制读取</td><td>{@link #readFloat}, {@link #readDouble}, {@link #readUInt16}, {@link #readInt16}, {@link #readUInt8}</td><td>大端序基本类型读取</td></tr>
 *   <tr><td>二进制读取</td><td>{@link #readAscii}</td><td>定长 ASCII 字符串</td></tr>
 *   <tr><td>时间</td><td>{@link #readBcdTimestamp}, {@link #currentTimeMillis}</td><td>BCD 时间戳 + 当前时间</td></tr>
 *   <tr><td>工具</td><td>{@link #sha256}, {@link #toDouble}, {@link #toInt}</td><td>哈希 + 类型安全转换</td></tr>
 * </table>
 */
@Component
public class BuiltInFunctions {

    private static final Logger log = LoggerFactory.getLogger(BuiltInFunctions.class);

    /**
     * hex 字符串 → byte[]。
     *
     * <p>典型用法：国标协议报文通常以十六进制字符串传输，
     * 脚本需先 hex 解码再逐字段用 read* 系列方法解析。
     */
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

    /**
     * BCD 编码时间戳 (7 bytes) → epoch millis。
     *
     * <p>BCD (Binary-Coded Decimal) 每字节高 4 位=十位、低 4 位=个位。
     * 7 字节布局：
     * <pre>
     * [year_hundreds|year_tens] [year_ones|month] [day] [hour] [minute] [second]
     *   例: 0x20 0x26 0x06 0x18 0x14 0x30 0x00 → 2026-06-18 14:30:00
     * </pre>
     *
     * <p>使用系统默认时区转换为 epoch millis。解析失败时回退到当前时间。
     */
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

    /**
     * 边界检查——所有二进制读取的前置校验。
     *
     * <p>在真正读取字节前拦截越界访问，抛出清晰的
     * {@link IllegalArgumentException} 而非让 Groovy 侧收到晦涩的
     * {@link ArrayIndexOutOfBoundsException}。
     */
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
