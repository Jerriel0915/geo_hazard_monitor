package com.zwei.iot.broker.component;

import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备鉴权失败保护器。
 * <p>
 * 负责统计设备账号的连续失败次数，并在达到阈值后执行临时封禁，
 * 防止错误密码高频重试持续占用 Broker 与数据库资源。
 */
@Component
public class MqttAuthFailureGuard {
    private final MqttAuthCenterProperties properties;
    private final ConcurrentHashMap<String, FailureState> states = new ConcurrentHashMap<>();

    @Autowired
    public MqttAuthFailureGuard(MqttAuthCenterProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前账号是否仍在封禁期内。
     *
     * @param username 设备认证账号
     * @return {@code true} 表示账号仍被禁止接入
     */
    public boolean isBlocked(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        FailureState state = states.get(username);
        if (state == null) {
            return false;
        }
        long now = Instant.now().toEpochMilli();
        if (state.blockedUntilMillis > now) {
            return true;
        }
        if (state.blockedUntilMillis > 0) {
            states.remove(username, state);
        }
        return false;
    }

    /**
     * 获取当前账号剩余封禁时长。
     *
     * @param username 设备认证账号
     * @return 剩余秒数；未封禁时返回 0
     */
    public long getRemainingBlockSeconds(String username) {
        FailureState state = states.get(username);
        if (state == null) {
            return 0L;
        }
        long remainingMillis = state.blockedUntilMillis - Instant.now().toEpochMilli();
        return remainingMillis <= 0 ? 0L : Math.max(1L, remainingMillis / 1000L);
    }

    /**
     * 记录一次鉴权失败。
     * <p>
     * 若账号已过封禁期，会先重置历史状态，再重新开始累计失败次数。
     *
     * @param username 设备认证账号
     */
    public void recordFailure(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        states.compute(username, (key, existing) -> {
            FailureState state = existing == null ? new FailureState() : existing;
            long now = Instant.now().toEpochMilli();
            // 封禁已过期时，重置计数后重新开始累计。
            if (state.blockedUntilMillis > 0 && state.blockedUntilMillis <= now) {
                state.failureCount = 0;
                state.blockedUntilMillis = 0;
            }
            state.failureCount++;
            // 达到阈值后进入临时封禁，并清空本轮累计次数。
            if (state.failureCount >= properties.getFailureThreshold()) {
                state.failureCount = 0;
                state.blockedUntilMillis = now + properties.getBanDurationSeconds() * 1000L;
            }
            return state;
        });
    }

    /**
     * 鉴权成功后清除失败记录。
     *
     * @param username 设备认证账号
     */
    public void reset(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        states.remove(username);
    }

    /**
     * 清空全部失败状态，供测试或重置场景使用。
     */
    @Deprecated
    public void clear() {
        states.clear();
    }

    /**
     * 单个账号的失败统计状态。
     */
    private static final class FailureState {
        private int failureCount;
        private long blockedUntilMillis;
    }
}
