package com.zwei.iot.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 缓存预热任务注册中心
 * <p>
 * 负责收集所有注册的预热任务，并提供任务列表供 CacheWarmupRunner 执行
 *
 * @author zwei
 */
@Component
public class CacheWarmupTaskRegistry {
    private final List<CacheWarmupTask> tasks = new ArrayList<>();

    /**
     * 注册预热任务
     */
    public void registerTask(CacheWarmupTask task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    /**
     * 获取所有已注册的任务（按 order 排序）
     */
    public List<CacheWarmupTask> getTasks() {
        return tasks.stream()
                .sorted(Comparator.comparingInt(CacheWarmupTask::getOrder))
                .toList();
    }

    /**
     * 获取任务数量
     */
    public int getTaskCount() {
        return tasks.size();
    }
}