package com.zwei.iot.timeseries.domain;

import lombok.Builder;

/**
 * IoTDB 查询结果行。
 *
 * <p>封装 IoTDB 时间序列查询的单行结果，包含时间戳、指标值与质量码。
 * 供 {@link MonitorDataQueryService} 将时序数据组装为接口返回结构使用。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code time} — 数据时间，毫秒时间戳</li>
 *   <li>{@code value} — 指标数值</li>
 *   <li>{@code quality} — 质量码，0=正常，非0=异常</li>
 * </ul>
 */
@Builder
public record IotdbQueryRow(long time, Double value, Integer quality) {
}
