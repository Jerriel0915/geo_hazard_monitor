package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.springframework.stereotype.Component;

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

    public Integer getInt(String key) { return CacheUtil.getInt(key); }
    public int getInt(String key, int defaultValue) { return CacheUtil.getInt(key, defaultValue); }
}
