package com.zwei.iot.broker.component;

import com.zwei.iot.broker.model.MqttDeviceSession;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护 MQTT 设备鉴权会话。
 * <p>
 * 同时以 deviceId 与 clientId 建立双索引，便于在鉴权成功、重复登录挤占、
 * 连接离线和发布鉴权时快速定位当前活跃会话。
 */
@Component
public class MqttDeviceSessionRegistry {
    private final ConcurrentHashMap<Long, MqttDeviceSession> deviceSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MqttDeviceSession> clientSessions = new ConcurrentHashMap<>();

    /**
     * 注册或覆盖设备当前会话。
     *
     * @param session 新的鉴权会话
     * @return 被新会话替换掉的旧会话；不存在时返回 {@code null}
     */
    public synchronized MqttDeviceSession register(MqttDeviceSession session) {
        MqttDeviceSession previous = deviceSessions.put(session.deviceId(), session);
        clientSessions.put(session.clientId(), session);
        // 同一设备换 clientId 重新登录时，移除旧 client 维度索引，避免发布鉴权命中脏会话。
        if (previous != null && !Objects.equals(previous.clientId(), session.clientId())) {
            clientSessions.remove(previous.clientId(), previous);
        }
        return previous;
    }

    /**
     * 按 clientId 移除会话。
     *
     * @param clientId 当前连接的 clientId
     * @return 被移除的会话；未命中时为空
     */
    public synchronized Optional<MqttDeviceSession> removeByClientId(String clientId) {
        MqttDeviceSession removed = clientSessions.remove(clientId);
        if (removed == null) {
            return Optional.empty();
        }
        // 只有 device 维度索引仍指向当前 clientId 时才清理，避免误删新连接会话。
        deviceSessions.computeIfPresent(removed.deviceId(), (deviceId, current) ->
                Objects.equals(current.clientId(), clientId) ? null : current);
        return Optional.of(removed);
    }

    /**
     * 按 clientId 查询会话。
     *
     * @param clientId 当前连接的 clientId
     * @return 会话查询结果
     */
    public Optional<MqttDeviceSession> getByClientId(String clientId) {
        return Optional.ofNullable(clientSessions.get(clientId));
    }

    /**
     * 按设备 ID 查询会话。
     *
     * @param deviceId 设备主键
     * @return 会话查询结果
     */
    public Optional<MqttDeviceSession> getByDeviceId(Long deviceId) {
        return Optional.ofNullable(deviceSessions.get(deviceId));
    }

    /**
     * 清空全部会话缓存，供测试或重置场景使用。
     */
    @Deprecated
    public void clear() {
        deviceSessions.clear();
        clientSessions.clear();
    }
}
