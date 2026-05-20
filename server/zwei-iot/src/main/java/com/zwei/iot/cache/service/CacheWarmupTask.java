package com.zwei.iot.cache.service;

/**
 * 缓存预热任务接口
 * <p>
 * 实现此接口来定义具体的缓存预热逻辑
 *
 * @author zwei
 */
public interface CacheWarmupTask {
    /**
     * 获取任务名称（用于日志）
     */
    String getName();

    /**
     * 执行预热逻辑
     */
    void warmup();

    /**
     * 获取任务顺序（数字越小越先执行）
     * 默认返回 0
     */
    default int getOrder() {
        return 0;
    }

    /**
     * 是否在预热前清空已有缓存（幂等性保证）
     * 默认返回 false，保持向后兼容
     */
    default boolean clearCacheFirst() {
        return false;
    }
}