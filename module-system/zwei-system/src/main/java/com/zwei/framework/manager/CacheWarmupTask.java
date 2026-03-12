package com.zwei.framework.manager;

/**
 * 缓存预热任务接口
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-12
 */
public interface CacheWarmupTask {
    String getTaskName();

    /**
     * 缓存逻辑
     */
    void warmup() throws InterruptedException;
}
