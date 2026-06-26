package com.zwei.iot.broker.job;

import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.device.service.DeviceOnlineStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
 *   <li><b>启动对账</b> — {@link ApplicationReadyEvent} 触发，基于 broker 实际连接状态修正数据库</li>
 *   <li><b>周期对账</b> — 每 60s 定时执行，防止状态漂移</li>
 *   <li>设备网络中断但 MQTT keepalive 回调延迟</li>
 *   <li>异常关闭后未清理的残留在线状态</li>
 * </ul>
 *
 * <h3>与盲目重置的区别</h3>
 * 不在启动时无条件将所有设备标记为离线，而是以 broker 实际连接清单为基准：
 * 启动期间已重连的设备会正确保留在线状态，未连接的设备才会被标记离线。
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
     * 启动时立即对账：以 MQTT broker 实际在线设备为准修正数据库状态。
     * <p>
     * 使用 {@link ApplicationReadyEvent} 确保 broker 已完全初始化后再对账，
     * 避免盲目重置导致启动期间重连的设备被误标为离线。
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("启动对账开始...");
        try {
            doReconcile();
        } catch (Exception e) {
            log.warn("启动对账失败，将在周期对账中自动修复", e);
        }
    }

    /**
     * 每 60 秒对账一次：获取 MQTT broker 实际在线设备清单，标记不在清单内的设备为离线。
     */
    @Scheduled(fixedDelay = 60_000)
    public void reconcile() {
        try {
            doReconcile();
        } catch (Exception e) {
            log.warn("设备在线状态对账失败", e);
        }
    }

    private void doReconcile() {
        Set<Long> connectedDeviceIds = sessionRegistry.getConnectedDeviceIds();
        List<Long> ids = new ArrayList<>(connectedDeviceIds);
        onlineStatusService.reconcileOffline(ids);
    }
}
