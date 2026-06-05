package com.zwei.monitor.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.monitor.service.DashboardStatService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大屏仪表盘统计接口。
 * <p>
 * 为全息看板提供设备、传感器、隐患点、监测类型等多维度聚合数据。
 */
@RestController
@RequestMapping("/api/v1/monitor/dashboard")
public class DashboardStatController {

    private final DashboardStatService dashboardStatService;

    public DashboardStatController(DashboardStatService dashboardStatService) {
        this.dashboardStatService = dashboardStatService;
    }

    /**
     * 2.0 大屏一体化聚合（替代前端多次请求）
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/full")
    public AjaxResult full(@RequestParam(defaultValue = "60") int windowMinutes) {
        return AjaxResult.success(dashboardStatService.getFull(windowMinutes));
    }

    /**
     * 2.1 资源总览
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return AjaxResult.success(dashboardStatService.getOverview());
    }

    /**
     * 2.2 设备在线率
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/device-online-rate")
    public AjaxResult deviceOnlineRate() {
        return AjaxResult.success(dashboardStatService.getDeviceOnlineRate());
    }

    /**
     * 2.3 设备活跃率（基于 device_online_status.last_report_at 时间窗口）
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/device-active-rate")
    public AjaxResult deviceActiveRate(@RequestParam(defaultValue = "60") int windowMinutes) {
        return AjaxResult.success(dashboardStatService.getDeviceActiveRate(windowMinutes));
    }

    /**
     * 2.4 传感器在线率
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/sensor-online-rate")
    public AjaxResult sensorOnlineRate() {
        return AjaxResult.success(dashboardStatService.getSensorOnlineRate());
    }

    /**
     * 2.5 传感器活跃率（IoTDB 窗口，待 IoTDB 查询服务增强）
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/sensor-active-rate")
    public AjaxResult sensorActiveRate(@RequestParam(defaultValue = "60") int windowMinutes) {
        return AjaxResult.success(dashboardStatService.getSensorActiveRate(windowMinutes));
    }

    /**
     * 2.6 隐患点增长趋势
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/hazard-point-trend")
    public AjaxResult hazardPointTrend(@RequestParam(defaultValue = "12") int months) {
        return AjaxResult.success(dashboardStatService.getHazardPointTrend(months));
    }

    /**
     * 2.7 传感器按监测类型分布
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/sensor-distribution")
    public AjaxResult sensorDistribution() {
        return AjaxResult.success(dashboardStatService.getSensorDistribution());
    }

    /**
     * 2.8 系统健康度评分
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/health-score")
    public AjaxResult healthScore() {
        return AjaxResult.success(dashboardStatService.getHealthScore());
    }
}
