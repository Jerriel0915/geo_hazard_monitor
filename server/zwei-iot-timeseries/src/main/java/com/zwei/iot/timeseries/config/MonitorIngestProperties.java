package com.zwei.iot.timeseries.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 监测数据接入缓冲配置。
 *
 * <p>绑定前缀为 {@code iot.monitor-ingest} 的配置项，用于解耦 MQTT 实时接入与
 * IoTDB 异步写入，提供 Redis Stream 缓冲、消费组订阅与重试策略配置。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>{@code enabled} — 是否启用接入缓冲，默认为 true</li>
 *   <li>{@code streamKey} — Redis Stream 主消费流键名，默认 stream:monitor:ingest</li>
 *   <li>{@code deadLetterStreamKey} — 死信流键名，默认 stream:monitor:dlq</li>
 *   <li>{@code consumerGroup} — 消费组名称，默认 monitor-ingest-group</li>
 *   <li>{@code consumerName} — 消费者节点名称，默认 admin-node</li>
 *   <li>{@code dedupeKeyPrefix} — 去重 Key 前缀，默认 monitor:dedupe:</li>
 *   <li>{@code dedupeTtlSeconds} — 去重 Key 过期秒数，默认 86400（24小时）</li>
 *   <li>{@code pollBatchSize} — 每次轮询拉取条数，默认 20</li>
 *   <li>{@code pollBlockMs} — 轮询阻塞毫秒数，默认 2000</li>
 *   <li>{@code retryDelaysSeconds} — 重试延迟秒数列表，默认 [5, 30, 120]</li>
 * </ul>
 *
 * <p>新增 Redis Stream 接入缓冲参数，用于解耦 MQTT 实时接入与 IoTDB 异步写入。</p>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iot.monitor-ingest")
public class MonitorIngestProperties {
    private boolean enabled = true;
    private String streamKey = "stream:monitor:ingest";
    private String deadLetterStreamKey = "stream:monitor:dlq";
    private String consumerGroup = "monitor-ingest-group";
    private String consumerName = "admin-node";
    private String dedupeKeyPrefix = "monitor:dedupe:";
    private long dedupeTtlSeconds = 86400;
    private int pollBatchSize = 20;
    private long pollBlockMs = 2000;
    private List<Long> retryDelaysSeconds = new ArrayList<>(List.of(5L, 30L, 120L));
    /** PEL 回收最小空闲时间（毫秒），默认 5 分钟。超过此时间未被确认的消息将被重新入队。 */
    private long pelRecoverIdleMs = 300_000L;
    /** Stream 最大长度（近似值），超出时自动裁剪旧消息。0 表示不裁剪。 */
    private long maxStreamLen = 100_000L;
}
