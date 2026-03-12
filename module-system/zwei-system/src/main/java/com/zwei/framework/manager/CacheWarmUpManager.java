package com.zwei.framework.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热处理
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-12
 */
@Component
@Slf4j
public class CacheWarmUpManager {
    // 任务列表
    List<CacheWarmupTask> tasks;

    @Autowired
    CacheWarmUpManager(List<CacheWarmupTask> tasks) {
        this.tasks = tasks;
    }

    @PostConstruct
    public void init() {
        log.debug("发现 {} 个缓存任务", tasks.size());
    }

    /**
     * 批量异步执行缓存业务
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheWarmupExecutor")
    public void onApplicationReady() {
        log.info("应用构建完毕，正在执行批量缓存任务");

        CountDownLatch countDownLatch = new CountDownLatch(tasks.size());
        for (CacheWarmupTask task : tasks) {
            try {
                task.warmup();
            } catch (Exception e) {
                log.error("任务 [{}] 预热失败", task.getTaskName(), e);
            } finally {
                countDownLatch.countDown();
            }
        }

        // 缓存任务最多等待5分钟
        try {
            countDownLatch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("所有缓存预热任务已完成");
    }
}
