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
