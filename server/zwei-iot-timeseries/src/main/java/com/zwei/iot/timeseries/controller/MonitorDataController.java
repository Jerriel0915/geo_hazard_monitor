package com.zwei.iot.timeseries.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.timeseries.service.MonitorDataQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监测数据查询接口。
 *
 * <p>面向 IoTDB 的监测数据查询入口，统一提供最新值、分页历史和图表曲线查询能力。</p>
 */
@RestController
@RequestMapping("/api/v1/monitor-data")
public class MonitorDataController {
    private final MonitorDataQueryService monitorDataQueryService;

    /**
     * 构造监测数据查询控制器。
     *
     * @param monitorDataQueryService 监测数据查询服务
     */
    @Autowired
    public MonitorDataController(MonitorDataQueryService monitorDataQueryService) {
        this.monitorDataQueryService = monitorDataQueryService;
    }

    /**
     * 查询隐患点下所有测点的最新监测值。
     *
     * @param hazardPointId 隐患点ID
     * @return 最新监测数据集合
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/latest")
    public AjaxResult latest(@RequestParam Long hazardPointId) {
        return AjaxResult.success("成功", monitorDataQueryService.latest(hazardPointId));
    }

    /**
     * 分页查询隐患点下的历史监测数据。
     *
     * <p>支持两种分页模式：传 {@code cursor}（上一页最后一行时间戳）走 keyset 游标路径；
     * 传 {@code pageNum} 走传统 offset 路径（多测点时有合并行数上限守护）。</p>
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可选
     * @param sensorId      传感器ID，可选
     * @param attrCode      属性编码，可选
     * @param startTime     开始时间，可选
     * @param endTime       结束时间，可选
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param cursor        游标时间戳（毫秒），上一页最后一行时间，可选
     * @return 分页监测数据结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/page")
    public AjaxResult page(@RequestParam(required = false) Long hazardPointId,
                           @RequestParam(required = false) Long deviceId,
                           @RequestParam(required = false) Long sensorId,
                           @RequestParam(required = false) String attrCode,
                           @RequestParam(required = false) String valueType,
                           @RequestParam(required = false) String startTime,
                           @RequestParam(required = false) String endTime,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) Long cursor) {
        return AjaxResult.success("成功", monitorDataQueryService.page(
                hazardPointId, deviceId, sensorId, attrCode, valueType, startTime, endTime, pageNum, pageSize, cursor
        ));
    }

    /**
     * 查询监测指标的图表数据。
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可选
     * @param sensorId      传感器ID，可选
     * @param attrCode      属性编码，可选
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param granularity   降采样粒度（null=自动），可选值：auto,raw,1m,5m,10m,30m,1h,6h
     * @return 图表监测数据结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/chart")
    public AjaxResult chart(@RequestParam Long hazardPointId,
                            @RequestParam(required = false) Long deviceId,
                            @RequestParam(required = false) Long sensorId,
                            @RequestParam(required = false) String attrCode,
                            @RequestParam(required = false) String valueType,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            @RequestParam(required = false) String granularity) {
        return AjaxResult.success("成功", monitorDataQueryService.chart(
                hazardPointId, deviceId, sensorId, attrCode, valueType, startTime, endTime, granularity
        ));
    }
}
