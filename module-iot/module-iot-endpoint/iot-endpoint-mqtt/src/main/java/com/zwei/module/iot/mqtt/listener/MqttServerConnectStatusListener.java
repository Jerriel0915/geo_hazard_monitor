package com.zwei.module.iot.mqtt.listener;

import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.domain.DeviceAliveLog;
import com.zwei.module.iot.device.domain.DeviceStatus;
import com.zwei.module.iot.device.service.IDeviceAliveLogService;
import com.zwei.module.iot.device.service.IDeviceService;
import com.zwei.module.iot.device.service.IDeviceStatusService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;

import java.util.Date;
import java.util.List;

/**
 * Mqtt 设备链接状态监听
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-19
 */
@Slf4j
@Service
public class MqttServerConnectStatusListener implements IMqttConnectStatusListener {
    private final IDeviceService deviceService;
    private final IDeviceStatusService deviceStatusService;
    private final IDeviceAliveLogService deviceAliveLogService;

    @Autowired
    public MqttServerConnectStatusListener(IDeviceAliveLogService deviceAliveLogService, IDeviceService deviceService, IDeviceStatusService deviceStatusService) {
        this.deviceAliveLogService = deviceAliveLogService;
        this.deviceService = deviceService;
        this.deviceStatusService = deviceStatusService;
    }

    /**
     * 监听 client 设备上线
     *
     * @param context  ChannelContext
     * @param clientId 设备名称（可重复）
     * @param username 设备秘钥（deviceKey，唯一）
     */
    @Override
    public void online(ChannelContext context, String clientId, String username) {
        log.info("Mqtt Client Online: clientId={}, username={}", clientId, username);
        handleConnectStatus(username, true);
    }

    /**
     * 监听 client 设备下线
     *
     * @param context  ChannelContext
     * @param clientId 设备名称（可重复）
     * @param username 设备秘钥（deviceKey，唯一）
     */
    @Override
    public void offline(ChannelContext context, String clientId, String username, String reason) {
        log.info("Mqtt Client Offline: clientId={}, username={}, reason={}", clientId, username, reason);
        handleConnectStatus(username, false);
    }

    /**
     * 处理设备在线状态变化
     * @param deviceKey 设备秘钥（deviceKey，唯一）
     * @param online    设备新状态，true在线 false下线
     */
    private void handleConnectStatus(String deviceKey, boolean online) {
        if (deviceKey == null) {
            return;
        }

        try {
            Device queryDevice = new Device();
            queryDevice.setDeviceKey(deviceKey);
            List<Device> devices = deviceService.selectDeviceList(queryDevice);

            if (devices == null || devices.isEmpty()) {
                log.warn("Device not found! deviceKey={}", deviceKey);
                return;
            }

            Device device = devices.get(0);
            Long deviceId = device.getId();
            long now = System.currentTimeMillis();

            // 更新设备实时状态
            DeviceStatus status = deviceStatusService.selectDeviceStatusByDeviceId(deviceId.toString());
            if (status == null) {
                // 无历史数据则新增记录
                status = new DeviceStatus();
                status.setDeviceId(deviceId.toString());
                status.setStatus(online ? 1 : 0);
                if (online) {
                    status.setLastConnectTime(now);
                } else {
                    status.setLastOfflineTime(now);
                }

                deviceStatusService.insertDeviceStatus(status);
            } else {
                // 更新记录
                status.setStatus(online ? 1 : 0);
                if (online) {
                    status.setLastConnectTime(now);
                } else {
                    status.setLastOfflineTime(now);
                }

                deviceStatusService.updateDeviceStatus(status);
            }

            // 记录上下线日志
            if (online) {
                // 上线：插入新日志
                DeviceAliveLog aliveLog = new DeviceAliveLog();
                aliveLog.setDeviceId(deviceId);
                aliveLog.setLastConnectTime(new Date());
                deviceAliveLogService.insertDeviceAliveLog(aliveLog);
            } else {
                // 下线：更新最近一条未结束的日志
                DeviceAliveLog queryLog = new DeviceAliveLog();
                queryLog.setDeviceId(deviceId);
                List<DeviceAliveLog> logs = deviceAliveLogService.selectDeviceAliveLogList(queryLog);

                DeviceAliveLog targetLog = null;
                if (logs != null && !logs.isEmpty()) {
                    // 寻找最后一条 lastDisconnectTime 为空的记录
                    for (int i = logs.size() - 1; i >= 0; i--) {
                        DeviceAliveLog logItem = logs.get(i);
                        if (logItem.getLastDisconnectTime() == null) {
                            targetLog = logItem;
                            break;
                        }
                    }
                }

                if (targetLog != null) {
                    targetLog.setLastDisconnectTime(new Date());
                    deviceAliveLogService.updateDeviceAliveLog(targetLog);
                } else {
                    // 如果没找到对应的上线记录，补一条
                    DeviceAliveLog newLog = new DeviceAliveLog();
                    newLog.setDeviceId(deviceId);
                    newLog.setLastDisconnectTime(new Date());
                    deviceAliveLogService.insertDeviceAliveLog(newLog);
                }
            }

        } catch (Exception e) {
            log.error("Handle connect status error: deviceKey={}, online={}", deviceKey, online, e);
        }
    }
}
