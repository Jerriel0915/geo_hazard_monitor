package com.zwei.iot.timeseries.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 传感器在某时刻的数据快照。
 *
 * <p>由 {@code SensorDataQueryUtil.query} 查询产生，表示 {@code time <= 查询时刻} 的最近一条数据。
 * 多属性查询时各属性共享同一时间戳（对应传感器一次上报的整行）。</p>
 */
@Getter
@AllArgsConstructor
public class SensorSnapshot {
    /** 数据时间（{@code <=} 查询时刻的最近一条），毫秒时间戳 */
    private final long time;
    /** attrCode → value；查全部属性时排除 quality 列，只含非 null 值 */
    private final Map<String, Double> values;
}
