package com.zwei.monitor.domain.dashboard;

import lombok.Data;

/**
 * 大屏一体化聚合 VO。
 * <p>
 * 一次返回 overview + onlineRate + activeRate + trend + distribution + healthScore，
 * 将前端 6 次请求合并为 1 次。
 */
@Data
public class DashboardFullVO {
    private DashboardOverviewVO overview;
    private RateByTypeVO deviceOnlineRate;
    private RateByTypeVO deviceActiveRate;
    private RateByTypeVO sensorOnlineRate;
    private HazardPointTrendVO hazardPointTrend;
    private SensorDistributionVO sensorDistribution;
    private HealthScoreVO healthScore;
}
