package com.zwei.iot.alarm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 告警引擎配置属性。
 *
 * @author zwei
 */
@Component
@ConfigurationProperties(prefix = "iot.alarm")
public class AlarmProperties {

    /**
     * 告警引擎总开关
     */
    private boolean enabled = true;

    /**
     * 判据缓存 TTL（秒）
     */
    private int criteriaCacheTtlSeconds = 60;

    /**
     * 预触发计数 Redis TTL（秒）
     */
    private int preTriggerTtlSeconds = 600;

    /**
     * Groovy脚本执行超时（秒）
     */
    private int groovyTimeoutSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCriteriaCacheTtlSeconds() {
        return criteriaCacheTtlSeconds;
    }

    public void setCriteriaCacheTtlSeconds(int criteriaCacheTtlSeconds) {
        this.criteriaCacheTtlSeconds = criteriaCacheTtlSeconds;
    }

    public int getPreTriggerTtlSeconds() {
        return preTriggerTtlSeconds;
    }

    public void setPreTriggerTtlSeconds(int preTriggerTtlSeconds) {
        this.preTriggerTtlSeconds = preTriggerTtlSeconds;
    }

    public int getGroovyTimeoutSeconds() {
        return groovyTimeoutSeconds;
    }

    public void setGroovyTimeoutSeconds(int groovyTimeoutSeconds) {
        this.groovyTimeoutSeconds = groovyTimeoutSeconds;
    }
}
