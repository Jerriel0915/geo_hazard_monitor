package com.zwei.iot.report.datasource;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IAlarmQueryService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import com.zwei.iot.timeseries.domain.AggregationFunction;
import com.zwei.iot.timeseries.domain.AggregationResultVO;
import com.zwei.iot.timeseries.domain.CompletenessReportVO;
import com.zwei.iot.timeseries.domain.ExpressionSpec;
import com.zwei.iot.timeseries.domain.TimeWindowSpec;
import com.zwei.iot.timeseries.domain.TrendReportVO;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 按 (hazardPointId, period) 聚合数据为 {@link ReportContext}。
 *
 * <p>依赖 5 个跨模块 Service; 单 hp 单周期串行调用。</p>
 *
 * <p>实现策略:</p>
 * <ul>
 *   <li>设备: {@link IDeviceHazardRelationService#getDevicesByHazardPoint(Long)}</li>
 *   <li>传感器: {@link IDeviceSensorService#selectSensorListByDeviceId(Long)} (含 attrList)</li>
 *   <li>聚合: {@link IotdbTimeSeriesService#queryAggregate} 单次调用 5 个函数 (MAX/MIN/AVG/SUM/LAST_VALUE)</li>
 *   <li>完整度: {@link IotdbTimeSeriesService#queryCompleteness}</li>
 *   <li>趋势: {@link IotdbTimeSeriesService#queryTrend} (端点斜率法)</li>
 *   <li>告警: {@link IAlarmQueryService}</li>
 * </ul>
 */
@Component
public class ReportDataAssembler {

    private static final Logger log = LoggerFactory.getLogger(ReportDataAssembler.class);

    private final IDeviceHazardRelationService deviceRelation;
    private final IDeviceSensorService sensorService;
    private final IotdbTimeSeriesService timeSeries;
    private final IAlarmQueryService alarmQuery;

    /**
     * 构造器注入 4 个 service。
     *
     * @param deviceRelation 设备-隐患点关联服务
     * @param sensorService  传感器查询服务
     * @param timeSeries     IoTDB 时序查询服务
     * @param alarmQuery     告警查询服务
     */
    public ReportDataAssembler(IDeviceHazardRelationService deviceRelation,
                               IDeviceSensorService sensorService,
                               IotdbTimeSeriesService timeSeries,
                               IAlarmQueryService alarmQuery) {
        this.deviceRelation = deviceRelation;
        this.sensorService = sensorService;
        this.timeSeries = timeSeries;
        this.alarmQuery = alarmQuery;
    }

    /**
     * 聚合数据为 ReportContext。
     *
     * @param type   报告类型
     * @param period 报告周期
     * @param hp     隐患点摘要
     * @return 填充完毕的 ReportContext
     */
    public ReportContext build(ReportType type, ReportPeriod period, HazardPointBrief hp) {
        LocalDateTime start = period.start().atStartOfDay();
        LocalDateTime end = period.end().atTime(23, 59, 59);
        long startMs = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 1. 设备
        List<DeviceBrief> devices = deviceRelation.getDevicesByHazardPoint(hp.id());
        int devTotal = devices.size();
        int devOnline = (int) devices.stream()
                .filter(d -> d.onlineStatus() != null && d.onlineStatus() == 1)
                .count();
        int devOffline = devTotal - devOnline;
        double onlineRate = devTotal == 0 ? 0.0 : (devOnline * 100.0 / devTotal);

        // 2. 指标
        List<MetricRow> metrics = buildMetrics(devices, startMs, endMs);

        // 3. 风险 (月/季才查)
        AlarmSummary alarmSummary = null;
        List<AlarmEvent> alarmTop = null;
        if (type == ReportType.MONTHLY || type == ReportType.QUARTERLY) {
            alarmSummary = alarmQuery.summarizeByHazardPoint(hp.id(), start, end);
            alarmTop = alarmQuery.listTopByHazardPoint(hp.id(), start, end, 10);
        }

        // 4. 趋势 (季才查)
        Map<String, String> trendDirs = null;
        Map<String, Double> trendSlopes = null;
        Map<String, Integer> alarmMonthly = null;
        if (type == ReportType.QUARTERLY) {
            TimeWindowSpec trendWindow = new TimeWindowSpec(startMs, endMs, TimeWindowSpec.WindowGranularity.RAW);
            trendDirs = new LinkedHashMap<>();
            trendSlopes = new LinkedHashMap<>();
            for (MetricRow m : metrics) {
                fillTrend(m, trendWindow, trendDirs, trendSlopes);
            }
            alarmMonthly = alarmQuery.countByMonth(hp.id(), start, end);
        }

        return new ReportContext(
                type, period, hp, LocalDateTime.now(),
                devices, devTotal, devOnline, devOffline, onlineRate,
                metrics,
                alarmSummary, alarmTop,
                trendDirs, trendSlopes, alarmMonthly
        );
    }

    /**
     * 为每个设备的每个传感器的每个属性构建 MetricRow。
     */
    private List<MetricRow> buildMetrics(List<DeviceBrief> devices, long startMs, long endMs) {
        List<MetricRow> rows = new ArrayList<>();
        for (DeviceBrief dev : devices) {
            try {
                List<DeviceSensor> sensors = sensorService.selectSensorListByDeviceId(dev.id());
                if (sensors == null) {
                    continue;
                }
                for (DeviceSensor sensor : sensors) {
                    String sensorCode = sensor.getSensorCode();
                    List<SensorAttribute> attrs = sensor.getAttrList();
                    if (attrs == null) {
                        continue;
                    }
                    for (SensorAttribute attr : attrs) {
                        MetricRow row = buildMetricRow(dev.id(), sensorCode, attr, startMs, endMs);
                        if (row != null) {
                            rows.add(row);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[report] skip device {} metric aggregation: {}", dev.id(), e.getMessage());
            }
        }
        return rows;
    }

    /**
     * 单个 (deviceId, sensorCode, attr) 的聚合查询。
     *
     * <p>使用 {@link IotdbTimeSeriesService#queryAggregate} 一次查询 5 个聚合函数
     * (MAX/MIN/AVG/SUM/LAST_VALUE)，再单独查完整度。</p>
     *
     * @return MetricRow 或 null (查询失败/无数据)
     */
    private MetricRow buildMetricRow(Long deviceId, String sensorCode,
                                      SensorAttribute attr, long startMs, long endMs) {
        String attrCode = attr.getAttrCode();
        String attrName = attr.getAttrName();
        String unit = attr.getUnit();

        TimeWindowSpec window = new TimeWindowSpec(startMs, endMs, TimeWindowSpec.WindowGranularity.RAW);

        // 5 个聚合函数一次查询
        List<ExpressionSpec> expressions = List.of(
                new ExpressionSpec.FunctionCall(AggregationFunction.MAX),
                new ExpressionSpec.FunctionCall(AggregationFunction.MIN),
                new ExpressionSpec.FunctionCall(AggregationFunction.AVG),
                new ExpressionSpec.FunctionCall(AggregationFunction.SUM),
                new ExpressionSpec.FunctionCall(AggregationFunction.LAST_VALUE)
        );

        List<AggregationResultVO> aggResults;
        try {
            aggResults = timeSeries.queryAggregate(deviceId, sensorCode, attrCode, window, expressions, null, null);
        } catch (Exception e) {
            log.warn("[report] agg query failed: deviceId={}, sensorCode={}, attrCode={}, err={}",
                    deviceId, sensorCode, attrCode, e.getMessage());
            return null;
        }

        Double maxVal = null;
        Double minVal = null;
        Double avgVal = null;
        Double sumVal = null;
        Double latestVal = null;

        // RAW 粒度只有一行结果 (或空)
        if (aggResults != null && !aggResults.isEmpty()) {
            Map<String, Double> metrics = aggResults.get(0).metrics();
            if (metrics != null) {
                maxVal = metrics.get("MAX");
                minVal = metrics.get("MIN");
                avgVal = metrics.get("AVG");
                sumVal = metrics.get("SUM");
                latestVal = metrics.get("LAST_VALUE");
            }
        }

        // 完整度
        Double completenessPct = null;
        try {
            CompletenessReportVO completeness = timeSeries.queryCompleteness(
                    deviceId, sensorCode, attrCode, window, null);
            if (completeness != null) {
                completenessPct = completeness.completenessRate() * 100.0;
            }
        } catch (Exception e) {
            log.debug("[report] completeness query failed: deviceId={}, attrCode={}, err={}",
                    deviceId, attrCode, e.getMessage());
        }

        return new MetricRow(
                deviceId, sensorCode, attrCode, attrName, unit,
                latestVal, maxVal, minVal, avgVal, sumVal, completenessPct
        );
    }

    /**
     * 季报趋势填充: 使用 {@link IotdbTimeSeriesService#queryTrend} 的端点斜率法。
     *
     * <p>若趋势查询失败或返回 unknown, 回退到 max-min 差值推断方向。</p>
     */
    private void fillTrend(MetricRow m, TimeWindowSpec window,
                           Map<String, String> dirs, Map<String, Double> slopes) {
        String key = m.attrCode();
        try {
            TrendReportVO trend = timeSeries.queryTrend(m.deviceId(), m.sensorCode(), m.attrCode(), window);
            if (trend != null && trend.trendDirection() != null && !"unknown".equals(trend.trendDirection())) {
                // 映射 IoTDB 方向到报告方向
                String dir = switch (trend.trendDirection()) {
                    case "rising" -> "UP";
                    case "falling" -> "DOWN";
                    case "stable" -> "STABLE";
                    default -> "STABLE";
                };
                dirs.put(key, dir);
                slopes.put(key, trend.ratePerDay() != null ? trend.ratePerDay() : 0.0);
                return;
            }
        } catch (Exception e) {
            log.debug("[report] trend query failed for {}: {}, falling back to max-min",
                    key, e.getMessage());
        }

        // 回退方案: 用 max-min 差值推断
        double slope = 0.0;
        String dir = "STABLE";

        if (m.maxValue() != null && m.minValue() != null) {
            slope = m.maxValue() - m.minValue();
            double denominator = Math.max(Math.abs(m.maxValue()), Math.max(Math.abs(m.minValue()), 1e-9));
            double ratio = m.maxValue() / denominator - m.minValue() / denominator;
            if (ratio > 0.05) {
                dir = "UP";
            } else if (ratio < -0.05) {
                dir = "DOWN";
            }
        }

        dirs.put(key, dir);
        slopes.put(key, slope);
    }
}
