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
    private int criteriaCacheTtlSeconds = 3 * 24 * 60 * 60;

    /**
     * 预触发计数 Redis TTL（秒）
     */
    private int preTriggerTtlSeconds = 3 * 24 * 60 * 60;

    /**
     * Groovy脚本执行超时（秒）
     */
    private int groovyTimeoutSeconds = 30;

    /**
     * Python 算法集成配置
     */
    private Algo algo = new Algo();

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

    public Algo getAlgo() {
        return algo;
    }

    public void setAlgo(Algo algo) {
        this.algo = algo;
    }

    /**
     * Python 算法集成子配置
     */
    public static class Algo {
        /** 算法工作空间目录（相对路径） */
        private String workspaceDir = "algo-workspace";

        /** Python 可执行文件路径 */
        private String pythonCmd = "python";

        /** 单次算法执行超时（秒） */
        private int timeoutSeconds = 60;

        /** 算法执行线程池大小 */
        private int poolSize = 4;

        public String getWorkspaceDir() {
            return workspaceDir;
        }

        public void setWorkspaceDir(String workspaceDir) {
            this.workspaceDir = workspaceDir;
        }

        public String getPythonCmd() {
            return pythonCmd;
        }

        public void setPythonCmd(String pythonCmd) {
            this.pythonCmd = pythonCmd;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
    }
}
