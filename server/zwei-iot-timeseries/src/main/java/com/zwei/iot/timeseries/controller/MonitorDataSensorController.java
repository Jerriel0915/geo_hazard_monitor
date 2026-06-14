package com.zwei.iot.timeseries.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.timeseries.domain.*;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import com.zwei.iot.timeseries.service.MonitorDataAggregationService;
import com.zwei.iot.timeseries.service.MonitorDataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传感器维度监测数据查询接口。
 *
 * <p>基于 (deviceId, sensorCode) 入口,提供批量最新值、区间数据、多表达式聚合、完整度、趋势。
 * 与现有 hazardPointId 维度的 /latest /page /chart 接口并存,前端可按需选择调用。</p>
 */
@RestController
@RequestMapping("/api/v1/monitor-data/sensor")
@PreAuthorize("@ss.hasPermi('basic:device:query')")
public class MonitorDataSensorController {

    private final IotdbTimeSeriesService iotdbTimeSeriesService;
    private final MonitorDataAggregationService aggregationService;
    private final MonitorDataAnalysisService analysisService;

    @Autowired
    public MonitorDataSensorController(
            IotdbTimeSeriesService iotdbTimeSeriesService,
            MonitorDataAggregationService aggregationService,
            MonitorDataAnalysisService analysisService) {
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
        this.aggregationService = aggregationService;
        this.analysisService = analysisService;
    }

    /** 1. 传感器下所有指标最新值(可指定 attrCode 过滤) */
    @GetMapping("/latest")
    public AjaxResult latest(@RequestParam Long deviceId,
                             @RequestParam String sensorCode,
                             @RequestParam(required = false) String attrCode) {
        return AjaxResult.success("成功",
                aggregationService.latestBySensor(deviceId, sensorCode, attrCode));
    }

    /** 2. 区间数据(支持数值范围) */
    @GetMapping("/range")
    public AjaxResult range(@RequestParam Long deviceId,
                            @RequestParam String sensorCode,
                            @RequestParam(required = false) String attrCode,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            @RequestParam(required = false) Double minValue,
                            @RequestParam(required = false) Double maxValue,
                            @RequestParam(defaultValue = "5000") int limit,
                            @RequestParam(defaultValue = "0") int offset) {
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        List<String> attrCodes = StringUtils.isBlank(attrCode) ? List.of() : List.of(attrCode);
        return AjaxResult.success("成功",
                iotdbTimeSeriesService.queryRangeBySensor(deviceId, sensorCode, attrCodes,
                        startMillis, endMillis, minValue, maxValue, limit, offset));
    }

    /** 3. 多表达式聚合(POST 因 body 复杂) */
    @PostMapping("/aggregate")
    public AjaxResult aggregate(@RequestParam Long deviceId,
                                @RequestParam String sensorCode,
                                @RequestParam String startTime,
                                @RequestParam String endTime,
                                @RequestParam(required = false, defaultValue = "raw") String granularity,
                                @RequestParam(required = false) Double minValue,
                                @RequestParam(required = false) Double maxValue,
                                @RequestBody List<ExpressionSpec> expressions) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime),
                TimeWindowSpec.WindowGranularity.valueOf(granularity.toUpperCase()));
        SensorAggregationVO vo = aggregationService.aggregateAllAttrs(
                deviceId, sensorCode, window, expressions, minValue, maxValue);
        return AjaxResult.success("成功", vo);
    }

    /** 4. 完整度 */
    @GetMapping("/completeness")
    public AjaxResult completeness(@RequestParam Long deviceId,
                                   @RequestParam String sensorCode,
                                   @RequestParam String attrCode,
                                   @RequestParam String startTime,
                                   @RequestParam String endTime,
                                   @RequestParam(required = false) Long expectedIntervalMs) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime), TimeWindowSpec.WindowGranularity.RAW);
        return AjaxResult.success("成功",
                analysisService.completeness(deviceId, sensorCode, attrCode, window, expectedIntervalMs));
    }

    /** 5. 趋势(端点斜率近似) */
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam Long deviceId,
                            @RequestParam String sensorCode,
                            @RequestParam String attrCode,
                            @RequestParam String startTime,
                            @RequestParam String endTime) {
        TimeWindowSpec window = new TimeWindowSpec(
                toMillis(startTime), toMillis(endTime), TimeWindowSpec.WindowGranularity.RAW);
        return AjaxResult.success("成功",
                analysisService.trend(deviceId, sensorCode, attrCode, window));
    }

    private Long toMillis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return DateUtils.parseDate(text).getTime();
    }
}
