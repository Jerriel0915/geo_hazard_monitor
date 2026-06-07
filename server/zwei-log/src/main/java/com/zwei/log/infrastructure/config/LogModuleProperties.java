package com.zwei.log.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 日志模块配置
 *
 * @author zwei
 */
@ConfigurationProperties(prefix = "zwei.log")
public class LogModuleProperties {

    private boolean runtimeInfoEnabled = false;

    /**
     * 运行日志持久化级别，默认仅采集 WARN/ERROR
     */
    private Set<String> runtimeLevels = new LinkedHashSet<>(Set.of("WARN", "ERROR"));

    private long sseTimeoutMs = 300000L;

    private long sseRetryMs = 3000L;

    /**
     * SSE断点最小落库间隔，避免每条事件都触发数据库写入
     */
    private long sseCheckpointFlushIntervalMs = 5000L;

    /**
     * 普通日志保留天数
     */
    private int cleanupRetentionDays = 30;

    /**
     * SSE断点保留天数
     */
    private int cleanupCheckpointRetentionDays = 30;

    /**
     * 单次清理批次大小
     */
    private int cleanupBatchSize = 1000;

    public boolean isRuntimeInfoEnabled() {
        return runtimeInfoEnabled;
    }

    public void setRuntimeInfoEnabled(boolean runtimeInfoEnabled) {
        this.runtimeInfoEnabled = runtimeInfoEnabled;
    }

    public Set<String> getRuntimeLevels() {
        return runtimeLevels;
    }

    public void setRuntimeLevels(Set<String> runtimeLevels) {
        if (runtimeLevels == null || runtimeLevels.isEmpty()) {
            this.runtimeLevels = new LinkedHashSet<>(Set.of("WARN", "ERROR"));
            return;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String level : runtimeLevels) {
            if (level != null && !level.isBlank()) {
                normalized.add(level.trim().toUpperCase(Locale.ROOT));
            }
        }
        this.runtimeLevels = normalized.isEmpty() ? new LinkedHashSet<>(Set.of("WARN", "ERROR")) : normalized;
    }

    public long getSseTimeoutMs() {
        return sseTimeoutMs;
    }

    public void setSseTimeoutMs(long sseTimeoutMs) {
        this.sseTimeoutMs = sseTimeoutMs;
    }

    public long getSseRetryMs() {
        return sseRetryMs;
    }

    public void setSseRetryMs(long sseRetryMs) {
        this.sseRetryMs = sseRetryMs;
    }

    public long getSseCheckpointFlushIntervalMs() {
        return sseCheckpointFlushIntervalMs;
    }

    public void setSseCheckpointFlushIntervalMs(long sseCheckpointFlushIntervalMs) {
        this.sseCheckpointFlushIntervalMs = Math.max(0L, sseCheckpointFlushIntervalMs);
    }

    public int getCleanupRetentionDays() {
        return cleanupRetentionDays;
    }

    public void setCleanupRetentionDays(int cleanupRetentionDays) {
        this.cleanupRetentionDays = Math.max(1, cleanupRetentionDays);
    }

    public int getCleanupCheckpointRetentionDays() {
        return cleanupCheckpointRetentionDays;
    }

    public void setCleanupCheckpointRetentionDays(int cleanupCheckpointRetentionDays) {
        this.cleanupCheckpointRetentionDays = Math.max(1, cleanupCheckpointRetentionDays);
    }

    public int getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize) {
        this.cleanupBatchSize = Math.max(100, cleanupBatchSize);
    }

    public boolean supportsRuntimeLevel(String level) {
        if (level == null || level.isBlank()) {
            return false;
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        if ("INFO".equals(normalized) && runtimeInfoEnabled) {
            return true;
        }
        return runtimeLevels.contains(normalized);
    }
}
