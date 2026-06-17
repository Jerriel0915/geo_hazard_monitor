package com.zwei.iot.device.domain.brief;

import java.util.Map;

/**
 * 按隐患点+时间窗聚合的告警摘要。
 * <ul>
 *   <li>levelCount: key=告警级别(1蓝/2黄/3橙/4红), value=次数</li>
 *   <li>statusCount: key=状态(1待处理/2处理中/3已销警/4误报), value=次数</li>
 * </ul>
 */
public record AlarmSummary(
    Long hazardPointId,
    int total,
    int maxLevel,
    int pendingCount,
    Map<Integer, Integer> levelCount,
    Map<Integer, Integer> statusCount
) {}
