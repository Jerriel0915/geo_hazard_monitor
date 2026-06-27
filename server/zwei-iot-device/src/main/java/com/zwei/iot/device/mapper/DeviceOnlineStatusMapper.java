package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import com.zwei.iot.device.domain.DeviceOnlineStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备在线状态 Mapper。
 * <p>
 * 操作 device_online_status 和 device_online_event_log 两张运维表。
 */
public interface DeviceOnlineStatusMapper {

    // ========== device_online_status 快照表 ==========

    /** 上线时写入/更新在线状态 */
    int upsertOnline(DeviceOnlineStatus status);

    /** 按设备 ID 查询在线状态记录 */
    DeviceOnlineStatus selectByDeviceId(@Param("deviceId") Long deviceId);

    /** 离线时更新离线状态与会话时长 */
    int upsertOffline(@Param("deviceId") Long deviceId,
                      @Param("offlineAt") String offlineAt,
                      @Param("reason") String reason);

    /** 更新最后数据上报时间 */
    int updateLastReportAt(@Param("deviceId") Long deviceId,
                           @Param("lastReportAt") String lastReportAt);

    /** 统计当前在线设备数 */
    int countOnline();

    /** 统计时间窗口内活跃设备数（有数据上报） */
    int countActiveInWindow(@Param("windowMinutes") int windowMinutes);

    /** 启动恢复: 将所有在线设备重置为离线（异常关闭后 MQTT broker 无留存连接） */
    int resetAllOnlineToOffline(@Param("offlineAt") String offlineAt);

    /** 批量标记不在连接清单内的设备为离线（周期性对账） */
    int markOfflineExcept(@Param("offlineAt") String offlineAt,
                          @Param("connectedDeviceIds") List<Long> connectedDeviceIds);

    // ========== device_online_event_log 事件表 ==========

    /** 插入一条上下线事件日志 */
    int insertEventLog(DeviceOnlineEventLog log);

    /** 按设备统计过去 N 分钟内的掉线次数（用于健康度计算） */
    int countOfflineEventsInWindow(@Param("deviceId") Long deviceId,
                                   @Param("windowMinutes") int windowMinutes);
}
