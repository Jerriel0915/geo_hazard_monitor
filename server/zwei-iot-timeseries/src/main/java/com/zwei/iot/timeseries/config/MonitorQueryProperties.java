package com.zwei.iot.timeseries.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 监测数据查询性能配置。
 *
 * <p>绑定前缀为 {@code iot.monitor.query}，控制图表自动降采样、raw 查询行数上限等性能参数。
 * 解决"历史数据量过高时图表查询消耗性能"的核心问题。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>{@code maxChartPoints} — 图表最大返回点数阈值，默认 2000。区间估算点数超过此值时自动降采样</li>
 *   <li>{@code downsampleEstimateHz} — 点数估算频率（点/秒），默认 1.0（保守值，即 1Hz）</li>
 *   <li>{@code rawLimitCap} — raw 查询路径的硬 LIMIT 上限，默认 4000，防止 OOM</li>
 *   <li>{@code downsampleFunc} — 降采样聚合函数，默认 AVG（保趋势，地质累计指标适用）</li>
 *   <li>{@code maxMergeRows} — 多测点分页合并行数上限，默认 5000，防止深翻页内存放大</li>
 * </ul>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iot.monitor.query")
public class MonitorQueryProperties {
    private int maxChartPoints = 2000;
    private double downsampleEstimateHz = 1.0;
    private int rawLimitCap = 4000;
    private String downsampleFunc = "AVG";
    private int maxMergeRows = 5000;

    /**
     * 根据时间范围和目标点数计算合适的降采样间隔（对齐到 IoTDB 可识别的粒度）。
     *
     * <p>对齐表保证视觉均匀且 IoTDB 可识别：</p>
     * <ul>
     *   <li>桶宽 &lt; 60s → {@code 1s}</li>
     *   <li>60s-600s → {@code 1m} 或 {@code 5m}</li>
     *   <li>600s-1h → {@code 10m} 或 {@code 30m}</li>
     *   <li>1h-1d → {@code 1h}</li>
     *   <li>1d-7d → {@code 6h}</li>
     *   <li>&gt; 7d → {@code 1d}</li>
     * </ul>
     *
     * @param rangeMs     时间范围毫秒数
     * @param targetPoints 目标点数
     * @return IoTDB GROUP BY 间隔字符串（如 "1m"、"6h"、"1d"）
     */
    public String computeDownsampleInterval(long rangeMs, int targetPoints) {
        if (targetPoints <= 0) {
            targetPoints = maxChartPoints;
        }
        long bucketMs = rangeMs / targetPoints;

        if (bucketMs <= 1_000L) return "1s";
        if (bucketMs <= 60_000L) return "1m";
        if (bucketMs <= 300_000L) return "5m";
        if (bucketMs <= 600_000L) return "10m";
        if (bucketMs <= 1_800_000L) return "30m";
        if (bucketMs <= 3_600_000L) return "1h";
        if (bucketMs <= 21_600_000L) return "6h";
        return "1d";
    }
}
