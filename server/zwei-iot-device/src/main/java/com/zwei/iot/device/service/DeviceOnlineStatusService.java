package com.zwei.iot.device.service;

import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.common.event.DeviceOnlineEvent;
import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import com.zwei.iot.device.domain.DeviceOnlineStatus;
import com.zwei.iot.device.mapper.DeviceOnlineStatusMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 设备在线状态服务。
 * <p>
 * 监听 DeviceOnlineEvent / DeviceOfflineEvent，将运维指标独立写入
 * device_online_status 和 device_online_event_log 表，与设备业务主表解耦。
 */
@Slf4j
@Service
public class DeviceOnlineStatusService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceOnlineStatusMapper mapper;

    @Autowired
    public DeviceOnlineStatusService(DeviceOnlineStatusMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 启动恢复: 异常关闭后 MQTT broker 无留存连接，将所有在线状态重置为离线。
     * <p>
     * 监听 {@link ApplicationReadyEvent}（在所有 Bean 初始化、MQTT broker 启动完毕后触发），
     * 确保设备在启动期间重连成功后不会被误重置为离线。
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        String now = LocalDateTime.now().format(DT_FMT);
        int affected = mapper.resetAllOnlineToOffline(now);
        if (affected > 0) {
            log.info("启动恢复: 已将 {} 台设备在线状态重置为离线", affected);
        }
    }

    /**
     * 处理设备上线事件。
     */
    @EventListener
    public void onDeviceOnline(DeviceOnlineEvent event) {
        String now = LocalDateTime.now().format(DT_FMT);
        DeviceOnlineStatus status = new DeviceOnlineStatus();
        status.setDeviceId(event.getDeviceId());
        status.setClientId(event.getClientId());
        status.setOnlineAt(now);
        mapper.upsertOnline(status);
        insertLog(event.getDeviceId(), "ONLINE", event.getClientId(), event.getClientIp(), null);
        log.debug("设备上线记录完成 deviceId={} clientId={}", event.getDeviceId(), event.getClientId());
    }

    /**
     * 处理设备离线事件。
     */
    @EventListener
    public void onDeviceOffline(DeviceOfflineEvent event) {
        String now = LocalDateTime.now().format(DT_FMT);
        mapper.upsertOffline(event.getDeviceId(), now, event.getReason());
        insertLog(event.getDeviceId(), "OFFLINE", event.getClientId(), event.getClientIp(), event.getReason());
        log.debug("设备离线记录完成 deviceId={} reason={}", event.getDeviceId(), event.getReason());
    }

    /**
     * 更新设备最后数据上报时间。由消息消费链路在 IoTDB 写入成功后调用。
     */
    public void updateLastReportAt(Long deviceId) {
        String now = LocalDateTime.now().format(DT_FMT);
        mapper.updateLastReportAt(deviceId, now);
    }

    /**
     * 按设备 ID 查询在线状态记录。
     *
     * @param deviceId 设备主键
     * @return 在线状态记录; null 表示无记录
     */
    public DeviceOnlineStatus getByDeviceId(Long deviceId) {
        return mapper.selectByDeviceId(deviceId);
    }

    /**
     * 对账: 根据 MQTT broker 实际在线设备列表，将不在其中的设备标记为离线。
     *
     * @param connectedDeviceIds 当前实际在线的设备 ID 列表（来自 MqttDeviceSessionRegistry）
     */
    public void reconcileOffline(List<Long> connectedDeviceIds) {
        String now = LocalDateTime.now().format(DT_FMT);
        int affected;
        if (connectedDeviceIds == null || connectedDeviceIds.isEmpty()) {
            affected = mapper.resetAllOnlineToOffline(now);
        } else {
            affected = mapper.markOfflineExcept(now, connectedDeviceIds);
        }
        if (affected > 0) {
            log.info("设备在线对账: 已将 {} 台设备标记为离线 (当前实际在线 {} 台)",
                    affected, connectedDeviceIds == null ? 0 : connectedDeviceIds.size());
        }
    }

    private void insertLog(Long deviceId, String eventType, String clientId, String clientIp, String reason) {
        DeviceOnlineEventLog log = new DeviceOnlineEventLog();
        log.setDeviceId(deviceId);
        log.setEventType(eventType);
        log.setClientId(clientId);
        log.setClientIp(clientIp);
        log.setEventTime(LocalDateTime.now().format(DT_FMT));
        log.setReason(reason);
        mapper.insertEventLog(log);
    }
}
