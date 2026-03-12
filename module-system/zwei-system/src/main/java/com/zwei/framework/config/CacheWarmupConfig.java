package com.zwei.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-12
 */
@Configuration
public class CacheWarmupConfig {

    /**
     * 轻量级预热专用线程池
     */
    @Bean(name = "cacheWarmupExecutor")
    public ThreadPoolTaskExecutor cacheWarmupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);           // 单线程顺序预热，避免数据库压力过大
        executor.setMaxPoolSize(2);            // 最大2个并发
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("cache-warmup-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy()); // 静默丢弃，不影响启动
        executor.setDaemon(true);              // 守护线程，不阻止JVM退出
        executor.initialize();
        return executor;
    }
}
