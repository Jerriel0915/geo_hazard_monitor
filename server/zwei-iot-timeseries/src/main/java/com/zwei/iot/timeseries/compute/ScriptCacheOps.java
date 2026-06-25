package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Groovy 脚本可调用的 Redis 缓存实例外壳 — 委托 {@link CacheUtil} 静态方法。
 *
 * <p>沙箱禁 {@code Class} receiver, 无法直接绑定 {@code CacheUtil.class} 让脚本调静态方法。
 * 本类作为 {@code @Component} 实例注入到脚本 Binding 的 {@code cache} 变量。
 *
 * <p>异常策略: <b>透传</b> — cache 失败应让脚本感知 (与 ScriptSensorQuery 的"吞噬"相反)。
 */
@Component
public class ScriptCacheOps {

    // ==================== getters (7 类 × 2 重载) ====================

    public Integer getInt(String key) { return CacheUtil.getInt(key); }
    public int getInt(String key, int defaultValue) { return CacheUtil.getInt(key, defaultValue); }

    public Long getLong(String key) { return CacheUtil.getLong(key); }
    public long getLong(String key, long defaultValue) { return CacheUtil.getLong(key, defaultValue); }

    public Double getDouble(String key) { return CacheUtil.getDouble(key); }
    public double getDouble(String key, double defaultValue) { return CacheUtil.getDouble(key, defaultValue); }

    public Float getFloat(String key) { return CacheUtil.getFloat(key); }
    public float getFloat(String key, float defaultValue) { return CacheUtil.getFloat(key, defaultValue); }

    public BigDecimal getBigDecimal(String key) { return CacheUtil.getBigDecimal(key); }
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) { return CacheUtil.getBigDecimal(key, defaultValue); }

    public String getString(String key) { return CacheUtil.getString(key); }
    public String getString(String key, String defaultValue) { return CacheUtil.getString(key, defaultValue); }

    public Boolean getBoolean(String key) { return CacheUtil.getBoolean(key); }
    public boolean getBoolean(String key, boolean defaultValue) { return CacheUtil.getBoolean(key, defaultValue); }

    // ==================== 通用操作 ====================

    public void set(String key, Object value) { CacheUtil.set(key, value); }
    public void set(String key, Object value, long timeout, TimeUnit unit) { CacheUtil.set(key, value, timeout, unit); }

    public boolean delete(String key) { return CacheUtil.delete(key); }
    public boolean hasKey(String key) { return CacheUtil.hasKey(key); }
    public boolean expire(String key, long timeout) { return CacheUtil.expire(key, timeout); }
    public boolean expire(String key, long timeout, TimeUnit unit) { return CacheUtil.expire(key, timeout, unit); }
    public long getExpire(String key) { return CacheUtil.getExpire(key); }
}
