package com.zwei.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSE日志推送配置
 *
 * @author zwei
 */
@Component
@ConfigurationProperties(prefix = "log-sse")
public class LogSseConfig {

    /** 是否启用SSE日志推送 */
    private boolean enabled = true;

    /** 推送队列容量 */
    private int queueSize = 100;

    /** 最大推送条数/秒 (0=不限制) */
    private int rateLimit = 10;

    /** 连接超时时间(秒) */
    private int timeout = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}