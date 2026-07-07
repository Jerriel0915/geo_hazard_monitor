package com.zwei.monitor.service;

import com.zwei.iot.device.service.IDeviceStatService;
import com.zwei.monitor.domain.dashboard.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 大屏仪表盘统计服务。
 * <p>
 * 通过 IDeviceStatService 接口聚合 MySQL 基础统计与运维指标，
 * 为全息看板提供设备、传感器、隐患点、监测类型等多维度数据。
 */
@Slf4j
@Service
public class DashboardStatService {

    private static final double WEIGHT_DATA_COMPLETENESS = 0.20;
    private static final double WEIGHT_DEVICE_ONLINE = 0.15;
    private static final double WEIGHT_DEVICE_NORMAL = 0.15;
    private static final double WEIGHT_ALARM_RESPONSE = 0.20;
    private static final double WEIGHT_SLOPE_STABILITY = 0.30;

    private final IDeviceStatService deviceStatService;

    public DashboardStatService(IDeviceStatService deviceStatService) {
        this.deviceStatService = deviceStatService;
    }

    // ==================== 2.0 一体化聚合 ====================

    public DashboardFullVO getFull(int windowMinutes) {
        DashboardFullVO vo = new DashboardFullVO();
        vo.setOverview(getOverview());
        vo.setDeviceOnlineRate(getDeviceOnlineRate());
        vo.setDeviceActiveRate(getDeviceActiveRate(windowMinutes));
        vo.setSensorOnlineRate(getSensorOnlineRate());
        vo.setHazardPointTrend(getHazardPointTrend(12));
        vo.setSensorDistribution(getSensorDistribution());
        vo.setHealthScore(getHealthScore());
        return vo;
    }

    // ==================== 2.1 资源总览 ====================

    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        int deviceTotal = deviceStatService.countAllDevices();
        DashboardOverviewVO.DeviceSummary ds = new DashboardOverviewVO.DeviceSummary();
        ds.setTotal(deviceTotal);
        ds.setByStatus(toMap(deviceStatService.countDevicesByStatus(), "status"));
        vo.setDevice(ds);

        // 设备在线率：改用 device_online_status 独立表查询
        int deviceOnline = deviceStatService.countOnlineDevices();
        DashboardOverviewVO.DeviceOnlineRateSummary dr = new DashboardOverviewVO.DeviceOnlineRateSummary();
        dr.setTotal(deviceTotal);
        dr.setOnline(deviceOnline);
        dr.setOnlineRate(deviceTotal > 0 ? Math.round(deviceOnline * 10000.0 / deviceTotal) / 100.0 : 0);
        vo.setDeviceOnlineRate(dr);

        int sensorTotal = deviceStatService.countAllSensors();
        int sensorEnabled = deviceStatService.countSensorsByStatus().stream()
                .filter(m -> Objects.equals(1, m.get("status")))
                .mapToInt(m -> ((Number) m.get("cnt")).intValue()).sum();
        DashboardOverviewVO.SensorSummary ss = new DashboardOverviewVO.SensorSummary();
        ss.setTotal(sensorTotal);
        ss.setEnabled(sensorEnabled);
        ss.setDisabled(sensorTotal - sensorEnabled);
        ss.setOnlineRate(sensorTotal > 0 ? Math.round(sensorEnabled * 10000.0 / sensorTotal) / 100.0 : 0);
        vo.setSensor(ss);

        int hpTotal = deviceStatService.countAllHazardPoints();
        DashboardOverviewVO.HazardPointSummary hs = new DashboardOverviewVO.HazardPointSummary();
        hs.setTotal(hpTotal);
        hs.setByStatus(toMap(deviceStatService.countHazardPointsByStatus(), "status"));
        vo.setHazardPoint(hs);

        DashboardOverviewVO.MonitorTypeSummary ms = new DashboardOverviewVO.MonitorTypeSummary();
        ms.setTotal(deviceStatService.countAllMonitorTypes());
        vo.setMonitorType(ms);

        int vdTotal = deviceStatService.countAllVideoDevices();
        DashboardOverviewVO.VideoDeviceSummary vs = new DashboardOverviewVO.VideoDeviceSummary();
        vs.setTotal(vdTotal);
        vs.setByStatus(toMap(deviceStatService.countVideoDevicesByStatus(), "status"));
        vo.setVideoDevice(vs);

        vo.setTotalMonitorCount(deviceStatService.countTotalMonitorDataPoints());

        return vo;
    }

    // ==================== 2.2 设备在线率（基于 device_online_status 独立表） ====================

    public RateByTypeVO getDeviceOnlineRate() {
        int total = deviceStatService.countAllDevices();
        int online = deviceStatService.countOnlineDevices();

        RateByTypeVO vo = new RateByTypeVO();
        vo.setTotal(total);
        vo.setOnline(online);
        vo.setOffline(total - online);
        vo.setOnlineRate(total > 0 ? Math.round(online * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildOnlineTypeStats(deviceStatService.countOnlineDevicesByMonitorType()));
        return vo;
    }

    /** 使用真实在线数构建按监测类型分组的统计 */
    private List<RateByTypeVO.TypeStat> buildOnlineTypeStats(List<Map<String, Object>> typeRows) {
        return typeRows.stream().map(row -> {
            RateByTypeVO.TypeStat ts = new RateByTypeVO.TypeStat();
            ts.setMonitorTypeId(((Number) row.get("monitorTypeId")).longValue());
            ts.setMonitorTypeName((String) row.get("monitorTypeName"));
            ts.setSortOrder(((Number) row.getOrDefault("sortOrder", 999)).intValue());
            int cnt = ((Number) row.get("total")).intValue();
            int typeOnline = ((Number) row.get("online")).intValue();
            ts.setTotal(cnt);
            ts.setOnline(typeOnline);
            ts.setOffline(cnt - typeOnline);
            ts.setOnlineRate(cnt > 0 ? Math.round(typeOnline * 10000.0 / cnt) / 100.0 : 0);
            return ts;
        }).collect(Collectors.toList());
    }

    // ==================== 2.3 设备活跃率（基于 device_online_status.last_report_at 时间窗口） ====================

    public RateByTypeVO getDeviceActiveRate(int windowMinutes) {
        int total = deviceStatService.countAllDevices();
        int active = deviceStatService.countActiveDevicesInWindow(windowMinutes);

        RateByTypeVO vo = new RateByTypeVO();
        vo.setWindowMinutes(windowMinutes);
        vo.setTotal(total);
        vo.setOnline(active);
        vo.setOffline(total - active);
        vo.setOnlineRate(total > 0 ? Math.round(active * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildActiveTypeStats(deviceStatService.countActiveDevicesByMonitorType(windowMinutes)));
        return vo;
    }

    // ==================== 2.4 传感器在线率（传感器在线 = 所属设备已连接 MQTT） ====================

    public RateByTypeVO getSensorOnlineRate() {
        int total = deviceStatService.countAllSensors();
        int online = deviceStatService.countSensorsByDeviceOnline();

        RateByTypeVO vo = new RateByTypeVO();
        vo.setTotal(total);
        vo.setOnline(online);
        vo.setOffline(total - online);
        vo.setOnlineRate(total > 0 ? Math.round(online * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildOnlineTypeStats(deviceStatService.countOnlineSensorsByMonitorType()));
        return vo;
    }

    // ==================== 2.5 传感器活跃率（基于 device_sensor.last_report_time 时间窗口） ====================

    public RateByTypeVO getSensorActiveRate(int windowMinutes) {
        int total = deviceStatService.countAllSensors();
        int active = deviceStatService.countActiveSensorsInWindow(windowMinutes);

        RateByTypeVO vo = new RateByTypeVO();
        vo.setWindowMinutes(windowMinutes);
        vo.setTotal(total);
        vo.setOnline(active);
        vo.setOffline(total - active);
        vo.setOnlineRate(total > 0 ? Math.round(active * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildActiveTypeStats(deviceStatService.countActiveSensorsByMonitorType(windowMinutes)));
        return vo;
    }

    // ==================== 2.6 隐患点增长趋势 ====================

    public HazardPointTrendVO getHazardPointTrend(int months) {
        List<Map<String, Object>> rows = deviceStatService.countHazardPointsByMonth(months);
        List<String> monthLabels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Integer> cumulative = new ArrayList<>();
        int cum = 0;
        for (Map<String, Object> row : rows) {
            String m = (String) row.get("month");
            int cnt = ((Number) row.get("cnt")).intValue();
            monthLabels.add(m);
            counts.add(cnt);
            cum += cnt;
            cumulative.add(cum);
        }
        HazardPointTrendVO vo = new HazardPointTrendVO();
        vo.setMonths(monthLabels);
        vo.setCounts(counts);
        vo.setCumulativeCounts(cumulative);
        return vo;
    }

    // ==================== 2.7 传感器按监测类型分布 ====================

    public SensorDistributionVO getSensorDistribution() {
        List<Map<String, Object>> rows = deviceStatService.countSensorsByMonitorType();
        List<SensorDistributionVO.TypeCount> list = rows.stream().map(row -> {
            SensorDistributionVO.TypeCount tc = new SensorDistributionVO.TypeCount();
            tc.setMonitorTypeId(((Number) row.get("monitorTypeId")).longValue());
            tc.setMonitorTypeName((String) row.get("monitorTypeName"));
            tc.setSensorCount(((Number) row.get("cnt")).intValue());
            return tc;
        }).collect(Collectors.toList());
        SensorDistributionVO vo = new SensorDistributionVO();
        vo.setList(list);
        return vo;
    }

    // ==================== 2.8 系统健康度 ====================

    public HealthScoreVO getHealthScore() {
        int deviceTotal = deviceStatService.countAllDevices();
        int deviceComplete = deviceStatService.countDevicesComplete();
        int deviceOnline = deviceStatService.countOnlineDevices();
        int deviceNormal = deviceStatService.countDevicesNormal();
        int hpTotal = deviceStatService.countAllHazardPoints();

        double dataCompleteness = deviceTotal > 0 ? Math.round(deviceComplete * 10000.0 / deviceTotal) / 100.0 : 0;
        double deviceOnlinePct = deviceTotal > 0 ? Math.round(deviceOnline * 10000.0 / deviceTotal) / 100.0 : 0;
        double deviceNormalPct = deviceTotal > 0 ? Math.round(deviceNormal * 10000.0 / deviceTotal) / 100.0 : 0;
        // 告警响应率与边坡稳定率需要告警模块数据，当前使用占位值
        double alarmResponse = 100.0;
        double slopeStability = 100.0;

        List<HealthScoreVO.HealthItem> items = List.of(
                HealthScoreVO.HealthItem.of("资料完善率", dataCompleteness, WEIGHT_DATA_COMPLETENESS, "#52c41a", "computed"),
                HealthScoreVO.HealthItem.of("设备在线率", deviceOnlinePct, WEIGHT_DEVICE_ONLINE, "#1890ff", "computed"),
                HealthScoreVO.HealthItem.of("设备正常率", deviceNormalPct, WEIGHT_DEVICE_NORMAL, "#722ed1", "computed"),
                HealthScoreVO.HealthItem.of("告警及时响应率", alarmResponse, WEIGHT_ALARM_RESPONSE, "#fa8c16", "placeholder"),
                HealthScoreVO.HealthItem.of("边坡稳定率", slopeStability, WEIGHT_SLOPE_STABILITY, "#eb2f96", "placeholder")
        );

        double overall = 0;
        for (HealthScoreVO.HealthItem item : items) {
            overall += item.getValue() * item.getWeight();
        }
        overall = Math.round(overall * 10.0) / 10.0;

        HealthScoreVO vo = new HealthScoreVO();
        vo.setOverallScore(overall);
        vo.setItems(items);
        return vo;
    }

    // ==================== 工具方法 ====================

    private Map<String, Integer> toMap(List<Map<String, Object>> rows, String keyCol) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object key = row.get(keyCol);
            Number val = (Number) row.get("cnt");
            result.put(key != null ? key.toString() : "null", val != null ? val.intValue() : 0);
        }
        return result;
    }

    /** 使用真实活跃数构建按监测类型分组的统计 */
    private List<RateByTypeVO.TypeStat> buildActiveTypeStats(List<Map<String, Object>> typeRows) {
        return typeRows.stream().map(row -> {
            RateByTypeVO.TypeStat ts = new RateByTypeVO.TypeStat();
            ts.setMonitorTypeId(((Number) row.get("monitorTypeId")).longValue());
            ts.setMonitorTypeName((String) row.get("monitorTypeName"));
            ts.setSortOrder(((Number) row.getOrDefault("sortOrder", 999)).intValue());
            int cnt = ((Number) row.get("total")).intValue();
            int typeActive = ((Number) row.get("active")).intValue();
            ts.setTotal(cnt);
            ts.setOnline(typeActive);
            ts.setOffline(cnt - typeActive);
            ts.setOnlineRate(cnt > 0 ? Math.round(typeActive * 10000.0 / cnt) / 100.0 : 0);
            return ts;
        }).collect(Collectors.toList());
    }
}
