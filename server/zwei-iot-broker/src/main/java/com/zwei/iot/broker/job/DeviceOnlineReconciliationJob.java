package com.zwei.iot.broker.job;

import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.device.service.DeviceOnlineStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 设备在线状态周期性对账。
 * <p>
 * 以 {@link MqttDeviceSessionRegistry}（MQTT broker 实际活跃连接）为准，
 * 将 {@code device_online_status} 表中不在活跃连接清单内的设备标记为离线。
 *
 * <h3>触发场景</h3>
 * <ul>
 *   <li>设备网络中断但 MQTT keepalive 回调延迟</li>
 *   <li>异常关闭后未清理的残留在线状态（已有启动恢复兜底）</li>
 *   <li>极端情况下的状态漂移</li>
 * </ul>
 */
@Component
public class DeviceOnlineReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(DeviceOnlineReconciliationJob.class);

    private final MqttDeviceSessionRegistry sessionRegistry;
    private final DeviceOnlineStatusService onlineStatusService;

    public DeviceOnlineReconciliationJob(MqttDeviceSessionRegistry sessionRegistry,
                                         DeviceOnlineStatusService onlineStatusService) {
        this.sessionRegistry = sessionRegistry;
        this.onlineStatusService = onlineStatusService;
    }

    /**
     * 每 60 秒对账一次：获取 MQTT broker 实际在线设备清单，标记不在清单内的设备为离线。
     */
    @Scheduled(fixedDelay = 60_000)
    public void reconcile() {
        try {
            Set<Long> connectedDeviceIds = sessionRegistry.getConnectedDeviceIds();
            List<Long> ids = new ArrayList<>(connectedDeviceIds);
            onlineStatusService.reconcileOffline(ids);
        } catch (Exception e) {
            log.warn("设备在线状态对账失败", e);
        }
    }
}
