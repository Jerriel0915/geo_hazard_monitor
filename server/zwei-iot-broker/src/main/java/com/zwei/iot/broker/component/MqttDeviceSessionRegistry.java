package com.zwei.iot.broker.component;

import com.zwei.iot.broker.model.MqttDeviceSession;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 设备会话注册中心 — 维护当前所有活跃连接的内存索引。
 *
 * <h3>数据结构</h3>
 * 双 {@link ConcurrentHashMap} 索引，O(1) 查找：
 * <ul>
 *   <li><b>deviceSessions</b>：Long deviceId → MqttDeviceSession（按设备定位）</li>
 *   <li><b>clientSessions</b>：String clientId → MqttDeviceSession（按连接定位）</li>
 * </ul>
 *
 * <h3>关键并发语义</h3>
 * 所有写操作（register/removeByClientId）均为 {@code synchronized}，
 * 保证双索引之间的一致性。读操作（getByClientId/getByDeviceId）无锁。
 *
 * <h3>会话生命周期</h3>
 * <pre>
 * CONNECT 鉴权成功 → register()         → 写入双索引（若同设备旧连接则清除旧 clientId）
 * 数据上报鉴权     → getByClientId()    → 只读
 * DISCONNECT/超时  → removeByClientId() → 仅当 deviceSessions 仍指向该 clientId 时才清除 device 索引
 * 密码重置断连     → getByDeviceId()    → DeviceSessionServiceImpl.disconnectDevice()
 * </pre>
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
     * 获取当前所有活跃连接的设备 ID 集合（不可变快照）。
     *
     * @return 已连接设备 ID 集合；无连接时为空集
     */
    public Set<Long> getConnectedDeviceIds() {
        return Collections.unmodifiableSet(new HashSet<>(deviceSessions.keySet()));
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
