package com.zwei.monitor.domain.dashboard;

import lombok.Data;

import java.util.Map;

/**
 * 大屏资源总览聚合 VO。
 */
@Data
public class DashboardOverviewVO {
    private DeviceSummary device;
    private SensorSummary sensor;
    private DeviceOnlineRateSummary deviceOnlineRate;
    private HazardPointSummary hazardPoint;
    private MonitorTypeSummary monitorType;
    private VideoDeviceSummary videoDevice;

    /** 累计监测数据点总数 (Redis 计数器) */
    private long totalMonitorCount;

    @Data
    public static class DeviceSummary {
        private int total;
        private Map<String, Integer> byStatus;
    }

    @Data
    public static class SensorSummary {
        private int total;
        private int enabled;
        private int disabled;
        private double onlineRate;
    }

    @Data
    public static class DeviceOnlineRateSummary {
        private int total;
        private int online;
        private double onlineRate;
    }

    @Data
    public static class HazardPointSummary {
        private int total;
        private Map<String, Integer> byStatus;
    }

    @Data
    public static class MonitorTypeSummary {
        private int total;
    }

    @Data
    public static class VideoDeviceSummary {
        private int total;
        private Map<String, Integer> byStatus;
    }
}
