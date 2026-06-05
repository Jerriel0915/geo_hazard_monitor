package com.zwei.monitor.service;

import com.zwei.iot.device.service.IDeviceStatService;
import com.zwei.monitor.domain.dashboard.DashboardOverviewVO;
import com.zwei.monitor.domain.dashboard.HazardPointTrendVO;
import com.zwei.monitor.domain.dashboard.RateByTypeVO;
import com.zwei.monitor.domain.dashboard.SensorDistributionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 大屏仪表盘统计服务。
 * <p>
 * 通过 IDeviceStatService 接口聚合 MySQL 基础统计，
 * 为全息看板提供设备、传感器、隐患点、监测类型等多维度数据。
 */
@Slf4j
@Service
public class DashboardStatService {

    private final IDeviceStatService deviceStatService;

    public DashboardStatService(IDeviceStatService deviceStatService) {
        this.deviceStatService = deviceStatService;
    }

    // ==================== 2.1 资源总览 ====================

    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        // 设备
        int deviceTotal = deviceStatService.countAllDevices();
        DashboardOverviewVO.DeviceSummary ds = new DashboardOverviewVO.DeviceSummary();
        ds.setTotal(deviceTotal);
        ds.setByStatus(toMap(deviceStatService.countDevicesByStatus(), "status"));
        ds.setByRunStatus(toMap(deviceStatService.countDevicesByRunStatus(), "runStatus"));
        vo.setDevice(ds);

        // 设备在线率（MySQL run_status）
        int deviceOnline = deviceStatService.countDevicesByRunStatus().stream()
                .filter(m -> Objects.equals(1, m.get("runStatus")))
                .mapToInt(m -> ((Number) m.get("cnt")).intValue()).sum();
        DashboardOverviewVO.DeviceOnlineRateSummary dr = new DashboardOverviewVO.DeviceOnlineRateSummary();
        dr.setTotal(deviceTotal);
        dr.setOnline(deviceOnline);
        dr.setOnlineRate(deviceTotal > 0 ? Math.round(deviceOnline * 10000.0 / deviceTotal) / 100.0 : 0);
        vo.setDeviceOnlineRate(dr);

        // 传感器
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

        // 隐患点
        int hpTotal = deviceStatService.countAllHazardPoints();
        DashboardOverviewVO.HazardPointSummary hs = new DashboardOverviewVO.HazardPointSummary();
        hs.setTotal(hpTotal);
        hs.setByStatus(toMap(deviceStatService.countHazardPointsByStatus(), "status"));
        vo.setHazardPoint(hs);

        // 监测类型
        DashboardOverviewVO.MonitorTypeSummary ms = new DashboardOverviewVO.MonitorTypeSummary();
        ms.setTotal(deviceStatService.countAllMonitorTypes());
        vo.setMonitorType(ms);

        // 视频设备
        int vdTotal = deviceStatService.countAllVideoDevices();
        DashboardOverviewVO.VideoDeviceSummary vs = new DashboardOverviewVO.VideoDeviceSummary();
        vs.setTotal(vdTotal);
        vs.setByStatus(toMap(deviceStatService.countVideoDevicesByStatus(), "status"));
        vo.setVideoDevice(vs);

        return vo;
    }

    // ==================== 2.2 设备在线率（MySQL） ====================

    public RateByTypeVO getDeviceOnlineRate() {
        int total = deviceStatService.countAllDevices();
        List<Map<String, Object>> runStatusRows = deviceStatService.countDevicesByRunStatus();
        int online = runStatusRows.stream()
                .filter(m -> Objects.equals(1, m.get("runStatus")))
                .mapToInt(m -> ((Number) m.get("cnt")).intValue()).sum();

        RateByTypeVO vo = new RateByTypeVO();
        vo.setTotal(total);
        vo.setOnline(online);
        vo.setOffline(total - online);
        vo.setOnlineRate(total > 0 ? Math.round(online * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildTypeStats(deviceStatService.countDevicesByMonitorType(), total));
        return vo;
    }

    // ==================== 2.3 设备活跃率（IoTDB 窗口） ====================

    public RateByTypeVO getDeviceActiveRate(int windowMinutes) {
        int total = deviceStatService.countAllDevices();
        List<Map<String, Object>> typeRows = deviceStatService.countDevicesByMonitorType();
        List<Map<String, Object>> runStatusRows = deviceStatService.countDevicesByRunStatus();
        int active = runStatusRows.stream()
                .filter(m -> Objects.equals(1, m.get("runStatus")))
                .mapToInt(m -> ((Number) m.get("cnt")).intValue()).sum();

        RateByTypeVO vo = new RateByTypeVO();
        vo.setWindowMinutes(windowMinutes);
        vo.setTotal(total);
        vo.setOnline(active);
        vo.setOffline(total - active);
        vo.setOnlineRate(total > 0 ? Math.round(active * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildTypeStats(typeRows, total));
        return vo;
    }

    // ==================== 2.4 传感器在线率（MySQL） ====================

    public RateByTypeVO getSensorOnlineRate() {
        int total = deviceStatService.countAllSensors();
        List<Map<String, Object>> statusRows = deviceStatService.countSensorsByStatus();
        int enabled = statusRows.stream()
                .filter(m -> Objects.equals(1, m.get("status")))
                .mapToInt(m -> ((Number) m.get("cnt")).intValue()).sum();

        RateByTypeVO vo = new RateByTypeVO();
        vo.setTotal(total);
        vo.setOnline(enabled);
        vo.setOffline(total - enabled);
        vo.setOnlineRate(total > 0 ? Math.round(enabled * 10000.0 / total) / 100.0 : 0);
        vo.setByType(buildTypeStats(deviceStatService.countSensorsByMonitorType(), total));
        return vo;
    }

    // ==================== 2.5 传感器活跃率（IoTDB 窗口） ====================

    public RateByTypeVO getSensorActiveRate(int windowMinutes) {
        RateByTypeVO vo = getSensorOnlineRate();
        vo.setWindowMinutes(windowMinutes);
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

    private List<RateByTypeVO.TypeStat> buildTypeStats(List<Map<String, Object>> typeRows, int total) {
        return typeRows.stream().map(row -> {
            RateByTypeVO.TypeStat ts = new RateByTypeVO.TypeStat();
            ts.setMonitorTypeId(((Number) row.get("monitorTypeId")).longValue());
            ts.setMonitorTypeName((String) row.get("monitorTypeName"));
            int cnt = ((Number) row.get("cnt")).intValue();
            ts.setTotal(cnt);
            ts.setOnline(cnt);
            ts.setOffline(0);
            ts.setOnlineRate(cnt > 0 ? 100.0 : 0);
            return ts;
        }).collect(Collectors.toList());
    }
}
