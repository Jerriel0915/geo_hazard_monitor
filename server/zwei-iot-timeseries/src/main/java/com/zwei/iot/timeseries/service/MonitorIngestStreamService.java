package com.zwei.iot.timeseries.service;

import com.alibaba.fastjson2.JSON;
import com.zwei.iot.timeseries.config.MonitorIngestProperties;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 缓冲写入服务。
 *
 * <p>作为 MQTT 解析链路与 IoTDB 异步消费链路之间的缓冲层：
 * <ul>
 *   <li>{@link #enqueue} — 将标准化时序点 JSON 序列化后写入主消费流（stream:monitor:ingest），
 *       附带 retryCount=0 初始标记</li>
 *   <li>{@link #enqueueDeadLetter} — 写入失败超过重试上限的测点进入死信流，
 *       附带失败原因供人工排查</li>
 * </ul>
 *
 * <p>主消费流由 {@link MonitorIngestConsumerService} 异步轮询消费。
 */
@Service
public class MonitorIngestStreamService {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final MonitorIngestProperties properties;

    /**
     * 构造 Stream 写入服务。
     *
     * @param redisTemplate Redis 模板
     * @param properties    接入缓冲配置
     */
    @Autowired
    public MonitorIngestStreamService(RedisTemplate<Object, Object> redisTemplate,
                                      MonitorIngestProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 将标准化时序点批量写入主消费流。
     *
     * @param points 标准化时序点集合
     */
    public void enqueue(List<StandardMeasurementPoint> points) {
        for (StandardMeasurementPoint point : points) {
            Map<String, String> body = new HashMap<>();
            body.put("payload", JSON.toJSONString(point));
            body.put("retryCount", "0");
            redisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), body));
        }
    }

    /**
     * 将处理失败的时序点写入死信流。
     *
     * @param point  标准化时序点
     * @param reason 失败原因
     */
    public void enqueueDeadLetter(StandardMeasurementPoint point, String reason) {
        Map<String, String> body = new HashMap<>();
        body.put("payload", JSON.toJSONString(point));
        body.put("reason", reason);
        redisTemplate.opsForStream().add(MapRecord.create(properties.getDeadLetterStreamKey(), body));
    }
}
