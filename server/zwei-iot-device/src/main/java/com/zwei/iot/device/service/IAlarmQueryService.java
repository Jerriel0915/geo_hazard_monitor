package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警查询服务 (跨模块接口, 实现在 zwei-iot-alarm)。
 */
public interface IAlarmQueryService {

    /**
     * 按隐患点+时间窗汇总告警。
     */
    AlarmSummary summarizeByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end);

    /**
     * 按隐患点+时间窗列出最近 limit 条告警事件。
     */
    List<AlarmEvent> listTopByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end, int limit);

    /**
     * 按隐患点+时间窗按月分组统计告警次数。
     *
     * @return key=yyyy-MM, value=count
     */
    Map<String, Integer> countByMonth(Long hazardPointId, LocalDateTime start, LocalDateTime end);

    /** 批量查询隐患点是否有待处理告警 (status 1=待处理 2=处理中) */
    Map<Long, Boolean> hasPendingAlarm(List<Long> hazardPointIds);
}
