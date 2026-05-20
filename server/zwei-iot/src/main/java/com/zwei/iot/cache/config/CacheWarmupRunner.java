package com.zwei.iot.cache.config;

import com.zwei.iot.cache.service.CacheWarmupTask;
import com.zwei.iot.cache.service.IotCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存预热Runner
 * <p>
 * 应用启动后从 CacheWarmupTaskRegistry 获取所有注册的任务，并行执行预热
 *
 * @author zwei
 */
@Component
public class CacheWarmupRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final CacheWarmupTaskRegistry registry;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final IotCacheService cacheService;

    // 预热开关（可通过配置覆盖）
    @Value("${iot.cache.warmup.enabled:true}")
    private volatile boolean warmupEnabled;

    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    @Autowired
    public CacheWarmupRunner(
            CacheWarmupTaskRegistry registry,
            @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor threadPoolTaskExecutor,
            IotCacheService cacheService) {
        this.registry = registry;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.cacheService = cacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 防止重复执行
        if (!hasRun.compareAndSet(false, true)) {
            log.info("[CacheWarmup] Already executed, skipping");
            return;
        }

        if (!warmupEnabled) {
            log.info("[CacheWarmup] Warmup is disabled, skipping");
            return;
        }

        var tasks = registry.getTasks();
        if (tasks.isEmpty()) {
            log.info("[CacheWarmup] No tasks registered, skipping");
            return;
        }

        log.info("[CacheWarmup] Starting cache warmup with {} tasks...", tasks.size());
        long startTime = System.currentTimeMillis();

        // 清空已有缓存（幂等性保证）- 只清空一次
        log.info("[CacheWarmup] Clearing existing cache before warmup...");
        cacheService.clearAllCache();

        // 并行执行所有预热任务
        var futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(() -> executeTask(task), threadPoolTaskExecutor))
                .toList();

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        log.info("[CacheWarmup] Cache warmup completed in {} ms, {} tasks executed", duration, tasks.size());
    }

    /**
     * 执行单个预热任务
     */
    private void executeTask(CacheWarmupTask task) {
        String taskName = task.getName();
        log.info("[CacheWarmup] Executing task: {}", taskName);
        try {
            task.warmup();
            log.info("[CacheWarmup] Task completed: {}", taskName);
        } catch (Exception e) {
            log.error("[CacheWarmup] Task failed: {}", taskName, e);
        }
    }
}