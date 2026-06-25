package com.zwei.common.utils;

import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.utils.spring.SpringUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存静态工具类 — 二次封装 {@link RedisCache}。
 *
 * <p>不修改 {@code RedisCache}，仅在其上补充：
 * <ul>
 *   <li>通用 set/get/delete/expire/hasKey 委托</li>
 *   <li>基础类型便捷 get（{@link #getInt} 等），宽松转换 + 默认值重载</li>
 * </ul>
 *
 * <p>供非 Spring 管理的调用方使用，通过 {@code SpringUtils.getBean(RedisCache.class)} 获取实例。
 * 核心逻辑在包私有方法中（接收 {@link RedisCache} 参数），便于单测注入 mock。</p>
 */
public final class CacheUtil {
    private CacheUtil() {}

    // ==================== 通用操作（公开静态）====================

    public static void set(String key, Object value) {
        doSet(SpringUtils.getBean(RedisCache.class), key, value);
    }

    public static void set(String key, Object value, long timeout, TimeUnit unit) {
        doSet(SpringUtils.getBean(RedisCache.class), key, value, timeout, unit);
    }

    public static <T> T get(String key) {
        return SpringUtils.getBean(RedisCache.class).getCacheObject(key);
    }

    public static boolean delete(String key) {
        return doDelete(SpringUtils.getBean(RedisCache.class), key);
    }

    public static boolean hasKey(String key) {
        return doHasKey(SpringUtils.getBean(RedisCache.class), key);
    }

    public static boolean expire(String key, long timeout) {
        return doExpire(SpringUtils.getBean(RedisCache.class), key, timeout);
    }

    public static boolean expire(String key, long timeout, TimeUnit unit) {
        return doExpire(SpringUtils.getBean(RedisCache.class), key, timeout, unit);
    }

    public static long getExpire(String key) {
        return doGetExpire(SpringUtils.getBean(RedisCache.class), key);
    }

    // ==================== 基础类型 getters（公开静态）====================

    public static Integer getInt(String key) {
        return toInt(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static int getInt(String key, int defaultValue) {
        Integer v = getInt(key);
        return v == null ? defaultValue : v;
    }

    public static Long getLong(String key) {
        return toLong(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static long getLong(String key, long defaultValue) {
        Long v = getLong(key);
        return v == null ? defaultValue : v;
    }

    public static Double getDouble(String key) {
        return toDouble(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static double getDouble(String key, double defaultValue) {
        Double v = getDouble(key);
        return v == null ? defaultValue : v;
    }

    public static Float getFloat(String key) {
        return toFloat(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static float getFloat(String key, float defaultValue) {
        Float v = getFloat(key);
        return v == null ? defaultValue : v;
    }

    public static BigDecimal getBigDecimal(String key) {
        return toBigDecimal(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        BigDecimal v = getBigDecimal(key);
        return v == null ? defaultValue : v;
    }

    public static String getString(String key) {
        return toStringValue(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static String getString(String key, String defaultValue) {
        String v = getString(key);
        return v == null ? defaultValue : v;
    }

    public static Boolean getBoolean(String key) {
        return toBoolean(SpringUtils.getBean(RedisCache.class).getCacheObject(key));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Boolean v = getBoolean(key);
        return v == null ? defaultValue : v;
    }

    // ==================== 通用操作（包私有，便于单测注入 mock）====================

    static void doSet(RedisCache cache, String key, Object value) {
        cache.setCacheObject(key, value);
    }

    static void doSet(RedisCache cache, String key, Object value, long timeout, TimeUnit unit) {
        cache.setCacheObject(key, value, (int) timeout, unit);
    }

    static Object doGet(RedisCache cache, String key) {
        return cache.getCacheObject(key);
    }

    static boolean doDelete(RedisCache cache, String key) {
        return cache.deleteObject(key);
    }

    static boolean doHasKey(RedisCache cache, String key) {
        Boolean result = cache.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    static boolean doExpire(RedisCache cache, String key, long timeout) {
        return cache.expire(key, timeout);
    }

    static boolean doExpire(RedisCache cache, String key, long timeout, TimeUnit unit) {
        return cache.expire(key, timeout, unit);
    }

    static long doGetExpire(RedisCache cache, String key) {
        return cache.getExpire(key);
    }

    // ==================== 基础类型 getters（包私有核心，便于单测注入 mock）====================

    static Integer doGetInt(RedisCache cache, String key) {
        return toInt(cache.getCacheObject(key));
    }

    static int doGetInt(RedisCache cache, String key, int defaultValue) {
        Integer v = doGetInt(cache, key);
        return v == null ? defaultValue : v;
    }

    static Long doGetLong(RedisCache cache, String key) {
        return toLong(cache.getCacheObject(key));
    }

    static long doGetLong(RedisCache cache, String key, long defaultValue) {
        Long v = doGetLong(cache, key);
        return v == null ? defaultValue : v;
    }

    static Double doGetDouble(RedisCache cache, String key) {
        return toDouble(cache.getCacheObject(key));
    }

    static double doGetDouble(RedisCache cache, String key, double defaultValue) {
        Double v = doGetDouble(cache, key);
        return v == null ? defaultValue : v;
    }

    static Float doGetFloat(RedisCache cache, String key) {
        return toFloat(cache.getCacheObject(key));
    }

    static float doGetFloat(RedisCache cache, String key, float defaultValue) {
        Float v = doGetFloat(cache, key);
        return v == null ? defaultValue : v;
    }

    static BigDecimal doGetBigDecimal(RedisCache cache, String key) {
        return toBigDecimal(cache.getCacheObject(key));
    }

    static BigDecimal doGetBigDecimal(RedisCache cache, String key, BigDecimal defaultValue) {
        BigDecimal v = doGetBigDecimal(cache, key);
        return v == null ? defaultValue : v;
    }

    static String doGetString(RedisCache cache, String key) {
        return toStringValue(cache.getCacheObject(key));
    }

    static String doGetString(RedisCache cache, String key, String defaultValue) {
        String v = doGetString(cache, key);
        return v == null ? defaultValue : v;
    }

    static Boolean doGetBoolean(RedisCache cache, String key) {
        return toBoolean(cache.getCacheObject(key));
    }

    static boolean doGetBoolean(RedisCache cache, String key, boolean defaultValue) {
        Boolean v = doGetBoolean(cache, key);
        return v == null ? defaultValue : v;
    }

    // ==================== 类型转换辅助（私有，宽松语义）====================

    private static Integer toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Float toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** 严格: 仅 String 类型值原样返回，Number 不做 toString 转换（避免误导）。 */
    private static String toStringValue(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
}
