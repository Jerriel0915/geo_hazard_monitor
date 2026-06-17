package com.zwei.iot.report.datasource;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 渲染器输入: 聚合后的报告数据。
 *
 * <p>不同 {@link ReportType} 只填充相关字段, 其余保持 {@code null}/空。</p>
 *
 * @param type             报告类型
 * @param period           报告周期
 * @param hazardPoint      隐患点摘要
 * @param generatedAt      生成时间
 * @param devices          设备列表 (周/月/季都用)
 * @param deviceTotal      设备总数
 * @param deviceOnline     在线数
 * @param deviceOffline    离线数
 * @param onlineRatePct    在线率 (0-100)
 * @param metrics          监测数据指标行 (周/月/季都用)
 * @param alarmSummary     告警摘要 (月/季用, 周报为 null)
 * @param alarmTopEvents   告警 Top 事件 (月/季用, 周报为 null)
 * @param trendDirections  趋势方向 (季用, 周/月为 null); key = attrCode, value = UP/DOWN/STABLE
 * @param trendSlopes      趋势斜率 (季用, 周/月为 null); key = attrCode, value = 最大最小差值
 * @param alarmMonthlyCount 月度告警次数 (季用, 周/月为 null); key = yyyy-MM, value = count
 */
public record ReportContext(
        ReportType type,
        ReportPeriod period,
        HazardPointBrief hazardPoint,
        LocalDateTime generatedAt,

        // === 设备 (周/月/季都用) ===
        List<DeviceBrief> devices,
        int deviceTotal,
        int deviceOnline,
        int deviceOffline,
        double onlineRatePct,

        // === 监测数据指标 (周/月/季都用) ===
        List<MetricRow> metrics,

        // === 风险 (月/季用, 周报为 null) ===
        AlarmSummary alarmSummary,
        List<AlarmEvent> alarmTopEvents,

        // === 趋势 (季用, 周/月为 null) ===
        Map<String, String> trendDirections,
        Map<String, Double> trendSlopes,
        Map<String, Integer> alarmMonthlyCount
) {}
