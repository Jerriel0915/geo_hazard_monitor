package com.zwei.log.infrastructure.config;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
