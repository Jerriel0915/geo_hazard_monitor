package com.zwei.iot.device.service;

import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.common.event.DeviceOnlineEvent;
import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import com.zwei.iot.device.domain.DeviceOnlineStatus;
import com.zwei.iot.device.mapper.DeviceOnlineStatusMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
